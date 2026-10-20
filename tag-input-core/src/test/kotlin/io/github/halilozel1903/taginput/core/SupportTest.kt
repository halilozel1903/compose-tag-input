package io.github.halilozel1903.taginput.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitterTest {
    private val comma = setOf(',')

    @Test
    fun splitKeepsTheRemainder() {
        assertEquals(TagSplitter.Split(listOf("a", " b"), " c"), TagSplitter.split("a, b, c", comma))
        assertEquals(TagSplitter.Split(emptyList(), "abc"), TagSplitter.split("abc", comma))
        assertEquals(TagSplitter.Split(listOf("a"), ""), TagSplitter.split("a,", comma))
    }

    @Test
    fun lineBreaksAlwaysSplit() {
        assertEquals(listOf("a", "b", "c", "d"), TagSplitter.tokens("a\nb\r\tc,d", comma).filter { it.isNotEmpty() })
    }

    @Test
    fun currentToken() {
        assertEquals("sa", TagSplitter.currentToken("bad, sa", comma).trim())
        assertEquals("bad,", TagSplitter.beforeCurrentToken("bad,sa", comma))
    }

    @Test
    fun joiner() {
        assertEquals(", ", TagSplitter.joiner(linkedSetOf(',', ' ')))
        assertEquals(" ", TagSplitter.joiner(linkedSetOf(' ', ',')))
        assertEquals("; ", TagSplitter.joiner(setOf(';')))
        assertEquals("\n", TagSplitter.joiner(emptySet()))
    }
}

class ValidatorTest {
    @Test
    fun email() {
        val v = TagValidators.email()
        listOf("alex@example.com", "a.b+c@mail.example.org", "x_y@example.co").forEach { assertNull(v.validate(it), it) }
        listOf("alex", "alex@", "@example.com", "alex@example", "a..b@example.com", ".a@example.com", "a.@example.com", "a b@example.com")
            .forEach { assertNotNull(v.validate(it), it) }
    }

    @Test
    fun maxLengthAndPattern() {
        assertNull(TagValidators.maxLength(3).validate("abc"))
        assertEquals("Use at most 3 characters", TagValidators.maxLength(3).validate("abcd"))
        assertFailsWith<IllegalArgumentException> { TagValidators.maxLength(0) }
        val slug = TagValidators.pattern(Regex("[a-z0-9-]+"), "Letters, digits and dashes only")
        assertNull(slug.validate("jetpack-compose"))
        assertEquals("Letters, digits and dashes only", slug.validate("Jetpack Compose"))
    }

    @Test
    fun allOfReturnsTheFirstError() {
        val v = TagValidators.allOf(TagValidators.maxLength(5, "long"), TagValidators.pattern(Regex("[a-z]+"), "lower"))
        assertNull(v.validate("abc"))
        assertEquals("long", v.validate("ABCDEFG"))
        assertEquals("lower", v.validate("ABC"))
    }

    @Test
    fun configRejectsZeroMax() {
        assertFailsWith<IllegalArgumentException> { TagInputConfig(maxTags = 0) }
        assertFalse(TagInputConfig().hasLimit)
        assertTrue(TagInputConfig(maxTags = 5).hasLimit)
    }

    @Test
    fun checkOrder() {
        val cfg = TagInputConfig(maxTags = 1, validator = TagValidators.email())
        val tags = listOf(Tag(0, "a@example.com"))
        assertEquals(TagError.LimitReached(1), TagInputLogic.check(tags, "nope", cfg))
        val open = cfg.copy(maxTags = 5)
        assertEquals(TagError.Invalid("nope", "Not a valid email address"), TagInputLogic.check(tags, "nope", open))
        assertEquals(TagError.Duplicate("A@example.com"), TagInputLogic.check(tags, "A@example.com", open))
        assertNull(TagInputLogic.check(tags, "b@example.com", open))
    }
}

class SuggestionsTest {
    private val topics = listOf("jetpack-compose", "kotlin", "compose-multiplatform", "Compiler-plugins", "accompanist", "coroutines")

    @Test
    fun prefixThenWordStartThenContains() {
        val result = TagSuggestions.filter(topics, "comp")
        assertEquals(listOf("compose-multiplatform", "Compiler-plugins", "jetpack-compose", "accompanist"), result.map { it.text })
        assertEquals(SuggestionMatch("jetpack-compose", 8, 12), result[2])
        assertEquals(SuggestionMatch("accompanist", 2, 6), result[3])
        assertTrue(result.all { it.hasMatch })
    }

