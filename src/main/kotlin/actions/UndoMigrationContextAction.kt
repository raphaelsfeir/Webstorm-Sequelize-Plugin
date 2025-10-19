/**
 * UndoMigrationContextAction.kt
 * -----------------------------
 * Defines a context (right-click) action that allows developers to undo
 * a specific Sequelize migration file directly from the `/migrations` directory.
 *
 * When the user right-clicks a migration file and triggers this action:
 *  1. The plugin confirms that the file is a valid `.js` or `.ts` migration.
 *  2. The current environment is retrieved from [EnvManager].
 *  3. The package manager (npm, yarn, or pnpm) is detected automatically.
 *  4. The command `sequelize-cli db:migrate:undo --name <file>` is executed
 *     inside the shared terminal tab managed by [TerminalRunner].
 *  5. A localized success notification is displayed through [Notif].
 *
 * This provides a fast, contextual way to revert a specific migration
 * without typing commands in a terminal, directly from the project view.
 *
 * Example workflow:
 * ```
 * Right-click → Undo This Migration
 * → Executes: npx sequelize-cli db:migrate:undo --name 20241018151234-create-users.js --env development
 * → Shows: "Migration '20241018151234-create-users.js' has been reverted successfully"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Provides the current environment (e.g., "development").
 *  - [PackageManagerDetector]: Detects which package manager (npm/yarn/pnpm) to use.
 *  - [TerminalRunner]: Executes Sequelize commands in the IDE terminal.
 *  - [Notif]: Displays localized notifications.
 *  - [Labels]: Provides internationalized UI strings.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of targeted migration rollback.
 *
 * @license
 *   MIT License
 */

package actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.LocalFileSystem
import core.EnvManager
import core.Notif
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels

/**
 * Context-menu action that runs `sequelize-cli db:migrate:undo --name <file>`
 * when right-clicking a specific migration in the `/migrations` directory.
 *
 * Implements [DumbAware] to ensure availability even during indexing.
 */
class UndoMigrationContextAction : AnAction(), DumbAware {

    /**
     * Specifies that this action executes safely on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Determines whether this action should appear in the context menu.
     * The option is visible only for `.js` or `.ts` files under `/migrations/`.
     *
     * @param e The [AnActionEvent] containing the currently selected file context.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null &&
                    vf.path.replace("\\", "/").contains("/migrations/") &&
                    (vf.name.endsWith(".js") || vf.name.endsWith(".ts"))
    }

    /**
     * Handles the logic of undoing a specific migration:
     *  - Detects the package manager and environment.
     *  - Executes the corresponding `sequelize-cli` undo command.
     *  - Displays a localized notification upon success.
     *
     * @param e The [AnActionEvent] triggered from the context menu.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // Detect package manager and environment
        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)
        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        // Build and execute the Sequelize undo command
        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "db:migrate:undo", "--name", vf.name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)

        // Display success notification
        Notif.success(project, Labels.t("notifMigrationUndo", "file" to vf.name))
    }
}
