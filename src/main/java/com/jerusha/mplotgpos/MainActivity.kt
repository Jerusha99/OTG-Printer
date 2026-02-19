package com.jerusha.mplotgpos

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity(), PrinterManager.ConnectionCallback {

    companion object {
        private const val PREFS_NAME = "mplotgpos_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PRINT_HISTORY = "print_history"
        private const val MAX_HISTORY_ITEMS = 30

        private const val THEME_SYSTEM = "system"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
    }

    private enum class Section {
        DASHBOARD, HISTORY, SETTINGS, ABOUT
    }

    private lateinit var printerManager: PrinterManager
    private lateinit var prefs: SharedPreferences

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var navigationView: NavigationView
    private lateinit var sectionTitle: TextView

    private lateinit var dashboardSection: LinearLayout
    private lateinit var historySection: LinearLayout
    private lateinit var settingsSection: LinearLayout
    private lateinit var aboutSection: LinearLayout

    private lateinit var printerStatusText: TextView
    private lateinit var printerNameText: TextView
    private lateinit var statusMessage: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var discoverButton: Button
    private lateinit var debugLogText: TextView
    private lateinit var tsplCommandsInput: EditText
    private lateinit var printTsplButton: Button
    private lateinit var statusIndicator: View
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private lateinit var zoomLevelText: TextView

    private var currentTSPLDocument = ""
    private var currentZoomLevel = 100  // percentage

    private lateinit var historyText: TextView
    private lateinit var clearHistoryButton: Button

    private lateinit var themeModeGroup: RadioGroup
    private lateinit var settingsPrinterListText: TextView
    private lateinit var selectPrinterButton: Button

    private var availableDevices: List<UsbDevice> = emptyList()
    private val debugLogs = mutableListOf<String>()

    private var currentBill = Bill()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> {
                    showMessage("USB device attached")
                    updatePrinterStatus()
                }

                "android.hardware.usb.action.USB_DEVICE_DETACHED" -> {
                    showMessage("USB device detached")
                    printerManager.disconnect()
                    updatePrinterStatus()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash theme to app theme
        setTheme(R.style.Theme_MPLOtgPos)
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        printerManager = PrinterManager(this)
        printerManager.setConnectionCallback(this)

        initializeUI()
        setupDrawer()
        setupThemeControls()
        setupListeners()

        val intentFilter = IntentFilter().apply {
            addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
            addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
        }
        ContextCompat.registerReceiver(this, usbReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)

        updatePrinterStatus()
        loadPrintHistory()
        setSection(Section.DASHBOARD)

        addDebugLog("✅ App started")
        addDebugLog("Paste BarTender TSPL document and print in high-fidelity mode")
        updateDebugDisplay()
    }

    private fun initializeUI() {
        drawerLayout = findViewById(R.id.drawerLayout)
        topAppBar = findViewById(R.id.topAppBar)
        navigationView = findViewById(R.id.navigationView)
        sectionTitle = findViewById(R.id.sectionTitle)

        dashboardSection = findViewById(R.id.dashboardSection)
        historySection = findViewById(R.id.historySection)
        settingsSection = findViewById(R.id.settingsSection)
        aboutSection = findViewById(R.id.aboutSection)

        printerStatusText = findViewById(R.id.printerStatusText)
        printerNameText = findViewById(R.id.printerNameText)
        statusMessage = findViewById(R.id.statusMessage)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        discoverButton = findViewById(R.id.discoverButton)
        debugLogText = findViewById(R.id.debugLogText)
        tsplCommandsInput = findViewById(R.id.tsplCommandsInput)
        printTsplButton = findViewById(R.id.printTsplButton)
        statusIndicator = findViewById(R.id.statusIndicator)
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        zoomLevelText = findViewById(R.id.zoomLevelText)

        historyText = findViewById(R.id.historyText)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)

        themeModeGroup = findViewById(R.id.themeModeGroup)
        settingsPrinterListText = findViewById(R.id.settingsPrinterListText)
        selectPrinterButton = findViewById(R.id.selectPrinterButton)
    }

    private fun setupDrawer() {
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> setSection(Section.DASHBOARD)
                R.id.nav_history -> setSection(Section.HISTORY)
                R.id.nav_settings -> setSection(Section.SETTINGS)
                R.id.nav_about -> setSection(Section.ABOUT)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        navigationView.setCheckedItem(R.id.nav_dashboard)
    }

    private fun setupListeners() {
        connectButton.setOnClickListener { connectToPrinter() }
        disconnectButton.setOnClickListener { disconnectPrinter() }
        discoverButton.setOnClickListener { discoverDevices(showDialog = true) }

        printTsplButton.setOnClickListener { printRawTSPL() }
        clearHistoryButton.setOnClickListener { clearPrintHistory() }
        selectPrinterButton.setOnClickListener { discoverDevices(showDialog = true) }

        zoomInButton.setOnClickListener { handleZoomChange(10) }
        zoomOutButton.setOnClickListener { handleZoomChange(-10) }

        tsplCommandsInput.doAfterTextChanged {
            // Text changed - user is editing TSPL content
        }
    }

    private fun setupThemeControls() {
        val currentTheme = prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
        val checkedId = when (currentTheme) {
            THEME_LIGHT -> R.id.themeLightRadio
            THEME_DARK -> R.id.themeDarkRadio
            else -> R.id.themeSystemRadio
        }
        themeModeGroup.check(checkedId)

        themeModeGroup.setOnCheckedChangeListener { _, checkedRadioId ->
            val mode = when (checkedRadioId) {
                R.id.themeLightRadio -> THEME_LIGHT
                R.id.themeDarkRadio -> THEME_DARK
                else -> THEME_SYSTEM
            }

            if (mode != currentThemeMode()) {
                prefs.edit().putString(KEY_THEME_MODE, mode).apply()
                applySavedTheme()
                recreate()
            }
        }
    }

    private fun applySavedTheme() {
        when (prefs.getString(KEY_THEME_MODE, THEME_LIGHT)) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun currentThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
    }

    private fun handleZoomChange(delta: Int) {
        val newZoom = (currentZoomLevel + delta).coerceIn(50, 200)  // min 50%, max 200%
        if (newZoom != currentZoomLevel) {
            currentZoomLevel = newZoom
            val textSizeSp = (12 * newZoom / 100f)
            tsplCommandsInput.textSize = textSizeSp
            zoomLevelText.text = "$newZoom%"
        }
    }

    private fun setSection(section: Section) {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        
        // Hide all sections with fade out
        listOf(dashboardSection, historySection, settingsSection, aboutSection).forEach { 
            if (it.visibility == View.VISIBLE) {
                it.startAnimation(fadeOut)
            }
            it.visibility = View.GONE
        }
        
        // Show selected section with fade in
        val targetSection = when (section) {
            Section.DASHBOARD -> dashboardSection
            Section.HISTORY -> historySection
            Section.SETTINGS -> settingsSection
            Section.ABOUT -> aboutSection
        }
        
        targetSection.visibility = View.VISIBLE
        targetSection.startAnimation(fadeIn)

        sectionTitle.text = when (section) {
            Section.DASHBOARD -> "Dashboard"
            Section.HISTORY -> "Print History"
            Section.SETTINGS -> "Settings"
            Section.ABOUT -> "About"
        }

        when (section) {
            Section.HISTORY -> loadPrintHistory()
            Section.SETTINGS -> updatePrinterListInSettings()
            else -> Unit
        }
    }

    private fun discoverDevices(showDialog: Boolean) {
        showMessage("Scanning USB devices...")
        Thread {
            availableDevices = printerManager.getAvailableDevices()
            runOnUiThread {
                if (availableDevices.isEmpty()) {
                    showMessage("No USB devices found")
                    Toast.makeText(this@MainActivity, "No devices found", Toast.LENGTH_SHORT).show()
                } else if (showDialog) {
                    showDeviceSelectionDialog()
                } else if (availableDevices.size == 1) {
                    printerManager.connectToDevice(availableDevices.first())
                }
                updatePrinterListInSettings()
            }
        }.start()
    }

    private fun showDeviceSelectionDialog() {
        val deviceNames = availableDevices.map { device ->
            "${device.productName ?: device.deviceName} (${device.vendorId}:${device.productId})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select USB Printer")
            .setItems(deviceNames) { _, which ->
                val selectedDevice = availableDevices[which]
                showMessage("Connecting to ${selectedDevice.productName ?: selectedDevice.deviceName}")
                printerManager.connectToDevice(selectedDevice)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun connectToPrinter() {
        showMessage("Connecting to printer...")
        Thread {
            val devices = printerManager.getAvailableDevices()
            availableDevices = devices
            runOnUiThread {
                when {
                    devices.isEmpty() -> showMessage("No USB printers found")
                    devices.size == 1 -> printerManager.connectToDevice(devices.first())
                    else -> showDeviceSelectionDialog()
                }
                updatePrinterListInSettings()
            }
        }.start()
    }

    private fun disconnectPrinter() {
        printerManager.disconnect()
        showMessage("Disconnected from printer")
        updatePrinterStatus()
    }

    private fun updatePrinterStatus() {
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        
        if (printerManager.isConnected()) {
            printerStatusText.text = "✅ Connected"
            printerStatusText.setTextColor(ContextCompat.getColor(this, R.color.success))
            printerNameText.text = "Device: ${printerManager.getPrinterName()}"
            statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success)
            statusIndicator.startAnimation(scaleUp)
            connectButton.isEnabled = false
            disconnectButton.isEnabled = true
            printTsplButton.isEnabled = true
        } else {
            printerStatusText.text = "❌ Not Connected"
            printerStatusText.setTextColor(ContextCompat.getColor(this, R.color.danger))
            printerNameText.text = "Device: None"
            statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.danger)
            statusIndicator.startAnimation(scaleUp)
            connectButton.isEnabled = true
            disconnectButton.isEnabled = false
            printTsplButton.isEnabled = false
        }

        updatePrinterListInSettings()
    }

    private fun updatePrinterListInSettings() {
        val connected = if (printerManager.isConnected()) {
            "Connected: ${printerManager.getPrinterName()}"
        } else {
            "Connected: none"
        }

        val discovered = if (availableDevices.isEmpty()) {
            "Discovered: none"
        } else {
            availableDevices.joinToString("\n") {
                "• ${it.productName ?: it.deviceName} (${it.vendorId}:${it.productId})"
            }
        }

        settingsPrinterListText.text = "$connected\n\n$discovered"
    }

    private fun printRawTSPL() {
        try {
            if (!printerManager.isConnected()) {
                showMessage("Printer not connected")
                return
            }

            var tsplCommands = tsplCommandsInput.text.toString().trim()
            if (tsplCommands.isEmpty()) {
                showMessage("Please paste TSPL document")
                return
            }

            // Validate text length to prevent crashes
            if (tsplCommands.length > 100000) {
                showMessage("TSPL document too large (max 100KB)")
                addDebugLog("❌ TSPL document exceeds 100KB limit: ${tsplCommands.length} bytes")
                return
            }

            // Clean XPML if present
            if (tsplCommands.contains("<xpml>") || tsplCommands.contains("</xpml>")) {
                tsplCommands = TSPLParser.cleanXPMLDocument(tsplCommands)
                addDebugLog("✂️ Cleaned XPML - removed XML tags")
            }

            addDebugLog("📤 Sending TSPL: ${tsplCommands.length} bytes in ${tsplCommands.lines().size} lines")
            showMessage("Printing TSPL document...")
            
            Thread {
                val success = try {
                    val result = printerManager.printTSPLDocument(tsplCommands)
                    if (!result) {
                        addDebugLog("⚠️ Printer returned false status")
                    }
                    result
                } catch (e: OutOfMemoryError) {
                    addDebugLog("❌ Out of memory: TSPL document too large - ${e.message}")
                    runOnUiThread {
                        showMessage("Error: Document too large for device memory")
                    }
                    false
                } catch (e: Exception) {
                    addDebugLog("❌ TSPL print exception: ${e.javaClass.simpleName} - ${e.message}")
                    Log.e("MainActivity", "Print failed with exception", e)
                    false
                }

                runOnUiThread {
                    if (success) {
                        showMessage("✓ Label document printed successfully")
                        addDebugLog("✅ TSPL document printed successfully")
                        addPrintHistory("TSPL Label", tsplCommands.take(800), true)
                        Toast.makeText(this@MainActivity, "Label printed", Toast.LENGTH_SHORT).show()
                    } else {
                        showMessage("❌ Failed to print TSPL document")
                        addDebugLog("❌ TSPL document print failed")
                        addPrintHistory("TSPL Label", tsplCommands.take(800), false)
                    }
                    loadPrintHistory()
                    updateDebugDisplay()
                }
            }.start()
        } catch (e: Exception) {
            addDebugLog("❌ Unexpected error in printRawTSPL: ${e.message}")
            showMessage("Error: ${e.message}")
            Log.e("MainActivity", "Unexpected error in printRawTSPL", e)
        }
    }

    private fun addPrintHistory(type: String, payloadPreview: String, success: Boolean) {
        val historyArray = try {
            JSONArray(prefs.getString(KEY_PRINT_HISTORY, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }

        val entry = JSONObject().apply {
            put("time", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(System.currentTimeMillis()))
            put("type", type)
            put("success", success)
            put("printer", printerManager.getPrinterName())
            put("preview", payloadPreview)
        }

        historyArray.put(entry)
        while (historyArray.length() > MAX_HISTORY_ITEMS) {
            historyArray.remove(0)
        }

        prefs.edit().putString(KEY_PRINT_HISTORY, historyArray.toString()).apply()
        loadPrintHistory()
    }

    private fun loadPrintHistory() {
        val historyArray = try {
            JSONArray(prefs.getString(KEY_PRINT_HISTORY, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }

        if (historyArray.length() == 0) {
            historyText.text = "No print history yet"
            return
        }

        val lines = mutableListOf<String>()
        for (index in historyArray.length() - 1 downTo 0) {
            val entry = historyArray.optJSONObject(index) ?: continue
            val successText = if (entry.optBoolean("success", false)) "SUCCESS" else "FAILED"
            lines.add("[${entry.optString("time")}] $successText")
            lines.add("Type: ${entry.optString("type")}")
            lines.add("Printer: ${entry.optString("printer")}")
            lines.add("Preview:")
            lines.add(entry.optString("preview").take(200))
            lines.add("${"-".repeat(34)}")
        }

        historyText.text = lines.joinToString("\n")
    }

    private fun clearPrintHistory() {
        prefs.edit().putString(KEY_PRINT_HISTORY, "[]").apply()
        loadPrintHistory()
        showMessage("Print history cleared")
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            statusMessage.text = message
            statusMessage.startAnimation(fadeIn)
        }
    }

    private fun formatAmount(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun addDebugLog(log: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(System.currentTimeMillis())
        debugLogs.add("[$timestamp] $log")
        if (debugLogs.size > 80) {
            debugLogs.removeAt(0)
        }
    }

    private fun updateDebugDisplay() {
        runOnUiThread {
            debugLogText.text = debugLogs.joinToString("\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {
        }
        printerManager.cleanup()
    }

    override fun onDevicesFound(devices: List<UsbDevice>) {
        runOnUiThread {
            availableDevices = devices
            addDebugLog("Found ${devices.size} USB device(s)")
            updatePrinterListInSettings()
            updateDebugDisplay()
        }
    }

    override fun onConnected() {
        runOnUiThread {
            updatePrinterStatus()
            showMessage("Connected to printer")
            addDebugLog("Printer connected")
            updateDebugDisplay()
        }
    }

    override fun onConnectionFailed(error: String) {
        runOnUiThread {
            updatePrinterStatus()
            showMessage("Connection failed: $error")
            addDebugLog("Connection failed: $error")
            updateDebugDisplay()
        }
    }

    override fun onPermissionRequired() {
        runOnUiThread {
            showMessage("USB permission required")
            addDebugLog("Waiting for USB permission")
            updateDebugDisplay()
        }
    }

    override fun onDebugLog(message: String) {
        addDebugLog(message)
        updateDebugDisplay()
    }
}
