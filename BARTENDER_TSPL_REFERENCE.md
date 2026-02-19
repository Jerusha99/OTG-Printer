# Bartender TSPL Commands - Quick Reference for USB OTG Printing

## How to Extract TSPL from Bartender

### Step 1: Create/Open Design in Bartender
1. Open Bartender label design software
2. Create your label design
3. Go to **File → Print** (or Ctrl+P)

### Step 2: Export as PRN File
1. Choose printer: **"Generic / Text Only"** or **"Zebra (TSPL)"**
2. Click **"Print to file"**
3. Save as: `your_label.prn`

### Step 3: Extract TSPL Commands
1. Open the `.prn` file in a text editor (Notepad, VS Code, etc.)
2. Copy all the commands between first `CLS` and last `PRINT` command
3. Paste into the app's TSPL input field

## Common Bartender TSPL Examples

### Receipt/Invoice Label (57mm x 100mm)
```
CLS
SIZE 57mm,100mm
DENSITY 12
SPEED 8
DIRECTION 0,0

TEXT 10,20,"0",0,1,1,"INVOICE"
TEXT 10,40,"0",0,1,1,"Date: 2025-02-16"
LINE 5,50,200,50,2

TEXT 10,60,"0",0,1,1,"Item: Widget"
TEXT 10,75,"0",0,1,1,"Qty: 5"
TEXT 10,90,"0",0,1,1,"Price: $49.99"

TEXT 10,110,"0",0,1,1,"Total: $249.95"
TEXT 10,130,"0",0,1,1,"Thank You!"

BARCODE 20,150,"CODE128",40,1,0,2,2,"123456789"

PRINT 1,1
```

### Product Label (50mm x 75mm)
```
CLS
SIZE 50mm,75mm
TEXT 5,10,"0",0,1,1,"PRODUCT"
TEXT 5,25,"0",0,1,1,"SKU-12345"
TEXT 5,40,"0",0,1,1,"MFG: 2025-02-16"
TEXT 5,55,"0",0,1,1,"EXP: 2026-02-16"
BARCODE 5,65,"EAN13",20,1,0,1,1,"5901234123457"
PRINT 1,1
```

### Shipping Label (100mm x 150mm)
```
CLS
SIZE 100mm,150mm
TEXT 10,20,"0",0,1,1,"SHIP TO"
TEXT 10,40,"0",0,1,1,"John Doe"
TEXT 10,55,"0",0,1,1,"123 Main St"
TEXT 10,70,"0",0,1,1,"City, State 12345"

LINE 5,85,370,85,2

TEXT 10,100,"0",0,1,1,"TRACK: 1Z999AA10123456784"
BARCODE 20,120,"CODE128",50,1,0,2,2,"1Z999AA10123456784"

PRINT 1,1
```

## TSPL Commands Reference

### Initialization
```
CLS                          # Clear label (must be first)
SIZE width,height           # Set label size (e.g., 57mm,100mm or 2.36x3.94")
DENSITY level               # Darkness 0-15 (12 is standard)
SPEED level                 # Print speed 1-8 (8 is fastest)
DIRECTION dir               # 0=forward, 1=reverse
REFERENCE x,y              # Reference point (usually 0,0)
OFFSET 0mm                  # Media offset
```

### Text
```
TEXT X,Y,"FONT",ROTATION,X-MUL,Y-MUL,"MESSAGE"
  X,Y        = Position in dots from top-left
  FONT       = 0-5 (0=small, higher=larger)
  ROTATION   = 0=normal, 1=90°, 2=180°, 3=270°
  X-MUL      = Width multiplier (1-6 for normal to 6x wide)
  Y-MUL      = Height multiplier (1-6)
  MESSAGE    = Text to print

Example: TEXT 10,20,"0",0,1,1,"Hello"
```

### Barcodes
```
BARCODE X,Y,"TYPE",HEIGHT,XW,ROTATION,XMUL,YMUL,"DATA"
  Types: CODE128, CODE39, EAN13, EAN8, UPC-A, UPC-E, CODE93, CODABAR, ITF, MSI, PLESSEY, QR, QRCODE

Example: BARCODE 20,80,"CODE128",40,1,0,2,2,"123456789"
```

### Graphics
```
LINE X1,Y1,X2,Y2,WIDTH     # Draw line
  Example: LINE 5,50,200,50,2

BOX X1,Y1,X2,Y2,WIDTH      # Draw box
  Example: BOX 10,30,190,150,2

CIRCLE X,Y,RADIUS,WIDTH    # Draw circle
  Example: CIRCLE 100,100,50,2
```

### Printing
```
PRINT count,set              # Print count copies of set sets
  Example: PRINT 1,1         # Print 1 copy

PRINT 3,2                    # Print 2 sets of 3 copies each
```

### Options
```
SET PEEL OFF/ON             # Peel-off mode
SET TEAR ON/OFF             # Tear-off mode
GAP 0mm                      # Gap between labels (0 for continuous)
```

## How to Use with the App

### Method 1: Copy-Paste from Bartender PRN File
1. Export `.prn` from Bartender
2. Open in text editor
3. Select all commands between `CLS` and `PRINT`
4. Copy to clipboard
5. Open app → Paste into "Custom TSPL Commands" field
6. Click "Print Label (TSPL)"

### Method 2: Type Commands Manually
```
CLS
SIZE 57mm,100mm
TEXT 10,20,"0",0,1,1,"Custom Text"
PRINT 1,1
```

### Method 3: Load from File
```kotlin
// Read from assets or file
val commands = assets.open("my_label.tspl").bufferedReader().use { it.readText() }
tsplCommandsInput.setText(commands)
```

