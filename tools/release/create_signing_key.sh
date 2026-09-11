#!/bin/sh
set -eu

SCRIPT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "$SCRIPT_DIRECTORY/../.." && pwd)
SIGNING_DIRECTORY="$REPOSITORY_ROOT/local-signing"
KEYSTORE_PATH="$SIGNING_DIRECTORY/places-release.p12"
PROPERTIES_PATH="$REPOSITORY_ROOT/release-signing.properties"
KEY_ALIAS="places-release"

if [ -e "$KEYSTORE_PATH" ] || [ -e "$PROPERTIES_PATH" ]; then
    echo "Signing material already exists; refusing to replace it." >&2
    exit 1
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL="$JAVA_HOME/bin/keytool"
else
    KEYTOOL=$(command -v keytool || true)
fi
if [ -z "$KEYTOOL" ]; then
    echo "keytool was not found. Set JAVA_HOME to the JDK 17 installation." >&2
    exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl was not found." >&2
    exit 1
fi

umask 077
mkdir -p "$SIGNING_DIRECTORY"
SIGNING_PASSWORD=$(openssl rand -hex 32)
export SIGNING_PASSWORD

cleanup() {
    status=$?
    unset SIGNING_PASSWORD
    if [ "$status" -ne 0 ]; then
        [ ! -e "$KEYSTORE_PATH" ] || rm "$KEYSTORE_PATH"
        [ ! -e "$PROPERTIES_PATH" ] || rm "$PROPERTIES_PATH"
    fi
    exit "$status"
}
trap cleanup EXIT HUP INT TERM

"$KEYTOOL" -genkeypair \
    -keystore "$KEYSTORE_PATH" \
    -storetype PKCS12 \
    -storepass:env SIGNING_PASSWORD \
    -keypass:env SIGNING_PASSWORD \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -dname "CN=Places App Release, O=Personal" \
    -noprompt

printf '%s\n' \
    "storeFile=local-signing/places-release.p12" \
    "storePassword=$SIGNING_PASSWORD" \
    "keyAlias=$KEY_ALIAS" \
    "keyPassword=$SIGNING_PASSWORD" \
    > "$PROPERTIES_PATH"

chmod 600 "$KEYSTORE_PATH" "$PROPERTIES_PATH"
unset SIGNING_PASSWORD
trap - EXIT HUP INT TERM

echo "Created ignored local release-signing material. The password was not printed."