    @Test
    fun ignoresCaseAndExcludesTags() {
        val result = TagSuggestions.filter(topics, "COMP", exclude = listOf("Compose-Multiplatform"))
        assertEquals("Compiler-plugins", result.first().text)
        assertTrue(result.none { it.text == "compose-multiplatform" })
    }

    @Test
    fun emptyQueryReturnsTheFirstOnes() {
        val result = TagSuggestions.filter(topics, " ", limit = 2)
        assertEquals(listOf("jetpack-compose", "kotlin"), result.map { it.text })
        assertFalse(result[0].hasMatch)
    }

    @Test
    fun limitAndDuplicates() {
        assertEquals(1, TagSuggestions.filter(listOf("a", "A", "ab"), "a", limit = 5).count { it.text.equals("a", true) })
        assertEquals(1, TagSuggestions.filter(topics, "o", limit = 1).size)
        assertEquals(emptyList(), TagSuggestions.filter(topics, "o", limit = 0))
        assertEquals(emptyList(), TagSuggestions.filter(topics, "zzz"))
    }

    @Test
    fun prefersAWordStartOccurrence() {
        assertEquals(SuggestionMatch("tom.thomson@example.com", 4, 7), TagSuggestions.filter(listOf("tom.thomson@example.com"), "tho").single())
    }
}

class FlowTest {
    @Test
    fun fieldTakesTheRestOfTheLine() {
        val a = ChipFlowMath.arrange(listOf(100, 100), fieldMinWidth = 80, maxWidth = 400, spacing = 10)
        assertEquals(listOf(0, 0), a.itemLines)
        assertEquals(listOf(0, 110), a.itemX)
        assertEquals(0, a.fieldLine)
        assertEquals(220, a.fieldX)
        assertEquals(180, a.fieldWidth)
        assertEquals(1, a.lineCount)
    }

    @Test
    fun chipsWrapAndFieldMovesDown() {
        val a = ChipFlowMath.arrange(listOf(150, 150, 150), fieldMinWidth = 120, maxWidth = 400, spacing = 10)
        assertEquals(listOf(0, 0, 1), a.itemLines)
        assertEquals(listOf(0, 160, 0), a.itemX)
        assertEquals(1, a.fieldLine)
        assertEquals(160, a.fieldX)
        assertEquals(240, a.fieldWidth)

        val full = ChipFlowMath.arrange(listOf(150, 150), fieldMinWidth = 120, maxWidth = 400, spacing = 10)
        assertEquals(1, full.fieldLine)
        assertEquals(0, full.fieldX)
        assertEquals(400, full.fieldWidth)
    }

    @Test
    fun noChipsGivesTheFieldEverything() {
        val a = ChipFlowMath.arrange(emptyList(), 80, 300, 8)
        assertEquals(ChipFlowArrangement(emptyList(), emptyList(), 0, 0, 300), a)
    }

    @Test
    fun wideChipGetsItsOwnLine() {
        val a = ChipFlowMath.arrange(listOf(50, 500, 50), 40, 300, 10)
        assertEquals(listOf(0, 1, 2), a.itemLines)
        assertEquals(2, a.fieldLine)
        assertEquals(60, a.fieldX)
    }
}

class AvatarAndDescriptionTest {
    @Test
    fun initials() {
        assertEquals("AR", TagAvatars.initials("alex.rivera@example.com"))
        assertEquals("S", TagAvatars.initials("sam@example.com"))
        assertEquals("JC", TagAvatars.initials("Jetpack Compose"))
        assertEquals("K", TagAvatars.initials("kotlin"))
        assertEquals("?", TagAvatars.initials("  "))
    }

    @Test
    fun colorIndexIsStable() {
        val i = TagAvatars.colorIndex("Alex@example.com", 6)
        assertTrue(i in 0 until 6)
        assertEquals(i, TagAvatars.colorIndex("alex@example.com", 6))
        assertFailsWith<IllegalArgumentException> { TagAvatars.colorIndex("a", 0) }
    }

    @Test
    fun descriptions() {
        val d = TagInputDescriptions()
        assertEquals("kotlin, tag 2 of 3", d.tag("kotlin", 2, 3))
        assertEquals("Remove kotlin", d.remove("kotlin"))
        assertEquals("3 of 5 tags", d.count(3, 5))
        assertEquals("1 tag", d.count(1, null))
        assertEquals("2 suggestions", d.suggestions(2))
    }
}
