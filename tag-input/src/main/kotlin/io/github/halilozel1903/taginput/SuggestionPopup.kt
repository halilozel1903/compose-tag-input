package io.github.halilozel1903.taginput

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.halilozel1903.taginput.core.SuggestionMatch
import io.github.halilozel1903.taginput.core.TagInputDescriptions

/**
 * The suggestion list under the field. It never takes focus, so the keyboard stays in the text field and the
 * arrow keys keep moving the highlight.
 */
@Composable
internal fun SuggestionPopup(
    matches: List<SuggestionMatch>,
    highlighted: Int,
    widthPx: Int,
    colors: TagInputColors,
    descriptions: TagInputDescriptions,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val gap = with(density) { 4.dp.roundToPx() }
    val provider = remember(gap) { BelowAnchor(gap) }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(highlighted) {
            if (highlighted in matches.indices) {
                val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
                if (highlighted !in visible.dropLast(1)) listState.animateScrollToItem(highlighted)
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.suggestionContainer,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.width(with(density) { widthPx.toDp() }),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .padding(vertical = 6.dp)
                    .semantics {
                        paneTitle = descriptions.suggestions(matches.size)
                        collectionInfo = CollectionInfo(rowCount = matches.size, columnCount = 1)
                    },
            ) {
                itemsIndexed(matches, key = { _, m -> m.text }) { index, match ->
                    SuggestionRow(
                        match = match,
                        highlighted = index == highlighted,
                        colors = colors,
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = descriptions.suggestion(match.text, index + 1, matches.size)
                            selected = index == highlighted
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        },
                        onClick = { onPick(match.text) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    match: SuggestionMatch,
    highlighted: Boolean,
    colors: TagInputColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val text = buildAnnotatedString {
        if (match.hasMatch) {
            append(match.text.substring(0, match.matchStart))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.suggestionMatch)) {
                append(match.text.substring(match.matchStart, match.matchEnd))
            }
            append(match.text.substring(match.matchEnd))
        } else {
            append(match.text)
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.suggestionText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .background(
                if (highlighted) colors.suggestionHighlight else colors.suggestionContainer,
                RoundedCornerShape(8.dp),
            )
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

/** Puts the popup under its anchor, start-aligned, or above it when there is no room below. */
private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (below + popupContentSize.height <= windowSize.height || above < 0) below else above
        return IntOffset(x, y)
    }
}
