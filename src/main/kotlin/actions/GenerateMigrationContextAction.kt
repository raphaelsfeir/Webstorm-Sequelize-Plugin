/**
 * GenerateMigrationContextAction.kt
 * ---------------------------------
 * Defines the context (right-click) action that allows developers to generate
 * a new Sequelize migration directly from the `/migrations` directory.
 *
 * When a user right-clicks within the `migrations` folder and triggers this action:
 *  1. They are prompted for a migration name via an input dialog.
 *  2. The plugin detects the project's package manager (npm, yarn, or pnpm).
 *  3. The current environment is fetched from [EnvManager].
 *  4. The appropriate `sequelize-cli migration:generate` command is executed
 *     inside the shared terminal tab managed by [TerminalRunner].
 *  5. A localized success notification is shown through [Notif].
 *
 * This action improves developer workflow by allowing Sequelize migrations
 * to be scaffolded contextually from the project tree, without switching to the terminal.
 *
 * Example workflow:
 * ```
 * Right-click → New Migration
 * → Prompt: "Enter migration name"
 * → Executes: npx sequelize-cli migration:generate --name <name> --env development
 * → Shows: "Migration '<name>' created successfully"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Provides the active environment (e.g., "development").
 *  - [PackageManagerDetector]: Determines which package manager to use.
 *  - [TerminalRunner]: Runs Sequelize commands in the IDE terminal.
 *  - [Notif]: Displays success/failure notifications.
 *  - [Labels]: Supplies localized strings for UI text.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of context-aware migration generation.
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
import core.EnvManager
import core.Notif
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels

/**
 * Context-menu action that allows the user to create a new Sequelize migration
 * when right-clicking inside a `/migrations` directory.
 *
 * Implements [DumbAware] so it remains available during IDE indexing.
 */
class GenerateMigrationContextAction : AnAction(), DumbAware {

    /**
     * Specifies that this action runs safely on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Determines whether the action should appear in the context menu.
     * It is only shown when the user right-clicks a file or folder inside `/migrations`.
     *
     * @param e The current action event containing the selected file reference.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf.isInMigrationsDir()
    }

    /**
     * Handles the main logic of the action:
     *  - Prompts the user for a migration name.
     *  - Detects the project's package manager and environment.
     *  - Executes the Sequelize CLI command inside the IDE terminal.
     *  - Displays a success notification.
     *
     * @param e The action event triggered from the IDE context menu.
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

        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)
        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        // Construct and execute the Sequelize CLI command
        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "migration:generate", "--name", name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)

        // Show success notification
        Notif.success(project, Labels.t("notifMigrationCreated", "name" to name))
    }

    /**
     * Checks whether the selected file resides within a `/migrations` directory.
     *
     * @receiver The [VirtualFile] to check.
     * @return `true` if the file path includes "/migrations", otherwise `false`.
     */
    private fun VirtualFile?.isInMigrationsDir(): Boolean {
        if (this == null) return false
        val normalizedPath = path.replace("\\", "/")
        return normalizedPath.contains("/migrations")
    }
}
