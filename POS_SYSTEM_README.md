# OTG POS Bill Printer - Complete System Documentation

## Overview
This Android application provides a complete Point-of-Sale (POS) bill printing system for Zebra PE200 thermal mobile printers connected via USB OTG cable.

## ✅ Features Implemented

### 1. **Shop Configuration**
- Adjustable shop name, address, and phone number
- Settings dialog accessible via "🏪 Config" button
- Defaults to: "ABC STORE", "123 Main Street", "+1-800-123-4567"
- Settings persist during app session

### 2. **Item Management**
- Add multiple items to bill with:
  - Item name
  - Quantity
  - Unit price
  - Discount percentage (optional)
- Remove items individually
- Clear entire bill
- View running bill summary with totals

### 3. **Automatic Calculations**
- **Subtotal**: Sum of all items (qty × price)
- **Per-Item Discount**: Applied as percentage
- **Total Discount**: Sum of all individual discounts
- **Grand Total**: Subtotal - Total Discount
- All calculations displayed in debug log

### 4. **Bill Generation**
- Auto-generated bill numbers: `BL` + last 8 digits of timestamp
- Current date and time automatically captured
- Properly formatted for 57mm thermal receipt paper

### 5. **Barcode Generation**
- CODE128 barcode format
- Auto-generated using timestamp (ensures unique barcodes)
- Printed on every bill for tracking

### 6. **TSPL Command Integration**
- Uses Zebra PE200's native TSPL language
- Dynamic Y-position tracking for proper layout
- Professional receipt formatting with:
  - Shop header
  - Bill info section
  - Item listing table
  - Discount breakdown
  - Grand total
  - Barcode
  - Footer message

## 📁 File Structure

```
app/src/main/java/com/jerusha/mplotgpos/
├── MainActivity.kt          (UI logic & POS system)
├── PrinterManager.kt        (USB communication & TSPL generation)
├── BillModel.kt            (Data classes for bills & items)
└── ExampleInstrumentedTest.kt

res/
├── layout/
│   └── activity_main.xml   (UI layout with POS buttons)
├── drawable/
└── values/
```

## 🔧 Data Classes

### BillItem.kt
```kotlin
data class BillItem(
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0  // percentage
)
```

**Methods:**
- `getSubtotal()`: quantity × unitPrice
- `getDiscountAmount()`: subtotal × (discount / 100)
- `getTotal()`: subtotal - discountAmount

### Bill.kt
```kotlin
data class Bill(
    val shopName: String = "ABC STORE",
    val shopAddress: String = "123 Main Street",
    val shopPhone: String = "+1-800-123-4567",
    val billNumber: String = "",
    val date: String = "",
    val time: String = "",
    val items: List<BillItem> = emptyList(),
    val notes: String = ""
)
```

**Methods:**
- `getSubtotal()`: Sum of all items' subtotals
- `getTotalDiscount()`: Sum of all items' discounts
- `getGrandTotal()`: getSubtotal() - getTotalDiscount()
- `getItemCount()`: Number of items

## 🎮 UI Controls

### Main Screen Buttons

| Button | Function | Icon |
|--------|----------|------|
| 🏪 Config | Open shop configuration dialog | Green |
| ➕ Item | Add new item to bill | Blue |
| 🗑️ Clear | Clear all items from bill | Orange |
| 🧾 Print POS Bill | Print current bill to printer | Green |

### Configuration Dialog
- **Shop Name** input field
- **Shop Address** input field  
- **Shop Phone** input field
- Save button to apply changes

### Add Item Dialog
- **Item Name** text field
- **Quantity** number field
- **Unit Price** decimal field
- **Discount (%)** decimal field (default: 0)
- Add button to add to bill

## 🔄 Workflow

### Creating and Printing a Bill:

1. **Configure Shop Details** (optional)
   - Tap "🏪 Config" button
   - Enter shop name, address, phone
   - Tap "Save"

2. **Add Items to Bill**
   - Tap "➕ Item" button
   - Enter item details
   - Tap "Add"
   - Repeat for each item

3. **Review Bill**
   - Check debug log for bill summary
   - See totals: Subtotal, Discount, Grand Total

4. **Clear Items** (if needed)
   - Tap "🗑️ Clear" to start fresh

5. **Print Bill**
   - Connect printer via USB OTG
   - Tap "Connect" button to pair
   - Tap "🧾 Print POS Bill"
   - Bill prints automatically
   - Bill auto-clears after successful print

