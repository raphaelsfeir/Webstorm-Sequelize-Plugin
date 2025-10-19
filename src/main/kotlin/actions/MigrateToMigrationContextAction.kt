/**
 * MigrateToMigrationContextAction.kt
 * ----------------------------------
 * Defines a context (right-click) action that allows developers to run
 * Sequelize migrations **up to a specific migration file** directly
 * from the `/migrations` directory in the project tree.
 *
 * When the user right-clicks a migration file and triggers this action:
 *  1. The action verifies that the selected file is a valid `.js` or `.ts` migration.
 *  2. The current environment is retrieved from [EnvManager].
 *  3. The user is asked to confirm the migration target via a yes/no dialog.
 *  4. The appropriate `sequelize-cli db:migrate --to <file>` command is executed
 *     inside the persistent terminal tab managed by [TerminalRunner].
 *  5. A localized success notification is displayed through [Notif].
 *
 * This provides a fast, contextual way to partially migrate the database without
 * typing CLI commands manually.
 *
 * Example workflow:
 * ```
 * Right-click → Migrate Up To This Migration
 * → Prompt: "Run migrations up to 20241018151234-create-users.js in 'development'?"
 * → Executes: npx sequelize-cli db:migrate --to 20241018151234-create-users.js --env development
 * → Shows: "Successfully migrated to 20241018151234-create-users.js"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Provides the active environment (e.g., "development").
 *  - [PackageManagerDetector]: Detects which package manager (npm/yarn/pnpm) to use.
 *  - [TerminalRunner]: Executes the Sequelize CLI command within the IDE terminal.
 *  - [Notif]: Displays success or error notifications.
 *  - [Labels]: Supplies localized strings for all UI messages and confirmations.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of targeted migration execution.
 *
 * @license
 *   MIT License
 */

package actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import core.EnvManager
import core.Notif
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels

/**
 * Context-menu action that runs `sequelize-cli db:migrate --to <file>` when
 * right-clicking a specific migration script in the `/migrations` folder.
 *
 * Implements [DumbAware] to remain available even during IDE indexing.
 */
class MigrateToMigrationContextAction : AnAction(), DumbAware {

    /**
     * Specifies that this action executes safely on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Controls the visibility of the action in the context menu.
     * The option is only shown for `.js` or `.ts` files located under `/migrations/`.
     *
     * @param e The current [AnActionEvent] containing context data such as the selected file.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null &&
                    vf.path.replace("\\", "/").contains("/migrations/") &&
                    (vf.name.endsWith(".js") || vf.name.endsWith(".ts"))
    }

    /**
     * Handles the main logic when the action is invoked:
     *  - Confirms the operation with the user.
     *  - Detects the package manager and environment.
     *  - Executes the Sequelize CLI command inside the IDE terminal.
     *  - Displays a localized success notification.
     *
     * @param e The [AnActionEvent] triggered from the context menu.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        // Ask for user confirmation before running a potentially destructive command
        val ok = Messages.showYesNoDialog(
            project,
            Labels.t("confirmMigrateToBody", "file" to vf.name, "env" to env),
            Labels.t("confirmMigrateToTitle"),
            null
        )
        if (ok != Messages.YES) return

        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)

        // Build and execute the Sequelize command
        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "db:migrate", "--to", vf.name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)

        // Display success notification
        Notif.success(project, Labels.t("notifMigrateToDone", "file" to vf.name))
    }
}
