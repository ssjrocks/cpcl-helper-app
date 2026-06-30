# Security And Signing

This document explains the security posture of CPCL Helper, the current debug signing setup, and recommended production signing/deployment practices.

## App Security Posture

CPCL Helper is a narrow local Bluetooth print bridge.

It does not request:

- internet access
- camera access
- contacts access
- file/storage access
- location access
- account access
- microphone access

The meaningful runtime permission on modern Android is:

- `BLUETOOTH_CONNECT`

Older Android versions may also see legacy Bluetooth permissions declared for compatibility:

- `BLUETOOTH`
- `BLUETOOTH_ADMIN`

The app stores only:

- saved printer name
- saved printer Bluetooth MAC address

It does not store freight records, print history, login credentials, or scanned data.

## Intended Workplace Use

In the intended workplace workflow, labels are usually printed by the customer before freight arrives or is processed.

CPCL Helper is not intended to create new freight identities or authorize new freight movements. It is intended to reprint an existing label from authorized workplace data when the original label is missing, damaged, unreadable, or otherwise unsuitable for scanning.

Because the label value should come from an approved Power Apps record, the main operational risk is not arbitrary label creation. The more realistic risk is operator error during pallet identification.

Examples:

- an operator selects the wrong pallet record in Power Apps
- an operator applies a reprinted label to the wrong pallet
- an operator scans or handles the wrong pallet after reprinting
- an operator bypasses the normal SOP for confirming pallet identity
- duplicate labels create confusion if the original label is still present elsewhere

The primary impact would be misidentified freight, not data exfiltration or device compromise.

## Risk Mitigations

Recommended mitigations sit mostly in the Power Apps workflow and workplace SOPs:

- log every print and reprint request in Power Apps
- record the user, timestamp, pallet/load ID, and reason for reprint
- require operators to select from authorized freight records only
- prevent free-form label values where possible
- show confirmation details before reprinting, such as customer, route, load, dock, or pallet details
- train operators to verify pallet identity before applying a replacement label
- review print logs when freight is misidentified
- manage repeated SOP failures through normal performance management processes

This makes the print bridge part of an auditable workflow rather than an uncontrolled label generator.

## Residual Risk

Even with logging, a user with access to the Power Apps workflow may still print and apply the wrong label.

That risk is best treated as an operational process risk:

```text
authorized data + incorrect operator action = mislabeled pallet
```

The control should therefore be:

```text
Power Apps audit trail + clear SOP + supervisor review/performance management
```

The helper app itself has a small technical security footprint because it has no network permission, no storage access, no camera access, and only prints to a paired Bluetooth printer.

## Deep Link Surface

The helper listens for:

```text
freightprint://print
```

Accepted query parameters:

```text
code
label
text
copies
```

Any app or browser on the tablet that can launch a deep link may be able to trigger the helper.

Current mitigations:

- the app can only print to the saved paired Bluetooth printer
- the app has no network access
- copies are capped
- label data is sanitized before being inserted into CPCL
- the intended data source is Power Apps, not free-form manual entry

Possible future hardening:

- require a shared token query parameter
- allowlist expected code formats
- show a confirmation for unknown templates
- keep a local print log
- restrict template names
- deploy only through managed tablets with restricted app installs

## Debug Signing

Current test APKs are built with Gradle's debug signing configuration.

The project does not currently define a release signing key, release keystore, or production signing config.

Debug APKs are suitable for:

- local testing
- proof of concept
- printer compatibility testing
- Power Apps handoff testing

Debug APKs are not ideal for production rollout because:

- debug keys are development credentials
- signing identity may differ between build machines
- updates can fail if future APKs are signed with a different key
- managed deployment tools generally expect stable release signing
- debug builds are easier to inspect and attach tooling to
- debug signing does not represent an administrator-controlled release process

## Where The Debug Key Comes From

When running:

```powershell
.\build-debug-apk.ps1
```

Gradle runs:

```text
assembleDebug
```

The Android Gradle Plugin automatically signs the APK with a debug keystore.

On a typical Windows Android setup, the debug keystore is commonly located at:

```text
C:\Users\<user>\.android\debug.keystore
```

This project also uses local portable build tooling under:

```text
.tools
.android-sdk
.gradle-home
```

The exact debug key location is generated or managed by the Android build tooling. It is not a company-controlled release key.

## Production Signing Recommendation

For production deployment:

1. Create a release keystore controlled by the deployment administrator.
2. Store the keystore securely.
3. Do not commit the keystore or passwords to GitHub.
4. Configure Gradle release signing using local properties, environment variables, or CI/CD secrets.
5. Build a release APK.
6. Deploy the release APK through the normal managed Android app deployment process.

Example files that should remain private:

```text
release-keystore.jks
keystore.properties
```

Example `.gitignore` entries:

```text
*.jks
*.keystore
keystore.properties
```

## Production Deployment Recommendation

For workplace rollout:

- use a release-signed APK
- keep the package name stable
- keep the signing key stable
- increment `versionCode` for each release
- deploy through managed tablet tooling where possible
- restrict unapproved app installs on tablets if inappropriate reprinting is a concern

## Risk Summary

The app has a small permission footprint and no network capability.

The main risk is not data exfiltration. The main risk is operational misuse: reprinting a valid label from authorized data and applying it to the wrong pallet, or otherwise failing to follow the SOP for pallet identification.

For early testing, debug APK side-loading is acceptable.

For production, use release signing, managed deployment, Power Apps logging, and clear operational controls around label reprints.
