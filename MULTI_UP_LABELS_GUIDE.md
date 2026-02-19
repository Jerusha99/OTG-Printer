# BarTender Multi-Up Label Printing Guide

## Features Added

### 1. Automatic XPML Cleaning
- **Problem:** BarTender exports labels as XPML+TSPL hybrid files with XML tags mixed into commands
- **Solution:** App now automatically detects and removes XML tags (`<xpml>`, `</xpml>`, etc.)
- **Result:** Clean, pure TSPL commands sent to printer

### 2. Multi-Up Label Layout Selection
You can now print **1-up**, **2-up**, **3-up**, or **4-up** label layouts from a single BarTender design.

**How it works:**
1. Design your label in BarTender (single sticker)
2. Print to file (PRN format)
3. Paste into the app
4. Select the layout you want: **1-up**, **2-up**, **3-up**, or **4-up**
5. The app automatically duplicates and offsets all drawing commands
6. Print in real-time to your USB printer

**Example:**
- BarTender design: 57mm × 100mm label (1 sticker)
- Select **2-up** → App converts to 114mm × 100mm (2 stickers side-by-side)
- Select **3-up** → App converts to 171mm × 100mm (3 stickers side-by-side)

### 3. Real Visual Preview
Instead of showing raw TSPL commands, the app now renders a **visual preview** of what will print:
- Shows TEXT elements as text
- Shows BARCODE elements as boxes with barcode pattern
- Shows LINE, BOX, BAR elements as vector shapes
- Displays label dimensions and printer settings
- Updates live as you type or change layout

---

## Step-by-Step Usage

### Step 1: Design in BarTender
1. Open BarTender label design software
2. Create your label design (single sticker, 57mm × 100mm recommended)
3. Include TEXT, BARCODE, shapes, borders as needed
4. Go to **File → Print** (Ctrl+P)

### Step 2: Export as PRN File
1. Select printer: **"Generic / Text Only"** or **"Zebra (TSPL)"**
2. Click **"Print to File"**
3. Save as: `my_label.prn`

### Step 3: Paste into App
1. Open the POS Mobile App
2. Go to **Dashboard** tab
3. Scroll to **"BarTender TSPL Document"** section
4. Click in the text field
5. Paste the entire contents of your `.prn` file

**⚠️ Important:** Include everything from the file, even if it has `<xpml>` tags — the app will clean it automatically.

### Step 4: Select Layout
Click one of the layout buttons:
- **1-up** — Single sticker per label
- **2-up** — Two stickers side-by-side (57mm × 2 = 114mm wide)
- **3-up** — Three stickers (57mm × 3 = 171mm wide)
- **4-up** — Four stickers (57mm × 4 = 228mm wide)

**Note:** Button becomes highlighted (unavailable) when selected.

### Step 5: Preview
The **"Label Visual Preview"** canvas shows:
- Label outline (gray border)
- Grid reference lines
- Rendered TEXT, BARCODE, and vector elements
- Label dimensions and printer settings at bottom

### Step 6: Print
1. Make sure printer is connected and ready
2. Click **"Print Label Document"**
3. Check the **Debug Log** for status

---

## Fixing Common Issues

### Issue: XPML tags mixed with TSPL
**Before:**
```
<xpml><page quantity='0' pitch='37.0 mm'></xpml>SIZE 97.5 mm, 37 mm
...
<xpml></page></xpml>
```

**After (app cleans automatically):**
```
SIZE 97.5 mm, 37 mm
...
PRINT 1,1
```

### Issue: Border looks like dot-matrix / dithered
**Root cause:** XPML export includes BITMAP (raster graphics) instead of native TSPL drawing commands

**Solution in BarTender:**
1. Use native shapes (boxes, lines) instead of images
2. Ensure export is set to **TSPL** format, not "Print as Graphics"
3. Verify label resolution matches printer (usually 203dpi for mobile/label printers)

**In app:** The parser automatically skips BITMAP lines, so you see only clean vector commands.

---

## Advanced: Understanding Multi-Up Conversion

When you select **2-up**, the app:

1. **Reads your TSPL:**
   ```
   SIZE 57mm,100mm
   TEXT 10,20,"0",0,1,1,"LABEL"
   BARCODE 10,50,"CODE128",40,1,0,2,2,"123456"
   PRINT 1,1
   ```

