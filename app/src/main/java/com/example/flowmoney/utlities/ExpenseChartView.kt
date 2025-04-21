package com.example.flowmoney.utlities

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.flowmoney.R
import kotlin.math.max

class ExpenseChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects
    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#27865C")
    }

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#1A27865C")
    }

    private val dashPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#AAAAAA")
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private val path = Path()
    private val fillPath = Path()

    // Data points (expenses for each month)
    private val dataPoints = listOf(850f, 1050f, 1230f, 1600f, 1400f, 1500f)
    private val maxValue = dataPoints.maxOrNull() ?: 0f
    private val minValue = max(0f, dataPoints.minOrNull() ?: 0f)

    // Highlighted point index (index 2 corresponds to May)
    private val highlightedIndex = 2

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()

        // Calculate the segment width
        val segmentWidth = width / (dataPoints.size - 1)

        // Clear previous paths
        path.reset()
        fillPath.reset()

        // Starting point for the path
        val startX = 0f
        val startY = calculateYPosition(dataPoints[0], height)
        path.moveTo(startX, startY)
        fillPath.moveTo(startX, height)
        fillPath.lineTo(startX, startY)

        // Draw curve through data points
        for (i in 1 until dataPoints.size) {
            val x = i * segmentWidth
            val y = calculateYPosition(dataPoints[i], height)

            // Calculate control points for cubic curve
            val previousX = (i - 1) * segmentWidth
            val previousY = calculateYPosition(dataPoints[i - 1], height)
            val controlX1 = previousX + segmentWidth / 3
            val controlY1 = previousY
            val controlX2 = x - segmentWidth / 3
            val controlY2 = y

            // Add cubic curve to path
            path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
        }

        // Complete the fill path
        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw the filled area
        canvas.drawPath(fillPath, fillPaint)

        // Draw the line
        canvas.drawPath(path, linePaint)

        // Draw vertical dashed line at highlighted point
        val highlightedX = highlightedIndex * segmentWidth
        canvas.drawLine(highlightedX, 0f, highlightedX, height, dashPaint)
    }

    // Calculate Y position for a data point
    private fun calculateYPosition(value: Float, height: Float): Float {
        val range = maxValue - minValue
        val valuePercentage = if (range > 0) (value - minValue) / range else 0.5f

        // Invert Y coordinates (0 is at the top, height is at the bottom)
        return height - valuePercentage * height * 0.8f - height * 0.1f
    }
}