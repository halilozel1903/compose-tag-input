package io.github.halilozel1903.taginput.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Small icons drawn on a 24 unit grid, so the sample needs no icon library. They are tinted where they are used. */
object DemoIcons {
    val Mail = icon("Mail", "M3 5h18v14H3V5zm2 2.4V17h14V7.4l-7 4.6-7-4.6zM6.2 7l5.8 3.8L17.8 7H6.2z")
    val Edit = icon("Edit", "M3 17.2V21h3.8l11-11-3.8-3.8-11 11zM20.7 7a1 1 0 0 0 0-1.4l-2.3-2.3a1 1 0 0 0-1.4 0l-1.8 1.8 3.8 3.8L20.7 7z")
    val Person = icon("Person", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-4 0-8 2-8 5v1h16v-1c0-3-4-5-8-5z")

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(pathData), pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
            .build()
}
