package io.github.halilozel1903.taginput.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagInputLogicTest {
    private val config = TagInputConfig()
    private val emails = TagInputConfig(validator = TagValidators.email())

    private fun typed(text: String, cfg: TagInputConfig = config, from: TagInputSnapshot = TagInputSnapshot()) =
        TagInputLogic.onTextChange(from, text, cfg)

    @Test
    fun textWithoutSeparatorStaysInTheField() {
        val s = typed("kotl")
        assertEquals(emptyList(), s.tagTexts)
        assertEquals("kotl", s.text)
    }

    @Test
    fun commaCommitsTheTag() {
        val s = typed("kotlin,")
        assertEquals(listOf("kotlin"), s.tagTexts)
        assertEquals("", s.text)
    }

    @Test
    fun spaceCommitsWithDefaultSeparators() {
        val s = typed("kotlin compose")
        assertEquals(listOf("kotlin"), s.tagTexts)
        assertEquals("compose", s.text)
    }

    @Test
    fun customSeparator() {
        val cfg = TagInputConfig(separators = setOf(';'))
        val s = typed("Jetpack Compose;Kotlin", cfg)
        assertEquals(listOf("Jetpack Compose"), s.tagTexts)
        assertEquals("Kotlin", s.text)
    }

    @Test
    fun pasteSplitsIntoManyTags() {
        val s = typed("android, kotlin,compose\ngradle\ttesting", TagInputConfig(separators = setOf(',')))
        assertEquals(listOf("android", "kotlin", "compose", "gradle"), s.tagTexts)
        assertEquals("testing", s.text)
    }

    @Test
    fun blankPiecesAreSkipped() {
        val s = typed(",, ,a,,")
        assertEquals(listOf("a"), s.tagTexts)
        assertEquals("", s.text)
    }

    @Test
    fun idsAreUniqueAndIncreasing() {
        val s = typed("a,b,c,")
        assertEquals(listOf(0L, 1L, 2L), s.tags.map { it.id })
        assertEquals(3L, s.nextId)
        val removed = TagInputLogic.remove(s, 0)
        val again = typed("d,", from = removed)
        assertEquals(listOf(1L, 2L, 3L), again.tags.map { it.id })
    }

    @Test
    fun transformIsApplied() {
        val cfg = TagInputConfig(separators = setOf(','), transform = { it.lowercase().replace(' ', '-') })
        assertEquals(listOf("jetpack-compose"), typed("Jetpack Compose,", cfg).tagTexts)
    }

    @Test
    fun invalidTextStaysWithAnError() {
        val s = typed("alex@example.com, nope,", emails)
        assertEquals(listOf("alex@example.com"), s.tagTexts)
        assertEquals("nope", s.text)
        val error = assertIs<TagError.Invalid>(s.error)
        assertEquals("nope", error.text)
        assertEquals("Not a valid email address", error.defaultMessage)
    }

    @Test
    fun refusedPiecesKeepTheRemainderAfterThem() {
        val s = typed("bad1,bad2,alex@example.com,sam", emails)
        assertEquals(listOf("alex@example.com"), s.tagTexts)
        assertEquals("bad1, bad2, sam", s.text)
    }

    @Test
    fun keptTextIsStableWhenTypingContinues() {
        val cfg = TagInputConfig(separators = setOf(','), validator = TagValidators.email())
        val first = typed("bad,sam", cfg)
        assertEquals("bad, sam", first.text)
        val next = typed(first.text + "x", cfg, first)
        assertEquals("bad, samx", next.text)
        assertIs<TagError.Invalid>(next.error)
    }

    @Test
    fun duplicatesAreDroppedAndReported() {
        val start = TagInputSnapshot.of(listOf("Kotlin"))
        val s = typed("kotlin,", from = start)
        assertEquals(listOf("Kotlin"), s.tagTexts)
        assertEquals("", s.text)
        assertEquals(TagError.Duplicate("kotlin"), s.error)
        assertEquals("\"kotlin\" is already added", s.error?.defaultMessage)
    }

    @Test
    fun duplicatePolicies() {
        val start = TagInputSnapshot.of(listOf("Kotlin"))
        val exact = TagInputConfig(duplicates = DuplicatePolicy.Reject)
        assertEquals(listOf("Kotlin", "kotlin"), typed("kotlin,", exact, start).tagTexts)
        assertEquals(listOf("Kotlin"), typed("Kotlin,", exact, start).tagTexts)
        val allow = TagInputConfig(duplicates = DuplicatePolicy.Allow)
        assertEquals(listOf("Kotlin", "Kotlin"), typed("Kotlin,", allow, start).tagTexts)
    }

    @Test
    fun duplicatesWithinOnePaste() {
        assertEquals(listOf("a", "b"), typed("a,b,A,").tagTexts)
    }

    @Test
    fun maxTagsKeepsTheRestInTheField() {
        val cfg = TagInputConfig(maxTags = 2)
        val s = typed("a,b,c,d", cfg)
        assertEquals(listOf("a", "b"), s.tagTexts)
        assertEquals("c, d", s.text)
        assertEquals(TagError.LimitReached(2), s.error)
        assertEquals("You can add up to 2 tags", s.error?.defaultMessage)
        assertTrue(TagInputLogic.isFull(s, cfg))
    }

    @Test
    fun typingClearsTheError() {
        val s = typed("nope,", emails)
        assertIs<TagError.Invalid>(s.error)
        val next = typed("nope.", emails, s)
        assertNull(next.error)
    }

    @Test
    fun commitAddsTheWholeText() {
        val s = TagInputLogic.commit(TagInputSnapshot(text = "kotlin"), config)
        assertEquals(listOf("kotlin"), s.tagTexts)
        assertEquals("", s.text)
        val blank = TagInputSnapshot(text = "  ")
        assertEquals(blank, TagInputLogic.commit(blank, config))
    }

    @Test
    fun enterPicksTheHighlightedSuggestion() {
        val s = TagInputSnapshot(text = "ko", highlightedIndex = 1)
        val next = TagInputLogic.onEnter(s, listOf("kotlin", "koin"), config)!!
        assertEquals(listOf("koin"), next.tagTexts)
        assertEquals("", next.text)
    }

    @Test
    fun enterCommitsWithoutHighlight() {
        val next = TagInputLogic.onEnter(TagInputSnapshot(text = "ko"), listOf("kotlin"), config)!!
        assertEquals(listOf("ko"), next.tagTexts)
    }

    @Test
    fun enterOnEmptyTextIsNotHandled() {
        assertNull(TagInputLogic.onEnter(TagInputSnapshot(), emptyList(), config))
        val noEnter = TagInputConfig(commitOnEnter = false)
        assertNull(TagInputLogic.onEnter(TagInputSnapshot(text = "a"), emptyList(), noEnter))
    }

    @Test
    fun acceptSuggestionReplacesTheCurrentPiece() {
        val cfg = TagInputConfig(separators = setOf(','), validator = TagValidators.email())
        val s = TagInputSnapshot(text = "bad, sa")
        val next = TagInputLogic.acceptSuggestion(s, "sam@example.com", cfg)
        assertEquals(listOf("sam@example.com"), next.tagTexts)
        assertEquals("bad", next.text)
        assertIs<TagError.Invalid>(next.error)
    }

    @Test
    fun backspaceSelectsThenDeletes() {
        val s = TagInputSnapshot.of(listOf("a", "b", "c"))
        val selected = TagInputLogic.onBackspace(s)!!
        assertEquals(2, selected.selectedIndex)
        assertEquals("c", selected.selectedTag?.text)
        val deleted = TagInputLogic.onBackspace(selected)!!
        assertEquals(listOf("a", "b"), deleted.tagTexts)
        assertEquals(-1, deleted.selectedIndex)
    }

    @Test
    fun backspaceWithTextBelongsToTheField() {
        assertNull(TagInputLogic.onBackspace(TagInputSnapshot.of(listOf("a"), text = "x")))
        assertNull(TagInputLogic.onBackspace(TagInputSnapshot()))
    }

    @Test
    fun deleteRemovesTheSelectedChip() {
        val s = TagInputSnapshot.of(listOf("a", "b")).copy(selectedIndex = 0)
        assertEquals(listOf("b"), TagInputLogic.onDelete(s)!!.tagTexts)
        assertNull(TagInputLogic.onDelete(TagInputSnapshot.of(listOf("a"))))
    }

    @Test
    fun arrowKeysWalkTheChips() {
        val s = TagInputSnapshot.of(listOf("a", "b", "c"))
        val left = TagInputLogic.moveSelection(s, -1)!!
        assertEquals(2, left.selectedIndex)
        val left2 = TagInputLogic.moveSelection(TagInputLogic.moveSelection(left, -1)!!, -1)!!
        assertEquals(0, left2.selectedIndex)
        assertEquals(0, TagInputLogic.moveSelection(left2, -1)!!.selectedIndex)
        val right = TagInputLogic.moveSelection(left, 1)!!
        assertEquals(-1, right.selectedIndex)
        assertNull(TagInputLogic.moveSelection(s, 1))
        assertNull(TagInputLogic.moveSelection(s.copy(text = "x"), -1))
    }

    @Test
    fun typingClearsTheSelection() {
        val s = TagInputSnapshot.of(listOf("a")).copy(selectedIndex = 0)
        assertEquals(-1, typed("x", from = s).selectedIndex)
    }

    @Test
    fun toggleSelection() {
        val s = TagInputSnapshot.of(listOf("a", "b"))
        val on = TagInputLogic.toggleSelection(s, 1)
        assertEquals(1, on.selectedIndex)
        assertEquals(-1, TagInputLogic.toggleSelection(on, 1).selectedIndex)
        assertEquals(-1, TagInputLogic.toggleSelection(on, 9).selectedIndex)
    }

    @Test
    fun removeOutOfRangeChangesNothing() {
        val s = TagInputSnapshot.of(listOf("a"))
        assertEquals(s, TagInputLogic.remove(s, 3))
    }

    @Test
    fun highlightWraps() {
        val s = TagInputSnapshot()
        val down = TagInputLogic.moveHighlight(s, 1, 3)
        assertEquals(0, down.highlightedIndex)
        assertEquals(2, TagInputLogic.moveHighlight(s, -1, 3).highlightedIndex)
        assertEquals(2, TagInputLogic.moveHighlight(down, -1, 3).highlightedIndex)
        assertEquals(0, TagInputLogic.moveHighlight(down.copy(highlightedIndex = 2), 1, 3).highlightedIndex)
        assertEquals(-1, TagInputLogic.moveHighlight(down, 1, 0).highlightedIndex)
    }

    @Test
    fun typingResetsTheHighlight() {
        val s = TagInputSnapshot(text = "k", highlightedIndex = 1)
        assertEquals(-1, typed("ko", from = s).highlightedIndex)
    }

    @Test
    fun escapeClosesSuggestionsThenClearsSelection() {
        val s = TagInputSnapshot.of(listOf("a")).copy(selectedIndex = 0, highlightedIndex = 1)
        val closed = TagInputLogic.onEscape(s, suggestionsOpen = true)!!
        assertTrue(closed.suggestionsDismissed)
        assertEquals(-1, closed.highlightedIndex)
        assertEquals(-1, TagInputLogic.onEscape(closed, suggestionsOpen = false)!!.selectedIndex)
        assertNull(TagInputLogic.onEscape(TagInputSnapshot(), suggestionsOpen = false))
        assertTrue(!typed("x", from = closed).suggestionsDismissed)
    }

    @Test
    fun addKeepsTheFieldText() {
        val s = TagInputSnapshot(text = "typing")
        val next = TagInputLogic.add(s, listOf("bob@example.com", "nope"), emails)
        assertEquals(listOf("bob@example.com"), next.tagTexts)
        assertEquals("typing", next.text)
        assertIs<TagError.Invalid>(next.error)
    }

    @Test
    fun noSeparatorsOnlyEnterCommits() {
        val cfg = TagInputConfig(separators = emptySet())
        val s = typed("a, b c", cfg)
        assertEquals(emptyList(), s.tagTexts)
        assertEquals(listOf("a, b c"), TagInputLogic.commit(s, cfg).tagTexts)
    }
}
