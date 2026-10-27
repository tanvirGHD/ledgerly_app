package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.CategoryBreakdown
import com.example.myapplication.model.MonthStat
import com.example.myapplication.model.SeriesPoint
import kotlin.math.max

val chartColors = listOf(
    Color(0xFF14B8A6), // Teal
    Color(0xFF3B82F6), // Blue
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFF10B981), // Green
    Color(0xFFEF4444)  // Red
)

// 1. Line Chart for Income vs Expense
@Composable
fun LineChart(
    data: List<MonthStat>,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF10B981),
    expenseColor: Color = Color(0xFFEF4444)
) {
    if (data.isEmpty()) return

    val maxVal = max(1.0, data.maxOf { max(it.income, it.expense) }) * 1.15

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = size.width
            val height = size.height
            val padding = 40f
            val chartWidth = width - (padding * 2)
            val chartHeight = height - (padding * 2)

            val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

            // Draw horizontal background lines
            for (i in 0..3) {
                val y = padding + (chartHeight / 3) * i
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 2f
                )
            }

            val incomePath = Path()
            val expensePath = Path()
            val incomeFillPath = Path()
            val expenseFillPath = Path()

            data.forEachIndexed { index, stat ->
                val x = padding + (index * stepX)
                val yInc = height - padding - ((stat.income / maxVal) * chartHeight).toFloat()
                val yExp = height - padding - ((stat.expense / maxVal) * chartHeight).toFloat()

                if (index == 0) {
                    incomePath.moveTo(x, yInc)
                    expensePath.moveTo(x, yExp)
                    incomeFillPath.moveTo(x, height - padding)
                    incomeFillPath.lineTo(x, yInc)
                    expenseFillPath.moveTo(x, height - padding)
                    expenseFillPath.lineTo(x, yExp)
                } else {
                    incomePath.lineTo(x, yInc)
                    expensePath.lineTo(x, yExp)
                    incomeFillPath.lineTo(x, yInc)
                    expenseFillPath.lineTo(x, yExp)
                }

                if (index == data.size - 1) {
                    incomeFillPath.lineTo(x, height - padding)
                    expenseFillPath.lineTo(x, height - padding)
                }
            }

            // Draw area fills
            drawPath(path = incomeFillPath, color = incomeColor.copy(alpha = 0.15f))
            drawPath(path = expenseFillPath, color = expenseColor.copy(alpha = 0.15f))

            // Draw line strokes
            drawPath(path = incomePath, color = incomeColor, style = Stroke(width = 4f))
            drawPath(path = expensePath, color = expenseColor, style = Stroke(width = 4f))

            // Draw data points
            data.forEachIndexed { index, stat ->
                val x = padding + (index * stepX)
                val yInc = height - padding - ((stat.income / maxVal) * chartHeight).toFloat()
                val yExp = height - padding - ((stat.expense / maxVal) * chartHeight).toFloat()

                drawCircle(color = incomeColor, radius = 6f, center = Offset(x, yInc))
                drawCircle(color = Color.White, radius = 3f, center = Offset(x, yInc))

                drawCircle(color = expenseColor, radius = 6f, center = Offset(x, yExp))
                drawCircle(color = Color.White, radius = 3f, center = Offset(x, yExp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { stat ->
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 2. Bar Chart for Reports
@Composable
fun BarChart(
    data: List<SeriesPoint>,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF10B981),
    expenseColor: Color = Color(0xFFEF4444)
) {
    if (data.isEmpty()) return

    val maxVal = max(1.0, data.maxOf { max(it.income, it.expense) })

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height
            val padding = 30f
            val chartWidth = width - (padding * 2)
            val chartHeight = height - (padding * 2)

            val groupWidth = chartWidth / data.size
            val barWidth = (groupWidth / 3).coerceAtMost(30f)

            data.forEachIndexed { index, point ->
                val groupX = padding + (index * groupWidth) + (groupWidth / 6)

                val incH = ((point.income / maxVal) * chartHeight).toFloat()
                val expH = ((point.expense / maxVal) * chartHeight).toFloat()

                // Income bar
                drawRect(
                    color = incomeColor,
                    topLeft = Offset(groupX, height - padding - incH),
                    size = Size(barWidth, incH)
                )

                // Expense bar
                drawRect(
                    color = expenseColor,
                    topLeft = Offset(groupX + barWidth + 4f, height - padding - expH),
                    size = Size(barWidth, expH)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.take(8).forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 3. Donut / Pie Chart for Expense Breakdown
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChart(
    data: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text(
            text = "No expenses recorded for this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val totalSum = data.sumOf { it.total }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp)
        ) {
            var startAngle = -90f
            val strokeWidth = 36f

            data.forEachIndexed { index, item ->
                val sweepAngle = ((item.total / totalSum) * 360f).toFloat()
                val color = chartColors[index % chartColors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, item ->
                val color = chartColors[index % chartColors.size]
                val pct = if (totalSum > 0) (item.total / totalSum * 100).toInt() else 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.category} ($pct%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
