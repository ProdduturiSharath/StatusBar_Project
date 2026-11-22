package com.sharath.statusbartotal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PieSlice(val value: Long, val color: Color)

/** Donut chart of positive [slices]; [centerText] is shown in the middle. */
@Composable
fun PieChart(slices: List<PieSlice>, centerText: String, modifier: Modifier = Modifier) {
    val sum = slices.sumOf { it.value }.toFloat()
    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val stroke = 28.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            for (s in slices) {
                val sweep = if (sum > 0f) 360f * s.value / sum else 0f
                drawArc(
                    color = s.color, startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }
        Text(
            centerText, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
