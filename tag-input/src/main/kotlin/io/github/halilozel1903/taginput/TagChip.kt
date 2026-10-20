package io.github.halilozel1903.taginput

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.taginput.core.TagAvatars

/**
 * One chip. [TagInput] draws these for its tags, and you can use it on its own, for example to show tags read-only.
 *
 * @param selected drawn with the selected colors, as after Backspace in a [TagInput].
 * @param leading an avatar ([TagAvatar]) or an icon ([TagIcon]) before the text.
 * @param onRemove shows a remove button when set.
 * @param onClick makes the chip clickable when set.
 */
@Composable
public fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    style: TagChipStyle = TagChipStyle.Filled,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = TagInputDefaults.ChipShape,
    colors: TagChipColors = TagInputDefaults.chipColors(style),
    leading: (@Composable () -> Unit)? = null,
    removeDescription: String = "Remove $text",
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val container = if (selected) colors.selectedContainer else colors.container
    val content = when {
        !enabled -> colors.disabledContent
        selected -> colors.selectedContent
        else -> colors.content
    }
    val borderColor = when {
        style != TagChipStyle.Outlined -> Color.Transparent
        selected -> colors.selectedContainer
        else -> colors.outline
    }
    var chipModifier = modifier
        .heightIn(min = TagInputDefaults.ChipHeight)
        .clip(shape)
        .background(container, shape)
    if (style == TagChipStyle.Outlined) chipModifier = chipModifier.border(1.dp, borderColor, shape)
    if (onClick != null && enabled) {
        // Chips never take focus: the text field keeps it, so Backspace and the arrow keys keep working.
        chipModifier = chipModifier
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick)
    }
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            modifier = chipModifier.padding(
                start = if (leading != null) 4.dp else 12.dp,
                end = if (onRemove != null && enabled) 4.dp else 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { leading() }
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 260.dp),
            )
            if (onRemove != null && enabled) {
                Spacer(Modifier.size(4.dp))
                RemoveButton(onRemove, removeDescription, content)
            }
        }
    }
}

@Composable
private fun RemoveButton(onRemove: () -> Unit, description: String, tint: Color) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .focusProperties { canFocus = false }
            .clickable(interactionSource = interaction, indication = ripple(bounded = true), onClick = onRemove)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(TagIcons.Close, contentDescription = null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(16.dp))
    }
}

/**
 * A round avatar with initials from [text] (see `TagAvatars.initials`) on a color picked from
 * [TagInputDefaults.AvatarPalette] for that text.
 */
@Composable
public fun TagAvatar(
    text: String,
    modifier: Modifier = Modifier,
    palette: List<Pair<Color, Color>> = TagInputDefaults.AvatarPalette,
) {
    val (background, foreground) = palette[TagAvatars.colorIndex(text, palette.size)]
    Box(
        modifier = modifier
            .size(24.dp)
            .background(background, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = TagAvatars.initials(text),
            color = foreground,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            maxLines = 1,
        )
    }
}

/** An icon for a chip's leading slot, tinted with the chip's content color. */
@Composable
public fun TagIcon(imageVector: ImageVector, modifier: Modifier = Modifier) {
    Image(
        imageVector = imageVector,
        contentDescription = null,
        colorFilter = ColorFilter.tint(LocalContentColor.current),
        modifier = modifier.size(18.dp),
    )
}

/** Icons the library draws itself, so it needs no icon dependency. */
public object TagIcons {
    /** A close mark, used on remove buttons. */
    public val Close: ImageVector = icon(
        "Close",
        "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
    )

    /** A tag. */
    public val Tag: ImageVector = icon(
        "Tag",
        "M21.4 11.6l-9-9A2 2 0 0 0 11 2H4a2 2 0 0 0-2 2v7c0 .55.22 1.05.59 1.42l9 9a2 2 0 0 0 2.82 0l7-7a2 2 0 0 0 0-2.82zM6.5 8a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z",
    )

    /** A hash sign. */
    public val Hash: ImageVector = icon(
        "Hash",
        "M9.6 3h2l-.7 5h4l.7-5h2l-.7 5H20v2h-3.4l-.6 4H19v2h-3.3l-.7 5h-2l.7-5h-4l-.7 5H7l.7-5H4v-2h4l.6-4H5V8h3.9l.7-5zm.9 7l-.6 4h4l.6-4h-4z",
    )

    /** A star. */
    public val Star: ImageVector = icon("Star", "M12 2l3 6.9 7.5.7-5.7 5 1.7 7.4L12 18.1 5.5 22l1.7-7.4-5.7-5 7.5-.7L12 2z")

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(pathData), pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
            .build()
}
