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

OUTPUT_PATH=$1
if [ -e "$OUTPUT_PATH" ]; then
    echo "Destination already exists; refusing to overwrite it: $OUTPUT_PATH" >&2
    exit 1
fi
if [ ! -f "$KEYSTORE_PATH" ] || [ ! -f "$PROPERTIES_PATH" ]; then
    echo "The local release keystore or signing properties are missing." >&2
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

property_value() {
    property_name=$1
    sed -n "s/^${property_name}=//p" "$PROPERTIES_PATH"
}

STORE_FILE=$(property_value storeFile)
STORE_PASSWORD=$(property_value storePassword)
KEY_ALIAS=$(property_value keyAlias)
KEY_PASSWORD=$(property_value keyPassword)
if [ "$STORE_FILE" != "$EXPECTED_STORE_FILE" ] || [ "$KEY_ALIAS" != "$EXPECTED_ALIAS" ]; then
    echo "Signing properties do not identify the approved Places release key." >&2
    exit 1
fi
if [ -z "$STORE_PASSWORD" ] || [ -z "$KEY_PASSWORD" ]; then
    echo "Signing properties are incomplete." >&2
    exit 1
fi

export STORE_PASSWORD KEY_PASSWORD
KEY_DETAILS=$(
    "$KEYTOOL" -list -v \
        -keystore "$KEYSTORE_PATH" \
        -storetype PKCS12 \
        -storepass:env STORE_PASSWORD \
        -alias "$KEY_ALIAS"
)
if ! printf '%s\n' "$KEY_DETAILS" | grep -F "SHA256: $EXPECTED_SHA256" >/dev/null; then
    echo "The keystore fingerprint does not match the approved Places release signer." >&2
    exit 1
fi

umask 077
OUTPUT_DIRECTORY=$(dirname -- "$OUTPUT_PATH")
if [ ! -d "$OUTPUT_DIRECTORY" ]; then
    echo "Destination directory does not exist: $OUTPUT_DIRECTORY" >&2
    exit 1
fi
OUTPUT_DIRECTORY=$(CDPATH= cd -- "$OUTPUT_DIRECTORY" && pwd -P)
OUTPUT_PATH="$OUTPUT_DIRECTORY/$(basename -- "$OUTPUT_PATH")"
case "$OUTPUT_PATH" in
    "$REPOSITORY_ROOT"/*)
        echo "Signing recovery bundles must be stored outside the Git repository." >&2
        exit 1
        ;;
esac

cleanup() {
    status=$?
    unset STORE_PASSWORD KEY_PASSWORD PLACES_SIGNING_BUNDLE_PASSWORD
    if [ "$status" -ne 0 ]; then
        [ ! -e "$OUTPUT_PATH" ] || rm -f -- "$OUTPUT_PATH"
    fi
    exit "$status"
}
trap cleanup EXIT HUP INT TERM

if [ -n "${PLACES_SIGNING_BUNDLE_PASSWORD:-}" ]; then
    tar -czf - -C "$REPOSITORY_ROOT" \
        local-signing/places-release.p12 \
        release-signing.properties |
        openssl enc -aes-256-cbc -salt -pbkdf2 -iter 600000 \
            -pass env:PLACES_SIGNING_BUNDLE_PASSWORD -out "$OUTPUT_PATH"
else
    echo "Choose an encryption password and store it separately in your password manager."
    tar -czf - -C "$REPOSITORY_ROOT" \
        local-signing/places-release.p12 \
        release-signing.properties |
        openssl enc -aes-256-cbc -salt -pbkdf2 -iter 600000 -out "$OUTPUT_PATH"
fi

chmod 600 "$OUTPUT_PATH"
unset STORE_PASSWORD KEY_PASSWORD PLACES_SIGNING_BUNDLE_PASSWORD
trap - EXIT HUP INT TERM
echo "Created encrypted signing recovery bundle: $OUTPUT_PATH"
echo "The Maps API key is not included; configure it separately on each computer."
