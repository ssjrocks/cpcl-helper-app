# Power Apps Integration

This guide explains how a Microsoft Power Apps canvas app can call CPCL Helper on an Android tablet.

## Required Tablet Setup

1. Install `app-debug.apk` or a release-signed CPCL Helper APK.
2. Pair the Bluetooth label printer in Android Bluetooth settings.
3. Open **CPCL Helper**.
4. Grant Bluetooth permission if prompted.
5. Select the paired printer.
6. Tap **Save printer**.
7. Tap **Test Print** to confirm the printer works before testing Power Apps.

After this setup, Power Apps only needs to launch a URL.

When Power Apps launches the URL, CPCL Helper uses a hidden receiver activity and short-lived print service. Operators should be returned to Power Apps immediately instead of being left on the CPCL Helper setup screen.

## Basic Power Apps Button

For a button inside a gallery or form where `ThisItem.LoadId` is the label value:

```powerfx
Launch("freightprint://print?code=" & EncodeUrl(ThisItem.LoadId))
```

For a value from a text input:

```powerfx
Launch("freightprint://print?code=" & EncodeUrl(txtLoadId.Text))
```

## Copies

CPCL Helper accepts an optional `copies` query parameter:

```powerfx
Launch(
    "freightprint://print?code=" &
    EncodeUrl(ThisItem.LoadId) &
    "&copies=2"
)
```

If `copies` is missing, invalid, or less than 1, the helper prints one copy. The current app caps copies at 20 to avoid accidental runaway print jobs.

## Accepted Query Parameters

CPCL Helper reads the label value from the first non-empty parameter in this order:

```text
code
label
text
```

These are equivalent:

```text
freightprint://print?code=APLP10054543001
freightprint://print?label=APLP10054543001
freightprint://print?text=APLP10054543001
```

Using `code` is recommended.

## Example Button Formula

This is a more defensive formula for a Power Apps button:

```powerfx
If(
    IsBlank(ThisItem.LoadId),
    Notify("No load ID to print", NotificationType.Error),
    Launch(
        "freightprint://print?code=" &
        EncodeUrl(ThisItem.LoadId) &
        "&copies=1"
    )
)
```

## Testing Without Power Apps

Install the simulator APK:

```text
powerapps-simulator\build\outputs\apk\debug\powerapps-simulator-debug.apk
```

Open **Power Apps Simulator**, enter a label code and copies, then tap **Launch CPCL Helper**.

This proves Android deep linking works before changing the real Power Apps app.

For future custom label layouts, see [CPCL Label Design](cpcl-label-design.md). The current app supports the default `code` and `copies` workflow only, but the docs include suggested `template`, `route`, `dock`, and other optional parameters for later extension.

## What Integration May Need To Configure

For managed work tablets, the deployment or integration administrator may need to:

- allow installation of the helper APK
- deploy the APK through MDM
- approve Bluetooth permission
- pair or pre-pair printers
- decide whether the APK should be release-signed with a company signing key
- decide whether the custom URL scheme `freightprint://print` is acceptable

## Deployment Recommendation

For early testing, debug APK side-loading is fine.

For workplace rollout:

1. Create a release build.
2. Sign it with an administrator-controlled signing key.
3. Deploy through MDM or the organization's normal Android app deployment process.
4. Keep the URL contract stable:

```text
freightprint://print?code={labelValue}&copies={number}
```

## Troubleshooting

If Power Apps opens a browser or says the link cannot be opened, CPCL Helper is not installed or Android has not associated the `freightprint` scheme with the app.

If CPCL Helper opens but does not print, open the helper directly and run **Test Print**.

If **Test Print** fails, the issue is below Power Apps: Bluetooth permission, saved printer, pairing, printer firmware, printer language, or Bluetooth Classic compatibility.

If Android briefly switches away from Power Apps, that is expected. Android deep links are activity-based. The helper minimizes the interruption by closing the receiver activity immediately after starting the print job.
