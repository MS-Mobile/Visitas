package com.msmobile.visitas.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CopyDataIcon(
    modifier: Modifier = Modifier,
    backFillColor: Color = Color.Transparent,
    backStrokeColor: Color = MaterialTheme.colorScheme.primary,
    frontFillColor: Color = MaterialTheme.colorScheme.surface,
    frontStrokeColor: Color = MaterialTheme.colorScheme.primary,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * (2.5f / 24f)
        val lineStrokeWidth = w * (2f / 24f)
        val cornerRadius = w * (3f / 24f)
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Back rectangle (top-left)
        val backLeft = w * (2f / 24f)
        val backTop = h * (2f / 24f)
        val backWidth = w * (13f / 24f)
        val backHeight = h * (15f / 24f)
        drawRoundRect(
            color = backFillColor,
            topLeft = Offset(backLeft, backTop),
            size = Size(backWidth, backHeight),
            cornerRadius = CornerRadius(cornerRadius),
        )
        drawRoundRect(
            color = backStrokeColor,
            topLeft = Offset(backLeft, backTop),
            size = Size(backWidth, backHeight),
            cornerRadius = CornerRadius(cornerRadius),
            style = stroke
        )

        // Front rectangle (bottom-right)
        val frontLeft = w * (9f / 24f)
        val frontTop = h * (7f / 24f)
        val frontWidth = w * (13f / 24f)
        val frontHeight = h * (15f / 24f)
        drawRoundRect(
            color = frontFillColor,
            topLeft = Offset(frontLeft, frontTop),
            size = Size(frontWidth, frontHeight),
            cornerRadius = CornerRadius(cornerRadius),
        )
        drawRoundRect(
            color = frontStrokeColor,
            topLeft = Offset(frontLeft, frontTop),
            size = Size(frontWidth, frontHeight),
            cornerRadius = CornerRadius(cornerRadius),
            style = stroke
        )

        // Horizontal line 1
        val line1Y = h * (13f / 24f)
        drawLine(
            color = lineColor,
            start = Offset(w * (13f / 24f), line1Y),
            end = Offset(w * (18f / 24f), line1Y),
            strokeWidth = lineStrokeWidth,
            cap = StrokeCap.Round
        )

        // Horizontal line 2
        val line2Y = h * (16.5f / 24f)
        drawLine(
            color = lineColor,
            start = Offset(w * (13f / 24f), line2Y),
            end = Offset(w * (17f / 24f), line2Y),
            strokeWidth = lineStrokeWidth,
            cap = StrokeCap.Round
        )
    }
}

