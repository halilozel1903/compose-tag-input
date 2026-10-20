package io.github.halilozel1903.taginput.core

/**
 * Where chips and the text field go in a tag input. Lines are numbered from 0 and x positions are from the start
 * edge (the composable mirrors them in right-to-left layouts).
 */
public data class ChipFlowArrangement(
    val itemLines: List<Int>,
    val itemX: List<Int>,
    val fieldLine: Int,
    val fieldX: Int,
    val fieldWidth: Int,
) {
    /** How many lines the chips and the field take. */
    public val lineCount: Int get() = fieldLine + 1
}

/** Flows chips inline and gives the text field the rest of the last line. */
public object ChipFlowMath {
    /**
     * Places items of [itemWidths] from the start, wrapping to a new line when the next one does not fit in
     * [maxWidth]. The field takes the rest of the last line when at least [fieldMinWidth] is left, and a full line
     * of its own otherwise. Items wider than [maxWidth] get a line of their own.
     */
    public fun arrange(itemWidths: List<Int>, fieldMinWidth: Int, maxWidth: Int, spacing: Int): ChipFlowArrangement {
        require(maxWidth >= 0) { "maxWidth must not be negative" }
        val lines = ArrayList<Int>(itemWidths.size)
        val xs = ArrayList<Int>(itemWidths.size)
        var line = 0
        var x = 0
        var lineHasItems = false
        for (width in itemWidths) {
            if (lineHasItems && x + width > maxWidth) {
                line++
                x = 0
                lineHasItems = false
            }
            lines += line
            xs += x
            x += width + spacing
            lineHasItems = true
        }
        if (!lineHasItems) return ChipFlowArrangement(lines, xs, line, 0, maxWidth)
        val left = maxWidth - x
        return if (left >= fieldMinWidth.coerceAtMost(maxWidth) && left > 0) {
            ChipFlowArrangement(lines, xs, line, x, left)
        } else {
            ChipFlowArrangement(lines, xs, line + 1, 0, maxWidth)
        }
    }
}

/** Avatars for chips: initials and a stable color slot. */
public object TagAvatars {
    /**
     * One or two letters for [text]. An email address uses the part before `@`, split on `.`, `_`, `-` and `+`
     * (`alex.rivera@example.com` gives `AR`); other text is split on spaces (`Jetpack Compose` gives `JC`).
     */
    public fun initials(text: String): String {
        val name = text.trim().substringBefore('@')
        val words = name.split(' ', '.', '_', '-', '+').filter { it.isNotEmpty() && it.first().isLetterOrDigit() }
        return when {
            words.isEmpty() -> text.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
            words.size == 1 -> words[0].first().uppercase()
            else -> (words.first().first().toString() + words.last().first()).uppercase()
        }
    }

    /** A slot in a palette of [size] colors that stays the same for the same text, ignoring case. */
    public fun colorIndex(text: String, size: Int): Int {
        require(size > 0) { "size must be positive" }
        val hash = text.trim().lowercase().fold(7) { h, c -> h * 31 + c.code }
        return Math.floorMod(hash, size)
    }
}

/**
 * What screen readers say. The defaults are English; pass your own lambdas to translate them.
 *
 * @param tag a chip: its text, position (from 1) and the number of chips.
 * @param selected the state of the chip selected with Backspace.
 * @param remove the label of a chip's remove button and action.
 * @param count the input's state: tags so far and the limit, or `null` without one.
 * @param suggestion a suggestion: its text, position (from 1) and the number of suggestions.
 * @param suggestions the title of the open suggestion list.
 */
public data class TagInputDescriptions(
    val tag: (text: String, position: Int, count: Int) -> String = { text, position, count ->
        "$text, tag $position of $count"
    },
    val selected: String = "Selected. Press Backspace to remove",
    val remove: (text: String) -> String = { "Remove $it" },
    val count: (count: Int, max: Int?) -> String = { count, max ->
        when {
            max != null -> "$count of $max tags"
            count == 1 -> "1 tag"
            else -> "$count tags"
        }
    },
    val suggestion: (text: String, position: Int, count: Int) -> String = { text, position, count ->
        "$text, suggestion $position of $count"
    },
    val suggestions: (count: Int) -> String = { if (it == 1) "1 suggestion" else "$it suggestions" },
)
