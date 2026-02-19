# Real-Time Printing Integration Guide for MainActivity

## Configuration on App Start

Add this to `MainActivity.onCreate()` after creating the `PrinterManager`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    // Initialize PrinterManager
    printerManager = PrinterManager(this)
    printerManager.setConnectionCallback(this)
    
    // Configure for real-time reliable printing
    printerManager.setRetryConfiguration(
        maxRetries = 3,
        retryDelayMs = 2000L,
        printTimeoutMs = 10000
    )
    printerManager.setPaperCheckEnabled(true)
    
    // ... rest of initialization
}
```

## Enhanced Print Button Click Listeners

### For POS Bill Printing
```kotlin
private fun printPOSBill() {
    if (!printerManager.isConnected()) {
        showMessage("❌ Printer not connected")
        return
    }
    
    if (currentBill.items.isEmpty()) {
        showMessage("⚠️ Add items before printing")
        return
    }
    
    showMessage("🖨️ Printing bill...")
    Thread {
        try {
            // Print will automatically handle paper errors with retry logic
            val success = printerManager.printPOSBill(currentBill)
            runOnUiThread {
                if (success) {
                    showMessage("✅ Bill printed successfully!")
                    addDebugLog("✅ Bill printed successfully")
                    Toast.makeText(this@MainActivity, "✅ Bill printed", Toast.LENGTH_SHORT).show()
                    clearBill()
                } else {
                    showMessage("❌ Failed to print bill")
                    addDebugLog("❌ Print failed - check printer status and paper")
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                showMessage("❌ Error: ${e.message}")
                addDebugLog("❌ Exception: ${e.message}")
            }
        }
    }.start()
}
```

### For Raw TSPL/Bartender Commands
```kotlin
private fun printRawTSPL() {
    if (!printerManager.isConnected()) {
        showMessage("❌ Printer not connected")
        return
    }
    
    val tsplCommands = tsplCommandsInput.text.toString().trim()
    if (tsplCommands.isEmpty()) {
        showMessage("⚠️ Please enter TSPL commands")
        return
    }
    
    showMessage("🖨️ Printing label...")
    Thread {
        try {
            // Send with automatic retry on communication errors
            val success = printerManager.sendRawTSPL(tsplCommands)
            runOnUiThread {
                if (success) {
                    showMessage("✅ Label printed successfully!")
                    addDebugLog("✅ Label printed from TSPL commands")
                    Toast.makeText(this@MainActivity, "✅ Label printed", Toast.LENGTH_SHORT).show()
                    tsplCommandsInput.text.clear()
                } else {
                    showMessage("❌ Failed to print label")
                    addDebugLog("❌ TSPL print failed - check printer")
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                showMessage("❌ Error: ${e.message}")
                addDebugLog("❌ Exception: ${e.message}")
            }
        }
    }.start()
}
```

## Adding Printer Status View

Add a new button to check printer status in real-time:

### XML Layout Addition
```xml
<Button
    android:id="@+id/checkStatusButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="📋 Check Printer Status"
    android:textSize="16sp"
    android:layout_margin="8dp"
    app:layout_constraintTop_toBottomOf="@id/connectButton" />
```

### Kotlin Implementation
```kotlin
private lateinit var checkStatusButton: Button

private fun initializeUI() {
    // ... existing UI initialization ...
    checkStatusButton = findViewById(R.id.checkStatusButton)
    checkStatusButton.setOnClickListener { showPrinterStatus() }
}

private fun showPrinterStatus() {
    if (!printerManager.isConnected()) {
        showMessage("❌ Printer not connected")
        return
    }
    
    showMessage("📋 Checking printer status...")
    Thread {
        try {
            val status = printerManager.checkPrinterStatus()
            val info = printerManager.getPrinterInfo()
            
            runOnUiThread {
                val statusMessage = when (status) {
                    is PrinterStatusChecker.PrinterStatus.Ready -> "✅ Ready to print"
                    is PrinterStatusChecker.PrinterStatus.PaperOut -> "📄 Paper out - load paper"
                    is PrinterStatusChecker.PrinterStatus.CoverOpen -> "🔓 Cover open - close cover"
                    is PrinterStatusChecker.PrinterStatus.OverTemperature -> "🌡️ Overheated - cooling"
                    is PrinterStatusChecker.PrinterStatus.Offline -> "📡 Offline"
                    else -> "❓ Unknown status"
                }
                
                showMessage(statusMessage)
                addDebugLog(statusMessage)
                addDebugLog("Printer Info:\n$info")
                updateDebugDisplay()
            }
        } catch (e: Exception) {
            runOnUiThread {
                showMessage("❌ Error checking status: ${e.message}")
                addDebugLog("Status check error: ${e.message}")
            }
        }
    }.start()
}
```

## Advanced Configuration Options

### Add Settings Dialog for Print Configuration
```kotlin
private fun showPrintSettingsDialog() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Print Settings")
    
    val layout = android.widget.LinearLayout(this).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        setPadding(20, 20, 20, 20)
    }
    
    // Max Retries spinne
    val retryLabel = android.widget.TextView(this).apply {
        text = "Max Retries:"
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    
    val retrySpinner = android.widget.Spinner(this).apply {
        val adapter = android.widget.ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_spinner_item,
            listOf("1", "2", "3", "5", "10")
        )
        setAdapter(adapter)
        setSelection(2)  // Default 3
    }
    
    // Paper Check checkbox
    val paperCheckBox = android.widget.CheckBox(this).apply {
        text = "Enable Paper Error Checking"
        isChecked = true
    }
    
    layout.addView(retryLabel)
    layout.addView(retrySpinner)
    layout.addView(android.widget.Space(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            0, 20
        )
    })
    layout.addView(paperCheckBox)
    
    builder.setView(layout)
    builder.setPositiveButton("Apply") { _, _ ->
        val retries = retrySpinner.selectedItem.toString().toInt()
        printerManager.setRetryConfiguration(
            maxRetries = retries,
            retryDelayMs = 2000L,
            printTimeoutMs = 10000
        )
        printerManager.setPaperCheckEnabled(paperCheckBox.isChecked)
        
        showMessage("✅ Print settings updated")
        addDebugLog("Print config: retries=$retries, paperCheck=${paperCheckBox.isChecked}")
    }
    builder.setNegativeButton("Cancel", null)
    builder.show()
}
```

## Callback Implementation

Make sure your `ConnectionCallback` implementations handle all statuses:

```kotlin
override fun onDebugLog(message: String) {
    addDebugLog(message)
    updateDebugDisplay()
}

