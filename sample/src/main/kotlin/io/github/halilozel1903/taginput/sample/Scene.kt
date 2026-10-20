package io.github.halilozel1903.taginput.sample

/** Screenshot scenes, picked with the `scene` intent extra. */
enum class Scene(val key: String, val tab: Tab) {
    /** The email form with demo recipients as avatar chips. */
    Recipients("recipients", Tab.Recipients),

    /** The blog post form with "comp" typed and the suggestion list open. */
    Suggestions("suggestions", Tab.Post),

    /** The email form after an address without a domain ending was typed: the error is shown. */
    Validation("validation", Tab.Recipients),

    /** All three forms side by side (tablets in landscape), with the skills field full. */
    TabletForm("tablet-form", Tab.Skills),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

/** The sample's three demo forms. */
enum class Tab(val label: String) {
    Recipients("Email"),
    Post("Post"),
    Skills("Skills"),
}
