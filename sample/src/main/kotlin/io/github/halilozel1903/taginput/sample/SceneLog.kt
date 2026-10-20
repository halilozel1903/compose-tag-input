package io.github.halilozel1903.taginput.sample

import android.util.Log

/** The logcat tag `scripts/screenshots.sh` watches. */
const val SCENE_TAG = "TagInputScene"

/** Tells `scripts/screenshots.sh` that a scene is on screen and settled, or that it failed. */
fun logScene(message: String, failed: Boolean = false) {
    if (failed) Log.e(SCENE_TAG, message) else Log.i(SCENE_TAG, message)
}
