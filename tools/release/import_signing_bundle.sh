#!/bin/sh
set -eu

SCRIPT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "$SCRIPT_DIRECTORY/../.." && pwd)
KEYSTORE_PATH="$REPOSITORY_ROOT/local-signing/places-release.p12"
PROPERTIES_PATH="$REPOSITORY_ROOT/release-signing.properties"
EXPECTED_STORE_FILE="local-signing/places-release.p12"
EXPECTED_ALIAS="places-release"
EXPECTED_SHA256="A7:80:0D:55:E3:C0:C4:EA:56:CD:82:2E:E3:DB:C6:0B:BD:C2:BC:01:C6:79:D5:6C:34:AC:D6:D6:94:22:91:F4"

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 /path/to/places-signing-backup.tar.gz.enc" >&2
    exit 2
fi

BUNDLE_PATH=$1
if [ ! -f "$BUNDLE_PATH" ]; then
    echo "Signing recovery bundle was not found: $BUNDLE_PATH" >&2
    exit 1
fi
if [ -e "$KEYSTORE_PATH" ] || [ -e "$PROPERTIES_PATH" ]; then
    echo "Signing material already exists; refusing to replace it." >&2
    exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl was not found." >&2
    exit 1
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL="$JAVA_HOME/bin/keytool"
else
    KEYTOOL=$(command -v keytool || true)
fi
if [ -z "$KEYTOOL" ]; then
    echo "keytool was not found. Set JAVA_HOME to a JDK installation." >&2
    exit 1
fi

umask 077
TEMPORARY_DIRECTORY=$(mktemp -d "${TMPDIR:-/tmp}/places-signing-restore.XXXXXX")
DECRYPTED_ARCHIVE="$TEMPORARY_DIRECTORY/signing-bundle.tar.gz"
KEYSTORE_INSTALLED=false
PROPERTIES_INSTALLED=false
cleanup() {
    status=$?
    unset STORE_PASSWORD KEY_PASSWORD PLACES_SIGNING_BUNDLE_PASSWORD
    if [ "$status" -ne 0 ]; then
        [ "$KEYSTORE_INSTALLED" = false ] || rm -f -- "$KEYSTORE_PATH"
        [ "$PROPERTIES_INSTALLED" = false ] || rm -f -- "$PROPERTIES_PATH"
    fi
    rm -rf -- "$TEMPORARY_DIRECTORY"
    exit "$status"
}
trap cleanup EXIT HUP INT TERM

if [ -n "${PLACES_SIGNING_BUNDLE_PASSWORD:-}" ]; then
    openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 \
        -pass env:PLACES_SIGNING_BUNDLE_PASSWORD \
        -in "$BUNDLE_PATH" -out "$DECRYPTED_ARCHIVE"
else
    openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 \
        -in "$BUNDLE_PATH" -out "$DECRYPTED_ARCHIVE"
fi

ARCHIVE_CONTENTS=$(tar -tzf "$DECRYPTED_ARCHIVE")
EXPECTED_CONTENTS=$(printf '%s\n' \
    "local-signing/places-release.p12" \
    "release-signing.properties")
if [ "$ARCHIVE_CONTENTS" != "$EXPECTED_CONTENTS" ]; then
    echo "The decrypted bundle contains unexpected paths." >&2
    exit 1
fi
tar -xzf "$DECRYPTED_ARCHIVE" -C "$TEMPORARY_DIRECTORY"
rm -f -- "$DECRYPTED_ARCHIVE"

RESTORED_KEYSTORE="$TEMPORARY_DIRECTORY/local-signing/places-release.p12"
RESTORED_PROPERTIES="$TEMPORARY_DIRECTORY/release-signing.properties"
if [ ! -f "$RESTORED_KEYSTORE" ] || [ ! -f "$RESTORED_PROPERTIES" ]; then
    echo "The decrypted bundle does not contain the required signing files." >&2
    exit 1
fi
if find "$TEMPORARY_DIRECTORY" -type l -print -quit | grep . >/dev/null; then
    echo "The decrypted bundle contains an unexpected symbolic link." >&2
    exit 1
fi
FILE_COUNT=$(find "$TEMPORARY_DIRECTORY" -type f | wc -l | tr -d ' ')
if [ "$FILE_COUNT" -ne 2 ]; then
    echo "The decrypted bundle contains unexpected files." >&2
    exit 1
fi

property_value() {
    property_name=$1
    sed -n "s/^${property_name}=//p" "$RESTORED_PROPERTIES"
}

STORE_FILE=$(property_value storeFile)
STORE_PASSWORD=$(property_value storePassword)
KEY_ALIAS=$(property_value keyAlias)
KEY_PASSWORD=$(property_value keyPassword)
if [ "$STORE_FILE" != "$EXPECTED_STORE_FILE" ] || [ "$KEY_ALIAS" != "$EXPECTED_ALIAS" ]; then
    echo "The bundle does not identify the approved Places release key." >&2
    exit 1
fi
if [ -z "$STORE_PASSWORD" ] || [ -z "$KEY_PASSWORD" ]; then
    echo "The restored signing properties are incomplete." >&2
    exit 1
fi

export STORE_PASSWORD KEY_PASSWORD
KEY_DETAILS=$(
    "$KEYTOOL" -list -v \
        -keystore "$RESTORED_KEYSTORE" \
        -storetype PKCS12 \
        -storepass:env STORE_PASSWORD \
        -alias "$KEY_ALIAS"
)
if ! printf '%s\n' "$KEY_DETAILS" | grep -F "SHA256: $EXPECTED_SHA256" >/dev/null; then
    echo "The restored keystore fingerprint does not match the approved Places signer." >&2
    exit 1
fi

mkdir -p "$REPOSITORY_ROOT/local-signing"
mv "$RESTORED_KEYSTORE" "$KEYSTORE_PATH"
KEYSTORE_INSTALLED=true
mv "$RESTORED_PROPERTIES" "$PROPERTIES_PATH"
PROPERTIES_INSTALLED=true
chmod 600 "$KEYSTORE_PATH" "$PROPERTIES_PATH"

unset STORE_PASSWORD KEY_PASSWORD PLACES_SIGNING_BUNDLE_PASSWORD
trap - EXIT HUP INT TERM
rm -rf -- "$TEMPORARY_DIRECTORY"
echo "Restored and verified the approved Places release signer."
echo "Now create local.properties for this computer's Android SDK and Maps API key."
