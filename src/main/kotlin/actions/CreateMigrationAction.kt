/**
 * CreateMigrationAction.kt
 * -------------------------
 * Defines the "Create Migration" action available from the IDE Tools menu.
 *
 * When triggered, this action:
 *  1. Prompts the user for a migration name using an input dialog.
 *  2. Detects the project's package manager (npm, yarn, or pnpm).
 *  3. Retrieves the current environment from [EnvManager].
 *  4. Executes the appropriate `sequelize-cli migration:generate` command
 *     inside the shared terminal tab managed by [TerminalRunner].
 *  5. Displays a success notification once the migration is created.
 *
 * This action provides a convenient one-click way to scaffold new Sequelize
 * migrations directly from the IDE without opening a terminal manually.
 *
 * Example workflow:
 * ```
 * Tools → Create Migration
 * → Prompt: "Enter migration name"
 * → Executes: npx sequelize-cli migration:generate --name <name> --env development
 * → Shows: "Migration '<name>' created successfully"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: provides the current environment (e.g., "development").
 *  - [PackageManagerDetector]: determines the package manager used by the project.
 *  - [TerminalRunner]: executes the Sequelize CLI command inside the IDE terminal.
 *  - [Notif]: displays success/failure notifications.
 *  - [Labels]: provides i18n strings for UI labels and messages.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of migration generation via terminal.
 *
 * @license
 *   MIT License
 */

package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import core.EnvManager
import core.Notif
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels

/**
 * Action registered under the Tools menu that allows the user to generate
 * a new Sequelize migration file via the IDE UI.
 *
 * Implements [DumbAware] to remain available during indexing.
 */
class CreateMigrationAction : AnAction(), DumbAware {

    /**
     * Specifies that this action can safely run on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Invoked when the action is triggered from the Tools menu or the command palette.
     * Prompts the user for a migration name, builds the Sequelize CLI command,
     * and executes it in the shared terminal tab.
     *
     * @param e The action event containing the current project context.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)
        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        // Prompt the user for the migration name (localized message)
        val name = Messages.showInputDialog(
            project,
            Labels.t("askMigrationName"),
            Labels.t("appTitle"),
            null
        )?.trim() ?: return
        if (name.isEmpty()) return

        // Build the sequelize-cli command with package manager prefix
        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "migration:generate", "--name", name, "--env", env)
        )

        // Execute in the persistent terminal tab
        TerminalRunner.runInTerminal(project, cmd)

        // Show success notification
        Notif.success(project, Labels.t("notifMigrationCreated", "name" to name))
    }
}
