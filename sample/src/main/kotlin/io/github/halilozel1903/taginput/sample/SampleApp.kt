package io.github.halilozel1903.taginput.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A bottom bar with one form per tab on phones, and all three forms side by side on screens 840 dp and wider.
 * Each scene is run (and reported ready) by the form it belongs to.
 */
@Composable
fun SampleApp(scene: Scene?) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            WideLayout(scene)
        } else {
            var tab by rememberSaveable { mutableStateOf(scene?.tab ?: Tab.Recipients) }
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { entry ->
                            NavigationBarItem(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                icon = { TabIcon(entry) },
                                label = { Text(entry.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        DemoBadge()
                        when (tab) {
                            Tab.Recipients -> RecipientsForm(scene)
                            Tab.Post -> PostForm(scene)
                            Tab.Skills -> SkillsForm(scene)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WideLayout(scene: Scene?) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tag input", style = MaterialTheme.typography.headlineMedium)
            Box(Modifier.padding(start = 16.dp)) { DemoBadge(bottom = 0.dp) }
        }
        Row(
            Modifier.fillMaxSize().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                Modifier.weight(1f).widthIn(max = 640.dp).verticalScroll(rememberScrollState()),
            ) {
                RecipientsForm(scene)
            }
            Column(
                Modifier.weight(1f).widthIn(max = 640.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PostForm(scene)
                SkillsForm(scene)
            }
        }
    }
}

@Composable
private fun TabIcon(tab: Tab) {
    val icon: ImageVector = when (tab) {
        Tab.Recipients -> DemoIcons.Mail
        Tab.Post -> DemoIcons.Edit
        Tab.Skills -> DemoIcons.Person
    }
    Image(icon, contentDescription = null, colorFilter = ColorFilter.tint(LocalContentColor.current), modifier = Modifier.size(24.dp))
}

@Composable
private fun DemoBadge(bottom: androidx.compose.ui.unit.Dp = 12.dp) {
    Text(
        text = "Demo data · nothing is sent or saved",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.fillMaxWidth(fraction = 1f).padding(bottom = bottom),
    )
}