## Troubleshooting

### 2-up / 3-up labels (multiple stickers across)
**Yes — it works in real time** as long as the TSPL you paste already contains the multi-up layout (BarTender will output objects at different X positions on the same label “page”).

Key points:
- **2-up/3-up is a layout**, not the `PRINT` count.
- For a 2-up label page you usually still use:
  - `PRINT 1,1` (prints 1 “page” that contains 2 stickers)
- If you want multiple pages of that 2-up layout, increase *sets*:
  - `PRINT 10,1` prints 10 pages → 20 stickers total (2 per page)

What must match your media:
- `SIZE` must be the **full page size** (the combined width that contains both/all columns), not the single sticker size.
- `GAP` must match your liner gap (or `GAP 0mm` for continuous).

If you design 2-up/3-up in BarTender and “Print to File”, just paste the full TSPL document (between `CLS` and `PRINT`) into the app and print.

### Border/shape prints like dot-matrix (dotted / grainy)
This almost always happens when the design is being exported/printed as a **raster image (bitmap)** with anti-aliasing/dithering, instead of native TSPL drawing commands.

Best fixes:
- Prefer native TSPL primitives for borders/shapes:
  - `BOX x1,y1,x2,y2,2` (use thickness 2–4 dots for a solid border)
  - `LINE x1,y1,x2,y2,2`
- In BarTender, ensure you are using a **TSPL/TSC-compatible driver** (not a generic graphics pipeline), and avoid options that force “print as graphics”.
- Make sure the BarTender document resolution matches the printer (many mobile/label printers are **203dpi**). A 300dpi design sent to a 203dpi printer often looks “dithered”.
- Increase border thickness: a 1-dot border can look broken on some heads; try 2–3 dots.

### Issue: Text appears cut off
**Solution:** Increase SIZE height or reduce text multiplier
```
SIZE 57mm,150mm    # Larger height
TEXT 10,20,"0",0,1,1,"Text"  # Use multiplier 1
```

### Issue: Barcode doesn't scan
**Solution:** Adjust barcode height and width
```
BARCODE 20,80,"CODE128",50,1,0,2,2,"DATA"  # Height 50, better size
```

### Issue: Lines or boxes too faint
**Solution:** Increase density and line width
```
DENSITY 15         # Maximum darkness
LINE 5,50,200,50,4  # Thicker line (width 4)
```

### Issue: Multiple lines of text overlap
**Solution:** Increase Y position between text elements
```
TEXT 10,20,"0",0,1,1,"Line 1"
TEXT 10,40,"0",0,1,1,"Line 2"  # Y increased by 20 dots
TEXT 10,60,"0",0,1,1,"Line 3"
```

### Issue: Printer receives commands but prints nothing
**Solution:** Ensure PRINT command is last
```
CLS
SIZE 57mm,100mm
TEXT 10,20,"0",0,1,1,"Test"
PRINT 1,1       # Must be at the end
```

## Coordinates Reference

### Paper Sizes (in dots)
57mm width ≈ 203 dots
100mm height ≈ 355 dots
4" x 6" ≈ 576 x 832 dots

### Position Guide
```
(5,20) - Top left area
(100,50) - Center area
(180,100) - Right side
(5,300) - Near bottom
```

## Tips for Better Results

1. **Always start with CLS** - Clears previous content
2. **Always end with PRINT** - Without it, nothing prints
3. **Spacing** - Increase Y by 15-20 between text lines
4. **Barcodes** - Place barcode near bottom with adequate space
5. **Margins** - Leave 5-10 dots from edges
6. **Font sizes** - Font 0-1 for small text, 2+ for larger
7. **Line width** - Use 2-4 for visible lines
8. **Density** - Higher (12-15) for darker print

## Commands That Work with Zebra PE200/TSC TH240

✅ Tested & Working:
- CLS, SIZE, DENSITY, SPEED, DIRECTION, REFERENCE
- TEXT, LINE, BARCODE, CIRCLE, BOX
- PRINT, QUERY STATUS
- SET PEEL, SET TEAR, GAP

❌ May Not Work:
- GS/GD (graphics upload)
- Some advanced rotation modes
- Custom fonts (>5)

## Export from Popular POS Systems

### Shopify Etsy Labels
1. Right-click on label preview
2. Send to → Printer
3. Choose "Print to File" as .prn
4. Extract TSPL from file

### Square POS Labels
1. Settings → Label Template
2. Export/Print template
3. Send output to PRN file

### Shopkeeper/Retail Systems
1. Navigate to label printing
2. Options → Export as TSPL/ZPL
3. Copy-paste the content

## Example: Creating Dynamic Labels

```kotlin
// Dynamically build TSPL for product labels
fun generateProductLabel(productName: String, price: Double, sku: String): String {
    return """
CLS
SIZE 57mm,75mm
DENSITY 12
SPEED 8

TEXT 10,10,"0",0,1,1,"PRODUCT"
TEXT 10,25,"0",0,1,1,"$productName"
TEXT 10,40,"0",0,1,1,"SKU: $sku"
TEXT 10,55,"0",0,1,1,"Price: $$price"

BARCODE 10,65,"CODE128",30,1,0,1,1,"$sku"

PRINT 1,1
""".trimIndent()
}

// Usage
val label = generateProductLabel("Widget", 49.99, "SKU-12345")
printerManager.sendRawTSPL(label)
```

## More Resources

- [Zebra TSPL Manual](https://www.zebra.com/en/en-us/products/software/barcode/link-os/mobile-app.html)
- [TSC TSPL Documentation](https://www.tscprinters.com/support)
- [Bartender Tutorial Videos](https://www.seagullscientific.com/)
