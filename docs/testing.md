# Testing Guide

## Build APKs

From PowerShell in the project root:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\build-debug-apk.ps1
```

Outputs:

```text
app\build\outputs\apk\debug\app-debug.apk
powerapps-simulator\build\outputs\apk\debug\powerapps-simulator-debug.apk
```

## Tablet Test Flow

1. Install `app-debug.apk`.
2. Install `powerapps-simulator-debug.apk`.
3. Pair the printer in Android Bluetooth settings.
4. Open **CPCL Helper**.
5. Select the paired printer.
6. Tap **Save printer**.
7. Tap **Test Print**.
8. Open **Power Apps Simulator**.
9. Enter a code such as `APLP10054543001`.
10. Tap **Launch CPCL Helper**.

The simulator should open CPCL Helper, and CPCL Helper should print the label.

## Expected Label Layout

The current CPCL template is:

```text
! 0 200 200 320 1
PW 576
CENTER
BARCODE QR 200 30 M 2 U 11
MA,APLP10054543001
ENDQR
TEXT 4 0 24 270 APLP10054543001
FORM
PRINT
```

Expected output:

- QR code centered near the top
- human-readable label code near the bottom
- label height around 320 dots
- page width 576 dots

## Common Failures

### No Paired Devices

Pair the printer in Android settings first. The helper does not scan for printers.

### Bluetooth Permission Denied

Grant Bluetooth permission in Android app settings for CPCL Helper. Android 12 and newer require `BLUETOOTH_CONNECT`.

### Could Not Connect To Printer

Check:

- printer is powered on
- printer is paired with the tablet
- another app is not holding the Bluetooth connection
- printer supports Bluetooth Classic SPP
- printer is in CPCL mode

### Prints Blank Or Wrong Layout

The printer may be in a different command language mode or may need adjusted CPCL coordinates. Confirm the printer accepts this CPCL from another known-good app.

### Simulator Opens But Real Power Apps Does Not

Compare the exact URL launched by Power Apps with the simulator preview. Use `EncodeUrl()` around the label value.
