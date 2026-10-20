package io.github.halilozel1903.taginput.core

/**
 * Everything a tag input shows, as one immutable value.
 *
 * @param tags the committed tags, in order.
 * @param text the text in the field.
 * @param selectedIndex the chip selected with Backspace or the arrow keys, or -1.
 * @param error why the last commit refused something, or `null`.
 * @param highlightedIndex the suggestion highlighted with the arrow keys, or -1.
 * @param suggestionsDismissed true after Escape closed the suggestions; typing opens them again.
 * @param nextId the id the next tag gets.
 */
public data class TagInputSnapshot(
    val tags: List<Tag> = emptyList(),
    val text: String = "",
    val selectedIndex: Int = -1,
    val error: TagError? = null,
    val highlightedIndex: Int = -1,
    val suggestionsDismissed: Boolean = false,
    val nextId: Long = 0,
) {
    /** The tags' texts. */
    public val tagTexts: List<String> get() = tags.map { it.text }

    /** The selected tag, if any. */
    public val selectedTag: Tag? get() = tags.getOrNull(selectedIndex)

    public companion object {
        /** A snapshot holding [texts] as tags, without validating them. */
        public fun of(texts: List<String>, text: String = ""): TagInputSnapshot =
            TagInputSnapshot(
                tags = texts.mapIndexed { index, t -> Tag(index.toLong(), t) },
                text = text,
                nextId = texts.size.toLong(),
            )
    }
}

/**
 * The rules of a tag input as pure functions: each takes a [TagInputSnapshot] and returns the next one. The
 * composables call these, and you can call them in tests or in other UI toolkits.
 */
public object TagInputLogic {

    /** Whether [snapshot] holds as many tags as [config] allows. */
    public fun isFull(snapshot: TagInputSnapshot, config: TagInputConfig): Boolean =
        snapshot.tags.size >= config.maxTags

    /**
     * The field's text changed to [newText] (typing or a paste). Every piece followed by a separator is committed;
     * the rest stays in the field. Refused pieces stay in the field too, so nothing typed or pasted is lost, except
     * duplicates, which are dropped. The first refusal becomes the snapshot's error.
     */
    public fun onTextChange(snapshot: TagInputSnapshot, newText: String, config: TagInputConfig): TagInputSnapshot {
        if (newText == snapshot.text) return snapshot
        val base = snapshot.copy(selectedIndex = -1, highlightedIndex = -1, suggestionsDismissed = false, error = null)
        val split = TagSplitter.split(newText, config.separators)
        if (split.complete.isEmpty()) return base.copy(text = newText)
        return addTokens(base, split.complete, split.remainder.trimStart(), config)
    }

    /** Commits all the text in the field, as Enter does. Blank text changes nothing. */
    public fun commit(snapshot: TagInputSnapshot, config: TagInputConfig): TagInputSnapshot {
        if (snapshot.text.isBlank()) return snapshot
        val base = snapshot.copy(selectedIndex = -1, highlightedIndex = -1, error = null)
        return addTokens(base, TagSplitter.tokens(snapshot.text, config.separators), remainder = "", config)
    }

    /**
     * Enter was pressed. Picks [visibleSuggestions] at the highlighted index when one is highlighted, and otherwise
     * commits the text when [TagInputConfig.commitOnEnter] is on. Returns `null` when Enter does nothing here, so
     * the caller can let the key through (for example to submit a form).
     */
    public fun onEnter(
        snapshot: TagInputSnapshot,
        visibleSuggestions: List<String>,
        config: TagInputConfig,
    ): TagInputSnapshot? {
        val highlighted = visibleSuggestions.getOrNull(snapshot.highlightedIndex)
        if (highlighted != null) return acceptSuggestion(snapshot, highlighted, config)
        if (!config.commitOnEnter || snapshot.text.isBlank()) return null
        return commit(snapshot, config)
    }

    /**
     * Adds [suggestion] in place of the piece being typed. Pieces before it (for example ones refused earlier) are
     * tried again.
     */
    public fun acceptSuggestion(
        snapshot: TagInputSnapshot,
        suggestion: String,
        config: TagInputConfig,
    ): TagInputSnapshot {
        val before = TagSplitter.beforeCurrentToken(snapshot.text, config.separators)
        val earlier = TagSplitter.split(before, config.separators).complete
        val base = snapshot.copy(selectedIndex = -1, highlightedIndex = -1, suggestionsDismissed = false, error = null)
        return addTokens(base, earlier + suggestion, remainder = "", config)
    }

    /** Adds [texts] as tags, with the same checks as typing, and leaves the field's text alone. */
    public fun add(snapshot: TagInputSnapshot, texts: List<String>, config: TagInputConfig): TagInputSnapshot {
        val base = snapshot.copy(selectedIndex = -1, error = null)
        val result = addTokens(base.copy(text = ""), texts, remainder = "", config)
        // Refused texts are reported through the error but not put into the field.
        return result.copy(text = snapshot.text)
    }

