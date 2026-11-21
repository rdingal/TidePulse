package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TideEventType
import com.example.data.model.TideExtremum
import com.example.data.model.TidePoint
import com.example.data.model.TideUnit
import com.example.ui.theme.LocalTideExtendedColors
import java.util.TimeZone
import kotlin.math.abs

@Composable
fun TideWaveChart(
    curvePoints: List<TidePoint>,
    extrema: List<TideExtremum>,
    currentTimeMillis: Long,
    unit: TideUnit,
    windowHours: Int,
    timeZone: TimeZone,
    modifier: Modifier = Modifier,
) {
    val extendedColors = LocalTideExtendedColors.current
    val textMeasurer = rememberTextMeasurer()

    val filteredPoints = remember(curvePoints, currentTimeMillis, windowHours) {
        if (curvePoints.isEmpty()) emptyList()
        else {
            val endMillis = currentTimeMillis + windowHours * 3600_000L
            val slice = curvePoints.filter { it.timestampEpochMillis <= endMillis }
            if (slice.size >= 4) slice else curvePoints
        }
    }

    var scrubbedIndex by remember(filteredPoints) { mutableStateOf<Int?>(null) }

    if (filteredPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Tide curve data unavailable for this window",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val minTime = filteredPoints.first().timestampEpochMillis
    val maxTime = filteredPoints.last().timestampEpochMillis
    val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

    val minHeightRaw = filteredPoints.minOf { it.heightMeters }
    val maxHeightRaw = filteredPoints.maxOf { it.heightMeters }
    val paddingMeters = ((maxHeightRaw - minHeightRaw) * 0.20).coerceAtLeast(0.25)
    val minY = minHeightRaw - paddingMeters
    val maxY = maxHeightRaw + paddingMeters
    val heightSpan = (maxY - minY).coerceAtLeast(0.1).toFloat()

    val visibleExtrema = remember(extrema, minTime, maxTime) {
        extrema.filter { it.timestampEpochMillis in minTime..maxTime }
    }

    val axisLabelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
    )
    val highCalloutStyle = TextStyle(
        color = extendedColors.highTideBadge,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 11.sp,
    )
    val lowCalloutStyle = TextStyle(
        color = extendedColors.lowTideBadge,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 11.sp,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val activePoint = scrubbedIndex?.let { filteredPoints.getOrNull(it) }
            this@Column.AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                activePoint?.let { pt ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = TideFormatters.formatTimeInZone(pt.timestampEpochMillis, timeZone),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = TideFormatters.formatDateInZone(pt.timestampEpochMillis, timeZone),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = TideFormatters.formatHeight(pt.heightMeters, unit),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (activePoint == null) {
                Text(
                    text = "Next ${windowHours}h prediction • Drag or tap wave to inspect any hour",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(268.dp)
                .testTag("tide_wave_canvas")
                .pointerInput(filteredPoints) {
                    detectTapGestures { offset ->
                        val leftPad = 42.dp.toPx()
                        val rightPad = 14.dp.toPx()
                        val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        val fraction = ((offset.x - leftPad) / chartW).coerceIn(0f, 1f)
                        val targetTime = minTime + (fraction * timeSpan).toLong()
                        scrubbedIndex = filteredPoints.indices.minByOrNull { idx ->
                            abs(filteredPoints[idx].timestampEpochMillis - targetTime)
                        }
                    }
                }
                .pointerInput(filteredPoints) {
                    detectHorizontalDragGestures(
                        onDragEnd = { scrubbedIndex = null },
                        onDragCancel = { scrubbedIndex = null },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val leftPad = 42.dp.toPx()
                            val rightPad = 14.dp.toPx()
                            val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                            val fraction = ((change.position.x - leftPad) / chartW).coerceIn(0f, 1f)
                            val targetTime = minTime + (fraction * timeSpan).toLong()
                            scrubbedIndex = filteredPoints.indices.minByOrNull { idx ->
                                abs(filteredPoints[idx].timestampEpochMillis - targetTime)
                            }
                        }
                    )
                }
        ) {
            val leftPad = 42.dp.toPx()
            val rightPad = 14.dp.toPx()
            val topPad = 46.dp.toPx()
            val bottomPad = 56.dp.toPx()
            val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            fun xForTime(t: Long): Float =
                leftPad + ((t - minTime).toFloat() / timeSpan) * chartWidth

            fun yForHeight(h: Double): Float =
                topPad + chartHeight - (((h - minY).toFloat() / heightSpan) * chartHeight)

            // Horizontal dashed reference lines (min, mid, max)
            val gridValues = listOf(minHeightRaw, (minHeightRaw + maxHeightRaw) / 2.0, maxHeightRaw)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            gridValues.forEach { level ->
                val y = yForHeight(level)
                drawLine(
                    color = extendedColors.chartGrid,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
                val label = TideFormatters.formatHeight(level, unit, includeUnit = false)
                val measured = textMeasurer.measure(label, style = axisLabelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                        y = y - measured.size.height / 2f,
                    )
                )
            }

            // Build smooth cubic Bezier curve
            val offsets = filteredPoints.map { pt ->
                Offset(xForTime(pt.timestampEpochMillis), yForHeight(pt.heightMeters))
            }

            val linePath = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                for (i in 0 until offsets.lastIndex) {
                    val p0 = offsets[i]
                    val p1 = offsets[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(offsets.last().x, topPad + chartHeight)
                lineTo(offsets.first().x, topPad + chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.waveFillTop,
                        extendedColors.waveFillBottom,
                    ),
                    startY = topPad,
                    endY = topPad + chartHeight,
                )
            )

            drawPath(
                path = linePath,
                color = extendedColors.waveLine,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
            )

            // High / Low markers with explicit time + water level callouts
            var lastHighRightX = Float.NEGATIVE_INFINITY
            var lastLowRightX = Float.NEGATIVE_INFINITY
            val minLabelGapPx = 6.dp.toPx()

            visibleExtrema.forEach { event ->
                val ex = xForTime(event.timestampEpochMillis)
                val ey = yForHeight(event.heightMeters)
                val isHigh = event.type == TideEventType.HIGH
                val dotColor = if (isHigh) extendedColors.highTideBadge else extendedColors.lowTideBadge

                drawCircle(
                    color = surfaceColor,
                    radius = 5.5.dp.toPx(),
                    center = Offset(ex, ey),
                )
                drawCircle(
                    color = dotColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(ex, ey),
                )

                val levelStr = TideFormatters.formatHeightCompact(event.heightMeters, unit)
                val timeStr = TideFormatters.formatChartTimeInZone(event.timestampEpochMillis, timeZone)
                val prefix = if (isHigh) "H" else "L"
                val calloutText = "$prefix $levelStr\n$timeStr"
                val calloutStyle = if (isHigh) highCalloutStyle else lowCalloutStyle
                val measured = textMeasurer.measure(calloutText, style = calloutStyle)

                val pillPadH = 5.dp.toPx()
                val pillPadV = 3.dp.toPx()
                val pillW = measured.size.width + pillPadH * 2
                val pillH = measured.size.height + pillPadV * 2

                val pillX = (ex - pillW / 2f).coerceIn(
                    leftPad,
                    (size.width - rightPad - pillW).coerceAtLeast(leftPad),
                )

                val fitsWithoutOverlap = if (isHigh) {
                    pillX >= lastHighRightX + minLabelGapPx
                } else {
                    pillX >= lastLowRightX + minLabelGapPx
                }

                if (fitsWithoutOverlap) {
                    val pillY = if (isHigh) {
                        (ey - pillH - 8.dp.toPx()).coerceAtLeast(2.dp.toPx())
                    } else {
                        (ey + 8.dp.toPx()).coerceAtMost(size.height - 22.dp.toPx() - pillH)
                    }

                    // Connector hairline between dot and pill
                    val stemEndY = if (isHigh) pillY + pillH else pillY
                    drawLine(
                        color = dotColor.copy(alpha = 0.6f),
                        start = Offset(ex, ey),
                        end = Offset(ex, stemEndY),
                        strokeWidth = 1.dp.toPx(),
                    )

                    drawRoundRect(
                        color = surfaceVariantColor.copy(alpha = 0.92f),
                        topLeft = Offset(pillX, pillY),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                    drawRoundRect(
                        color = dotColor.copy(alpha = 0.45f),
                        topLeft = Offset(pillX, pillY),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(pillX + pillPadH, pillY + pillPadV),
                    )

                    if (isHigh) {
                        lastHighRightX = pillX + pillW
                    } else {
                        lastLowRightX = pillX + pillW
                    }
                }
            }

            // Live "NOW" vertical hairline & pulse dot
            if (currentTimeMillis in minTime..maxTime) {
                val nowX = xForTime(currentTimeMillis)
                val nearestNow = filteredPoints.minByOrNull { abs(it.timestampEpochMillis - currentTimeMillis) }
                val nowY = nearestNow?.let { yForHeight(it.heightMeters) } ?: (topPad + chartHeight / 2f)

                drawLine(
                    color = primaryColor.copy(alpha = 0.55f),
                    start = Offset(nowX, topPad),
                    end = Offset(nowX, topPad + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 11.dp.toPx(),
                    center = Offset(nowX, nowY),
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 6.dp.toPx(),
                    center = Offset(nowX, nowY),
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(nowX, nowY),
                )
            }

            // Scrubbed inspection hairline & marker
            scrubbedIndex?.let { idx ->
                val pt = filteredPoints.getOrNull(idx) ?: return@let
                val sx = xForTime(pt.timestampEpochMillis)
                val sy = yForHeight(pt.heightMeters)
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(sx, topPad),
                    end = Offset(sx, topPad + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 6.5.dp.toPx(),
                    center = Offset(sx, sy),
                )
                drawCircle(
                    color = extendedColors.lowTideBadge,
                    radius = 4.dp.toPx(),
                    center = Offset(sx, sy),
                )
            }

            // Bottom time axis ticks in user-selected timezone
            val tickIndices = listOf(
                0,
                filteredPoints.size / 3,
                (filteredPoints.size * 2) / 3,
                filteredPoints.lastIndex,
            ).distinct().filter { it in filteredPoints.indices }

            tickIndices.forEach { idx ->
                val pt = filteredPoints[idx]
                val tx = xForTime(pt.timestampEpochMillis)
                val label = TideFormatters.formatChartTimeInZone(pt.timestampEpochMillis, timeZone)
                val measured = textMeasurer.measure(label, style = axisLabelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (tx - measured.size.width / 2f).coerceIn(leftPad, size.width - rightPad - measured.size.width),
                        y = size.height - measured.size.height - 2.dp.toPx(),
                    )
                )
            }
        }
    }
}
