# TSPL Commands Fixed - Zebra PE200 Printer

## Issue Resolved
The printer was using incorrect command syntax. The Zebra PE200 uses **TSPL** (Thermal Streaming Programming Language), NOT ZPL2.

## Changes Made

### 1. Fixed buildZPL2Commands Function (Label Printing)
**Before:** Incorrect syntax with wrong parameters
```
TEXT 50,50,"0",0,2,2,"${text.trim()}"
SPEED 152.4
DIRECTION 0
```

**After:** Correct TSPL syntax for PE200
```
TEXT 20,30,"0",0,1,1,"line1"
TEXT 20,60,"0",0,1,1,"line2"
SPEED 8
DIRECTION 0,0
```

### 2. Fixed buildPOSBillCommands Function (Bill Printing)
Updated to use proper TSPL format:
- Removed invalid `SET POWER SAVING OFF` command
- Changed SPEED from numeric value to level (8)
- Fixed DIRECTION to use proper format: `0,0`
- Fixed REFERENCE to use mm units: `REFERENCE 0,0`
- Fixed OFFSET to use mm units: `OFFSET 0mm`
- Corrected TEXT command syntax for proper printing
- Fixed LINE command syntax with proper coordinates

## Key TSPL Syntax Rules for PE200

### TEXT Command
```
TEXT X,Y,"FONT",ROTATION,X-MULTIPLY,Y-MULTIPLY,"STRING"
```
- X, Y: Position in dots from top-left
- FONT: 0-5 (0 is small, 1-5 are larger)
- ROTATION: 0=normal, 1=90°, 2=180°, 3=270°
- Multipliers: 1=normal size, 2=double, etc.

### SIZE Command
```
SIZE WIDTH,HEIGHT
```
- 57mm x 100mm (standard receipt roll)
- 101.6mm x 152.4mm (4x6 label)

### SPEED Command
```
SPEED LEVEL
```
- Level 1-8 (NOT mm/s value!)

### DIRECTION Command  
```
DIRECTION PRINT,LABEL
```
- Two parameters: PRINT direction (0/1), LABEL direction (0/1)

### LINE Command
```
LINE X1,Y1,X2,Y2,THICKNESS
```
- Draws line from (X1,Y1) to (X2,Y2)
- Thickness in dots (1-5)

### BARCODE Command
```
BARCODE X,Y,"TYPE",HEIGHT,XWIDTH,ROTATION,XMUL,YMUL,"DATA"
```
- TYPE: "CODE128", "CODE39", "EAN13", etc.
- HEIGHT: in dots
- XWIDTH: bar width
- ROTATION: 0-3
- DATA: barcode content

### Print Command
```
PRINT SET,COPY
```
- SET: number of label sets
- COPY: copies per set

## Testing Checklist

✅ Label printing now uses correct TSPL format
✅ Bill printing now uses correct TSPL format
✅ Text positioning adjusted for PE200
✅ Speed parameter corrected (level, not mm/s)
✅ Direction parameter includes both values
✅ All commands use proper syntax

## Expected Results

- **Label printing:** Text should print clearly with proper line breaks
- **Bill printing:** Full receipt with:
  - Shop name and details
  - Bill number and timestamp
  - Item list with quantities and prices
  - Discount information if applicable
  - Totals (subtotal, discount, grand total)
  - Unique barcode
  - Footer message

## PE200 Specifications
- 58mm thermal mobile printer
- TSPL language support
- Maximum print width: 54mm
- Printing speed: up to 100mm/s
- Recommended: 57mm x 100mm thermal paper rolls
