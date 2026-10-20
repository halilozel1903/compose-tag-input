package io.github.halilozel1903.taginput.core

/**
 * Splits typed or pasted text on separators. Line breaks and tabs always split, so a pasted column of addresses
 * becomes one tag per line.
 */
public object TagSplitter {
    /**
     * The result of [split]: [complete] holds every piece that was followed by a separator, and [remainder] is the
     * text after the last separator, which the user is still typing.
     */
    public data class Split(val complete: List<String>, val remainder: String)

    /** True when [char] splits text for [separators]. */
    public fun isSeparator(char: Char, separators: Set<Char>): Boolean =
        char in separators || char in TagInputConfig.LineBreaks

    /** Splits [text] into finished pieces and the piece still being typed. Pieces are not trimmed. */
    public fun split(text: String, separators: Set<Char>): Split {
        val complete = ArrayList<String>()
        var start = 0
        text.forEachIndexed { index, char ->
            if (isSeparator(char, separators)) {
                complete += text.substring(start, index)
                start = index + 1
            }
        }
        return Split(complete, text.substring(start))
    }

    /** Every piece of [text], including the last one. Pieces are not trimmed. */
    public fun tokens(text: String, separators: Set<Char>): List<String> {
        val split = split(text, separators)
        return split.complete + split.remainder
    }

    /** The piece after the last separator: what suggestions are matched against. */
    public fun currentToken(text: String, separators: Set<Char>): String = split(text, separators).remainder

    /** The text before [currentToken], separators included. */
    public fun beforeCurrentToken(text: String, separators: Set<Char>): String =
        text.substring(0, text.length - currentToken(text, separators).length)

    /**
     * What goes between pieces that are put back into the field: the first separator, with a space after it unless
     * it is a space itself. With no separators, pieces are joined with a new line so they still split again.
     */
    public fun joiner(separators: Set<Char>): String {
        val first = separators.firstOrNull() ?: return "\n"
        return if (first.isWhitespace()) first.toString() else "$first "
    }
}
