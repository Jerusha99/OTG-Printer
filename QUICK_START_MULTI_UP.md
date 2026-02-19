# XPML + Multi-Up Label Printing - QUICK START

## ✅ What's New

### 1. Automatic XPML Cleanup
Your BarTender `.prn` files come with messy XML tags like:
```
<xpml><page quantity='0' pitch='37.0 mm'></xpml>SIZE 97.5 mm, 37 mm...
BITMAP 516,64,23,184,1,ÿÿÿÿÿ...
<xpml></page></xpml>
```

**The app now:**
- ✅ Removes all `<xpml>` tags automatically
- ✅ Strips BITMAP lines (which look like dot-matrix)
- ✅ Sends clean TSPL to printer
- ✅ Works in real-time over USB OTG

---

### 2. Multi-Up Label Layout Selection
**Before:** Print only 1 label per page

**Now:** Choose **1-up / 2-up / 3-up / 4-up** with buttons:

| Selection | Result |
|-----------|--------|
| **1-up** | 1 sticker (57×100mm) |
| **2-up** | 2 stickers side-by-side (114×100mm) |
| **3-up** | 3 stickers side-by-side (171×100mm) |
| **4-up** | 4 stickers side-by-side (228×100mm) |

**Example workflow:**
1. Design 1 label in BarTender (57mm width)
2. Export to `.prn` file
3. Paste into app
4. Click **"2-up"** button
5. Click **"Print Label Document"**
6. Printer prints 2 identical labels on one pass ✅

---

### 3. Real Visual Preview (Not Raw TSPL)
**Before:**
```
SIZE 57mm,100mm
TEXT 10,20,"0",0,1,1,"MY LABEL"
BARCODE 10,50,"CODE128",40,1,0,2,2,"123456"
LINE 5,80,150,80,2
PRINT 1,1
```

**Now:** 
You see a **canvas preview** showing:
- Label outline (gray border)
- TEXT rendered as visible text
- BARCODE as a box pattern
- LINE/BOX as drawn shapes
- Dimensions & settings: "57.0mm × 100mm | Density: 12 | Speed: 8"

---

## How to Use

### Step 1: Prepare BarTender File
```
BarTender → File → Print → "Print to File" → Save as my_label.prn
```

### Step 2: Paste into App
```
Dashboard → BarTender TSPL Document → Paste entire contents of .prn
```

### Step 3: Select Layout
```
Click one: [1-up] [2-up] [3-up] [4-up]
```

### Step 4: See Preview
```
Label Visual Preview canvas updates automatically
```

### Step 5: Print
```
Click "Print Label Document"
```

---

## Example: 2-Up Printing

**Your BarTender TSPL (1 sticker):**
```
SIZE 57mm,100mm
TEXT 10,20,"0",0,1,1,"LABEL 1"
BARCODE 10,50,"CODE128",40,1,0,2,2,"ABC123"
PRINT 1,1
```

**App processing (you select "2-up"):**
```
SIZE 114mm,100mm                      ← Width doubled!
TEXT 10,20,"0",0,1,1,"LABEL 1"        ← First copy at X=10
TEXT 67,20,"0",0,1,1,"LABEL 1"        ← Second copy at X=67 (10+57)
BARCODE 10,50,"CODE128",40,1,0,2,2,"ABC123"
BARCODE 67,50,"CODE128",40,1,0,2,2,"ABC123"
PRINT 1,1
```

**Result:** Printer prints 2 identical stickers on one page ✅

---

## Key Benefits

✨ **Real-time USB OTG printing** — No lag, instant output

🎨 **Visual preview before printing** — See exactly what prints

📋 **Auto-clean XPML** — Paste messy BarTender exports, app cleans them

🏷️ **1/2/3/4-up layouts** — Print multiple copies efficiently

📦 **Handle dot-matrix borders** — Skip BITMAP lines, use vector drawing

---

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| Preview shows "No TSPL content" | Paste TSPL first, then select layout |
| XPML tags still in preview | They're automatically removed; just print |
| Multi-up looks small | Check your BarTender "SIZE" setting |
| Border/shape looks pixelated | BarTender exported as BITMAP; use BOX/LINE instead |
| Print fails | Check "Paper out" / "Cover open" messages in Debug Log |

---

## USB/OTG Reliability (Behind the Scenes)

✅ Uses **BULK endpoints** (fastest, most reliable)
✅ Sends **CRLF line endings** (printer compatibility)
✅ Uses **ISO-8859-1 encoding** (single-byte, no UTF-8 issues)
✅ **Chunked transfer** (optimal packet sizes)
✅ **Automatic retry** (up to 3 attempts if printer was busy)

---

**Ready to print multi-up labels?** 🚀

1. Design 1 label in BarTender
2. Export → Paste → Select layout
3. Print!
