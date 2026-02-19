# POS System - Quick Start Guide

## Step-by-Step Usage

### 1. Connect Your Printer

```
Button: [Discover Devices]
├─ Click to scan for USB printers
└─ Select your Zebra PE200 from the list

Button: [Connect]
├─ Auto-detects available printer
└─ Shows "✅ Printer Connected" when ready
```

### 2. Configure Shop (Optional)

```
Button: [🏪 Config]
└─ Opens dialog with fields:
   ├─ Shop Name (default: ABC STORE)
   ├─ Shop Address (default: 123 Main Street)
   └─ Shop Phone (default: +1-800-123-4567)
```

**Example:**
```
Shop Name:    My Retail Store
Address:      456 Oak Avenue, Suite 100
Phone:        +1-888-555-1234
```

### 3. Add Items to Bill

```
Button: [➕ Item]
└─ Opens dialog with fields:
   ├─ Item Name (required)
   ├─ Quantity (number)
   ├─ Unit Price (decimal)
   └─ Discount (%) [optional, default: 0]
```

**Example 1 - Item without discount:**
```
Item Name:    Apple
Quantity:     5
Unit Price:   2.50
Discount:     0
Result: 5 × $2.50 = $12.50
```

**Example 2 - Item with discount:**
```
Item Name:    Orange Juice
Quantity:     2
Unit Price:   4.99
Discount:     10
Subtotal: 2 × $4.99 = $9.98
Discount (10%): -$0.99
Total: $8.99
```

**Example 3 - Multiple items:**
```
Item 1: Pen × 10 @ $0.99 = $9.90
Item 2: Notebook × 3 @ $5.99 (5% off) = $17.97 - $0.90 = $17.07
Item 3: Pencil × 20 @ $0.50 = $10.00

Bill Summary:
Subtotal:     $37.87
Discount:     -$0.90
TOTAL:        $36.97
```

### 4. Review Bill

The debug log shows real-time summary:
```
=== BILL SUMMARY ===
Items: 3
Subtotal: $37.87
Discount: -$0.90
TOTAL: $36.97
==================
```

### 5. Print Bill

```
Button: [🧾 Print POS Bill]
└─ Validates:
   ├─ Printer is connected ✓
   └─ Bill has items ✓
   
Then:
├─ Generates bill number (BL + timestamp)
├─ Captures current date/time
├─ Generates unique barcode
├─ Sends TSPL commands to printer
├─ Shows "✅ Bill printed successfully!"
└─ Auto-clears items from bill
```

### 6. Clear Items (if needed)

```
Button: [🗑️ Clear]
└─ Removes all items
└─ Ready for new bill
```

---

## Real-World Example - Retail Store

### Scenario: Selling office supplies

**Step 1:** Connect Printer
```
- Plug Zebra PE200 via USB OTG
- Click "Discover Devices"
- Select PE200 from list
- Click "Connect"
✓ Printer ready
```

**Step 2:** Configure Shop (first time)
```
- Click "🏪 Config"
- Enter: John's Office Supplies
- Enter: 789 Commerce Street
- Enter: +1-555-0123
- Click "Save"
```

**Step 3:** Add Items

```
Item 1: Ball Point Pen
├─ Quantity: 50
├─ Unit Price: $0.75
└─ Discount: 0%
✓ Added

Item 2: Legal Pad
├─ Quantity: 10  
├─ Unit Price: $4.99
└─ Discount: 5% (bulk discount)
✓ Added

Item 3: Paper Clip Box
├─ Quantity: 20
├─ Unit Price: $1.50
└─ Discount: 0%
✓ Added
```

**Step 4:** Review Summary
```
=== BILL SUMMARY ===
Items: 3
Subtotal: $68.90
Discount: -$2.50
TOTAL: $66.40
==================
```

**Step 5:** Print
```
- Click "🧾 Print POS Bill"
- Receipt prints with:
  • Shop name and address
  • Bill #: BL85743921
  • Date: 2024-01-29
  • Time: 14:45
  • Item listing
  • Barcode for tracking
  • "Thank You! Come Again!"
- Bill automatically cleared
✓ Ready for next customer
```

---

## Data Flow Diagram

```
User Input (UI Dialog)
         ↓
   BillItem Object
         ↓
   Add to billItems List
         ↓
   updateBillSummary()
   ├─ Calculate totals
   └─ Display in debug log
         ↓
   [Print Button Pressed]
         ↓
   createBill() - Convert List to Bill object
         ↓
   printPOSBill() - Main entry point
         ↓
   PrinterManager.printPOSBill(bill)
         ↓
   buildPOSBillCommands(bill)
   ├─ Generate TSPL text commands
   ├─ Dynamic Y-position tracking
   ├─ Barcode generation
   └─ Shop header/footer
         ↓
   sendData() - Send to USB printer
         ↓
   Zebra PE200 Thermal Printer
         ↓
   📄 Receipt printed
         ↓
   clearBill() - Auto-clear items
         ↓
   Ready for next bill
```

---

## Tips & Tricks

### Quick Add Multiple Similar Items
```
For 5 Pens at $2.00 each:
- Item: Pen
- Qty: 5
- Price: $2.00
- Total: $10.00 (one item line)

vs

Adding 5 separate items (more time-consuming)
```

### Using Discounts Efficiently
```
Scenario 1: Bulk discount on one item
└─ Add item with higher discount %

Scenario 2: Store-wide discount
└─ Add each item with same discount %

Scenario 3: No discount
└─ Leave discount field as 0
```

### Managing Shop Settings
```
- Settings apply to CURRENT session only
- Configure once per shift
- Use "🏪 Config" to change mid-day if needed
- Not saved to disk (reset on app restart)
```

### Reading Debug Log
```
✅ = Success
❌ = Error
🏪 = Shop info
🧾 = Bill printing
🗑️ = Clear/removal
⏳ = In progress
```

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| "Printer not connected" | Click "Connect" button first |
| "Bill is empty" | Add items using "➕ Item" button |
| Bill didn't print | Check printer power, USB cable, paper |
| Numbers look wrong | Check decimal points in price entry |
| Discount not applied | Make sure you entered % value (e.g., 10 for 10%) |
| Barcode missing | Barcode auto-generates - check printer paper width |

---

## File Organization

When printing multiple bills:
```
Bill #BL85743921 (2024-01-29 14:30)
├─ Items: 3
├─ Subtotal: $68.90
├─ Discount: -$2.50
└─ Total: $66.40

Bill #BL85743945 (2024-01-29 14:35)
├─ Items: 5
├─ Subtotal: $125.00
├─ Discount: -$5.00
└─ Total: $120.00
```

Each bill gets a unique number (BL + timestamp).

---

## Summary

The POS system is now fully operational with:
- ✅ Configurable shop details
- ✅ Multiple items with individual discounts
- ✅ Automatic calculations
- ✅ Professional receipt formatting
- ✅ Barcode generation
- ✅ Real-time bill tracking
- ✅ Simple, intuitive UI

**You're ready to start printing bills!** 🧾
