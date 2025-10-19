/**
 * UndoSeedContextAction.kt
 * ------------------------
 * Defines a context (right-click) action that allows developers to undo
 * the execution of a specific Sequelize seed file directly from the `/seeders` directory.
 *
 * When a user right-clicks a seed file and triggers this action:
 *  1. The plugin verifies that the file is a valid `.js` or `.ts` seed script.
 *  2. The current environment is retrieved from [EnvManager].
 *  3. The package manager (npm, yarn, or pnpm) is automatically detected.
 *  4. The command `sequelize-cli db:seed:undo --seed <file>` is executed
 *     inside the persistent terminal tab managed by [TerminalRunner].
 *  5. A localized success notification is displayed through [Notif].
 *
 * This enables quick rollback of individual seed operations directly from
 * the project tree without needing to manually open or type commands in a terminal.
 *
 * Example workflow:
 * ```
 * Right-click → Undo This Seed
 * → Executes: npx sequelize-cli db:seed:undo --seed 20241018160000-demo-user.js --env development
 * → Shows: "Seed '20241018160000-demo-user.js' reverted successfully"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Provides the active environment (e.g., "development").
 *  - [PackageManagerDetector]: Detects which package manager (npm/yarn/pnpm) to use.
 *  - [TerminalRunner]: Executes the Sequelize CLI command in the IDE terminal.
 *  - [Notif]: Displays localized notifications.
 *  - [Labels]: Provides internationalized strings for dialogs and messages.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of context-based seed rollback.
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
 * Context-menu action that runs `sequelize-cli db:seed:undo --seed <file>`
 * when right-clicking a specific seed file inside the `/seeders` directory.
 *
 * Implements [DumbAware] to remain available even during IDE indexing.
 */
class UndoSeedContextAction : AnAction(), DumbAware {

    /**
     * Specifies that this action can safely execute on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Determines whether this action should be visible and enabled in the context menu.
     * The option appears only for `.js` or `.ts` files located within `/seeders/`.
     *
     * @param e The [AnActionEvent] containing the current selection context.
     */
    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null &&
                    vf.path.replace("\\", "/").contains("/seeders/") &&
                    (vf.name.endsWith(".js") || vf.name.endsWith(".ts"))
    }

    /**
     * Executes the Sequelize CLI command to undo a specific seed file.
     * Automatically detects the package manager and environment, and
     * displays a success notification upon completion.
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
            pm, listOf("sequelize-cli", "db:seed:undo", "--seed", vf.name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)

        // Display success notification
        Notif.success(project, Labels.t("notifSeedUndo", "file" to vf.name))
    }
}
