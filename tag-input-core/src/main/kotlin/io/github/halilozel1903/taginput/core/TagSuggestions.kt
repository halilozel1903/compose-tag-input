package io.github.halilozel1903.taginput.core

/**
 * A suggestion that matches the query. [matchStart] until [matchEnd] is the matched part of [text], for
 * highlighting; both are -1 when the query was empty.
 */
public data class SuggestionMatch(val text: String, val matchStart: Int, val matchEnd: Int) {
    /** True when part of [text] is highlighted. */
    public val hasMatch: Boolean get() = matchStart in 0 until matchEnd
}

/** Filters and ranks suggestions for the piece being typed. */
public object TagSuggestions {
    /**
     * The suggestions that contain [query], ignoring case, without the ones already in [exclude] (also ignoring
     * case). Suggestions that start with the query come first, then ones where a word starts with it (after a
     * space, `-`, `_`, `.`, `/` or `@`), then the rest; within each group the original order is kept. An empty
     * query returns the first [limit] suggestions without highlights.
     */
    public fun filter(
        suggestions: List<String>,
        query: String,
        exclude: Collection<String> = emptyList(),
        limit: Int = 6,
    ): List<SuggestionMatch> {
        if (limit <= 0) return emptyList()
        val excluded = exclude.mapTo(HashSet()) { it.lowercase() }
        val seen = HashSet<String>()
        val candidates = suggestions.filter { it.isNotBlank() && it.lowercase() !in excluded && seen.add(it.lowercase()) }
        val q = query.trim()
        if (q.isEmpty()) return candidates.take(limit).map { SuggestionMatch(it, -1, -1) }
        val ranked = candidates.mapNotNull { text ->
            val start = bestMatch(text, q) ?: return@mapNotNull null
            val rank = when {
                start == 0 -> 0
                isWordStart(text, start) -> 1
                else -> 2
            }
            rank to SuggestionMatch(text, start, start + q.length)
        }
        return ranked.sortedBy { it.first }.take(limit).map { it.second }
    }

    /** The start of the best occurrence of [query] in [text]: the first word start, else the first occurrence. */
    private fun bestMatch(text: String, query: String): Int? {
        var index = text.indexOf(query, ignoreCase = true)
        if (index < 0) return null
        val first = index
        while (index >= 0) {
            if (index == 0 || isWordStart(text, index)) return index
            index = text.indexOf(query, startIndex = index + 1, ignoreCase = true)
        }
        return first
    }

    private fun isWordStart(text: String, index: Int): Boolean =
        index == 0 || text[index - 1] in WordBreaks

    private val WordBreaks = setOf(' ', '-', '_', '.', '/', '@')
}
