/**
 * RunSeedContextAction.kt
 * -----------------------
 * Defines a context (right-click) action that allows developers to run
 * a specific Sequelize seed file directly from the `/seeders` directory.
 *
 * When a user right-clicks a seed file and triggers this action:
 *  1. The action verifies that the file is a valid `.js` or `.ts` seed script.
 *  2. The plugin detects the project's package manager (npm, yarn, or pnpm).
 *  3. The current environment is retrieved from [EnvManager].
 *  4. The command `sequelize-cli db:seed --seed <file>` is executed
 *     inside the shared terminal tab managed by [TerminalRunner].
 *  5. A localized success notification is displayed through [Notif].
 *
 * This context action offers a fast and convenient way to run
 * individual seed scripts directly from the project view,
 * without needing to open or type in a terminal manually.
 *
 * Example workflow:
 * ```
 * Right-click → Run This Seed
 * → Executes: npx sequelize-cli db:seed --seed 20241018160000-demo-user.js --env development
 * → Shows: "Seed file '20241018160000-demo-user.js' executed successfully"
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Provides the active environment (e.g., "development").
 *  - [PackageManagerDetector]: Detects the project's package manager.
 *  - [TerminalRunner]: Executes the Sequelize CLI command in the IDE terminal.
 *  - [Notif]: Displays success or error notifications.
 *  - [Labels]: Provides localized UI strings.
 *
 * @autor
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of context-based seed execution.
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
 * Context-menu action that executes a specific Sequelize seed file
 * when right-clicking it inside the `/seeders` directory.
 *
 * Implements [DumbAware] to ensure it remains active even during IDE indexing.
 */
class RunSeedContextAction : AnAction(), DumbAware {

    /**
     * Specifies that this action executes safely on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Controls the visibility of the context action.
     * Only shown for `.js` or `.ts` files located within `/seeders/`.
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
     * Executes the selected seed file by running
     * `sequelize-cli db:seed --seed <file> --env <environment>` in the IDE terminal.
     * After successful execution, a localized notification is displayed.
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

        // Build and execute the Sequelize command
        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "db:seed", "--seed", vf.name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)

        // Notify success
        Notif.success(project, Labels.t("notifSeedRun", "file" to vf.name))
    }
}
