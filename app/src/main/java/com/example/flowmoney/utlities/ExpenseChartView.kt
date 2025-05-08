package com.example.flowmoney.utlities

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.flowmoney.Models.Transaction
import java.util.*
import kotlin.math.max

class ExpenseChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data periods
    enum class TimePeriod {
        DAY, WEEK, MONTH, YEAR
    }

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

    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#DDDDDD")
    }

    private val path = Path()
    private val fillPath = Path()

    // Data
    private var dataPoints = mutableListOf<Float>()
    private var labels = mutableListOf<String>()
    private var maxValue = 0f
    private var minValue = 0f
    private var highlightedIndex = -1
    private var currentPeriod = TimePeriod.MONTH
    private var currentTransactionType = "expense"

    // Callback for when a data point is selected
    var onDataPointSelected: ((index: Int, value: Float, label: String) -> Unit)? = null
    
    /**
     * Set the time period for the chart
     * 
     * @param period The time period to display (DAY, WEEK, MONTH, YEAR)
     */
    fun setTimePeriod(period: TimePeriod) {
        this.currentPeriod = period
        invalidate()
    }
    
    /**
     * Set the transaction type to filter data by
     * 
     * @param type The transaction type (expense, income, saving)
     */
    fun setTransactionType(type: String) {
        this.currentTransactionType = type.lowercase()
        invalidate()
    }

    /**
     * Update chart with transaction data
     * 
     * @param transactions List of transactions to analyze
     * @param period Time period to group by (DAY, WEEK, MONTH, YEAR)
     * @param transactionType Type of transactions to include (expense, income, saving)
     */
    fun updateWithTransactions(
        transactions: List<Transaction>,
        period: TimePeriod = TimePeriod.MONTH,
        transactionType: String = "expense"
    ) {
        // Filter transactions by type
        val filteredTransactions = transactions.filter { 
            it.type.equals(transactionType, ignoreCase = true) && !it.isDeleted
        }
        
        if (filteredTransactions.isEmpty()) {
            dataPoints = mutableListOf(0f)
            labels = mutableListOf("No data")
            maxValue = 100f
            minValue = 0f
            highlightedIndex = -1
            invalidate()
            return
        }
        
        currentPeriod = period
        currentTransactionType = transactionType
        
        // Group and aggregate transactions based on the time period
        val groupedData = when (period) {
            TimePeriod.DAY -> groupTransactionsByHour(filteredTransactions)
            TimePeriod.WEEK -> groupTransactionsByDay(filteredTransactions)
            TimePeriod.MONTH -> groupTransactionsByDay(filteredTransactions, 30)
            TimePeriod.YEAR -> groupTransactionsByMonth(filteredTransactions)
        }
        
        // Extract data points and labels
        dataPoints = groupedData.map { it.amount.toFloat() }.toMutableList()
        labels = groupedData.map { it.label }.toMutableList()
        
        // Calculate min/max values
        maxValue = dataPoints.maxOrNull() ?: 0f
        // Ensure min value is at least 10% of max to prevent flat chart
        minValue = max(0f, dataPoints.minOrNull() ?: 0f)
        
        // If max is 0, set it to 100 to have a reasonable scale
        if (maxValue == 0f) maxValue = 100f
        
        // Set highlighted index to the middle point
        highlightedIndex = if (dataPoints.isNotEmpty()) dataPoints.size / 2 else -1
        
        invalidate()
    }
    
    /**
     * Set the highlighted data point
     * 
     * @param index The index of the point to highlight
     */
    fun setHighlightedPoint(index: Int) {
        if (index in dataPoints.indices) {
            highlightedIndex = index
            
            // Notify listener
            if (dataPoints.isNotEmpty() && labels.isNotEmpty() && 
                index >= 0 && index < dataPoints.size && index < labels.size) {
                onDataPointSelected?.invoke(index, dataPoints[index], labels[index])
            }
            
            invalidate()
        }
    }

    /**
     * Draw the chart
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()

        // Calculate the segment width
        val segmentWidth = width / (dataPoints.size - 1).coerceAtLeast(1)

        // Draw grid lines (horizontal)
        drawGridLines(canvas, height)

        // Clear previous paths
        path.reset()
        fillPath.reset()

        // Draw the chart line
        drawChartLine(canvas, segmentWidth, height)
        
        // Draw the highlighted point and vertical line if needed
        if (highlightedIndex >= 0 && highlightedIndex < dataPoints.size) {
            drawHighlightedPoint(canvas, segmentWidth, height)
        }
    }
    
    /**
     * Draw horizontal grid lines
     */
    private fun drawGridLines(canvas: Canvas, height: Float) {
        // Draw 4 horizontal grid lines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = height - (height * i / gridCount)
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }
    
    /**
     * Draw the main chart line and fill
     */
    private fun drawChartLine(canvas: Canvas, segmentWidth: Float, height: Float) {
        // Only draw if we have data
        if (dataPoints.isEmpty()) return
        
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
        fillPath.lineTo(width.toFloat(), height)
        fillPath.close()

        // Draw the filled area
        canvas.drawPath(fillPath, fillPaint)

        // Draw the line
        canvas.drawPath(path, linePaint)
    }
    
    /**
     * Draw the highlighted point and its vertical line
     */
    private fun drawHighlightedPoint(canvas: Canvas, segmentWidth: Float, height: Float) {
        val highlightedX = highlightedIndex * segmentWidth
        val highlightedY = calculateYPosition(dataPoints[highlightedIndex], height)
        
        // Draw vertical dashed line
        canvas.drawLine(highlightedX, 0f, highlightedX, height, dashPaint)
        
        // Draw point circle
        canvas.drawCircle(highlightedX, highlightedY, 8f, Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#27865C")
        })
        
        canvas.drawCircle(highlightedX, highlightedY, 12f, Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.WHITE
        })
    }

    /**
     * Calculate Y position for a data point
     */
    private fun calculateYPosition(value: Float, height: Float): Float {
        val range = (maxValue - minValue).coerceAtLeast(1f)
        val valuePercentage = (value - minValue) / range

        // Invert Y coordinates (0 is at the top, height is at the bottom)
        // Leave some padding at top and bottom (10%)
        return height - valuePercentage * height * 0.8f - height * 0.1f
    }
    
    /**
     * Group transactions by hour for day view
     */
    private fun groupTransactionsByHour(transactions: List<Transaction>): List<DataPoint> {
        val calendar = Calendar.getInstance()
        val hourlyData = MutableList(24) { DataPoint("${it}:00", 0.0) }
        
        for (transaction in transactions) {
            calendar.time = transaction.getDateAsDate()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            // Add to the corresponding hour
            hourlyData[hour] = hourlyData[hour].copy(
                amount = hourlyData[hour].amount + transaction.amount
            )
        }
        
        return hourlyData
    }
    
    /**
     * Group transactions by day for week/month view
     */
    private fun groupTransactionsByDay(
        transactions: List<Transaction>, 
        daysToInclude: Int = 7
    ): List<DataPoint> {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        
        // Initialize with last X days
        val dailyData = mutableListOf<DataPoint>()
        
        // Create entries for each day
        for (i in daysToInclude - 1 downTo 0) {
            val day = Calendar.getInstance()
            day.add(Calendar.DAY_OF_MONTH, -i)
            
            val dayFormat = if (daysToInclude <= 7) {
                // For week view, use day names (Mon, Tue, etc)
                val dayOfWeek = day.get(Calendar.DAY_OF_WEEK)
                when (dayOfWeek) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> "???"
                }
            } else {
                // For month view, use day numbers
                day.get(Calendar.DAY_OF_MONTH).toString()
            }
            
            dailyData.add(DataPoint(dayFormat, 0.0))
        }
        
        // Sum transaction amounts by day
        for (transaction in transactions) {
            calendar.time = transaction.getDateAsDate()
            
            // Skip if transaction is older than our range
            val daysDiff = (today.timeInMillis - calendar.timeInMillis) / (24 * 60 * 60 * 1000)
            if (daysDiff < 0 || daysDiff >= daysToInclude) continue
            
            // Calculate index in our data array (newest day is at the end)
            val index = daysToInclude - 1 - daysDiff.toInt()
            
            if (index >= 0 && index < dailyData.size) {
                dailyData[index] = dailyData[index].copy(
                    amount = dailyData[index].amount + transaction.amount
                )
            }
        }
        
        return dailyData
    }
    
    /**
     * Group transactions by month for year view
     */
    private fun groupTransactionsByMonth(transactions: List<Transaction>): List<DataPoint> {
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        // Initialize with 12 months
        val monthlyData = MutableList(12) { i -> DataPoint(monthNames[i], 0.0) }
        
        for (transaction in transactions) {
            calendar.time = transaction.getDateAsDate()
            val month = calendar.get(Calendar.MONTH)
            
            // Add to the corresponding month
            monthlyData[month] = monthlyData[month].copy(
                amount = monthlyData[month].amount + transaction.amount
            )
        }
        
        return monthlyData
    }
    
    /**
     * Simple data class to hold aggregated transaction data
     */
    data class DataPoint(val label: String, val amount: Double)
}