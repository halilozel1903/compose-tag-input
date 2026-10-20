package io.github.halilozel1903.taginput

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.taginput.core.Tag
import io.github.halilozel1903.taginput.core.TagError
import io.github.halilozel1903.taginput.core.TagInputDescriptions
import io.github.halilozel1903.taginput.core.TagInputLogic
import io.github.halilozel1903.taginput.core.TagSplitter
import io.github.halilozel1903.taginput.core.TagSuggestions

/**
 * A chip and tag input: committed tags flow inline as chips, followed by a text field.
 *
 * - Typing a separator from [TagInputState.config] (comma and space by default), or pressing Enter, commits the
 *   text. Pasted text is split into many tags.
 * - Backspace in an empty field selects the last chip, and a second Backspace removes it. Left and Right walk the
 *   chips, Delete removes the selected one, and Escape clears the selection.
 * - [suggestions] that contain the text appear in a list under the field, with the matched part highlighted. Up and
 *   Down move the highlight, Enter or Tab picks it, Escape closes the list.
 * - The validator, duplicate policy and tag limit refuse text with an error under the field.
 *
 * @param state the tags and text; see [rememberTagInputState].
 * @param suggestions what to offer while typing. Tags already added are left out.
 * @param label shown above the field and read by screen readers.
 * @param placeholder shown when there are no tags and no text.
 * @param supportingText shown under the field when there is no error.
 * @param enabled when false, the field is read-only and chips have no remove buttons.
 * @param chipStyle filled or outlined chips.
 * @param chipShape the chips' shape; [TagInputDefaults.PillShape] suits avatars.
 * @param chipLeading an avatar or icon in front of each chip, for example `{ TagAvatar(it.text) }`.
 * @param showCounter shows "3/5" under the field when the config has a tag limit.
 * @param minQueryLength how many characters must be typed before suggestions show; 0 shows them on focus.
 * @param maxSuggestions the most suggestions shown.
 * @param commitOnFocusLoss commits the text when the field loses focus.
 * @param keyboardType the soft keyboard's type, such as [KeyboardType.Email].
 * @param errorText turns a [TagError] into the message shown and announced.
 * @param descriptions what screen readers say.
 */
