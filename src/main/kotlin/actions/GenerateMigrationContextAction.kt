/**
 * GenerateMigrationContextAction.kt
 * ---------------------------------
 * Defines the context-menu action that allows developers to generate
 * a new Sequelize migration directly from the `/migrations` directory.
 *
 * The goal is to offer a fast, IDE-native way to scaffold Sequelize migrations
 * without using the external `sequelize-cli`. The generated file respects the
 * project’s module system (ESM or CommonJS) and leverages resource-based templates.
 *
 * Workflow:
 *  1. User right-clicks within `/migrations` → selects “New Migration”.
 *  2. An input dialog prompts for the migration name.
 *  3. [ModuleKindDetector] determines whether the project uses ESM or CommonJS.
 *  4. [MigrationScaffolder] renders the correct migration template from `/resources/templates`.
 *  5. The resulting file is written under `migrations/<timestamp>-<name>.<ext>`.
 *  6. [Notif] shows a localized success message confirming creation.
 *
 * Notes:
 *  - The template is loaded from plugin resources, not generated inline.
 *  - ESM vs CJS detection honors historical `.mjs`, `.cjs`, or `.js` files
 *    and the `"type":"module"` field in package.json.
 *  - The created file is not empty: it includes a ready-to-edit boilerplate migration.
 *
 * Dependencies:
 *  - [ModuleKindDetector] → Determines module type and preferred extension.
 *  - [MigrationScaffolder] → Loads and renders Sequelize migration templates.
 *  - [Notif] → Displays success/warning notifications.
 *  - [Labels] → Provides localized strings for UI text.
 *
 * Example:
 * ```
 * Right-click → New Migration
 * → Prompt: "Enter migration name"
 * → Creates: migrations/20251020154532-create-users.mjs
 * → Notification: "Migration '20251020154532-create-users.mjs' created successfully"
 * ```
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.1.0 — Introduced template-based generation and automatic ESM/CJS detection.
 *
 * @license
 *   MIT License
 */
package actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import core.*
import i18n.Labels
import java.nio.file.Files
import java.nio.file.Path

/**
 * Context action available when right-clicking inside `/migrations`.
 * Uses native Kotlin-based migration scaffolding (no sequelize-cli).
 */
class GenerateMigrationContextAction : AnAction(), DumbAware {

    private val log = Logger.getInstance(GenerateMigrationContextAction::class.java)

    /** Ensures the action runs safely on a background thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Display this action only when the user right-clicks within a `/migrations` directory.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf.isInMigrationsDir()
    }

    /**
     * Main execution:
     *  1. Prompt for a migration name.
     *  2. Detect project module kind (ESM or CommonJS).
     *  3. Load the corresponding template from `/resources/templates`.
     *  4. Write the migration file to the `/migrations` directory.
     *  5. Notify the user of success or failure.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val basePath = project.basePath ?: return
        val rootVf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)

        log.info("[GenerateMigrationContextAction] Starting migration generation for project: $basePath")

        // Step 1 — Ask user for migration name
        val name = Messages.showInputDialog(
            project,
            Labels.t("askMigrationName"),
            Labels.t("appTitle"),
            null
        )?.trim() ?: return

        if (name.isEmpty()) {
            log.info("[GenerateMigrationContextAction] Cancelled: empty name")
            return
        }

        // Step 2 — Detect ESM/CJS type
        val moduleInfo = ModuleKindDetector.detect(rootVf)
        val kind = moduleInfo.kind
        val ext = moduleInfo.preferredExt
        log.info("[GenerateMigrationContextAction] Detected module kind=$kind ext=$ext (reason=${moduleInfo.reason})")

        // Step 3 — Render template from resource file
        val content = try {
            MigrationScaffolder.renderBlankTemplate(kind)
        } catch (t: Throwable) {
            log.warn("[GenerateMigrationContextAction] Failed to render template: ${t.message}", t)
            "// Failed to load migration template (${t.message})"
        }

        // Step 4 — Build final file path
        val path: Path = MigrationScaffolder.timestampedFilename(basePath, ext, name)
        log.info("[GenerateMigrationContextAction] Writing migration to $path")

        try {
            Files.createDirectories(path.parent)
            Files.writeString(path, content)

            // Step 5 — Notify success
            Notif.success(project, Labels.t("notifMigrationCreated", "name" to path.fileName.toString()))
            log.info("[GenerateMigrationContextAction] Migration created successfully at $path")

        } catch (ex: Throwable) {
            log.warn("[GenerateMigrationContextAction] Failed to write migration file: ${ex.message}", ex)
            Notif.warning(project, "Failed to create migration: ${ex.message ?: "Unknown error"}")
        }
    }

    /**
     * Helper to determine if a file or directory is inside `/migrations`.
     * Works even on Windows by normalizing backslashes.
     */
    private fun VirtualFile?.isInMigrationsDir(): Boolean {
        if (this == null) return false
        val normalizedPath = path.replace("\\", "/")
        return normalizedPath.contains("/migrations")
    }
}