2. **Updates SIZE to 114mm width:**
   ```
   SIZE 114mm,100mm
   ```

3. **Duplicates and offsets all commands:**
   ```
   SIZE 114mm,100mm
   TEXT 10,20,"0",0,1,1,"LABEL"          # Original at X=10
   TEXT 67,20,"0",0,1,1,"LABEL"          # Copy offset by 57mm (0 + 57)
   BARCODE 10,50,"CODE128",40,1,0,2,2,"123456"
   BARCODE 67,50,"CODE128",40,1,0,2,2,"123456"
   PRINT 1,1
   ```

Result: **Two identical labels side-by-side on one print.**

---

## Technical Details

### Supported TSPL Commands
Visually previewed:
- `TEXT` — Shown as text in preview
- `BARCODE` — Shown as filled rectangle with barcode pattern
- `LINE` — Drawn as line
- `BOX` — Drawn as rectangle  
- `BAR` — Drawn as filled rectangle
- `DIAGONAL` — Drawn as diagonal line

Parsed but not visually shown:
- `CLS`, `SIZE`, `DENSITY`, `SPEED`, `DIRECTION`, `REFERENCE`, `OFFSET`
- `PRINT`, `GAP`, `SET` commands

### Automatic Cleanup
- Removes all `<xpml>`, `</xpml>`, `<page>`, `</page>` tags
- Skips BITMAP lines (which contain dithered/raster data)
- Normalizes line endings (CRLF → LF → CRLF for TSPL)
- Converts to ISO-8859-1 bytes (single-byte encoding for TSPL compatibility)

### Resolution & Scaling
- Displays scale calculations based on label size
- Preview canvas uses ~3.78 pixels per mm (96dpi conversion)
- Fits label within window while maintaining aspect ratio

---

## Real-Time Printing Features

✅ **BULK USB endpoints** — Uses fastest, most reliable transfer method
✅ **CRLF line endings** — Ensures printer receives commands correctly  
✅ **ISO-8859-1 encoding** — Avoids UTF-8 issues with some printers
✅ **Retry logic** — Automatically retries up to 3 times on failure
✅ **Status checking** — Paper, cover, temperature checks before printing
✅ **Chunked transfer** — Large files split into optimal packet sizes

---

## Examples

### Example 1: Simple 2-up Receipt Labels
**BarTender design:** Receipt, 57mm × 100mm
```
TEXT 10,10,"0",0,1,1,"Receipt"
TEXT 10,30,"0",0,1,1,"Date: 2025-02-19"
BARCODE 10,50,"CODE128",40,1,0,2,2,"ORD12345"
```

**In app:**
1. Paste TSPL
2. Select **2-up**
3. App converts to 114mm width, duplicates BARCODE+TEXT
4. Print → Two receipts side-by-side ✅

### Example 2: 3-up Address Labels
**BarTender design:** 57mm × 75mm address label
```
TEXT 5,10,"0",0,1,1,"Name"
TEXT 5,25,"0",0,1,1,"Address"
LINE 5,35,200,35,1
```

**In app:**
1. Paste TSPL
2. Select **3-up**
3. App scales to 171mm wide
4. Print → Three address labels in one pass ✅

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Preview shows "No TSPL content" | Paste TSPL into the text field first |
| Layout buttons don't change preview | Make sure you paste TSPL before selecting layout |
| Multi-up labels print too small | Adjust label SIZE in BarTender or check printer scaling |
| XPML tags still visible | Paste the entire `.prn` file; app auto-cleans |
| Barcode looks fuzzy in preview | App shows simplified barcode preview; printer output will be sharp |
| Print fails with "Paper out" | Load paper, click OK in printer notification, try again |

---

## Tips for Best Results

1. **Use standard sizes:** 57mm × 100mm (receipt), 50mm × 75mm (small label)
2. **Leave margins:** Avoid text/images within 5mm of label edges
3. **Use native shapes:** BOX/LINE instead of bitmap backgrounds
4. **Check density:** Use `DENSITY 12–15` for darker prints
5. **Test first:** Print 1 label before multi-up production runs
6. **Verify resolution:** Design at 203dpi if printing on 203dpi printer

---

**Version:** 2.0 | **Updated:** Feb 19, 2026
