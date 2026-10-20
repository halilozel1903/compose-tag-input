package io.github.halilozel1903.taginput.core

/**
 * One committed tag. [id] is unique within an input and stays the same while the tag exists, so it works as a
 * key for lists and animations even when two tags have the same [text] (with [DuplicatePolicy.Allow]).
 */
public data class Tag(val id: Long, val text: String)

/** How an input treats a tag that is already there. */
public enum class DuplicatePolicy {
    /** Duplicates are added like any other tag. */
    Allow,

    /** A tag with exactly the same text is refused. */
    Reject,

    /** A tag with the same text, ignoring case, is refused. */
    RejectIgnoreCase,
    ;

    /** True when [candidate] counts as a duplicate of [existing] under this policy. */
    public fun isDuplicate(existing: String, candidate: String): Boolean = when (this) {
        Allow -> false
        Reject -> existing == candidate
        RejectIgnoreCase -> existing.equals(candidate, ignoreCase = true)
    }
}

/** Why a piece of text was not added as a tag. */
public sealed class TagError {
    /** The text that was refused, or `null` when the refusal is not about one text. */
    public abstract val text: String?

    /** An English message for the user. Pass your own `errorText` to the composable to translate it. */
    public abstract val defaultMessage: String

    /** The validator refused [text] with [message]. */
    public data class Invalid(override val text: String, val message: String) : TagError() {
        override val defaultMessage: String get() = message
    }

    /** [text] is already a tag. */
    public data class Duplicate(override val text: String) : TagError() {
        override val defaultMessage: String get() = "\"$text\" is already added"
    }

    /** The input already holds [max] tags. */
    public data class LimitReached(val max: Int) : TagError() {
        override val text: String? get() = null
        override val defaultMessage: String get() = "You can add up to $max tags"
    }
}

/** Checks a tag before it is added. Returns an error message, or `null` when the text is fine. */
public fun interface TagValidator {
    public fun validate(text: String): String?
}

/** Ready-made validators. */
public object TagValidators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    /** Accepts addresses such as `name@example.com`. */
    public fun email(message: String = "Not a valid email address"): TagValidator = TagValidator { text ->
        val ok = emailRegex.matches(text) && !text.contains("..") && !text.startsWith(".") && !text.contains(".@")
        if (ok) null else message
    }

    /** Refuses tags longer than [max] characters. */
    public fun maxLength(max: Int, message: String = "Use at most $max characters"): TagValidator {
        require(max > 0) { "max must be positive" }
        return TagValidator { text -> if (text.length <= max) null else message }
    }

    /** Accepts only text that fully matches [regex]. */
    public fun pattern(regex: Regex, message: String): TagValidator =
        TagValidator { text -> if (regex.matches(text)) null else message }

    /** Runs [validators] in order and returns the first error. */
    public fun allOf(vararg validators: TagValidator): TagValidator = TagValidator { text ->
        validators.firstNotNullOfOrNull { it.validate(text) }
    }
}

/**
 * The rules of a tag input.
 *
 * @param separators characters that commit the text typed before them. Pasted text is also split on new lines and
 *   tabs. An empty set means only Enter (or [TagInputLogic.commit]) adds a tag.
 * @param commitOnEnter whether Enter, and the keyboard's Done action, commit the text.
 * @param maxTags the most tags the input holds.
 * @param duplicates how text that is already a tag is treated.
 * @param validator checks each tag before it is added.
 * @param transform turns trimmed text into the tag's text, for example to lowercase it.
 */
public data class TagInputConfig(
    val separators: Set<Char> = DefaultSeparators,
    val commitOnEnter: Boolean = true,
    val maxTags: Int = Int.MAX_VALUE,
    val duplicates: DuplicatePolicy = DuplicatePolicy.RejectIgnoreCase,
    val validator: TagValidator? = null,
    val transform: (String) -> String = { it },
) {
    init {
        require(maxTags > 0) { "maxTags must be positive" }
    }

    /** True when [maxTags] is set to a real limit. */
    public val hasLimit: Boolean get() = maxTags != Int.MAX_VALUE

    public companion object {
        /** Comma and space. */
        public val DefaultSeparators: Set<Char> = linkedSetOf(',', ' ')

        /** Characters that always split pasted text, whatever the separators are. */
        public val LineBreaks: Set<Char> = setOf('\n', '\r', '\t')
    }
}
