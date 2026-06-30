# CPCL Helper

Small Android bridge app for printing CPCL QR labels from Microsoft Power Apps.

The app is a local print bridge for Android tablets. Power Apps opens a custom deep link, CPCL Helper receives the label value, builds a CPCL command, and sends it directly to a paired Bluetooth label printer.

This avoids trying to make Power Apps talk directly to Android Bluetooth, which is the part that is normally awkward or unsupported.

## Project Structure

```text
app/                     Android bridge app installed on the tablet
powerapps-simulator/     Test app that mimics the Power Apps Launch() call
docs/                    Architecture, integration, and test notes
build-debug-apk.ps1      Local debug APK build script
```

The intended workflow is:

1. Pair the Android tablet with the Bluetooth CPCL label printer in Android settings.
2. Open CPCL Helper and select the paired printer.
3. Use **Test Print** to confirm the printer accepts CPCL over Bluetooth Classic SPP.
4. From Power Apps, open a deep link such as:

```powerfx
Launch("freightprint://print?code=" & EncodeUrl(ThisItem.LoadId))
```

Optional copies:

```powerfx
Launch("freightprint://print?code=" & EncodeUrl(ThisItem.LoadId) & "&copies=2")
```

The app receives the link, builds a CPCL label, and sends the raw command text to the saved Bluetooth printer.

More detail is in:

- [Architecture](docs/architecture.md)
- [Power Apps Integration](docs/power-apps-integration.md)
- [Testing Guide](docs/testing.md)

## Android Studio

Open this folder in Android Studio, let Gradle sync, then run the `app` configuration on a tablet.

This project intentionally uses plain Android Java with no external app libraries. That keeps the bridge simple for integration review and avoids adding a framework just to send text to a Bluetooth socket.

## Build The Test APKs

This workspace has portable build tools installed under ignored local folders:

- `.tools`
- `.android-sdk`
- `.gradle-home`

To rebuild the debug APK from PowerShell:

```powershell
.\build-debug-apk.ps1
```

The debug APKs are created at:

```text
app\build\outputs\apk\debug\app-debug.apk
powerapps-simulator\build\outputs\apk\debug\powerapps-simulator-debug.apk
```

These APKs are signed with Android's debug signing key. They are suitable for testing on your tablet, but not for managed production deployment.

## Prebuilt Debug APKs

The repository also includes the latest debug APKs under:

```text
artifacts\debug\cpcl-helper-debug.apk
artifacts\debug\powerapps-simulator-debug.apk
artifacts\debug\SHA256SUMS.txt
```

Use these for quick tablet testing. Rebuild them with `.\build-debug-apk.ps1` after code changes, then copy the new APKs into `artifacts\debug` before committing.

## Power Apps Simulator

The `powerapps-simulator` module is a separate Android app that mimics the Power Apps handoff. Install both APKs on the same tablet:

1. `app-debug.apk`
2. `powerapps-simulator-debug.apk`

Open **Power Apps Simulator**, enter a freight/load code and copies, then tap **Launch CPCL Helper**. It will open this deep link:

```text
freightprint://print?code=APLP10056543001&copies=1
```

That is the same handoff shape the real Power Apps button should use.

## Notes for Integration

- The app uses a custom URL scheme: `freightprint://print`.
- It sends CPCL over Bluetooth Classic Serial Port Profile using UUID `00001101-0000-1000-8000-00805F9B34FB`.
- It lists already-paired devices only. Pairing is still handled by Android settings.
- Android 12 and newer require the `BLUETOOTH_CONNECT` runtime permission.
- Many CPCL label printers use Bluetooth Classic SPP, but some newer printers use BLE instead. BLE-only printers will need a different transport implementation.
- The helper does not call a cloud service, store freight records, or require user login. It only stores the saved Bluetooth printer name/address in local Android shared preferences.

## Default CPCL

```text
! 0 200 200 320 1
PW 576
CENTER
BARCODE QR 200 30 M 2 U 11
MA,APLP10056543001
ENDQR
TEXT 4 0 24 270 APLP10056543001
FORM
PRINT
```
