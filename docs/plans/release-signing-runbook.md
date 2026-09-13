# Places replacement release-signing runbook

**Status:** Permanent local signer created and verified; encrypted export/restore tooling is available, but an independently stored recovery bundle still requires the owner's chosen destination and password-manager entry.

The replacement package is `com.personal.favoriteplaces`. Because household APKs are shared directly rather than through Google Play, the same self-managed release key must sign every future replacement update. Losing this key would repeat the legacy-app problem: Android would refuse future APK updates and another side-by-side migration would be required.

## Repository wiring

- `app/build.gradle.kts` reads release credentials only from ignored `release-signing.properties`.
- `release-signing.properties.example` documents the four required values.
- Keystores and the completed properties file are ignored by Git.
- Debug builds continue to use the Mac's disposable debug certificate.
- With the completed ignored properties present, `assembleRelease` produces the signed replacement. Without them it remains an unsigned technical build and must not be distributed.

## One-time key creation

Use JDK 17's `keytool` and a strong, unique password. The user approved local storage under ignored `local-signing/`; the alias is `places-release`. The generated password remains only in ignored `release-signing.properties` and is never printed. A long validity period is appropriate because direct APK updates must retain the same certificate for the app's lifetime.

```sh
tools/release/create_signing_key.sh

# Equivalent keytool shape used by the script:
keytool -genkeypair \
  -keystore local-signing/places-release.p12 \
  -alias places-release \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

The script creates ignored `release-signing.properties`, sets owner-only permissions, refuses replacement, and never prints the generated password. Never paste passwords or the private key into this plan, Git, an issue, or a chat transcript.

## Recovery requirement

Before the first household release, make two independently readable recovery copies:

1. Working copy inside ignored `local-signing/` on the Mac, as approved by the user.
2. Off-device encrypted copy in a user-chosen password manager or encrypted cloud/archive location that will survive another Mac reset.

The recovery set must contain both `places-release.p12` and its alias/password information from `release-signing.properties`. Merely backing up one without the other is insufficient. Verify the copied keystore with `keytool -list` before calling the recovery copy complete.

Create a portable encrypted bundle without exposing either signing password on the command line:

```sh
tools/release/export_signing_bundle.sh \
  "/path/to/private-storage/places-signing-backup.tar.gz.enc"
```

The script verifies the local signer against the pinned production certificate, refuses to overwrite an existing destination, packages only the keystore and signing properties, and encrypts the stream with AES-256-CBC plus PBKDF2 before it is written. Store the archive password separately in a password manager; losing either the archive or that password makes the recovery copy unusable. The Maps API key is intentionally excluded.

On another computer, clone the repository, install a JDK and Android SDK, then restore into a checkout that does not already contain signing material:

```sh
tools/release/import_signing_bundle.sh \
  "/path/to/private-storage/places-signing-backup.tar.gz.enc"
```

The restore script decrypts into a private temporary directory, rejects unexpected files and links, validates the properties and exact production certificate fingerprint, refuses to replace an existing key, and installs both local files with owner-only permissions. Create that computer's ignored `local.properties` separately using `local.properties.example`; its `sdk.dir` is machine-specific and `MAPS_API_KEY` remains a separately managed Google credential.

## Verification

1. Build the signed APK with the pinned JBR 17 toolchain.
2. Use `apksigner verify --print-certs` to record only SHA-1 and SHA-256 certificate fingerprints in the rolling plan.
3. Confirm package `com.personal.favoriteplaces`, version code, and signer with `apkanalyzer`/`apksigner`.
4. Install beside `com.example.favoriteplaces`; never attempt to overwrite or uninstall the old app.
5. Restrict the future production Maps/Places key to `com.personal.favoriteplaces`, the release SHA-1, and only the native Android SDK APIs actually used.
6. On the first household device, import and read back every record before the same signed APK is offered to the second device.

Verified local certificate fingerprints:

- SHA-256: `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`
- SHA-1 for Google Cloud Android restrictions: `39:76:13:D8:92:34:E7:32:41:EB:BC:E6:BC:47:98:23:E4:60:13:13`

Primary reference: [Android — Sign your app](https://developer.android.com/studio/publish/app-signing).
