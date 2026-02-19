# Real-Time Printing - Quick Test Guide

## Before You Start

✅ Requirements:
- Android device with USB Host support (OTG capable)
- Zebra PE200, TSC TH240, or compatible TSPL printer
- USB OTG cable (USB-A to Micro-USB or USB-C)
- Paper loaded in printer
- App installed on device

## 5-Minute Test

### Step 1: Connect Printer (1 min)
1. Plug OTG cable into device
2. Connect printer via USB to OTG cable
3. Open the app
4. Click **"Discover Devices"**
5. Select your printer from the list
6. Click **"Connect"**
7. Grant USB permission when prompted

**Expected Result:** ✅ "Connected to printer" message

### Step 2: Check Printer Status (1 min)
Click **"Check Printer Status"** button (if available)

**Expected Result:** 
- ✅ Status: Ready
- Printer Type: Zebra/TSC/etc
- Output Endpoint: 0x81 or 0x02
- Input Endpoint: 0x82 or available

### Step 3: Print Test Receipt (1 min)
1. Click **"Config"** → Set shop name
2. Click **"Add Item"** → Add any item
3. Click **"Print Bill"**
4. Watch the debug log

**Expected Result:**
- 📤 Sending X bytes
- ✅ Successfully sent bytes
- Bill prints on paper

### Step 4: Test Error Recovery (1 min)
1. Prepare printer
2. Start a print job
3. Immediately remove paper
4. Check app shows: 📄 "Paper out - please load paper"
5. Reload paper quickly
6. Print should auto-retry and succeed

**Expected Result:**
- App detects paper error
- Waits for user action
- Auto-retries when fixed

### Step 5: Test Bartender Commands (1 min)
1. Paste these commands in the TSPL input field:
```
CLS
SIZE 57mm,100mm
DENSITY 12
SPEED 8
DIRECTION 0,0
TEXT 10,20,"0",0,1,1,"TEST LABEL"
TEXT 10,40,"0",0,1,1,"Real-time printing"
TEXT 10,60,"0",0,1,1,"with error recovery"
BARCODE 20,80,"CODE128",40,1,0,2,2,"1234567890"
PRINT 1,1
```
2. Click **"Print Label (TSPL)"**

**Expected Result:**
- Label prints with text and barcode
- No errors in logs

## Test Scenarios

### Scenario A: Normal Printing
```
✅ Paper loaded
✅ Cover closed
✅ Printer on

Action: Click Print
Result: Prints immediately (1-3 seconds)
```

### Scenario B: Paper Out Recovery
```
❌ Paper empty
Action: Click Print
Result: Shows "Load paper" → waits up to 15 seconds
User: Load paper
Result: Auto-retry → Prints successfully
```

### Scenario C: USB Disconnect Recovery
```
Action: Click Print
Interrupt: Unplug USB cable halfway
Result: USB error detected → Retry 3x automatically
User: Reconnect cable
Result: Prints on next print attempt
```

### Scenario D: Retry on Timeout
```
Action: Click Print
Interrupt: Printer freezes/offline
Result: Timeout after 5s → Retry 3x automatically
Interrupt: Printer recovers
Result: Prints successfully on next retry
```

## Debug Log Interpretation

### ✅ Successful Print
```
🖨  PRINTING BILL
📋 Checking printer status...
✅ Printer ready!
📤 Sending 2847 bytes (attempt 1/3)...
✅ Successfully sent 2847/2847 bytes
✅ Bill sent to printer!
```

### ❌ Paper Out
```
📋 Checking printer status...
📄 Paper out detected - waiting for paper reload...
⏰ Waiting for paper to be loaded...
❌ Timeout waiting for printer to be ready
❌ Printer still not ready
```

### ✅ Paper Out + Recovery
```
📄 Paper out detected - waiting for paper reload...
⏰ Waiting for paper to be loaded...
✅ Printer ready!
📤 Sending 2847 bytes (attempt 2/3)...
✅ Successfully sent 2847/2847 bytes
✅ Bill sent to printer!
```

### ❌ USB Timeout & Retry
```
📤 Sending 2847 bytes (attempt 1/3)...
❌ Attempt 1 failed: USB timeout
⏰ Retrying in 2000ms...
📤 Sending 2847 bytes (attempt 2/3)...
✅ Successfully sent 2847/2847 bytes
```

## What to Check