## 💾 MainActivity Functions

### POS System Functions
```kotlin
// Add item to bill
addItemToBill(itemName: String, quantity: Int, price: Double, discount: Double)

// Remove item by index
removeItemFromBill(index: Int)

// Clear all items
clearBill()

// Update shop information
updateShopInfo(name: String, address: String, phone: String)

// Update display with bill summary
updateBillSummary()

// Generate unique bill number
generateBillNumber(): String

// Print bill (main entry point)
printPOSBill()

// Show shop config dialog
showPOSConfigDialog()

// Show add item dialog
showAddItemDialog()
```

## 🖨️ PrinterManager Functions

### TSPL Command Generation
```kotlin
// Main POS bill printing function
fun printPOSBill(bill: Bill): Boolean

// Generate TSPL commands for bill
fun buildPOSBillCommands(bill: Bill): String
```

### TSPL Features Used
- `SIZE 57mm,100mm` - Paper size
- `TEXT X,Y,"FontNum",Rotation,X-scale,Y-scale,"Content"` - Text printing
- `BARCODE X,Y,"CODE128",Height,XDim,Rotation,Multiplier,Multiplier,"Data"` - Barcode
- `LINE X1,Y1,X2,Y2,Thickness` - Separator lines

## 📊 Bill Format (TSPL Output)

```
╔════════════════════════════╗
║       ABC STORE            ║
║   123 Main Street          ║
║   +1-800-123-4567         ║
╠════════════════════════════╣
║ Bill #: BL12345678        ║
║ Date: 2024-01-29          ║
║ Time: 14:30                ║
╠════════════════════════════╣
║ Item    Qty  Price  Total  ║
╠════════════════════════════╣
║ Pen       2  $5.00  $10.00 ║
║ Book      1  $10.00 $10.00 ║
║ (10% Discount: -$1.00)     ║
║ Notebook  1  $8.50  $8.50  ║
╠════════════════════════════╣
║ Subtotal............$28.50 ║
║ Discount............-$1.00 ║
║ TOTAL..............$27.50  ║
╠════════════════════════════╣
║  [Barcode: BL12345678]    ║
║  Thank You! Come Again!   ║
╚════════════════════════════╝
```

## 🐛 Debug Log

The debug log shows:
- App start messages
- Device connection/disconnection
- Item additions/removals
- Bill summaries with calculations
- Print status (success/failure)
- Any errors encountered

## ⚙️ Technical Details

### Printer: Zebra PE200
- **Language**: TSPL (Thermal Streaming Programming Language)
- **Connection**: USB Host via OTG cable
- **Paper Size**: 57mm width × variable length
- **Resolution**: 203 DPI
- **Barcode Format**: CODE128

### Android Requirements
- **Min API**: 24 (Android 7.0)
- **Target API**: 36 (Android 15)
- **Permissions**: USB, READ_EXTERNAL_STORAGE

### Threading
- USB operations run on background thread
- UI updates via runOnUiThread()
- No blocking on main thread

## 🚀 Usage Example

```kotlin
// Create bill items
val item1 = BillItem("Pen", 2, 5.00, 0.0)
val item2 = BillItem("Book", 1, 10.00, 10.0)  // 10% discount

// Create bill
val bill = Bill(
    shopName = "My Store",
    shopAddress = "123 Main St",
    shopPhone = "+1-800-123-4567",
    billNumber = "BL001",
    date = "2024-01-29",
    time = "14:30",
    items = listOf(item1, item2)
)

// Print
printerManager.printPOSBill(bill)  // Returns Boolean
```

## ✨ Key Improvements Made

1. **Configurable Shop Details**: No more hardcoded values
2. **Multiple Items Support**: Add unlimited items with individual discounts
3. **Automatic Calculations**: All math done in data classes
4. **Professional Formatting**: TSPL commands for clean receipt layout
5. **Barcode Integration**: Auto-generated unique barcodes
6. **User-Friendly Dialogs**: Easy item and shop configuration
7. **Robust Error Handling**: Validates all inputs before processing
8. **Debug Logging**: Full visibility into operations

## 📝 Notes

- Bill numbers are auto-generated using timestamp (unique per print)
- Shop settings persist during app session (not saved to disk)
- Items can be added/removed before printing
- Bill auto-clears after successful print
- Discount can be applied per item (percentage)
- All calculations happen in real-time

---

**Ready to use!** Connect your Zebra PE200 printer and start printing professional bills! 🧾
