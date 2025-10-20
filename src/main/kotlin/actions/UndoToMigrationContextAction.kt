/**
 * UndoToMigrationContextAction.kt
 * -------------------------------
 * Defines a context (right-click) action that allows developers to undo
 * **all Sequelize migrations up to a specific migration file** directly
 * from the `/migrations` directory in the project tree.
 *
 * When the user right-clicks a migration file and triggers this action:
 *  1. The plugin checks that the selected file is a valid `.js` or `.ts` migration.
 *  2. The current environment is retrieved from [EnvManager].
 *  3. A confirmation dialog is displayed to prevent accidental rollbacks.
 *  4. The command `sequelize-cli db:migrate:undo:all --to <file>` is executed
 *     inside the persistent terminal tab managed by [TerminalRunner].
 *  5. A localized success notification is displayed through [Notif].
 *
 * This context action provides a safe, convenient way to roll back migrations
 * to a specific point in time without manually typing CLI commands.
 *
 * Example workflow:
 * ```
 * Right-click → Undo All Up To This Migration
 * → Prompt: "Undo all migrations up to 20241018151234-create-users.js in 'development'?"
 * → Executes: npx sequelize-cli db:migrate:undo:all --to 20241018151234-create-users.js --env development
 * → Shows: "Successfully reverted migrations up to 20241018151234-create-users.js"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Provides the active Sequelize environment (e.g., "development").
 *  - [PackageManagerDetector]: Detects which package manager (npm, yarn, pnpm) to use.
 *  - [TerminalRunner]: Executes the Sequelize CLI command inside the IDE terminal.
 *  - [Notif]: Displays localized notifications.
 *  - [Labels]: Provides i18n strings for prompts and messages.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of targeted multi-migration rollback.
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
 * Context-menu action that executes
 * `sequelize-cli db:migrate:undo:all --to <file>` when right-clicking
 * a migration file inside the `/migrations` directory.
 *
 * Implements [DumbAware] so it remains active during IDE indexing.
 */
class UndoToMigrationContextAction : AnAction(), DumbAware {

    /**
     * Specifies that this action can safely execute on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Controls visibility of this action in the context menu.
     * Only shown for `.js` or `.ts` files located inside `/migrations/`.
     *
     * @param e The [AnActionEvent] containing the current file context.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null &&
                    vf.path.replace("\\", "/").contains("/migrations/") &&
                    (vf.name.endsWith(".js") || vf.name.endsWith(".ts"))
    }

    /**
     * Handles the rollback operation:
     *  - Confirms the action with the user.
     *  - Detects the package manager and environment.
     *  - Executes the Sequelize CLI command.
     *  - Displays a localized success notification.
     *
     * @param e The [AnActionEvent] triggered from the context menu.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        // Confirm rollback with the user
        val ok = Messages.showYesNoDialog(
            project,
            Labels.t("confirmUndoToBody", "file" to vf.name, "env" to env),
            Labels.t("confirmUndoToTitle"),
            null
        )
        if (ok != Messages.YES) return

        // Detect package manager and execute the command
        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)
        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "db:migrate:undo:all", "--to", vf.name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)

        // Show success notification
        Notif.success(project, Labels.t("notifUndoToDone", "file" to vf.name))
    }
}
