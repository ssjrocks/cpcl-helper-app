# Architecture

CPCL Helper is intentionally small. Its job is to bridge one gap:

```text
Microsoft Power Apps
        |
        | Launch("freightprint://print?code=APLP10054543001&copies=1")
        v
Android deep link intent
        |
        v
CPCL Helper
        |
        | Builds CPCL text
        v
Android Bluetooth Classic socket
        |
        v
Paired CPCL label printer
```

## Why a Helper App Exists

Power Apps can create buttons, forms, QR payload values, and business workflow logic. The hard part is local hardware access. On Android, Power Apps runs inside the Power Apps mobile container and does not expose a simple supported way to open a raw Bluetooth socket and send CPCL/ZPL printer commands.

The helper app keeps that hardware-specific piece outside Power Apps:

- Power Apps owns the freight workflow.
- CPCL Helper owns the local Bluetooth printer transport.
- The handoff between them is a plain Android deep link.

## Android Components

The bridge app is in the `app` module.

Important pieces:

- `AndroidManifest.xml` registers `freightprint://print` as a browsable deep link.
- `MainActivity` lists already paired Bluetooth devices.
- The selected printer name and MAC address are saved in Android shared preferences.
- `handlePrintIntent()` reads `code`, `label`, or `text` from the incoming deep link.
- `buildCpcl()` creates the CPCL label.
- `sendCpcl()` opens a Bluetooth Classic SPP socket and writes the CPCL as ASCII.

The simulator app is in the `powerapps-simulator` module. It does not print by itself. It only builds and launches the same deep link that Power Apps should launch.

## Bluetooth Transport

The helper uses Bluetooth Classic Serial Port Profile with this UUID:

```text
00001101-0000-1000-8000-00805F9B34FB
```

That is the common SPP UUID used by many Bluetooth thermal and label printers. The app first tries a standard RFCOMM socket, then falls back to an insecure RFCOMM socket because different printer firmware handles pairing/authentication differently.

The app does not scan for new devices. Operators pair the printer in Android settings first, then choose it inside CPCL Helper. This avoids extra Android Bluetooth scan/location permissions and keeps the app easier to support.

## Current CPCL Template

```text
! 0 200 200 320 {copies}
PW 576
CENTER
BARCODE QR 200 30 M 2 U 11
MA,{code}
ENDQR
TEXT 4 0 24 270 {code}
FORM
PRINT
```

The printer generates the QR code itself. The app does not render QR images.

## Data Handling

The app receives the label code in the deep link query string. It sanitizes the value before putting it into CPCL:

- keeps printable ASCII characters
- converts tabs/newlines to spaces
- trims whitespace
- limits the value length

The helper stores only:

- saved printer name
- saved printer MAC address

It does not store printed label history or freight data.

## Limitations

- Bluetooth Classic SPP printers are supported.
- BLE-only printers are not supported by the current transport.
- CPCL is assumed. ZPL, ESC/POS, TSPL, or PDF/image printing would need separate templates/transports.
- Debug APKs are for testing only. Production deployment should use a release-signed APK managed by IT.
