package org.purplei2p.i2pd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A quiet, outlined surface. It deliberately owns no state or click behaviour. */
@Composable
fun LineCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = colors,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content,
    )
}

/** Original, static network globe: no bitmap, animation, permission or runtime dependency. */
@Composable
fun NetworkWireframe(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val stroke = Stroke(width = 0.65.dp.toPx())
        val diameter = size.minDimension * 0.84f
        val left = (size.width - diameter) / 2f
        val top = (size.height - diameter) / 2f
        drawOval(ink, Offset(left, top), Size(diameter, diameter), style = stroke)
        listOf(0.28f, 0.58f, 0.83f).forEach { ratio ->
            val width = diameter * ratio
            drawOval(ink.copy(alpha = 0.7f), Offset((size.width - width) / 2f, top), Size(width, diameter), style = stroke)
        }
        listOf(0.26f, 0.53f, 0.79f).forEach { ratio ->
            val height = diameter * ratio
            drawOval(ink.copy(alpha = 0.7f), Offset(left, (size.height - height) / 2f), Size(diameter, height), style = stroke)
        }
        drawLine(ink, Offset(left, size.height / 2f), Offset(left + diameter, size.height / 2f), 0.65.dp.toPx())
        drawLine(ink, Offset(size.width / 2f, top), Offset(size.width / 2f, top + diameter), 0.65.dp.toPx())
        // Small registration marks recall the thin rule/cross language of the reference.
        val arm = 3.dp.toPx()
        listOf(Offset(arm, arm), Offset(size.width - arm, size.height - arm)).forEach { p ->
            drawLine(ink, Offset(p.x - arm, p.y), Offset(p.x + arm, p.y), stroke.width)
            drawLine(ink, Offset(p.x, p.y - arm), Offset(p.x, p.y + arm), stroke.width)
        }
    }
}
