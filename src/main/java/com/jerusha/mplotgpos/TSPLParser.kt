package com.jerusha.mplotgpos

import android.util.Log

/**
 * Parses and cleans TSPL commands, especially from BarTender XPML exports.
 * Handles multi-up label layout calculations and preview rendering.
 */
class TSPLParser {

    companion object {
        private const val TAG = "TSPLParser"

        /**
         * Clean BarTender XPML+TSPL output.
         * Removes XML tags, extracts pure TSPL commands.
         */
        fun cleanXPMLDocument(xpmlDocument: String): String {
            var cleaned = xpmlDocument
            
            Log.d(TAG, "📖 Original length: ${cleaned.length} chars")
            
            // Remove all XML tags (anything between < and >)
            cleaned = cleaned.replace(Regex("<[^>]*>"), "")
            Log.d(TAG, "✂️ After removing XML: ${cleaned.length} chars")
            
            // Normalize line endings
            cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n")
            
            // Split by line and filter
            val lines = cleaned.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            Log.d(TAG, "✅ Found ${lines.size} non-empty lines")
            
            // Rebuild valid TSPL
            val tspl = StringBuilder()
            var bitmapSkipped = 0
            for (line in lines) {
                // Skip binary data (BITMAP lines with garbled chars)
                if (line.startsWith("BITMAP", ignoreCase = true)) {
                    Log.d(TAG, "⏭️ Skipping BITMAP line (raster data): ${line.take(50)}...")
                    bitmapSkipped++
                    continue
                }
                tspl.append(line).append("\n")
            }
            
            if (bitmapSkipped > 0) {
                Log.d(TAG, "⏭️ Total BITMAP lines skipped: $bitmapSkipped")
            }
            
            val result = tspl.toString().trimEnd()
            Log.d(TAG, "🎯 Final TSPL: ${result.length} chars")
            return result
        }

        /**
         * Extract label size and structure from TSPL.
         * Returns (width_mm, height_mm, is_multi_up)
         */
        fun parseLabelSize(tspl: String): Triple<Float, Float, Boolean> {
            val sizeRegex = Regex("SIZE\\s+([\\d.]+)\\s*mm\\s*,\\s*([\\d.]+)\\s*mm", RegexOption.IGNORE_CASE)
            val match = sizeRegex.find(tspl)
            
            return if (match != null) {
                val width = match.groupValues[1].toFloatOrNull() ?: 57f
                val height = match.groupValues[2].toFloatOrNull() ?: 100f
                
                // Check if it looks like multi-up (very wide or multiple text/barcode objects)
                val textCount = Regex("TEXT|BARCODE", RegexOption.IGNORE_CASE).findAll(tspl).count()
                val isMultiUp = textCount > 5 && width > 100f
                
                Triple(width, height, isMultiUp)
            } else {
                Triple(57f, 100f, false)
            }
        }

        /**
         * Adjust TSPL for multi-up layout selection.
         * duplicates = 1 (1-up), 2 (2-up), 3 (3-up), etc.
         */
        fun adjustForMultiUp(tspl: String, duplicates: Int): String {
            if (duplicates <= 1) return tspl
            
            Log.d(TAG, "🔄 Adjusting for $duplicates-up layout...")
            
            // Parse current SIZE
            val sizeRegex = Regex("SIZE\\s+([\\d.]+)\\s*mm\\s*,\\s*([\\d.]+)\\s*mm", RegexOption.IGNORE_CASE)
            val sizeMatch = sizeRegex.find(tspl)
            
            if (sizeMatch == null) {
                Log.d(TAG, "❌ Could not parse SIZE - returning unmodified")
                return tspl
            }
            
            val width = sizeMatch.groupValues[1].toFloatOrNull() ?: 57f
            val height = sizeMatch.groupValues[2].toFloatOrNull() ?: 100f
            
            Log.d(TAG, "📏 Original: ${width}mm × ${height}mm")
            
            // Adjust SIZE based on duplicates
            var adjusted = tspl
            val newWidth = when (duplicates) {
                2 -> width * 2
                3 -> width * 3
                4 -> width * 4
                else -> width * duplicates
            }
            
            adjusted = adjusted.replaceFirst(sizeRegex, "SIZE $newWidth mm,$height mm")
            Log.d(TAG, "📐 New SIZE: ${newWidth}mm × ${height}mm")
            
            // Duplicate all drawing commands with X offset
            val commands = adjusted.split("\n").toMutableList()
            val result = StringBuilder()
            val xOffset = width.toInt()
            
            Log.d(TAG, "🔢 X offset per copy: $xOffset dots")
            
            var duplicatedCount = 0
            for (cmd in commands) {
                result.append(cmd).append("\n")
                
                // Check if command should be duplicated
                val cmdUpper = cmd.uppercase()
                val shouldDuplicate = (cmdUpper.startsWith("TEXT") || 
                                    cmdUpper.startsWith("BARCODE") ||
                                    cmdUpper.startsWith("BAR") ||
                                    cmdUpper.startsWith("BOX") ||
                                    cmdUpper.startsWith("LINE") ||
                                    cmdUpper.startsWith("DIAGONAL"))
                
                if (shouldDuplicate) {
                    // Create copies with X offset
                    for (dup in 1 until duplicates) {
                        val offsetCmd = offsetTSPLCommand(cmd, xOffset * dup)
                        if (offsetCmd != cmd) {
                            result.append(offsetCmd).append("\n")
                            duplicatedCount++
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ Created $duplicatedCount offset copies")
            return result.toString().trimEnd()
        }

        /**
         * Offset a TSPL command's X coordinate(s). Handles all drawing commands.
         */
        private fun offsetTSPLCommand(cmd: String, xOffset: Int): String {
            val trimmed = cmd.trim()
            if (trimmed.isEmpty()) return cmd
            
            // TEXT x,y,"font",rotation,xmul,ymul,"text"
            val textRegex = Regex("^TEXT\\s+(\\d+),(\\d+),(.*)", RegexOption.IGNORE_CASE)
            val textMatch = textRegex.find(trimmed)
            if (textMatch != null) {
                val x = textMatch.groupValues[1].toIntOrNull() ?: 0
                val y = textMatch.groupValues[2]
                val rest = textMatch.groupValues[3]
                return "TEXT ${x + xOffset},$y,$rest"
            }
            
            // BARCODE x,y,type,height,...
            val barcodeRegex = Regex("^BARCODE\\s+(\\d+),(\\d+),(.*)", RegexOption.IGNORE_CASE)
            val barcodeMatch = barcodeRegex.find(trimmed)
            if (barcodeMatch != null) {
                val x = barcodeMatch.groupValues[1].toIntOrNull() ?: 0
                val y = barcodeMatch.groupValues[2]
                val rest = barcodeMatch.groupValues[3]
                return "BARCODE ${x + xOffset},$y,$rest"
            }
            
            // DIAGONAL x1,y1,x2,y2,thickness (offset both X coords)
            val diagRegex = Regex("^DIAGONAL\\s+(\\d+),(\\d+),(\\d+),(\\d+),(.*)", RegexOption.IGNORE_CASE)
            val diagMatch = diagRegex.find(trimmed)
            if (diagMatch != null) {
                val x1 = diagMatch.groupValues[1].toIntOrNull() ?: 0
                val y1 = diagMatch.groupValues[2]
                val x2 = diagMatch.groupValues[3].toIntOrNull() ?: 0
                val y2 = diagMatch.groupValues[4]
                val rest = diagMatch.groupValues[5]
                return "DIAGONAL ${x1 + xOffset},$y1,${x2 + xOffset},$y2,$rest"
            }
            
            // BAR x,y,width,height
            val barRegex = Regex("^BAR\\s+(\\d+),(\\d+),(.*)", RegexOption.IGNORE_CASE)
            val barMatch = barRegex.find(trimmed)
            if (barMatch != null) {
                val x = barMatch.groupValues[1].toIntOrNull() ?: 0
                val y = barMatch.groupValues[2]
                val rest = barMatch.groupValues[3]
                return "BAR ${x + xOffset},$y,$rest"
            }
            
            // BOX x1,y1,x2,y2,thickness (offset both X coords)
            val boxRegex = Regex("^BOX\\s+(\\d+),(\\d+),(\\d+),(\\d+),(.*)", RegexOption.IGNORE_CASE)
            val boxMatch = boxRegex.find(trimmed)
            if (boxMatch != null) {
                val x1 = boxMatch.groupValues[1].toIntOrNull() ?: 0
                val y1 = boxMatch.groupValues[2]
                val x2 = boxMatch.groupValues[3].toIntOrNull() ?: 0
                val y2 = boxMatch.groupValues[4]
                val rest = boxMatch.groupValues[5]
                return "BOX ${x1 + xOffset},$y1,${x2 + xOffset},$y2,$rest"
            }
            
            // LINE x1,y1,x2,y2,thickness (offset both X coords)
            val lineRegex = Regex("^LINE\\s+(\\d+),(\\d+),(\\d+),(\\d+),(.*)", RegexOption.IGNORE_CASE)
            val lineMatch = lineRegex.find(trimmed)
            if (lineMatch != null) {
                val x1 = lineMatch.groupValues[1].toIntOrNull() ?: 0
                val y1 = lineMatch.groupValues[2]
                val x2 = lineMatch.groupValues[3].toIntOrNull() ?: 0
                val y2 = lineMatch.groupValues[4]
                val rest = lineMatch.groupValues[5]
                return "LINE ${x1 + xOffset},$y1,${x2 + xOffset},$y2,$rest"
            }
            
            return cmd
        }
    }
}

/**
 * Data class for TSPL label info
 */
data class TSPLLabelInfo(
    val widthMm: Float,
    val heightMm: Float,
    val commands: List<String> = emptyList(),
    val isMultiUp: Boolean = false,
    val density: Int = 12,
    val speed: Int = 8
)
