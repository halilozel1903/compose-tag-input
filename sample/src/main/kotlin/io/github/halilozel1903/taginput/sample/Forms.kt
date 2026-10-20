package io.github.halilozel1903.taginput.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.taginput.TagAvatar
import io.github.halilozel1903.taginput.TagChipStyle
import io.github.halilozel1903.taginput.TagIcon
import io.github.halilozel1903.taginput.TagIcons
import io.github.halilozel1903.taginput.TagInput
import io.github.halilozel1903.taginput.TagInputDefaults
import io.github.halilozel1903.taginput.core.TagError
import io.github.halilozel1903.taginput.core.TagInputConfig
import io.github.halilozel1903.taginput.core.TagValidators
import io.github.halilozel1903.taginput.rememberTagInputState
import kotlinx.coroutines.delay

/** Waits for a few frames so the scene is laid out before it is reported. */
private suspend fun settle(millis: Long = 1_200) {
    repeat(3) { withFrameNanos { } }
    delay(millis)
}

/** An email form: To and Cc take addresses, checked with the email validator and shown with avatars. */
@Composable
fun RecipientsForm(scene: Scene?) {
    val config = remember { TagInputConfig(validator = TagValidators.email()) }
    val to = rememberTagInputState(
        initialTags = listOf("alex.rivera@example.com", "sam.lee@example.com", "priya.shah@example.net"),
        config = config,
    )
    val cc = rememberTagInputState(initialTags = listOf("jordan.kim@example.org"), config = config)
    var subject by rememberSaveable { mutableStateOf("Planning notes for the demo project") }
    var body by rememberSaveable {
        mutableStateOf("Hi all,\n\nHere are the notes from today. Reply with anything I missed.\n\nAlex")
    }

    LaunchedEffect(scene) {
        when (scene) {
            Scene.Recipients -> {
                settle()
                logScene("SCENE_READY ${scene.key}")
            }
            Scene.Validation -> {
                // Typed as a user would: a valid address, then one without a domain ending, then a comma.
                to.setText("taylor.morgan@example.com, jordan.kim@example,")
                settle()
                if (to.error is TagError.Invalid && to.text == "jordan.kim@example") {
                    logScene("SCENE_READY ${scene.key}")
                } else {
                    logScene("SCENE_FAILED ${scene.key}: expected an invalid address error, got ${to.error}", failed = true)
                }
            }
            else -> Unit
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("New message", style = MaterialTheme.typography.headlineSmall)
        TagInput(
            state = to,
            suggestions = DemoData.contacts,
            label = "To",
            placeholder = "Add recipients",
            supportingText = "Comma, space or Enter adds an address",
            chipShape = TagInputDefaults.PillShape,
            chipLeading = { TagAvatar(it.text) },
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth(),
        )
        TagInput(
            state = cc,
            suggestions = DemoData.contacts,
            label = "Cc",
            placeholder = "Add recipients",
            chipShape = TagInputDefaults.PillShape,
            chipLeading = { TagAvatar(it.text) },
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Message") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${to.tags.size + cc.tags.size} recipients",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { to.commit(); cc.commit() }, enabled = to.tags.isNotEmpty() && to.error == null) {
                Text("Send (demo)")
            }
        }
    }
}

/** A blog post form: lowercase, dashed tags with suggestions. Only comma and Enter commit, so spaces become dashes. */
@Composable
fun PostForm(scene: Scene?) {
    val config = remember {
        TagInputConfig(
            separators = setOf(','),
            maxTags = 8,
            validator = TagValidators.allOf(
                TagValidators.maxLength(24),
                TagValidators.pattern(Regex("[a-z0-9-]+"), "Use letters, digits and dashes"),
            ),
            transform = { it.lowercase().replace(Regex("\\s+"), "-") },
        )
    }
    val tags = rememberTagInputState(initialTags = listOf("android", "kotlin"), config = config)
    var title by rememberSaveable { mutableStateOf("Getting started with chip inputs") }
    var summary by rememberSaveable {
        mutableStateOf("A short walkthrough of a tag field with suggestions, written for the demo blog.")
    }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(scene) {
        if (scene == Scene.Suggestions) {
            tags.setText("comp")
            settle(300)
            tags.requestFocus()
            settle(600)
            // The screenshot shows the list, not the soft keyboard.
            keyboard?.hide()
            tags.moveHighlight(1)
            tags.moveHighlight(1)
            settle(800)
            keyboard?.hide()
            settle(600)
            if (tags.highlightedIndex != null) {
                logScene("SCENE_READY ${scene.key}")
            } else {
                logScene("SCENE_FAILED ${scene.key}: the suggestion list did not open", failed = true)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Write a post", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TagInput(
            state = tags,
            suggestions = DemoData.topics,
            label = "Tags",
            placeholder = "Add a topic",
            supportingText = "Comma or Enter adds a tag",
            chipLeading = { TagIcon(TagIcons.Hash) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Summary") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A profile form: up to five skills, as outlined chips, with a semicolon as an extra separator. */
@Composable
fun SkillsForm(scene: Scene?) {
    val config = remember { TagInputConfig(separators = linkedSetOf(',', ';'), maxTags = 5) }
    val skills = rememberTagInputState(initialTags = listOf("Kotlin", "Jetpack Compose", "Testing"), config = config)
    var name by rememberSaveable { mutableStateOf("Demo User") }

    LaunchedEffect(scene) {
        if (scene == Scene.TabletForm) {
            // Pasted as one line: two more skills fill the field, the third is refused by the limit and stays in the field with the error.
            skills.setText("Accessibility; Gradle; Mentoring;")
            settle()
            if (skills.isFull) {
                logScene("SCENE_READY ${scene.key}")
            } else {
                logScene("SCENE_FAILED ${scene.key}: expected 5 skills, got ${skills.tags.size}", failed = true)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Text("Your profile", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TagInput(
            state = skills,
            suggestions = DemoData.skills,
            label = "Skills (up to 5)",
            placeholder = "Add a skill",
            supportingText = "Comma, semicolon or Enter adds a skill",
            chipStyle = TagChipStyle.Outlined,
            chipLeading = { TagIcon(TagIcons.Star) },
            errorText = { error ->
                when (error) {
                    is TagError.LimitReached -> "You can list up to ${error.max} skills"
                    else -> error.defaultMessage
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
