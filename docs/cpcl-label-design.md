# CPCL Label Design

This document explains the CPCL subset used by CPCL Helper and gives examples for designing labels that are closer to existing workplace labels.

CPCL varies a little between printer models and firmware versions. Treat these examples as practical starting points, then confirm the final layout on the actual printer and label stock.

## Current App Behavior

The app currently builds one fixed CPCL template. Power Apps sends only:

```text
code
copies
```

The helper then generates this label:

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

## Coordinate System

CPCL positions are measured in printer dots.

For the current layout:

- width is `576` dots
- height is `320` dots
- `x` grows left to right
- `y` grows top to bottom

So this text:

```text
TEXT 4 0 24 270 APLP10054543001
```

means:

- use font `4`
- rotation `0`
- x position `24`
- y position `270`
- print the label code

## Command Quick Reference

### Header

```text
! 0 200 200 320 1
```

Common shape:

```text
! 0 200 200 {labelHeightDots} {copies}
```

In this app, `{copies}` comes from the deep link:

```text
freightprint://print?code=APLP10054543001&copies=2
```

### Page Width

```text
PW 576
```

Sets the print width in dots. Keep this matched to the printer, media, and DPI.

### Alignment

```text
CENTER
LEFT
RIGHT
```

`CENTER` is used by the current QR layout. If you add left-positioned text after centered elements, reset alignment with `LEFT` before the text.

### Text

```text
TEXT 4 0 24 270 APLP10054543001
```

Common shape:

```text
TEXT {font} {rotation} {x} {y} {value}
```

The current label uses font `4` for a large human-readable code.

### QR Code

```text
BARCODE QR 200 30 M 2 U 11
MA,APLP10054543001
ENDQR
```

The QR data goes between the QR command and `ENDQR`.

In the current template:

- `200 30` places the QR code near the top centre for the current label size
- `U 11` makes the QR larger
- `MA,{code}` inserts the label value

If the QR is too large or too small, adjust the `U` value first.

### Lines And Boxes

Many CPCL printers support simple rules and boxes:

```text
LINE 20 60 556 60 2
BOX 20 20 556 300 2
```

Common shapes:

```text
LINE {x0} {y0} {x1} {y1} {width}
BOX {x0} {y0} {x1} {y1} {width}
```

Verify these on the target printer before relying on them in production labels.

### End Of Label

```text
FORM
PRINT
```

These commands finish and print the label.

## Example: Current QR Label

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

Use this when the QR and label code are the only required fields.

## Example: QR Plus Route And Dock

This is a possible workplace-style freight label with a route and dock/bay value:

```text
! 0 200 200 360 1
PW 576
BOX 12 12 564 348 2
CENTER
TEXT 4 0 0 24 APLP10054543001
BARCODE QR 190 70 M 2 U 9
MA,APLP10054543001
ENDQR
LEFT
TEXT 4 0 24 282 ROUTE: BNE-NORTH
TEXT 4 0 24 318 DOCK: 04
FORM
PRINT
```

Possible future Power Apps link shape:

```text
freightprint://print?template=route-dock&code=APLP10054543001&route=BNE-NORTH&dock=04
```

This is not implemented yet. It is a suggested extension.

## Example: Human-Readable Priority Label

This puts the route and priority above the QR code:

```text
! 0 200 200 400 1
PW 576
CENTER
TEXT 4 0 0 24 PRIORITY
TEXT 4 0 0 70 BNE-NORTH
BARCODE QR 190 118 M 2 U 8
MA,APLP10054543001
ENDQR
TEXT 4 0 0 330 APLP10054543001
FORM
PRINT
```

Possible future Power Apps link shape:

```text
freightprint://print?template=priority&code=APLP10054543001&route=BNE-NORTH&priority=PRIORITY
```

## Example: Two-Column Freight Detail Label

This is useful when operators need to read several fields without scanning:

```text
! 0 200 200 420 1
PW 576
BOX 12 12 564 408 2
LINE 12 96 564 96 2
CENTER
TEXT 4 0 0 36 APLP10054543001
LEFT
TEXT 4 0 24 122 ROUTE
TEXT 4 0 220 122 BNE-NORTH
TEXT 4 0 24 166 DOCK
TEXT 4 0 220 166 04
TEXT 4 0 24 210 CARTONS
TEXT 4 0 220 210 12
BARCODE QR 350 235 M 2 U 6
MA,APLP10054543001
ENDQR
FORM
PRINT
```

Possible future Power Apps link shape:

```text
freightprint://print?template=freight-detail&code=APLP10054543001&route=BNE-NORTH&dock=04&cartons=12
```

## Optional TODO: Custom Label Templates

The current helper is deliberately simple: one label code goes in, one fixed CPCL label comes out.

A useful future feature would be template selection from Power Apps:

```text
freightprint://print?template=route-dock&code=APLP10054543001&route=BNE-NORTH&dock=04
```

Suggested implementation:

1. Add a `template` query parameter in `PrintLinkActivity`.
2. Pass all supported fields to `PrintJobService`.
3. Add a small `LabelTemplate` or `CpclTemplates` class.
4. Keep one default template for backwards compatibility.
5. Add one method per label layout, for example:

```java
buildDefaultQrLabel(code, copies)
buildRouteDockLabel(code, route, dock, copies)
buildFreightDetailLabel(code, route, dock, cartons, copies)
```

Suggested fields:

```text
code
copies
template
route
dock
bay
customer
cartons
priority
date
```

Keep field names stable once Power Apps starts using them.

## Layout Tuning Checklist

When matching an existing workplace label:

1. Measure the physical label width and height.
2. Confirm the printer DPI.
3. Convert the label size to dots.
4. Set `PW` to the printable width.
5. Set the header height to the label height.
6. Place the largest required element first, usually the QR code.
7. Add human-readable text.
8. Test with the longest real values, not just the neat sample values.
9. Scan the printed QR code from a typical working distance.
10. Adjust coordinates, font, and QR `U` size until the label matches the workplace standard.

## Practical Advice

- Keep QR payloads short when possible.
- Use `EncodeUrl()` in Power Apps before passing values into the deep link.
- Avoid commas and control characters in fields that will be inserted directly into CPCL.
- Test with dirty or worn label stock if that is normal in the workplace.
- Keep the current default template working even after adding optional templates.
