package io.github.halilozel1903.taginput

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import io.github.halilozel1903.taginput.core.ChipFlowMath

/**
 * Flows chips inline and gives the last child (the text field) the rest of the last line, or a line of its own
 * when less than [fieldMinWidth] (or the text's own width) is left. Items in a line are centered vertically.
 */
@Composable
internal fun ChipFlowLayout(
    spacing: Dp,
    lineSpacing: Dp,
    fieldMinWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else 1_000_000
        val gap = spacing.roundToPx()
        val lineGap = lineSpacing.roundToPx()
        val chipMeasurables = measurables.dropLast(1)
        val field = measurables.last()
        val chips = chipMeasurables.map { it.measure(Constraints(maxWidth = maxWidth)) }
        val textWidth = field.maxIntrinsicWidth(Constraints.Infinity)
        val minField = maxOf(fieldMinWidth.roundToPx(), textWidth).coerceAtMost(maxWidth)
        val arrangement = ChipFlowMath.arrange(chips.map { it.width }, minField, maxWidth, gap)
        val fieldPlaceable = field.measure(Constraints.fixedWidth(arrangement.fieldWidth.coerceAtLeast(0)))

        val lineHeights = IntArray(arrangement.lineCount)
        chips.forEachIndexed { i, p ->
            val line = arrangement.itemLines[i]
            lineHeights[line] = maxOf(lineHeights[line], p.height)
        }
        lineHeights[arrangement.fieldLine] = maxOf(lineHeights[arrangement.fieldLine], fieldPlaceable.height)
        val lineTops = IntArray(arrangement.lineCount)
        for (line in 1 until arrangement.lineCount) {
            lineTops[line] = lineTops[line - 1] + lineHeights[line - 1] + lineGap
        }
        val height = lineTops.last() + lineHeights.last()
        val width = if (constraints.hasBoundedWidth) maxWidth else {
            maxOf(arrangement.fieldX + arrangement.fieldWidth, chips.indices.maxOfOrNull { arrangement.itemX[it] + chips[it].width } ?: 0)
        }
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            chips.forEachIndexed { i, p ->
                val line = arrangement.itemLines[i]
                p.placeRelative(arrangement.itemX[i], lineTops[line] + (lineHeights[line] - p.height) / 2)
            }
            val line = arrangement.fieldLine
            fieldPlaceable.placeRelative(arrangement.fieldX, lineTops[line] + (lineHeights[line] - fieldPlaceable.height) / 2)
        }
    }
}
