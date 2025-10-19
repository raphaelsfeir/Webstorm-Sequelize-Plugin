/**
 * SequelizeTerminalService.kt
 * ---------------------------
 * Provides a persistent terminal session within the IDE for running Sequelize commands.
 *
 * This service manages a single shared terminal tab called **"Sequelize Runner"**
 * for each IntelliJ project. All Sequelize-related CLI operations are executed
 * through this terminal to keep output centralized, persistent, and consistent.
 *
 * Responsibilities:
 *  - Create and manage a single persistent terminal widget per project.
 *  - Reuse the same terminal session for multiple Sequelize commands.
 *  - Automatically switch to the correct working directory (monorepo-compatible).
 *  - Dispose of resources properly when the project is closed.
 *
 * Example usage:
 * ```kotlin
 * TerminalRunner.runInTerminal(project, "npx sequelize-cli db:migrate")
 * ```
 *
 * Internally, this class is used by [TerminalRunner] and other plugin components
 * to abstract away terminal management details from higher-level actions.
 *
 * @see TerminalRunner
 * @see org.jetbrains.plugins.terminal.TerminalToolWindowManager
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of the persistent terminal service.
 *
 * @license
 *   MIT License
 */

package core

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.nio.file.Path
import java.nio.file.Paths

// The tab title displayed in the Terminal tool window.
private const val TAB_TITLE = "Sequelize Runner"

/**
 * A project-level service that manages a single, persistent terminal session
 * dedicated to executing Sequelize CLI commands.
 *
 * This allows all plugin features (migrations, seed operations, environment management, etc.)
 * to share the same terminal context for better UX and cleaner logs.
 */
@Service(Service.Level.PROJECT)
class SequelizeTerminalService(private val project: Project) : Disposable {

    /** Reference to the currently active terminal widget (if any). */
    @Volatile
    private var widget: ShellTerminalWidget? = null

    /**
     * Executes a shell command inside the shared “Sequelize Runner” terminal tab.
     *
     * If the terminal does not exist yet, it is created automatically.
     * The working directory defaults to the project root unless specified otherwise.
     *
     * @param command The shell command to execute (e.g., `"npx sequelize-cli db:migrate"`).
     * @param workingDir Optional working directory. Defaults to the project’s base path.
     */
    fun run(command: String, workingDir: String? = null) {
        val wd = workingDir ?: (project.basePath ?: ".")
        val shell = ensureWidget(wd)

        // Always switch to the correct directory before executing commands.
        // This is particularly important for multi-package (monorepo) projects.
        shell.executeCommand("cd ${quotePath(wd)}")
        shell.executeCommand(command)
    }

    /**
     * Ensures that the Sequelize terminal widget exists and is valid.
     * If it has not been created yet or was previously disposed, a new one is created.
     *
     * @param initialWorkingDir The directory to initialize the shell in.
     * @return A valid [ShellTerminalWidget] instance ready for command execution.
     */
    private fun ensureWidget(initialWorkingDir: String): ShellTerminalWidget {
        val existing = widget

        // Check that the existing terminal is still alive and usable.
        @Suppress("DEPRECATION")
        if (existing != null && !Disposer.isDisposed(existing)) {
            return existing
        }

        // Create a new local shell widget using the modern API (TerminalToolWindowManager).
        val manager = TerminalToolWindowManager.getInstance(project)
        val shell: ShellTerminalWidget = manager.createLocalShellWidget(initialWorkingDir, TAB_TITLE)

        widget = shell
        return shell
    }

    /**
     * Quotes the given path for shell safety (spaces and special characters).
     *
     * @param path The raw file system path.
     * @return A shell-safe, properly quoted path string.
     */
    private fun quotePath(path: String): String {
        val p: Path = Paths.get(path)
        return "\"${p.toString()}\""
    }

    /**
     * Disposes of the terminal widget when the project is closed or the service is unloaded.
     * Ensures that no terminal processes remain active after disposal.
     */
    override fun dispose() {
        try {
            widget?.let { Disposer.dispose(it) }
        } catch (_: Throwable) {
            // Suppress disposal exceptions to avoid cluttering logs.
        } finally {
            widget = null
        }
    }

    companion object {
        /**
         * Retrieves the singleton [SequelizeTerminalService] instance for the given project.
         *
         * @param project The current IntelliJ project context.
         * @return The service instance associated with this project.
         */
        fun getInstance(project: Project): SequelizeTerminalService = project.service()
    }
}