### Printer Connection
- [ ] Device shows in "Discover" list
- [ ] Device connects without permission errors
- [ ] Debug log shows "Successfully connected to printer"
- [ ] Printer type detected correctly (Zebra/TSC)

### USB Communication
- [ ] Output endpoint found (usually 0x81 or 0x02)
- [ ] Input endpoint available (optional, for status)
- [ ] Data bytes sent match TSPL size
- [ ] No timeout errors on normal prints

### Printing Functionality
- [ ] Simple text prints clearly
- [ ] Barcodes print and scan correctly
- [ ] Multiple items in bill format
- [ ] Discounts calculate correctly
- [ ] Footer and header present

### Error Handling
- [ ] Paper out detected automatically
- [ ] Cover open detected automatically
- [ ] Retry logic activates on timeout
- [ ] Auto-recovery when paper reloaded
- [ ] Status check before printing

## Common Issues & Fixes

### Issue: Permission Prompt Doesn't Appear
**Fix:** 
1. Unplug printer
2. Wait 3 seconds
3. Plug back in
4. Try connecting again

### Issue: "Device not found" even after connecting
**Fix:**
1. Restart app
2. Try different USB port
3. Try different USB cable
4. Check if device is recognized by other apps

### Issue: Print starts but stops halfway
**Fix:**
1. Increase timeout: `setRetryConfiguration(printTimeoutMs=15000)`
2. Reduce print speed: `SPEED 4` (slower)
3. Reduce complexity: Fewer fonts/graphics

### Issue: Paper error not detected
**Fix:**
1. Try: `setPaperCheckEnabled(false)` 
2. App will attempt print even if status check fails
3. Printer response is more important

### Issue: Can print from computer but not app
**Fix:**
1. Check OTG cable - try different one
2. Power off printer, wait 10s, power on
3. Disconnect and reconnect USB
4. Check device permissions aren't blocking

## Video Test Sequence

If recording for documentation:

**Scene 1: Normal Print (30 seconds)**
1. Show printer connected
2. Click Add Item
3. Enter details and click Add
4. Click Print Bill
5. Show label coming out

**Scene 2: Paper Recovery (60 seconds)**
1. Start print
2. Remove paper during print
3. Show app message "Load paper"
4. Reload paper
5. Show auto-retry and success

**Scene 3: TSPL Command (45 seconds)**
1. Paste complex TSPL
2. Click Print Label
3. Show result
4. Scan barcode to verify

**Scene 4: Status Check (30 seconds)**
1. Click "Check Status"
2. Show printer info
3. Show retry configuration
4. Demonstrate changing settings

## Performance Baseline

Record these on your device:

| Operation | Time | Device | Printer |
|-----------|------|--------|---------|
| Connect | ___ | ___ | ___ |
| Print Bill | ___ | ___ | ___ |
| Print Label | ___ | ___ | ___ |
| Paper Recovery | ___ | ___ | ___ |
| Status Check | ___ | ___ | ___ |

## Export for Analysis

Capture debug logs:
```bash
adb logcat | grep -E "PrinterManager|PrinterStatus" > printer_test.log
```

Share with development team if issues occur.

## Success Criteria

✅ All of these = Success:
- [ ] App connects to printer without errors
- [ ] Prints complete without user intervention
- [ ] Paper out is detected and recovered
- [ ] Bartender TSPL commands print correctly
- [ ] Status checks return proper values
- [ ] No USB communication errors
- [ ] Retry logic activates on timeout
- [ ] App handles disconnection gracefully

## Next Level Testing

### Advanced: Stress Testing
1. Print 50 receipts continuously
2. Monitor memory usage
3. Check for memory leaks
4. Verify no USB timeouts

### Advanced: Edge Cases
1. Disconnect USB mid-print
2. Cover open during print
3. Multiple rapid prints
4. Large TSPL commands (>10KB)

### Advanced: Different Printers
1. Test with TSC printer
2. Test with Star printer
3. Test with generic thermal printer
4. Document any differences

## Support Contact

If issues persist:
1. Collect diagnostic info:
   - Device model and Android version
   - Printer model and firmware version
   - Debug logs (adb logcat)
   - Error messages

2. Share with development team

3. Include:
   - Expected behavior
   - Actual behavior
   - Steps to reproduce
   - Frequency (always/sometimes/rare)

---

**Estimated Test Time: 5-10 minutes**

Good luck with testing! 🚀
