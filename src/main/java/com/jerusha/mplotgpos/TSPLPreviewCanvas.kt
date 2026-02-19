package com.jerusha.mplotgpos

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Renders a TSPL label visually so users can see what will print.
 */
class TSPLPreviewCanvas(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var tsplCommands: String = ""
    private var labelInfo: TSPLLabelInfo? = null
    
    private val paintBlack = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        typeface = Typeface.MONOSPACE
    }
    
    private val paintBorder = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    fun setTSPLContent(tsplContent: String) {
        this.tsplCommands = tsplContent
        parseTSPL()
        invalidate()
    }

    private fun parseTSPL() {
        val lines = tsplCommands.split("\n")
        val commandList = mutableListOf<String>()
        var width = 57f
        var height = 100f
        var density = 12
        var speed = 8

        for (line in lines) {
            val trimmed = line.trim()
            
            // Parse SIZE
            val sizeRegex = Regex("SIZE\\s+([\\d.]+)\\s*mm\\s*,\\s*([\\d.]+)\\s*mm", RegexOption.IGNORE_CASE)
            val sizeMatch = sizeRegex.find(trimmed)
            if (sizeMatch != null) {
                width = sizeMatch.groupValues[1].toFloatOrNull() ?: 57f
                height = sizeMatch.groupValues[2].toFloatOrNull() ?: 100f
            }
            
            // Parse DENSITY
            val densRegex = Regex("DENSITY\\s+([\\d]+)", RegexOption.IGNORE_CASE)
            val densMatch = densRegex.find(trimmed)
            if (densMatch != null) {
                density = densMatch.groupValues[1].toIntOrNull() ?: 12
            }
            
            // Parse SPEED
            val speedRegex = Regex("SPEED\\s+([\\d]+)", RegexOption.IGNORE_CASE)
            val speedMatch = speedRegex.find(trimmed)
            if (speedMatch != null) {
                speed = speedMatch.groupValues[1].toIntOrNull() ?: 8
            }
            
            // Collect drawing commands
            if (trimmed.startsWith("TEXT", ignoreCase = true) ||
                trimmed.startsWith("BARCODE", ignoreCase = true) ||
                trimmed.startsWith("BAR", ignoreCase = true) ||
                trimmed.startsWith("BOX", ignoreCase = true) ||
                trimmed.startsWith("LINE", ignoreCase = true) ||
                trimmed.startsWith("DIAGONAL", ignoreCase = true)) {
                commandList.add(trimmed)
            }
        }

        labelInfo = TSPLLabelInfo(
            widthMm = width,
            heightMm = height,
            commands = commandList,
            density = density,
            speed = speed
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (labelInfo == null) {
            canvas.drawText("No TSPL content", 10f, 50f, paintText)
            return
        }

        val info = labelInfo!!
        
        // Calculate scaling: fit label to view
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        val scaleX = (viewWidth - 20) / (info.widthMm * 3.78f)  // mm to pixels (~3.78 px/mm at 96dpi)
        val scaleY = (viewHeight - 40) / (info.heightMm * 3.78f)
        val scale = min(scaleX, scaleY).coerceAtLeast(0.5f)
        
        val offsetX = 10f
        val offsetY = 30f
        
        // Draw label border
        val labelPixelWidth = (info.widthMm * 3.78f * scale).toInt()
        val labelPixelHeight = (info.heightMm * 3.78f * scale).toInt()
        canvas.drawRect(offsetX, offsetY, offsetX + labelPixelWidth, offsetY + labelPixelHeight, paintBorder)
        
        // Draw grid for reference
        for (i in 0..10) {
            val x = offsetX + labelPixelWidth * i / 10
            canvas.drawLine(x, offsetY, x, offsetY + labelPixelHeight, paintBorder)
        }
        
        // Render commands
        for (cmd in info.commands) {
            renderCommand(canvas, cmd, offsetX, offsetY, scale)
        }
        
        // Draw label info
        val infoText = "Label: ${info.widthMm}mm × ${info.heightMm}mm | Density: ${info.density} | Speed: ${info.speed}"
        canvas.drawText(infoText, 10f, height - 5f, paintText.apply { textSize = 9f })
    }

    private fun renderCommand(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        when {
            cmd.startsWith("TEXT", ignoreCase = true) -> renderText(canvas, cmd, offsetX, offsetY, scale)
            cmd.startsWith("BARCODE", ignoreCase = true) -> renderBarcode(canvas, cmd, offsetX, offsetY, scale)
            cmd.startsWith("BAR", ignoreCase = true) -> renderBar(canvas, cmd, offsetX, offsetY, scale)
            cmd.startsWith("BOX", ignoreCase = true) -> renderBox(canvas, cmd, offsetX, offsetY, scale)
            cmd.startsWith("LINE", ignoreCase = true) -> renderLine(canvas, cmd, offsetX, offsetY, scale)
            cmd.startsWith("DIAGONAL", ignoreCase = true) -> renderDiagonal(canvas, cmd, offsetX, offsetY, scale)
        }
    }

    private fun renderText(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        // TEXT x,y,"font",rotation,xmul,ymul,"text"
        try {
            val parts = cmd.split(",").take(2)
            if (parts.size < 2) return
            
            val x = parts[0].replace("TEXT", "").trim().toIntOrNull() ?: return
            val y = parts[1].toIntOrNull() ?: return
            
            val pixelX = offsetX + x * scale
            val pixelY = offsetY + y * scale
            
            // Extract text from quotes
            val textMatch = Regex("\"([^\"]+)\"").findAll(cmd).lastOrNull()
            val text = textMatch?.groupValues?.get(1) ?: "?"
            
            paintText.textSize = (10f * scale).coerceAtLeast(6f)
            canvas.drawText(text, pixelX, pixelY + 4, paintText)
        } catch (e: Exception) {
            // silently skip malformed
        }
    }

    private fun renderBarcode(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        // BARCODE x,y,"type",height,...,"data"
        try {
            val parts = cmd.split(",").take(3)
            if (parts.size < 3) return
            
            val x = parts[0].replace("BARCODE", "").trim().toIntOrNull() ?: return
            val y = parts[1].toIntOrNull() ?: return
            
            val pixelX = offsetX + x * scale
            val pixelY = offsetY + y * scale
            
            // Draw barcode as simple rectangle with text
            val width = (80f * scale).coerceAtLeast(30f)
            val height = (40f * scale).coerceAtLeast(20f)
            
            canvas.drawRect(pixelX, pixelY, pixelX + width, pixelY + height, paintBlack)
            paintText.textSize = (8f * scale).coerceAtLeast(6f)
            canvas.drawText("|||||||", pixelX + 5, pixelY + height / 2 + 3, paintText)
        } catch (e: Exception) {
            // silently skip
        }
    }

    private fun renderBar(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        // BAR x,y,width,height
        try {
            val parts = cmd.split(",").take(4)
            if (parts.size < 4) return
            
            val x = parts[0].replace("BAR", "").trim().toIntOrNull() ?: return
            val y = parts[1].toIntOrNull() ?: return
            val barWidth = parts[2].toIntOrNull() ?: 10
            val barHeight = parts[3].toIntOrNull() ?: 10
            
            val pixelX = offsetX + x * scale
            val pixelY = offsetY + y * scale
            val pixelW = (barWidth * scale).coerceAtLeast(1f)
            val pixelH = (barHeight * scale).coerceAtLeast(1f)
            
            canvas.drawRect(pixelX, pixelY, pixelX + pixelW, pixelY + pixelH, paintBlack)
        } catch (e: Exception) {
            // silently skip
        }
    }

    private fun renderBox(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        // BOX x1,y1,x2,y2,thickness
        try {
            val parts = cmd.split(",").take(4)
            if (parts.size < 4) return
            
            val x1 = parts[0].replace("BOX", "").trim().toIntOrNull() ?: return
            val y1 = parts[1].toIntOrNull() ?: return
            val x2 = parts[2].toIntOrNull() ?: return
            val y2 = parts[3].toIntOrNull() ?: return
            
            val pixel1X = offsetX + x1 * scale
            val pixel1Y = offsetY + y1 * scale
            val pixel2X = offsetX + x2 * scale
            val pixel2Y = offsetY + y2 * scale
            
            canvas.drawRect(pixel1X, pixel1Y, pixel2X, pixel2Y, paintBlack)
        } catch (e: Exception) {
            // silently skip
        }
    }

    private fun renderLine(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        // LINE x1,y1,x2,y2,thickness
        try {
            val parts = cmd.split(",").take(4)
            if (parts.size < 4) return
            
            val x1 = parts[0].replace("LINE", "").trim().toIntOrNull() ?: return
            val y1 = parts[1].toIntOrNull() ?: return
            val x2 = parts[2].toIntOrNull() ?: return
            val y2 = parts[3].toIntOrNull() ?: return
            
            val pixel1X = offsetX + x1 * scale
            val pixel1Y = offsetY + y1 * scale
            val pixel2X = offsetX + x2 * scale
            val pixel2Y = offsetY + y2 * scale
            
            canvas.drawLine(pixel1X, pixel1Y, pixel2X, pixel2Y, paintBlack)
        } catch (e: Exception) {
            // silently skip
        }
    }

    private fun renderDiagonal(canvas: Canvas, cmd: String, offsetX: Float, offsetY: Float, scale: Float) {
        // DIAGONAL x1,y1,x2,y2,thickness
        renderLine(canvas, cmd.replace("DIAGONAL", "LINE"), offsetX, offsetY, scale)
    }
}
