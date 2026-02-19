package com.jerusha.mplotgpos

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.util.Log
import java.io.IOException

/**
 * Monitors printer status in real-time to detect:
 * - Paper out
 * - Cover open
 * - Over temperature
 * - Offline status
 * - Head temperature
 */
class PrinterStatusChecker(
    private val connection: UsbDeviceConnection,
    private val inputEndpoint: UsbEndpoint?,
    private val outputEndpoint: UsbEndpoint?
) {
    
    companion object {
        private const val TAG = "PrinterStatusChecker"
        
        // Status bytes for different printer types
        private const val STATUS_PAPER_OUT = 0x20
        private const val STATUS_COVER_OPEN = 0x04
        private const val STATUS_OFFLINE = 0x08
        private const val STATUS_HEAD_TEMP = 0x02
    }
    
    sealed class PrinterStatus {
        object Ready : PrinterStatus()
        object PaperOut : PrinterStatus()
        object CoverOpen : PrinterStatus()
        object Offline : PrinterStatus()
        object OverTemperature : PrinterStatus()
        object Unknown : PrinterStatus()
        data class Error(val errorCode: Int) : PrinterStatus()
    }
    
    /**
     * Query printer status using TSPL command
     * Different printers have different status query commands
     */
    fun queryStatus(printerType: PrinterType = PrinterType.ZEBRA): PrinterStatus {
        return try {
            val statusCommand = buildStatusQuery(printerType)
            sendStatusCommand(statusCommand)
            readStatusResponse(printerType)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying printer status: ${e.message}")
            PrinterStatus.Unknown
        }
    }
    
    /**
     * Send status query command to printer
     */
    private fun sendStatusCommand(command: ByteArray) {
        if (outputEndpoint == null) {
            throw IOException("Output endpoint not available")
        }
        
        val bytesWritten = connection.bulkTransfer(outputEndpoint, command, command.size, 2000)
        if (bytesWritten <= 0) {
            throw IOException("Failed to send status query: wrote $bytesWritten bytes")
        }
    }
    
    /**
     * Read status response from printer
     */
    private fun readStatusResponse(printerType: PrinterType): PrinterStatus {
        if (inputEndpoint == null) {
            return PrinterStatus.Unknown
        }
        
        return try {
            val buffer = ByteArray(32)
            val bytesRead = connection.bulkTransfer(inputEndpoint, buffer, buffer.size, 2000)
            
            if (bytesRead <= 0) {
                PrinterStatus.Ready  // Assume ready if no response
            } else {
                parseStatusResponse(buffer, bytesRead, printerType)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read status response: ${e.message}")
            PrinterStatus.Ready  // Assume ready if read fails
        }
    }
    
    /**
     * Parse status response based on printer type
     */
    private fun parseStatusResponse(
        buffer: ByteArray,
        length: Int,
        printerType: PrinterType
    ): PrinterStatus {
        if (length == 0) return PrinterStatus.Ready
        
        val statusByte = buffer[0].toInt()
        
        return when {
            statusByte and STATUS_PAPER_OUT != 0 -> PrinterStatus.PaperOut
            statusByte and STATUS_COVER_OPEN != 0 -> PrinterStatus.CoverOpen
            statusByte and STATUS_OFFLINE != 0 -> PrinterStatus.Offline
            statusByte and STATUS_HEAD_TEMP != 0 -> PrinterStatus.OverTemperature
            statusByte == 0 -> PrinterStatus.Ready
            else -> {
                Log.d(TAG, "Unknown status byte: 0x${statusByte.toString(16)}")
                PrinterStatus.Error(statusByte)
            }
        }
    }
    
    /**
     * Build status query command based on printer type
     */
    private fun buildStatusQuery(printerType: PrinterType): ByteArray {
        return when (printerType) {
            PrinterType.ZEBRA -> "QUERY STATUS\n".toByteArray()
            PrinterType.TSC -> "QUERY STATUS\n".toByteArray()
            PrinterType.STAR_MICRONICS -> "\u001d\u0027".toByteArray()  // GS '
            PrinterType.CUSTOM -> "QUERY STATUS\n".toByteArray()
        }
    }
}

enum class PrinterType {
    ZEBRA,      // Zebra PE200, GK420d
    TSC,        // TSC TH240, TE310
    STAR_MICRONICS,  // Star printers
    CUSTOM
}

/**
 * Detects printer type from USB device info
 */
object PrinterDetector {
    
    fun detectPrinterType(
        vendorId: Int,
        productId: Int,
        productName: String?,
        manufacturerName: String?
    ): PrinterType {
        val name = "${manufacturerName ?: ""} ${productName ?: ""}".lowercase()
        
        return when {
            // Zebra printers
            name.contains("zebra") || vendorId == 0x0A5F -> PrinterType.ZEBRA
            // TSC printers  
            name.contains("tsc") || vendorId == 0x0B48 -> PrinterType.TSC
            // Star Micronics
            name.contains("star") || vendorId == 0x0B8B -> PrinterType.STAR_MICRONICS
            else -> PrinterType.CUSTOM
        }
    }
}
