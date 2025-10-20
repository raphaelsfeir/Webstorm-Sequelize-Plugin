/**
 * GenerateMigrationContextAction.kt
 * ---------------------------------
 * Defines the context (right-click) action that allows developers to generate
 * a new Sequelize migration directly from the `/migrations` directory.
 *
 * When a user right-clicks within the `migrations` folder and triggers this action:
 *  1. They are prompted for a migration name via an input dialog.
 *  2. The plugin detects the project's module type (ESM or CommonJS).
 *  3. The migration file is generated natively using [MigrationScaffolder]
 *     with the appropriate syntax and file extension.
 *  4. The file is saved under `migrations/<timestamp>-<name>.<ext>`.
 *  5. A localized success notification is shown through [Notif].
 *
 * This action improves developer workflow by scaffolding Sequelize migrations
 * contextually from the project tree, without invoking external CLI commands.
 *
 * Example workflow:
 * ```
 * Right-click → New Migration
 * → Prompt: "Enter migration name"
 * → Creates: migrations/20251019215320-create-users.mjs
 * → Shows: "Migration '20251019215320-create-users.mjs' created successfully"
 * ```
 *
 * Dependencies:
 *  - [ModuleKindDetector]: Detects whether the project uses ESM or CommonJS.
 *  - [MigrationScaffolder]: Generates the migration file content and name.
 *  - [EnvManager]: (optional) Current environment, kept for future features.
 *  - [Notif]: Displays success/failure notifications.
 *  - [Labels]: Supplies localized strings for UI text.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.1.0 — Switched to native ESM/CommonJS-aware migration generation.
 *
 * @license
 *   MIT License
 */

package actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import core.*
import i18n.Labels
import java.nio.file.Files

/**
 * Context-menu action that allows the user to create a new Sequelize migration
 * when right-clicking inside a `/migrations` directory.
 *
 * Implements [DumbAware] so it remains available during IDE indexing.
 */
class GenerateMigrationContextAction : AnAction(), DumbAware {

    /** Runs safely on a background thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Show the action only when the user right-clicks a file or folder inside `/migrations`.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf.isInMigrationsDir()
    }

    /**
     * Prompts for a migration name, detects module kind, generates the file,
     * and shows a success notification.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Prompt for migration name
        val name = Messages.showInputDialog(
            project,
            Labels.t("askMigrationName"),
            Labels.t("appTitle"),
            null
        )?.trim() ?: return
        if (name.isEmpty()) return

        // Detect module type (ESM or CommonJS) from project root
        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val moduleInfo = ModuleKindDetector.detect(rootVf)
        val kind = moduleInfo.kind
        val ext = moduleInfo.preferredExt

        // Build migration template (empty createTable example by default)
        val content = MigrationScaffolder.renderCreateTableTemplate(
            kind = kind,
            table = "example_table",
            columns = """
                id: { type: Sequelize.UUID, defaultValue: Sequelize.literal('gen_random_uuid()'), primaryKey: true },
                created_at: { type: Sequelize.DATE, allowNull: false, defaultValue: Sequelize.NOW },
                updated_at: { type: Sequelize.DATE, allowNull: false, defaultValue: Sequelize.NOW }
            """.trimIndent()
        )

        // Write file to migrations directory
        val basePath = project.basePath ?: return
        val path = MigrationScaffolder.timestampedFilename(basePath, ext, name)
        Files.createDirectories(path.parent)
        Files.writeString(path, content)

        // Notify success
        Notif.success(project, Labels.t("notifMigrationCreated", "name" to path.fileName.toString()))
    }

    /** Checks whether the selected file resides within a `/migrations` directory. */
    private fun VirtualFile?.isInMigrationsDir(): Boolean {
        if (this == null) return false
        val normalizedPath = path.replace("\\", "/")
        return normalizedPath.contains("/migrations")
    }
}
