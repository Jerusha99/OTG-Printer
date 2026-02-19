# Debugging Multi-Up Label Overlapping Issue

## What Was Fixed

Your printed labels showed overlapping text like "NO STMO SMOKING" + "NO SMOKING NO SMO SMOKING" because the offset calculation was wrong or not being applied.

**Updated:**
1. ✅ **Better offset calculation** — Now handles DIAGONAL lines and both X coordinates in BOX/LINE
2. ✅ **Robust regex parsing** — Properly extracts coordinates from complex TSPL commands like `TEXT 468,190,"0",180,26,26,"NO SMOKING"`
3. ✅ **Detailed logging** — Shows you exactly what's being parsed, cleaned, and offset
4. ✅ **Preview canvas debugging** — Tells you if content is reaching the canvas

---

## How to Debug

### Step 1: Paste Your BarTender XPML Output

Paste the exact output from BarTender (with `<xpml>` tags and BITMAP data).

### Step 2: Watch the Debug Log

When you paste or select a layout, you'll see detailed messages:

```
📥 Raw input: 2847 chars
🧹 Cleaning XPML tags...
📖 Original length: 2847 chars
✂️ After removing XML: 1456 chars
✅ Found 18 non-empty lines
⏭️ Skipping BITMAP line (raster data): BITMAP 516,64,23,184,1,ÿÿÿÿ...
🎯 Final TSPL: 812 chars
✂️ After cleaning: 812 chars
🔄 Adjusting for 2-up...
📏 Original: 97.5mm × 37mm
📐 New SIZE: 195mm × 37mm
🔢 X offset per copy: 97 dots
📋 dup[1]: TEXT 565,190,"0",180,26,26,"NO SMOKING"
📋 dup[1]: DIAGONAL 668,214,744,93,3
📋 dup[1]: DIAGONAL 653,203,729,86,3
...
✅ Created 12 offset copies
🎨 Updating canvas (31 lines)
```

### Step 3: Check for Red Flags

| Message | Means | Fix |
|---------|-------|-----|
| `❌ Could not parse SIZE` | TSPL doesn't have `SIZE` command | Add it manually or check BarTender export |
| `⏭️ Total BITMAP lines skipped: N` | Raster graphics found (expected) | Normal - we're removing them |
| `📭 No content` | Text field is empty | Paste TSPL first |
| `✅ Created 0 offset copies` | No drawable commands found | Check if TEXT/BARCODE/LINE present in TSPL |
| `❌ Preview error` | Canvas rendering failed | Check Debug Log for exception |

---

## What Each Log Section Tells You

### XPML Cleaning
```
📖 Original length: 2847 chars          ← Raw BarTender output
✂️ After removing XML: 1456 chars       ← After stripping <xpml> tags
✅ Found 18 non-empty lines
🎯 Final TSPL: 812 chars                ← Clean TSPL ready to print
```

**Good if:** Numbers decrease (XML being removed). Bad if: No change (might not be XPML).

### Multi-Up Adjustment
```
📏 Original: 97.5mm × 37mm              ← Your BarTender label size
📐 New SIZE: 195mm × 37mm               ← Doubled for 2-up
🔢 X offset per copy: 97 dots           ← 97.5mm → toInt() = 97
📋 dup[1]: TEXT 565,190,...             ← Original X: 468 + 97 = 565 ✅
✅ Created 12 offset copies
```

**Good if:** X offsets are calculated. Bad if: Says "Created 0 offset copies" (commands not found).

### Canvas Update
```
🎨 Updating canvas (31 lines)           ← TSPL being sent to visual preview
```

**Good if:** Shows a number. Bad if: Shows 0 or error.

---

## Troubleshooting Steps

### If Preview Still Shows Nothing

1. **Check Debug Log** — Look for error messages
2. **Try Minimal TSPL** — Paste this simple one:
   ```
   CLS
   SIZE 57mm,100mm
   TEXT 10,20,"0",0,1,1,"TEST"
   PRINT 1,1
   ```
   If this shows in preview, your BarTender XPML might be malformed.

3. **Check Canvas Element** — Make sure the canvas is visible on screen (might be below the fold)

### If Labels Still Print Overlapping

1. **Check the offset calculation**:
   - BarTender label size: X mm
   - Expected offset: X × 2 for 2-up, X × 3 for 3-up
   - Debug log shows: `🔢 X offset per copy: N dots`
   - If N looks wrong, the SIZE parsing failed

2. **Check coordinate parsing**:
   - Debug shows: `📋 dup[1]: TEXT 565,190,...`
   - Original in TSPL: `TEXT 468,190,...`
   - Math: 468 + 97 = 565 ✅ Correct
   - If offset doesn't match, regex might not be capturing coordinates

3. **Save the debug output** and share it with details about what printed wrong

---

## Example Good vs Bad Output

### ✅ GOOD (This will print correctly)
```
📥 Raw input: 1456 chars
🧹 Cleaning XPML tags...
✂️ After cleaning: 812 chars
🎯 Final TSPL: 812 chars
🔄 Adjusting for 2-up...
📐 New SIZE: 195mm × 37mm
🔢 X offset per copy: 97 dots
✅ Created 12 offset copies
📋 dup[1]: TEXT 565,190,"0",180,26,26,"NO SMOKING"
📋 dup[1]: DIAGONAL 668,214,744,93,3
🎨 Updating canvas (31 lines)
```

### ❌ BAD (This will have issues)
```
📥 Raw input: 2847 chars
✂️ After cleaning: 2847 chars        ← No change! XML tags didn't get removed
🎯 Final TSPL: 2847 chars
🔄 Adjusting for 2-up...
❌ Could not parse SIZE                ← SIZE command not found!
✅ Created 0 offset copies              ← No drawing commands found
```

---

## What to Do

1. **Update the app** — Build and run the latest version
2. **Paste your BarTender XPML** into the TSPL input field
3. **Select 2-up**
4. **Watch Debug Log** — Copy all the messages
5. **Take a screenshot** of the debug output
6. **Print one label** and check if it looks correct
7. **Share results** — Include the debug log and what printed

This will help us see exactly where the issue is!
