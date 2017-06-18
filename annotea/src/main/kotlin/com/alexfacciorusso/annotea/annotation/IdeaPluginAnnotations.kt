package com.alexfacciorusso.annotea.annotation

/**
 * @author alexfacciorusso
 */

/**
 * Annotates a class as ad Idea plugin.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class IdeaPlugin(
        val url: String,
        val id: String,
        val name: String,
        val description: String = "",
        val version: String = "",
        val vendor: Vendor = Vendor(""),
        val ideaVersion: IdeaVersion = IdeaVersion(-1, -1),
        val changeNotes: ChangeNotes = ChangeNotes(""),
        vararg val dependencies: Dependency
)

/**
 * The vendor of the plugin.
 *
 * @param name The vendor name
 * @param url The vendor homepage
 * @param email The vendor email
 * @param logo The path of the 16x16 vendor logo
 */
annotation class Vendor(
        val name: String,
        val url: String = "",
        val email: String = "",
        val logo: String = ""
)

annotation class IdeaVersion(
        val sinceBuild: Int,
        val untilBuild: Int
)

annotation class ChangeNotes(
        val text: String
)

annotation class Dependency(
        val name: String,
        val optional: Boolean = false,
        val configFile: String = ""
)

annotation class Action(
        val id: String,
        val text: String,
        val description: String = "",
        vararg val keyboardShortcuts: KeyboardShortcut
)

annotation class KeyboardShortcut(
        val firstKeystroke: String,
        val secondKeystroke: String = "",
        val keymap: String = "\$default"
)