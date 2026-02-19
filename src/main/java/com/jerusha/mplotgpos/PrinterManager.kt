package com.jerusha.mplotgpos

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.Locale

class PrinterManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PrinterManager"
        private const val ACTION_USB_PERMISSION = "com.jerusha.mplotgpos.USB_PERMISSION"
        private const val ESC = 0x1B.toByte()
        private const val GS = 0x1D.toByte()
        private const val LF = 0x0A.toByte()
        private const val CR = 0x0D.toByte()
    }
    
    private var usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var printerDevice: UsbDevice? = null
    private var usbConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var claimedInterface: android.hardware.usb.UsbInterface? = null
    private var outputEndpoint: android.hardware.usb.UsbEndpoint? = null
    private var inputEndpoint: android.hardware.usb.UsbEndpoint? = null
    private var permissionGranted = false
    private var pendingDevice: UsbDevice? = null
    private var receiverRegistered = false
    private var printerType: PrinterType = PrinterType.CUSTOM
    private var statusChecker: PrinterStatusChecker? = null
    
    // Real-time printing configuration
    private var maxRetries = 3
    private var retryDelayMs = 2000L
    private var paperCheckEnabled = true
    private var printTimeout = 10000
    
    // Callback interface
    interface ConnectionCallback {
        fun onDevicesFound(devices: List<UsbDevice>)
        fun onConnected()
        fun onConnectionFailed(error: String)
        fun onPermissionRequired()
        fun onDebugLog(message: String)
    }
    
    private var connectionCallback: ConnectionCallback? = null
    
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            debugLog("📻 Broadcast received: ${intent?.action}")
            
            if (ACTION_USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    try {
                        @Suppress("DEPRECATION")
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        
                        debugLog("=== PERMISSION RESPONSE ===")
                        debugLog("Device: ${device?.deviceName ?: "null"}")
                        debugLog("Permission granted: $permissionGranted")
                        
                        if (permissionGranted && device != null) {
                            debugLog("✅ SUCCESS! Permission GRANTED")
                            printerDevice = device
                            pendingDevice = null
                            
                            debugLog("Opening USB connection...")
                            val connectionSuccess = openConnection(device)
                            
                            if (connectionSuccess) {
                                debugLog("✅✅✅ SUCCESSFULLY CONNECTED! ✅✅✅")
                                debugLog("You can now print labels!")
                                connectionCallback?.onConnected()
                            } else {
                                debugLog("❌ Failed to open USB connection")
                                connectionCallback?.onConnectionFailed("Failed to open USB connection")
                            }
                        } else {
                            debugLog("❌ Permission DENIED")
                            connectionCallback?.onConnectionFailed("USB permission denied. Please try again and accept the permission.")
                        }
                    } catch (e: Exception) {
                        debugLog("❌ Exception in permission receiver: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    init {
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        ContextCompat.registerReceiver(context, usbPermissionReceiver, permissionFilter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
        debugLog("USB Permission receiver registered on init")
    }
    
    fun setConnectionCallback(callback: ConnectionCallback) {
        this.connectionCallback = callback
    }
    
    private fun debugLog(message: String) {
        Log.d(TAG, message)
        connectionCallback?.onDebugLog(message)
    }

    private fun formatAmount(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
    
    fun getAvailableDevices(): List<UsbDevice> {
        try {
            val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
            Log.d(TAG, "Total USB devices found: ${deviceList.size}")
            
            if (deviceList.isEmpty()) {
                Log.e(TAG, "No USB devices found")
                return emptyList()
            }
            
            val allDevices = deviceList.values.toList()
            for (device in allDevices) {
                val deviceInfo = "Device: ${device.deviceName}, Product: ${device.productName}, " +
                        "Class: ${device.deviceClass}, SubClass: ${device.deviceSubclass}, " +
                        "VendorID: 0x${device.vendorId.toString(16)}, ProductID: 0x${device.productId.toString(16)}"
                Log.d(TAG, "Found USB device: $deviceInfo")
            }
            
            connectionCallback?.onDevicesFound(allDevices)
            return allDevices
        } catch (e: Exception) {
            Log.e(TAG, "Error getting devices: ${e.message}", e)
            return emptyList()
        }
    }
    
    fun connectToDevice(device: UsbDevice) {
        try {
            Log.d(TAG, "Attempting to connect to device: ${device.deviceName}")
            pendingDevice = device
            printerDevice = device
            
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                requestPermissionAndConnect(device)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating connection: ${e.message}", e)
            connectionCallback?.onConnectionFailed(e.message ?: "Unknown error")
        }
    }
    
    fun connectToPrinter(): Boolean {
        try {
            val deviceList = getAvailableDevices()
            
            if (deviceList.isEmpty()) {
                connectionCallback?.onConnectionFailed("No USB devices found. Please connect a USB printer.")
                return false
            }
            
            for (device in deviceList) {
                if (device.deviceClass == 7 || device.deviceSubclass == 1 || isLikelyPrinter(device)) {
                    debugLog("Device matches printer criteria. Attempting connection...")
                    connectToDevice(device)
                    return true
                }
            }
            
            debugLog("No specific printer class found. Using first device...")
            if (deviceList.isNotEmpty()) {
                connectToDevice(deviceList.first())
                return true
            }
            
            return false
        } catch (e: Exception) {
            debugLog("Error connecting to printer: ${e.message}")
            connectionCallback?.onConnectionFailed(e.message ?: "Unknown error")
            return false
        }
    }
    
    private fun requestPermissionAndConnect(device: UsbDevice) {
        try {
            debugLog("=== REQUESTING USB PERMISSION ===")
            debugLog("Device: ${device.deviceName}")
            debugLog("Vendor: ${device.manufacturerName}")
            debugLog("Product: ${device.productName}")
            
            if (usbManager.hasPermission(device)) {
                debugLog("✓ Already has permission!")
                printerDevice = device
                val success = openConnection(device)
                if (success) {
                    debugLog("✓✓✓ CONNECTED ✓✓✓")
                    connectionCallback?.onConnected()
                } else {
                    debugLog("✗ Failed to open connection")
                    connectionCallback?.onConnectionFailed("Failed to open USB connection")
                }
                return
            }
            
            debugLog("No permission yet - requesting from system...")
            debugLog("📱 IMPORTANT: Check your phone!")
            debugLog("A USB permission dialog should appear")
            
            val permIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).apply {
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_MUTABLE
            )
            
            debugLog("Sending permission request to Android system...")
            usbManager.requestPermission(device, permIntent)
            debugLog("✓ Request sent")
            
            connectionCallback?.onPermissionRequired()
            
            Thread {
                Thread.sleep(40000)
                if (usbConnection == null && printerDevice == device) {
                    debugLog("⏱️ TIMEOUT: No response received")
                    connectionCallback?.onConnectionFailed("Permission request timed out")
                }
            }.start()
            
        } catch (e: Exception) {
            debugLog("✗ ERROR: ${e.message}")
            e.printStackTrace()
            connectionCallback?.onConnectionFailed("Error: ${e.message}")
        }
    }
    
    private fun openConnection(device: UsbDevice): Boolean {
        try {
            Log.d(TAG, "=== Opening USB connection for device: ${device.deviceName} ===")
            
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.e(TAG, "CRITICAL: Failed to open USB connection - usbManager.openDevice() returned null")
                return false
            }
            
            Log.d(TAG, "✓ USB connection opened successfully")
            usbConnection = connection
            
            Log.d(TAG, "Device has ${device.interfaceCount} interface(s)")
            
            if (device.interfaceCount <= 0) {
                Log.e(TAG, "ERROR: Device has no interfaces")
                connection.close()
                return false
            }
            
            val intf = device.getInterface(0)
            Log.d(TAG, "Interface 0: class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, " +
                    "protocol=${intf.interfaceProtocol}, endpoints=${intf.endpointCount}")
            
            val claimSuccess = connection.claimInterface(intf, true)
            if (!claimSuccess) {
                Log.e(TAG, "ERROR: Failed to claim interface 0")
                
                for (i in 1 until device.interfaceCount) {
                    val otherIntf = device.getInterface(i)
                    Log.d(TAG, "Trying Interface $i...")
                    if (connection.claimInterface(otherIntf, true)) {
                        Log.d(TAG, "✓ Successfully claimed Interface $i instead")
                        claimedInterface = otherIntf
                        return findAndUseEndpoint(connection, otherIntf, device)
                    }
                }
                
                connection.close()
                return false
            }
            
            Log.d(TAG, "✓ Interface 0 claimed successfully")
            claimedInterface = intf
            return findAndUseEndpoint(connection, intf, device)
            
        } catch (e: Exception) {
            Log.e(TAG, "ERROR opening USB connection: ${e.message}", e)
            e.printStackTrace()
            return false
        }
    }
    
    private fun findAndUseEndpoint(connection: android.hardware.usb.UsbDeviceConnection, 
                                   intf: android.hardware.usb.UsbInterface, 
                                   device: UsbDevice): Boolean {
        try {
            Log.d(TAG, "Looking for output endpoint in interface with ${intf.endpointCount} endpoints...")
            
            if (intf.endpointCount <= 0) {
                Log.e(TAG, "ERROR: Interface has no endpoints")
                connection.close()
                return false
            }
            
            var bulkOut: android.hardware.usb.UsbEndpoint? = null
            var firstOut: android.hardware.usb.UsbEndpoint? = null
            var bulkIn: android.hardware.usb.UsbEndpoint? = null
            var firstIn: android.hardware.usb.UsbEndpoint? = null

            for (i in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(i)
                val directionStr = if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) "OUT" else "IN"
                val typeStr = when (ep.type) {
                    android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                    android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                    android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "Endpoint $i: direction=$directionStr, type=$typeStr, address=0x${ep.address.toString(16)}, maxPacket=${ep.maxPacketSize}")

                when (ep.direction) {
                    android.hardware.usb.UsbConstants.USB_DIR_OUT -> {
                        if (firstOut == null) firstOut = ep
                        if (ep.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK && bulkOut == null) {
                            bulkOut = ep
                        }
                    }
                    android.hardware.usb.UsbConstants.USB_DIR_IN -> {
                        if (firstIn == null) firstIn = ep
                        if (ep.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK && bulkIn == null) {
                            bulkIn = ep
                        }
                    }
                }
            }

            outputEndpoint = bulkOut ?: firstOut
            inputEndpoint = bulkIn ?: firstIn

            if (outputEndpoint != null) {
                Log.d(TAG, "✓ Selected output endpoint address=0x${outputEndpoint!!.address.toString(16)} type=${outputEndpoint!!.type}")
            }

            if (inputEndpoint != null) {
                Log.d(TAG, "✓ Selected input endpoint address=0x${inputEndpoint!!.address.toString(16)} type=${inputEndpoint!!.type}")
            }
            
            if (outputEndpoint == null) {
                Log.e(TAG, "ERROR: No output endpoint found in interface")
                connection.close()
                return false
            }
            
            statusChecker = PrinterStatusChecker(connection, inputEndpoint, outputEndpoint)
            
            printerType = PrinterDetector.detectPrinterType(
                device.vendorId,
                device.productId,
                device.productName,
                device.manufacturerName
            )
            Log.d(TAG, "Detected printer type: $printerType")
            
            Log.d(TAG, "=== Successfully connected to printer ===")
            Log.d(TAG, "Device: ${device.deviceName}")
            Log.d(TAG, "Printer Type: $printerType")
            Log.d(TAG, "Output Endpoint: 0x${outputEndpoint!!.address.toString(16)}")
            Log.d(TAG, "Input Endpoint: ${if (inputEndpoint != null) "0x${inputEndpoint!!.address.toString(16)}" else "Not available"}")
            Log.d(TAG, "Max packet size: ${outputEndpoint!!.maxPacketSize}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "ERROR finding endpoint: ${e.message}", e)
            return false
        }
    }
    
    private fun isLikelyPrinter(device: UsbDevice): Boolean {
        val productName = device.productName?.lowercase() ?: ""
        val printerKeywords = listOf("printer", "thermal", "label", "pos", "receipt")
        return printerKeywords.any { productName.contains(it) }
    }
    
    fun checkPrinterStatus(): PrinterStatusChecker.PrinterStatus {
        return try {
            if (statusChecker == null) {
                debugLog("⚠️ Status checker not initialized")
                return PrinterStatusChecker.PrinterStatus.Unknown
            }
            
            val status = statusChecker!!.queryStatus(printerType)
            debugLog("📋 Printer Status: $status")
            status
        } catch (e: Exception) {
            debugLog("⚠️ Could not query printer status: ${e.message}")
            PrinterStatusChecker.PrinterStatus.Unknown
        }
    }
    
    private fun waitForPrinterReady(timeoutMs: Int = 10000): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!isConnected()) {
                debugLog("❌ Connection lost while waiting for ready")
                return false
            }
            
            val status = statusChecker?.queryStatus(printerType) ?: PrinterStatusChecker.PrinterStatus.Ready
            
            when (status) {
                PrinterStatusChecker.PrinterStatus.Ready -> {
                    debugLog("✅ Printer ready!")
                    return true
                }
                PrinterStatusChecker.PrinterStatus.PaperOut -> {
                    debugLog("📄 Waiting for paper to be loaded...")
                    Thread.sleep(500)
                }
                PrinterStatusChecker.PrinterStatus.CoverOpen -> {
                    debugLog("🔓 Waiting for cover to be closed...")
                    Thread.sleep(500)
                }
                PrinterStatusChecker.PrinterStatus.OverTemperature -> {
                    debugLog("🌡️ Printer overheated - cooling down...")
                    Thread.sleep(1000)
                }
                PrinterStatusChecker.PrinterStatus.Offline -> {
                    debugLog("📡 Printer offline...")
                    Thread.sleep(500)
                }
                else -> {
                    debugLog("❓ Unknown printer status")
                    Thread.sleep(500)
                }
            }
        }
        
        debugLog("⏱️ Timeout waiting for printer to be ready")
        return false
    }
    
    fun printLabel(text: String, copies: Int = 1): Boolean {
        if (!isConnected()) {
            debugLog("❌ Printer not connected")
            return false
        }
        
        return try {
            debugLog("")
            debugLog("🖨️ PRINTING LABEL")
            debugLog("Text: $text")
            debugLog("Copies: $copies")
            
            for (i in 1..copies) {
                if (!isConnected()) {
                    debugLog("❌ Connection lost during print")
                    return false
                }
                
                debugLog("📄 Label $i of $copies")
                val commands = buildZPL2Commands(text)
                debugLog("📤 Sending ${commands.size} bytes...")
                sendData(commands)
                debugLog("✅ Label $i sent")
                
                if (i < copies) {
                    Thread.sleep(100)
                }
            }
            
            debugLog("✅ All labels sent to printer!")
            true
        } catch (e: Exception) {
            debugLog("❌ Print error: ${e.message}")
            false
        }
    }
    
    fun printBill(): Boolean {
        if (!isConnected()) {
            debugLog("❌ Printer not connected")
            return false
        }
        
        return try {
            debugLog("")
            debugLog("🧾 PRINTING BILL")
            debugLog("Format: 57mm x 40mm thermal receipt")
            
            val commands = buildBillCommands()
            debugLog("📤 Sending ${commands.size} bytes...")
            sendData(commands)
            debugLog("✅ Bill sent to printer!")
            true
        } catch (e: Exception) {
            debugLog("❌ Bill print error: ${e.message}")
            false
        }
    }
    
    fun printPOSBill(bill: Bill): Boolean {
        if (!isConnected()) {
            debugLog("❌ Printer not connected")
            return false
        }
        
        return try {
            debugLog("")
            debugLog("🧾 PRINTING POS BILL")
            debugLog("Bill #${bill.billNumber} - Items: ${bill.getItemCount()}")
            debugLog("Total: $${formatAmount(bill.getGrandTotal())}")
            
            if (paperCheckEnabled) {
                debugLog("📋 Checking printer status...")
                if (!waitForPrinterReady(printTimeout)) {
                    debugLog("⚠️ Printer not ready - retrying...")
                    Thread.sleep(1000)
                    if (!waitForPrinterReady(5000)) {
                        debugLog("❌ Printer still not ready")
                        return false
                    }
                }
            }
            
            val success = sendWithRetry { 
                buildPOSBillCommands(bill) 
            }
            
            if (success) {
                debugLog("✅ Bill sent to printer!")
            } else {
                debugLog("❌ Failed to send bill after $maxRetries attempts")
            }
            success
        } catch (e: Exception) {
            debugLog("❌ Bill print error: ${e.message}")
            false
        }
    }
    
    private fun buildZPL2Commands(text: String): ByteArray {
        val tspl = StringBuilder()
        
        tspl.append("CLS\n")
        tspl.append("SIZE 101.6mm,152.4mm\n")
        tspl.append("DENSITY 12\n")
        tspl.append("SPEED 8\n")
        tspl.append("DIRECTION 0,0\n")
        tspl.append("REFERENCE 0,0\n")
        tspl.append("OFFSET 0mm\n")
        tspl.append("SET PEEL OFF\n")
        tspl.append("SET TEAR ON\n")
        
        val lines = text.trim().split("\n")
        var yPosition = 30
        
        for (line in lines) {
            if (line.isNotBlank()) {
                tspl.append("TEXT 20,$yPosition,\"0\",0,1,1,\"$line\"\n")
                yPosition += 30
            }
        }
        
        tspl.append("PRINT 1,1\n")
        
        val command = tspl.toString()
        debugLog("📋 TSPL Commands for PE200:")
        debugLog(command.replace("\n", "\\n"))
        debugLog("Total bytes: ${command.length}")
        
        return command.toByteArray(Charsets.UTF_8)
    }
    
    private fun buildBillCommands(): ByteArray {
        val tspl = StringBuilder()
        
        tspl.append("CLS\n")
        tspl.append("SET POWER SAVING OFF\n")
        tspl.append("SIZE 57mm,40mm\n")
        tspl.append("DENSITY 12\n")
        tspl.append("SPEED 8\n")
        tspl.append("DIRECTION 0,0\n")
        tspl.append("REFERENCE 0,0\n")
        tspl.append("OFFSET 0mm\n")
        
        tspl.append("TEXT 50,10,\"1\",0,1,1,\"ABC STORE\"\n")
        tspl.append("LINE 10,30,220,30,2\n")
        tspl.append("TEXT 10,40,\"0\",0,1,1,\"Pen x2\"\n")
        tspl.append("TEXT 150,40,\"0\",0,1,1,\"5.00\"\n")
        tspl.append("TEXT 10,52,\"0\",0,1,1,\"Book x1\"\n")
        tspl.append("TEXT 150,52,\"0\",0,1,1,\"10.00\"\n")
        tspl.append("TEXT 10,64,\"0\",0,1,1,\"Notebook\"\n")
        tspl.append("TEXT 150,64,\"0\",0,1,1,\"8.50\"\n")
        tspl.append("LINE 10,74,220,74,2\n")
        tspl.append("TEXT 10,84,\"0\",0,1,1,\"Total\"\n")
        tspl.append("TEXT 140,84,\"0\",0,1,1,\"23.50\"\n")
        tspl.append("TEXT 30,100,\"0\",0,1,1,\"Thank You!\"\n")
        tspl.append("PRINT 1,1\n")
        
        val command = tspl.toString()
        debugLog("📋 TSPL Bill Command (57mm x 40mm):")
        debugLog(command.replace("\n", "\\n"))
        debugLog("Bytes: ${command.length}")
        
        return command.toByteArray(Charsets.UTF_8)
    }
    
    private fun buildPOSBillCommands(bill: Bill): ByteArray {
        val tspl = StringBuilder()
        
        when (printerType) {
            PrinterType.ZEBRA -> buildTSPLForZebra(tspl, bill)
            PrinterType.TSC -> buildTSPLForTSC(tspl, bill)
            else -> buildTSPLForZebra(tspl, bill)
        }
        
        val command = tspl.toString()
        debugLog("📋 TSPL Command created for $printerType")
        debugLog("Lines: ${tspl.count { it == '\n' }}, Bytes: ${command.length}")
        
        return command.toByteArray(Charsets.UTF_8)
    }
    
    private fun buildTSPLForZebra(tspl: StringBuilder, bill: Bill) {
        tspl.append("CLS\n")
        tspl.append("SIZE 57mm,100mm\n")
        tspl.append("DENSITY 12\n")
        tspl.append("SPEED 8\n")
        tspl.append("DIRECTION 0,0\n")
        tspl.append("REFERENCE 0,0\n")
        tspl.append("OFFSET 0mm\n")
        tspl.append("GAP 0mm\n")
        
        var yPos = 20
        tspl.append("TEXT 15,${yPos},\"0\",0,1,1,\"${bill.shopName}\"\n")
        yPos += 20
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${bill.shopAddress}\"\n")
        yPos += 15
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Ph: ${bill.shopPhone}\"\n")
        yPos += 15
        
        tspl.append("LINE 5,${yPos},200,${yPos},2\n")
        yPos += 10
        
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Bill #${bill.billNumber}\"\n")
        yPos += 15
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${bill.date} ${bill.time}\"\n")
        yPos += 15
        
        tspl.append("LINE 5,${yPos},200,${yPos},1\n")
        yPos += 12
        
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Item    Qty Amt\"\n")
        yPos += 15
        tspl.append("LINE 5,${yPos},200,${yPos},1\n")
        yPos += 12
        
        for (item in bill.items.take(5)) {
            val total = formatAmount(item.getTotal())
            tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${item.itemName}\"\n")
            tspl.append("TEXT 5,${yPos + 12},\"0\",0,1,1,\"x${item.quantity} @ \$${formatAmount(item.unitPrice)} = \$$total\"\n")
            yPos += 28
            
            if (item.discount > 0) {
                val discountAmt = formatAmount(item.getDiscountAmount())
                tspl.append("TEXT 10,${yPos},\"0\",0,1,1,\"Disc(${item.discount}%): -\$$discountAmt\"\n")
                yPos += 12
            }
        }
        
        tspl.append("LINE 5,${yPos},200,${yPos},2\n")
        yPos += 15
        
        val subtotal = formatAmount(bill.getSubtotal())
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Subtotal: \$${subtotal}\"\n")
        yPos += 15
        
        if (bill.getTotalDiscount() > 0) {
            val discount = formatAmount(bill.getTotalDiscount())
            tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Discount: -\$$discount\"\n")
            yPos += 15
        }
        
        val total = formatAmount(bill.getGrandTotal())
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"TOTAL: \$${total}\"\n")
        yPos += 20
        
        val barcodeData = System.currentTimeMillis().toString().takeLast(10)
        tspl.append("BARCODE 20,${yPos},\"CODE128\",40,1,0,2,2,\"$barcodeData\"\n")
        yPos += 35
        
        tspl.append("TEXT 15,${yPos},\"0\",0,1,1,\"Thank You!\"\n")
        tspl.append("PRINT 1,1\n")
    }
    
    private fun buildTSPLForTSC(tspl: StringBuilder, bill: Bill) {
        tspl.append("CLS\n")
        tspl.append("SIZE 57mm,100mm\n")
        tspl.append("DENSITY 12\n")
        tspl.append("SPEED 5\n")
        tspl.append("DIRECTION 0,0\n")
        tspl.append("REFERENCE 0,0\n")
        tspl.append("OFFSET 0mm\n")
        tspl.append("SET PEEL OFF\n")
        tspl.append("SET TEAR ON\n")
        
        var yPos = 20
        
        tspl.append("TEXT 15,${yPos},\"0\",0,1,1,\"${bill.shopName}\"\n")
        yPos += 20
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${bill.shopAddress}\"\n")
        yPos += 15
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${bill.shopPhone}\"\n")
        yPos += 15
        
        tspl.append("LINE 5,${yPos},200,${yPos},2\n")
        yPos += 10
        
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Bill #${bill.billNumber}\"\n")
        yPos += 15
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${bill.date} ${bill.time}\"\n")
        yPos += 15
        
        tspl.append("LINE 5,${yPos},200,${yPos},1\n")
        yPos += 12
        
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Item    Qty Amt\"\n")
        yPos += 15
        tspl.append("LINE 5,${yPos},200,${yPos},1\n")
        yPos += 12
        
        for (item in bill.items.take(5)) {
            val total = formatAmount(item.getTotal())
            tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"${item.itemName}\"\n")
            tspl.append("TEXT 5,${yPos + 12},\"0\",0,1,1,\"x${item.quantity} @ \$${formatAmount(item.unitPrice)} = \$$total\"\n")
            yPos += 28
            
            if (item.discount > 0) {
                val discountAmt = formatAmount(item.getDiscountAmount())
                tspl.append("TEXT 10,${yPos},\"0\",0,1,1,\"Disc(${item.discount}%): -\$$discountAmt\"\n")
                yPos += 12
            }
        }
        
        tspl.append("LINE 5,${yPos},200,${yPos},2\n")
        yPos += 15
        
        val subtotal = formatAmount(bill.getSubtotal())
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Subtotal: \$${subtotal}\"\n")
        yPos += 15
        
        if (bill.getTotalDiscount() > 0) {
            val discount = formatAmount(bill.getTotalDiscount())
            tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"Discount: -\$$discount\"\n")
            yPos += 15
        }
        
        val total = formatAmount(bill.getGrandTotal())
        tspl.append("TEXT 5,${yPos},\"0\",0,1,1,\"TOTAL: \$${total}\"\n")
        yPos += 20
        
        val barcodeData = System.currentTimeMillis().toString().takeLast(10)
        tspl.append("BARCODE 20,${yPos},\"CODE128\",40,1,0,2,2,\"$barcodeData\"\n")
        yPos += 35
        
        tspl.append("TEXT 15,${yPos},\"0\",0,1,1,\"Thank You!\"\n")
        tspl.append("PRINT 1,1\n")
    }
    
    private fun sendData(data: ByteArray) {
        if (usbConnection == null || outputEndpoint == null) {
            throw IOException("USB connection lost")
        }
        
        try {
            debugLog("📤 Sending ${data.size} bytes...")
            val bytesWritten = usbConnection!!.bulkTransfer(outputEndpoint, data, data.size, 5000)
            
            when {
                bytesWritten < 0 -> {
                    debugLog("❌ USB error code: $bytesWritten")
                    throw IOException("USB error: $bytesWritten")
                }
                bytesWritten > 0 -> {
                    debugLog("✅ Successfully sent $bytesWritten/${data.size} bytes to printer")
                    if (bytesWritten < data.size) {
                        debugLog("⚠️ Warning: Only sent $bytesWritten of ${data.size} bytes")
                    }
                }
                else -> {
                    debugLog("❌ USB timeout - printer not responding")
                    throw IOException("USB timeout - no data transmitted")
                }
            }
        } catch (e: Exception) {
            debugLog("❌ Send failed: ${e.message}")
            throw e
        }
    }
    
    fun isConnected(): Boolean {
        return printerDevice != null && usbConnection != null && outputEndpoint != null
    }
    
    fun getPrinterName(): String {
        return printerDevice?.deviceName ?: "Unknown Printer"
    }
    
    fun disconnect() {
        try {
            if (usbConnection != null && printerDevice != null) {
                val intf = claimedInterface
                if (intf != null) {
                    usbConnection!!.releaseInterface(intf)
                }
                usbConnection!!.close()
            }
            usbConnection = null
            printerDevice = null
            claimedInterface = null
            outputEndpoint = null
            inputEndpoint = null
            permissionGranted = false
            Log.d(TAG, "Disconnected from printer")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from printer", e)
        }
    }
    
    fun sendRawTSPL(tsplCommands: String): Boolean {
        if (!isConnected()) {
            connectionCallback?.onConnectionFailed("Printer not connected")
            return false
        }
        
        return try {
            debugLog("📨 Sending raw TSPL commands...")
            debugLog("Printer Type: $printerType")
            
            if (paperCheckEnabled) {
                debugLog("📋 Checking printer status before sending...")
                val status = statusChecker?.queryStatus(printerType) ?: PrinterStatusChecker.PrinterStatus.Ready
                when (status) {
                    PrinterStatusChecker.PrinterStatus.PaperOut -> {
                        debugLog("❌ Paper out! Load paper and try again")
                        connectionCallback?.onConnectionFailed("Paper out - please load paper")
                        return false
                    }
                    PrinterStatusChecker.PrinterStatus.CoverOpen -> {
                        debugLog("❌ Cover open! Close the cover and try again")
                        connectionCallback?.onConnectionFailed("Cover open - please close cover")
                        return false
                    }
                    else -> debugLog("✅ Printer status OK")
                }
            }
            
            sendWithRetry {
                prepareTSPLDocument(tsplCommands, appendPrintIfMissing = false, copies = 1)
            }
        } catch (e: Exception) {
            debugLog("❌ Error sending TSPL: ${e.message}")
            Log.e(TAG, "Error sending raw TSPL", e)
            false
        }
    }

    fun printTSPLDocument(tsplDocument: String, copies: Int = 1): Boolean {
        if (!isConnected()) {
            connectionCallback?.onConnectionFailed("Printer not connected")
            return false
        }

        if (tsplDocument.isBlank()) {
            connectionCallback?.onConnectionFailed("TSPL document is empty")
            return false
        }

        return try {
            debugLog("🖼️ Printing TSPL document in high-fidelity mode")
            debugLog("Printer Type: $printerType")

            if (paperCheckEnabled && !waitForPrinterReady(printTimeout)) {
                debugLog("❌ Printer not ready for TSPL document")
                return false
            }

            sendWithRetry {
                prepareTSPLDocument(
                    tsplDocument,
                    appendPrintIfMissing = true,
                    copies = copies.coerceIn(1, 99)
                )
            }
        } catch (e: Exception) {
            debugLog("❌ TSPL document print error: ${e.message}")
            false
        }
    }

    private fun prepareTSPLDocument(
        tsplDocument: String,
        appendPrintIfMissing: Boolean,
        copies: Int
    ): ByteArray {
        var normalized = tsplDocument
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trimEnd()

        if (appendPrintIfMissing) {
            val hasPrintCommand = Regex("(?im)^\\s*PRINT\\s+").containsMatchIn(normalized)
            if (!hasPrintCommand) {
                normalized += "\nPRINT 1,${copies.coerceIn(1, 99)}"
            }
        }

        if (!normalized.endsWith("\n")) {
            normalized += "\n"
        }

        // TSPL is a line-oriented, single-byte command language; CRLF line endings are the most compatible.
        val crlfNormalized = normalized.replace("\n", "\r\n")
        return crlfNormalized.toByteArray(Charsets.ISO_8859_1)
    }

    private fun sendDataChunked(data: ByteArray) {
        if (usbConnection == null || outputEndpoint == null) {
            throw IOException("USB connection lost")
        }

        val endpoint = outputEndpoint!!
        val maxPacket = endpoint.maxPacketSize.coerceAtLeast(64)
        val chunkSize = (maxPacket * 8).coerceAtMost(4096)
        var offset = 0
        var totalWritten = 0

        while (offset < data.size) {
            val length = minOf(chunkSize, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + length)
            val written = usbConnection!!.bulkTransfer(endpoint, chunk, chunk.size, printTimeout)

            when {
                written < 0 -> throw IOException("USB error code: $written at offset $offset")
                written == 0 -> throw IOException("USB timeout while sending data at offset $offset")
                else -> {
                    offset += written
                    totalWritten += written
                }
            }
        }

        debugLog("✅ Successfully sent $totalWritten/${data.size} bytes")
    }
    
    private fun sendWithRetry(commandBuilder: () -> ByteArray): Boolean {
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                if (!isConnected()) {
                    throw IOException("Connection lost")
                }
                
                val data = commandBuilder()
                debugLog("📤 Sending ${data.size} bytes (attempt $attempt/$maxRetries)...")
                sendDataChunked(data)
                return true
            } catch (e: Exception) {
                lastException = e
                debugLog("❌ Attempt $attempt failed: ${e.message}")
                
                if (attempt < maxRetries) {
                    debugLog("⏰ Retrying in ${retryDelayMs}ms...")
                    Thread.sleep(retryDelayMs)
                    
                    if (paperCheckEnabled && statusChecker != null) {
                        val status = statusChecker!!.queryStatus(printerType)
                        when (status) {
                            PrinterStatusChecker.PrinterStatus.PaperOut -> {
                                debugLog("📄 Paper out detected - waiting for paper reload...")
                                if (!waitForPrinterReady(15000)) {
                                    debugLog("❌ User did not load paper - aborting")
                                    return false
                                }
                            }
                            PrinterStatusChecker.PrinterStatus.CoverOpen -> {
                                debugLog("🔓 Cover open - waiting for cover to close...")
                                if (!waitForPrinterReady(10000)) {
                                    debugLog("❌ User did not close cover - aborting")
                                    return false
                                }
                            }
                            else -> { }
                        }
                    }
                }
            }
        }
        
        debugLog("❌ Failed after $maxRetries attempts${if (lastException != null) ": ${lastException.message}" else ""}")
        return false
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(usbPermissionReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "Receiver already unregistered or not registered")
        }
        disconnect()
    }
    
    fun setRetryConfiguration(maxRetries: Int = 3, retryDelayMs: Long = 2000L, printTimeoutMs: Int = 10000) {
        this.maxRetries = maxRetries.coerceIn(1, 10)
        this.retryDelayMs = retryDelayMs.coerceIn(500, 10000)
        this.printTimeout = printTimeoutMs.coerceIn(5000, 30000)
        debugLog("⚙️ Retry config: max=$maxRetries, delay=${this.retryDelayMs}ms, timeout=${this.printTimeout}ms")
    }
    
    fun setPaperCheckEnabled(enabled: Boolean) {
        this.paperCheckEnabled = enabled
        debugLog("📄 Paper check ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun getPrinterType(): PrinterType = printerType
    
    fun getPrinterInfo(): String {
        return """
            Printer Name: ${getPrinterName()}
            Type: $printerType
            Connected: ${isConnected()}
            Output Endpoint: ${if (outputEndpoint != null) "0x${outputEndpoint!!.address.toString(16)}" else "None"}
            Input Endpoint: ${if (inputEndpoint != null) "0x${inputEndpoint!!.address.toString(16)}" else "None"}
        """.trimIndent()
    }
}