    /**
     * Backspace was pressed with the cursor at the start of the field. With empty text, the first press selects the
     * last chip and the second removes it. Returns `null` when the field has text, so the text field handles the key.
     */
    public fun onBackspace(snapshot: TagInputSnapshot): TagInputSnapshot? {
        if (snapshot.text.isNotEmpty()) return null
        if (snapshot.tags.isEmpty()) return null
        return if (snapshot.selectedIndex in snapshot.tags.indices) {
            remove(snapshot, snapshot.selectedIndex)
        } else {
            snapshot.copy(selectedIndex = snapshot.tags.lastIndex, highlightedIndex = -1, error = null)
        }
    }

    /** Forward Delete: removes the selected chip. Returns `null` when no chip is selected. */
    public fun onDelete(snapshot: TagInputSnapshot): TagInputSnapshot? {
        if (snapshot.selectedIndex !in snapshot.tags.indices) return null
        return remove(snapshot, snapshot.selectedIndex)
    }

    /**
     * Moves the chip selection with the arrow keys. [delta] -1 (Left) selects the last chip when none is selected
     * and then walks toward the first; +1 (Right) walks toward the field and clears the selection after the last
     * chip. Returns `null` when the key belongs to the text (the field has text, or Right with nothing selected).
     */
    public fun moveSelection(snapshot: TagInputSnapshot, delta: Int): TagInputSnapshot? {
        if (snapshot.text.isNotEmpty() || snapshot.tags.isEmpty()) return null
        val current = snapshot.selectedIndex.takeIf { it in snapshot.tags.indices }
        val next = when {
            delta < 0 && current == null -> snapshot.tags.lastIndex
            delta < 0 -> (current!! - 1).coerceAtLeast(0)
            current == null -> return null
            current + 1 > snapshot.tags.lastIndex -> -1
            else -> current + 1
        }
        return snapshot.copy(selectedIndex = next)
    }

    /** Selects the chip at [index], or clears the selection when it is already selected. */
    public fun toggleSelection(snapshot: TagInputSnapshot, index: Int): TagInputSnapshot {
        if (index !in snapshot.tags.indices) return snapshot.copy(selectedIndex = -1)
        return snapshot.copy(selectedIndex = if (snapshot.selectedIndex == index) -1 else index)
    }

    /** Removes the tag at [index]. Out of range indices change nothing. */
    public fun remove(snapshot: TagInputSnapshot, index: Int): TagInputSnapshot {
        if (index !in snapshot.tags.indices) return snapshot
        return snapshot.copy(
            tags = snapshot.tags.filterIndexed { i, _ -> i != index },
            selectedIndex = -1,
            error = null,
        )
    }

    /**
     * Moves the suggestion highlight by [delta] among [count] suggestions, wrapping around. With nothing highlighted,
     * Down highlights the first and Up the last.
     */
    public fun moveHighlight(snapshot: TagInputSnapshot, delta: Int, count: Int): TagInputSnapshot {
        if (count <= 0) return snapshot.copy(highlightedIndex = -1)
        val current = snapshot.highlightedIndex
        val next = if (current !in 0 until count) {
            if (delta >= 0) 0 else count - 1
        } else {
            Math.floorMod(current + delta, count)
        }
        return snapshot.copy(highlightedIndex = next, suggestionsDismissed = false, selectedIndex = -1)
    }

    /**
     * Escape: closes the suggestions when they are open, otherwise clears the chip selection. Returns `null` when
     * there is nothing to close.
     */
    public fun onEscape(snapshot: TagInputSnapshot, suggestionsOpen: Boolean): TagInputSnapshot? = when {
        suggestionsOpen -> snapshot.copy(suggestionsDismissed = true, highlightedIndex = -1)
        snapshot.selectedIndex >= 0 -> snapshot.copy(selectedIndex = -1)
        else -> null
    }

    /** Why [text] cannot be added to [tags], or `null` when it can. */
    public fun check(tags: List<Tag>, text: String, config: TagInputConfig): TagError? {
        if (tags.size >= config.maxTags) return TagError.LimitReached(config.maxTags)
        config.validator?.validate(text)?.let { return TagError.Invalid(text, it) }
        if (tags.any { config.duplicates.isDuplicate(it.text, text) }) return TagError.Duplicate(text)
        return null
    }

    private fun addTokens(
        snapshot: TagInputSnapshot,
        tokens: List<String>,
        remainder: String,
        config: TagInputConfig,
    ): TagInputSnapshot {
        val tags = snapshot.tags.toMutableList()
        var nextId = snapshot.nextId
        val kept = ArrayList<String>()
        var error: TagError? = null
        for (raw in tokens) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            val text = config.transform(trimmed).trim()
            if (text.isEmpty()) continue
            when (val refusal = check(tags, text, config)) {
                null -> tags += Tag(nextId++, text)
                is TagError.Duplicate -> if (error == null) error = refusal
                else -> {
                    kept += trimmed
                    if (error == null) error = refusal
                }
            }
        }
        val joiner = TagSplitter.joiner(config.separators)
        val text = when {
            kept.isEmpty() -> remainder
            remainder.isEmpty() -> kept.joinToString(joiner)
            else -> kept.joinToString(joiner) + joiner + remainder
        }
        return snapshot.copy(tags = tags, text = text, error = error, nextId = nextId)
    }
}
