package io.github.halilozel1903.taginput.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** An indigo theme with a coral accent for the Tag Input sample. */
@Composable
fun SampleTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFFBAC3FF),
            onPrimary = Color(0xFF1A2678),
            primaryContainer = Color(0xFF333F90),
            onPrimaryContainer = Color(0xFFDEE0FF),
            secondary = Color(0xFFC4C5DD),
            onSecondary = Color(0xFF2D2F42),
            secondaryContainer = Color(0xFF3B3E5A),
            onSecondaryContainer = Color(0xFFE0E1F9),
            tertiary = Color(0xFFFFB59E),
            onTertiary = Color(0xFF5D1900),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            background = Color(0xFF121318),
            onBackground = Color(0xFFE3E1E9),
            surface = Color(0xFF121318),
            onSurface = Color(0xFFE3E1E9),
            surfaceVariant = Color(0xFF45464F),
            onSurfaceVariant = Color(0xFFC6C5D0),
            surfaceContainerLow = Color(0xFF1A1B21),
            surfaceContainer = Color(0xFF1F1F25),
            surfaceContainerHigh = Color(0xFF292A2F),
            outline = Color(0xFF90909A),
            outlineVariant = Color(0xFF45464F),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF4A57A9),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDEE0FF),
            onPrimaryContainer = Color(0xFF00105C),
            secondary = Color(0xFF5B5D72),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0E1F9),
            onSecondaryContainer = Color(0xFF181A2C),
            tertiary = Color(0xFFA0401F),
            onTertiary = Color.White,
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            background = Color(0xFFFBF8FF),
            onBackground = Color(0xFF1B1B21),
            surface = Color(0xFFFBF8FF),
            onSurface = Color(0xFF1B1B21),
            surfaceVariant = Color(0xFFE3E1EC),
            onSurfaceVariant = Color(0xFF46464F),
            surfaceContainerLow = Color(0xFFF5F2FA),
            surfaceContainer = Color(0xFFEFEDF4),
            surfaceContainerHigh = Color(0xFFE9E7EF),
            outline = Color(0xFF767680),
            outlineVariant = Color(0xFFC6C5D0),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