override fun onConnected() {
    runOnUiThread {
        updatePrinterStatus()
        showMessage("✅ Connected to printer")
        addDebugLog("✅ Printer connected successfully")
        addDebugLog("Type: ${printerManager.getPrinterType()}")
        updateDebugDisplay()
    }
}

override fun onConnectionFailed(error: String) {
    runOnUiThread {
        updatePrinterStatus()
        showMessage("❌ Connection failed: $error")
        addDebugLog("❌ Connection failed: $error")
        
        // Suggest troubleshooting
        when {
            error.contains("Paper") -> addDebugLog("💡 Load paper and retry")
            error.contains("Cover") -> addDebugLog("💡 Close printer cover and retry")
            error.contains("Permission") -> addDebugLog("💡 Check USB permission not granted")
            error.contains("timeout") -> addDebugLog("💡 Try different USB cable or port")
        }
        
        updateDebugDisplay()
    }
}
```

## Testing with Bartender Files

When importing TSPL from Bartender PRN files:

```kotlin
// Copy-paste from .prn file - make sure line endings are included
private fun loadBartenderExample() {
    val bartenderTSPL = """
CLS
SIZE 57mm,100mm
DENSITY 12
SPEED 8
DIRECTION 0,0
TEXT 10,20,"0",0,1,1,"INVOICE"
TEXT 10,40,"0",0,1,1,"Item: Widget"
TEXT 10,60,"0",0,1,1,"Price: $49.99"
BARCODE 15,80,"CODE128",40,1,0,2,2,"123456789"
PRINT 1,1
""".trimIndent()
    
    tsplCommandsInput.setText(bartenderTSPL)
    showMessage("📋 Bartender example loaded - click Print to test")
}
```

## Handling Different Printer Scenarios

### Scenario 1: Paper Empty During Printing
```
User clicks Print
  ↓
TSPL commands sent to printer
  ↓
Printer returns "Paper Out" status
  ↓
System shows: "📄 Paper is empty - Please load paper"
  ↓
Waits 15 seconds for user to load paper
  ↓
Paper loaded → Auto-retries print
  ↓
Print successful
```

### Scenario 2: USB Disconnection During Print
```
Sending data
  ↓
USB error (-1, -2, or timeout)
  ↓
System logs error, waits 2 seconds
  ↓
Retries up to 3 times
  ↓
All failed → Shows "Print Failed" message
```

### Scenario 3: Offline/Not Responding
```
Print initiated
  ↓
Query status → Offline
  ↓
System attempts recovery
  ↓
Still offline after 5 seconds
  ↓
Shows "Printer offline" message
```

## Performance Tips

1. **Don't spam print requests** - Wait for previous print to complete
2. **Check status before critical prints** - Use `checkPrinterStatus()` for important jobs
3. **Use appropriate timeouts** - Balance between reliability and user experience
4. **Monitor USB connection** - Handle cable reconnection gracefully
5. **Test with different paper sizes** - Adjust SIZE command per your printer

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| "USB timeout" errors | Slow printer or cable issue | Increase `printTimeoutMs` to 15000 |
| Partial prints | Paper misalignment | Check paper loading, try smaller SIZE |
| "Paper out" false positive | Sensor dirty | Try `setPaperCheckEnabled(false)` |
| No status feedback | Printer doesn't support status queries | Automatic fallback to "Ready" |
| Printer gets hot | Printing too fast | Reduce SPEED or add delays |

## Next Steps

1. Test with your specific printer model
2. Adjust retry configuration for your environment
3. Monitor debug logs during initial deployment
4. Implement user-friendly error dialogs
5. Create fallback for when printer is unavailable
