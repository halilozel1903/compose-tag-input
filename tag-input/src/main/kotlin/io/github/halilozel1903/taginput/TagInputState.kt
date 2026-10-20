package io.github.halilozel1903.taginput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.halilozel1903.taginput.core.Tag
import io.github.halilozel1903.taginput.core.TagError
import io.github.halilozel1903.taginput.core.TagInputConfig
import io.github.halilozel1903.taginput.core.TagInputLogic
import io.github.halilozel1903.taginput.core.TagInputSnapshot

/**
 * The tags, the text being typed, the selected chip, the error and the suggestion highlight of a [TagInput].
 * Create it with [rememberTagInputState], read [tags] or [tagTexts] to get the result, and call the functions to
 * change it from code. The tags, text and selection survive configuration changes and process death.
 */
@Stable
public class TagInputState(
    initial: TagInputSnapshot = TagInputSnapshot(),
    config: TagInputConfig = TagInputConfig(),
) {
    /** The rules: separators, max tags, duplicates, validator and transform. */
    public var config: TagInputConfig = config

    /** Everything the input shows, as one value. */
    public var snapshot: TagInputSnapshot by mutableStateOf(initial)
        private set

    internal var fieldValue: TextFieldValue by mutableStateOf(TextFieldValue(initial.text, TextRange(initial.text.length)))
        private set

    /** The suggestions on screen right now, written by [TagInput]. */
    internal var visibleSuggestions: List<String> = emptyList()

    internal val focusRequester: FocusRequester = FocusRequester()

    /** The committed tags. */
    public val tags: List<Tag> get() = snapshot.tags

    /** The committed tags' texts. */
    public val tagTexts: List<String> get() = snapshot.tagTexts

    /** The text in the field that is not a tag yet. */
    public val text: String get() = snapshot.text

    /** Why the last commit refused something, or `null`. */
    public val error: TagError? get() = snapshot.error

    /** The chip selected with Backspace, the arrow keys or a tap, or `null`. */
    public val selectedIndex: Int? get() = snapshot.selectedIndex.takeIf { it in snapshot.tags.indices }

    /** The highlighted suggestion, or `null`. */
    public val highlightedIndex: Int? get() = snapshot.highlightedIndex.takeIf { it in visibleSuggestions.indices }

    /** True when the input holds [TagInputConfig.maxTags] tags. */
    public val isFull: Boolean get() = TagInputLogic.isFull(snapshot, config)

    /** Adds [text] as a tag, with the same checks as typing. Returns whether it was added. */
    public fun add(text: String): Boolean = addAll(listOf(text)) == 1

    /** Adds [texts] as tags, with the same checks as typing. Returns how many were added. */
    public fun addAll(texts: List<String>): Int {
        val before = snapshot.tags.size
        update(TagInputLogic.add(snapshot, texts, config))
        return snapshot.tags.size - before
    }

    /** Removes the tag at [index]. */
    public fun removeAt(index: Int) {
        update(TagInputLogic.remove(snapshot, index))
    }

    /** Removes [tag]. */
    public fun remove(tag: Tag) {
        removeAt(snapshot.tags.indexOfFirst { it.id == tag.id })
    }

    /** Removes every tag and the text. */
    public fun clear() {
        update(snapshot.copy(tags = emptyList(), text = "", selectedIndex = -1, error = null, highlightedIndex = -1))
    }

    /** Sets the field's text as if it was typed or pasted: separators in it commit tags. */
    public fun setText(text: String) {
        update(TagInputLogic.onTextChange(snapshot, text, config))
    }

    /** Commits the text in the field, as Enter does. */
    public fun commit() {
        update(TagInputLogic.commit(snapshot, config))
    }

    /** Selects the chip at [index], or clears the selection with `null`. */
    public fun select(index: Int?) {
        update(snapshot.copy(selectedIndex = index?.takeIf { it in snapshot.tags.indices } ?: -1))
    }

    /** Hides the error until the next commit. */
    public fun clearError() {
        update(snapshot.copy(error = null))
    }

    /** Moves the suggestion highlight, as the Down (+1) and Up (-1) keys do. */
    public fun moveHighlight(delta: Int) {
        update(TagInputLogic.moveHighlight(snapshot, delta, visibleSuggestions.size))
    }

    /** Adds [suggestion] in place of the text being typed, as a tap on it does. */
    public fun acceptSuggestion(suggestion: String) {
        update(TagInputLogic.acceptSuggestion(snapshot, suggestion, config))
    }

    /** Closes the suggestion list until the text changes, as Escape does. */
    public fun dismissSuggestions() {
        update(snapshot.copy(suggestionsDismissed = true, highlightedIndex = -1))
    }

    /** Moves focus to the text field. Call it after the [TagInput] is on screen. */
    public fun requestFocus() {
        focusRequester.requestFocus()
    }

    internal fun onValueChange(value: TextFieldValue) {
        update(TagInputLogic.onTextChange(snapshot, value.text, config), typed = value)
    }

    /** Applies a key's result; returns whether the key was handled. */
    internal fun handle(next: TagInputSnapshot?): Boolean {
        if (next == null) return false
        update(next)
        return true
    }

    private fun update(next: TagInputSnapshot, typed: TextFieldValue? = null) {
        snapshot = next
        fieldValue = when {
            typed != null && typed.text == next.text -> typed
            fieldValue.text == next.text -> fieldValue
            else -> TextFieldValue(next.text, TextRange(next.text.length))
        }
    }

    public companion object {
        /**
         * Saves the tags (with their ids), the text and the selected chip. [config] is not saved: it is passed in
         * again when the state is restored.
         */
        public fun saver(config: TagInputConfig): Saver<TagInputState, Any> = listSaver<TagInputState, Any>(
            save = { state ->
                val s = state.snapshot
                listOf(
                    ArrayList(s.tags.map { it.text }),
                    s.tags.map { it.id }.toLongArray(),
                    s.text,
                    s.nextId,
                    s.selectedIndex,
                )
            },
            restore = { values ->
                @Suppress("UNCHECKED_CAST")
                val texts = values[0] as List<String>
                val ids = values[1] as LongArray
                val snapshot = TagInputSnapshot(
                    tags = texts.mapIndexed { index, text -> Tag(ids[index], text) },
                    text = values[2] as String,
                    nextId = values[3] as Long,
                    selectedIndex = values[4] as Int,
                )
                TagInputState(snapshot, config)
            },
        )
    }
}

/**
 * Creates and remembers a [TagInputState] that survives configuration changes and process death.
 *
 * @param initialTags tags to start with. They are not validated.
 * @param config the rules; changing it later updates the state.
 */
@Composable
public fun rememberTagInputState(
    initialTags: List<String> = emptyList(),
    config: TagInputConfig = TagInputConfig(),
): TagInputState {
    val state = rememberSaveable(saver = TagInputState.saver(config)) {
        TagInputState(TagInputSnapshot.of(initialTags), config)
    }
    state.config = config
    return state
}