@Composable
public fun TagInput(
    state: TagInputState,
    suggestions: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    chipStyle: TagChipStyle = TagChipStyle.Filled,
    chipShape: Shape = TagInputDefaults.ChipShape,
    chipColors: TagChipColors = TagInputDefaults.chipColors(chipStyle),
    chipLeading: (@Composable (Tag) -> Unit)? = null,
    colors: TagInputColors = TagInputDefaults.colors(),
    showCounter: Boolean = true,
    minQueryLength: Int = 1,
    maxSuggestions: Int = TagInputDefaults.MaxSuggestions,
    commitOnFocusLoss: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: (TagError) -> String = { it.defaultMessage },
    descriptions: TagInputDescriptions = TagInputDescriptions(),
) {
    val snapshot = state.snapshot
    val config = state.config
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val error = snapshot.error
    val errorMessage = error?.let(errorText)
    val tags = snapshot.tags

    val query = TagSplitter.currentToken(snapshot.text, config.separators).trim()
    val matches = remember(suggestions, query, tags, maxSuggestions) {
        TagSuggestions.filter(suggestions, query, exclude = tags.map { it.text }, limit = maxSuggestions)
    }
    val full = TagInputLogic.isFull(snapshot, config)
    val showSuggestions = enabled && focused && !full && !snapshot.suggestionsDismissed &&
        query.length >= minQueryLength && matches.isNotEmpty()
    val visible = if (showSuggestions) matches.map { it.text } else emptyList()
    SideEffect { state.visibleSuggestions = visible }

    // Commit what was typed when focus leaves the field.
    val focusMemory = remember { booleanArrayOf(false) }
    LaunchedEffect(focused) {
        if (focused) {
            focusMemory[0] = true
        } else if (focusMemory[0]) {
            if (commitOnFocusLoss) state.commit()
            state.select(null)
        }
    }

    val borderColor = when {
        error != null -> colors.errorBorder
        focused -> colors.focusedBorder
        else -> colors.border
    }
    val countDescription = descriptions.count(tags.size, config.maxTags.takeIf { config.hasLimit })

    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    error != null -> colors.error
                    focused -> colors.focusedLabel
                    else -> colors.label
                },
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        var fieldWidth by remember { mutableIntStateOf(0) }
        Box(Modifier.fillMaxWidth().onSizeChanged { fieldWidth = it.width }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(colors.container, TagInputDefaults.FieldShape)
                    .border(if (focused || error != null) 2.dp else 1.dp, borderColor, TagInputDefaults.FieldShape)
                    // A tap on the empty part of the field focuses the text. Taps on chips are used by the chips.
                    .pointerInput(enabled) {
                        if (enabled) {
                            detectTapGestures {
                                state.select(null)
                                state.requestFocus()
                            }
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                ChipFlowLayout(
                    spacing = TagInputDefaults.ChipSpacing,
                    lineSpacing = TagInputDefaults.ChipSpacing,
                    fieldMinWidth = TagInputDefaults.FieldMinWidth,
                    modifier = Modifier.fillMaxWidth().semantics {
                        collectionInfo = CollectionInfo(rowCount = tags.size, columnCount = 1)
                    },
                ) {
                    tags.forEachIndexed { index, tag ->
                        key(tag.id) {
                            val selected = snapshot.selectedIndex == index
                            val removeLabel = descriptions.remove(tag.text)
                            TagChip(
                                text = tag.text,
                                style = chipStyle,
                                selected = selected,
                                enabled = enabled,
                                shape = chipShape,
                                colors = chipColors,
                                leading = chipLeading?.let { leading -> { leading(tag) } },
                                removeDescription = removeLabel,
                                onRemove = { state.remove(tag) },
                                onClick = {
                                    state.handle(TagInputLogic.toggleSelection(state.snapshot, state.tags.indexOf(tag)))
                                    state.requestFocus()
                                },
                                // One node per chip: its text and position, selection, and a Remove action.
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = descriptions.tag(tag.text, index + 1, tags.size)
                                    this.selected = selected
                                    if (selected) stateDescription = descriptions.selected
                                    collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                                    if (enabled) {
                                        customActions = listOf(CustomAccessibilityAction(removeLabel) { state.remove(tag); true })
                                    }
                                },
                            )
                        }
                    }
                    Box(Modifier.heightIn(min = TagInputDefaults.ChipHeight), contentAlignment = Alignment.CenterStart) {
                        if (snapshot.text.isEmpty() && tags.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.placeholder,
                                maxLines = 1,
                                modifier = Modifier.clearAndSetSemantics { },
                            )
                        }
                        BasicTextField(
                            value = state.fieldValue,
                            onValueChange = state::onValueChange,
                            enabled = enabled,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.cursor),
                            interactionSource = interaction,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = keyboardType,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                val handled = state.handle(TagInputLogic.onEnter(state.snapshot, state.visibleSuggestions, state.config))
                                if (!handled) defaultKeyboardAction(ImeAction.Done)
                            }),
                            decorationBox = { inner ->
                                Box(Modifier.heightIn(min = TagInputDefaults.ChipHeight), contentAlignment = Alignment.CenterStart) {
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(state.focusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    handleKey(state, event.key, showSuggestions)
                                }
                                .semantics {
                                    stateDescription = label?.let { "$it, $countDescription" } ?: countDescription
                                    if (errorMessage != null) error(errorMessage)
                                },
                        )
                    }
                }
            }
            if (showSuggestions) {
                SuggestionPopup(
                    matches = matches,
                    highlighted = snapshot.highlightedIndex,
                    widthPx = fieldWidth,
                    colors = colors,
                    descriptions = descriptions,
                    onPick = { state.acceptSuggestion(it) },
                    onDismiss = { state.dismissSuggestions() },
                )
            }
        }
        val bottomText = errorMessage ?: supportingText
        val counter = if (showCounter && config.hasLimit) "${tags.size}/${config.maxTags}" else null
        if (bottomText != null || counter != null) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp)) {
                Text(
                    text = bottomText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorMessage != null) colors.error else colors.supporting,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { if (errorMessage != null) liveRegion = LiveRegionMode.Polite },
                )
                if (counter != null) {
                    Text(
                        text = counter,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (full) colors.focusedLabel else colors.supporting,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp).clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}

/** Keyboard handling for the text field; returns whether the key was used. */
private fun handleKey(state: TagInputState, key: Key, suggestionsOpen: Boolean): Boolean {
    val s = state.snapshot
    val cursorAtStart = state.fieldValue.selection.let { it.collapsed && it.start == 0 }
    return when (key) {
        Key.Backspace -> cursorAtStart && state.handle(TagInputLogic.onBackspace(s))
        Key.Delete -> state.handle(TagInputLogic.onDelete(s))
        Key.DirectionLeft -> cursorAtStart && state.handle(TagInputLogic.moveSelection(s, -1))
        Key.DirectionRight -> state.handle(TagInputLogic.moveSelection(s, 1))
        Key.DirectionDown -> suggestionsOpen && run { state.moveHighlight(1); true }
        Key.DirectionUp -> suggestionsOpen && run { state.moveHighlight(-1); true }
        Key.Enter, Key.NumPadEnter -> state.handle(TagInputLogic.onEnter(s, state.visibleSuggestions, state.config))
        Key.Tab -> suggestionsOpen && s.highlightedIndex in state.visibleSuggestions.indices &&
            run { state.acceptSuggestion(state.visibleSuggestions[s.highlightedIndex]); true }
        Key.Escape -> state.handle(TagInputLogic.onEscape(s, suggestionsOpen))
        else -> false
    }
}
