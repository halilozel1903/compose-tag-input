package io.github.halilozel1903.taginput

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How chips are drawn. Add an avatar or an icon with the `chipLeading` slot of [TagInput] or [TagChip]. */
public enum class TagChipStyle {
    /** A tinted container without a border. */
    Filled,

    /** A border on the field's background. */
    Outlined,
}

/** Colors of a chip. */
@Immutable
public class TagChipColors(
    public val container: Color,
    public val content: Color,
    public val outline: Color,
    public val selectedContainer: Color,
    public val selectedContent: Color,
    public val disabledContent: Color,
) {
    /** A copy with some colors replaced. */
    public fun copy(
        container: Color = this.container,
        content: Color = this.content,
        outline: Color = this.outline,
        selectedContainer: Color = this.selectedContainer,
        selectedContent: Color = this.selectedContent,
        disabledContent: Color = this.disabledContent,
    ): TagChipColors = TagChipColors(container, content, outline, selectedContainer, selectedContent, disabledContent)

    override fun equals(other: Any?): Boolean = other is TagChipColors && container == other.container &&
        content == other.content && outline == other.outline && selectedContainer == other.selectedContainer &&
        selectedContent == other.selectedContent && disabledContent == other.disabledContent

    override fun hashCode(): Int {
        var result = container.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + outline.hashCode()
        result = 31 * result + selectedContainer.hashCode()
        result = 31 * result + selectedContent.hashCode()
        result = 31 * result + disabledContent.hashCode()
        return result
    }
}

/** Colors of the field around the chips, its texts and the suggestion list. */
@Immutable
public class TagInputColors(
    public val container: Color,
    public val border: Color,
    public val focusedBorder: Color,
    public val errorBorder: Color,
    public val label: Color,
    public val focusedLabel: Color,
    public val text: Color,
    public val placeholder: Color,
    public val cursor: Color,
    public val supporting: Color,
    public val error: Color,
    public val suggestionContainer: Color,
    public val suggestionText: Color,
    public val suggestionHighlight: Color,
    public val suggestionMatch: Color,
) {
    /** A copy with some colors replaced. */
    public fun copy(
        container: Color = this.container,
        border: Color = this.border,
        focusedBorder: Color = this.focusedBorder,
        errorBorder: Color = this.errorBorder,
        label: Color = this.label,
        focusedLabel: Color = this.focusedLabel,
        text: Color = this.text,
        placeholder: Color = this.placeholder,
        cursor: Color = this.cursor,
        supporting: Color = this.supporting,
        error: Color = this.error,
        suggestionContainer: Color = this.suggestionContainer,
        suggestionText: Color = this.suggestionText,
        suggestionHighlight: Color = this.suggestionHighlight,
        suggestionMatch: Color = this.suggestionMatch,
    ): TagInputColors = TagInputColors(
        container, border, focusedBorder, errorBorder, label, focusedLabel, text, placeholder, cursor, supporting,
        error, suggestionContainer, suggestionText, suggestionHighlight, suggestionMatch,
    )

    private fun all(): List<Color> = listOf(
        container, border, focusedBorder, errorBorder, label, focusedLabel, text, placeholder, cursor, supporting,
        error, suggestionContainer, suggestionText, suggestionHighlight, suggestionMatch,
    )

    override fun equals(other: Any?): Boolean = other is TagInputColors && all() == other.all()

    override fun hashCode(): Int = all().hashCode()
}

/** Default values for [TagInput] and [TagChip], taken from the Material 3 theme. */
public object TagInputDefaults {
    /** The shape of the field around the chips. */
    public val FieldShape: Shape = RoundedCornerShape(12.dp)

    /** The default chip shape, as Material 3 input chips. */
    public val ChipShape: Shape = RoundedCornerShape(8.dp)

    /** A fully rounded chip shape, which suits avatars. */
    public val PillShape: Shape = RoundedCornerShape(50)

    /** The height of a chip. */
    public val ChipHeight: Dp = 32.dp

    /** The space between chips, across and down. */
    public val ChipSpacing: Dp = 6.dp

    /** The narrowest the text field gets before it moves to a line of its own. */
    public val FieldMinWidth: Dp = 96.dp

    /** The number of suggestions shown at most. */
    public const val MaxSuggestions: Int = 6

    /** Field colors from the current Material 3 color scheme. */
    @Composable
    public fun colors(): TagInputColors {
        val scheme = MaterialTheme.colorScheme
        return TagInputColors(
            container = Color.Transparent,
            border = scheme.outline,
            focusedBorder = scheme.primary,
            errorBorder = scheme.error,
            label = scheme.onSurfaceVariant,
            focusedLabel = scheme.primary,
            text = scheme.onSurface,
            placeholder = scheme.onSurfaceVariant,
            cursor = scheme.primary,
            supporting = scheme.onSurfaceVariant,
            error = scheme.error,
            suggestionContainer = scheme.surfaceContainer,
            suggestionText = scheme.onSurface,
            suggestionHighlight = scheme.secondaryContainer,
            suggestionMatch = scheme.primary,
        )
    }

    /** Chip colors from the current Material 3 color scheme for [style]. */
    @Composable
    public fun chipColors(style: TagChipStyle = TagChipStyle.Filled): TagChipColors {
        val scheme = MaterialTheme.colorScheme
        return when (style) {
            TagChipStyle.Filled -> TagChipColors(
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                outline = Color.Transparent,
                selectedContainer = scheme.primary,
                selectedContent = scheme.onPrimary,
                disabledContent = scheme.onSurface.copy(alpha = 0.38f),
            )
            TagChipStyle.Outlined -> TagChipColors(
                container = Color.Transparent,
                content = scheme.onSurfaceVariant,
                outline = scheme.outline,
                selectedContainer = scheme.primaryContainer,
                selectedContent = scheme.onPrimaryContainer,
                disabledContent = scheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }

    /** Background colors for [TagAvatar], picked per text with `TagAvatars.colorIndex`. */
    public val AvatarPalette: List<Pair<Color, Color>> = listOf(
        Color(0xFF3F51B5) to Color.White,
        Color(0xFF00796B) to Color.White,
        Color(0xFFC2185B) to Color.White,
        Color(0xFF6D4C41) to Color.White,
        Color(0xFF7B1FA2) to Color.White,
        Color(0xFFE65100) to Color.White,
        Color(0xFF2E7D32) to Color.White,
        Color(0xFF0277BD) to Color.White,
    )
}
