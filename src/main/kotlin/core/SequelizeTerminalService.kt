/**
 * SequelizeTerminalService.kt
 * ---------------------------
 * Provides a persistent terminal session within the IDE for running Sequelize commands.
 *
 * This service manages a single shared terminal tab called "Sequelize Runner"
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
 *   TerminalRunner.runInTerminal(project, "npx sequelize-cli db:migrate")
 *
 * Internally, this class is used by TerminalRunner and other plugin components
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
 */
@Service(Service.Level.PROJECT)
class SequelizeTerminalService(private val project: Project) : Disposable {

    /** The current terminal widget instance used by the plugin. */
    @Volatile
    private var widget: ShellTerminalWidget? = null

    /** Tracks the last working directory used by the terminal to avoid redundant `cd` commands. */
    @Volatile
    private var lastWorkingDir: String? = null

    /**
     * Executes a shell command inside the shared "Sequelize Runner" terminal tab.
     *
     * If the terminal does not exist yet, it is created automatically.
     * The working directory defaults to the project root unless specified otherwise.
     *
     * @param command The shell command to execute (e.g., "npx sequelize-cli db:migrate").
     * @param workingDir Optional working directory. Defaults to the project’s base path.
     */
    fun run(command: String, workingDir: String? = null) {
        val wd = workingDir ?: (project.basePath ?: ".")
        val shell = ensureWidget(wd)

        // Ensure the terminal tool window is visible so the user can see output.
        val toolWindow = TerminalToolWindowManager.getInstance(project).getToolWindow()
        toolWindow?.show()

        // Change directory only if it differs from the previous one.
        // This prevents redundant `cd` commands when running multiple actions in the same folder.
        if (lastWorkingDir == null ||
            Paths.get(lastWorkingDir!!).normalize().toAbsolutePath() != Paths.get(wd).normalize().toAbsolutePath()
        ) {
            shell.executeCommand("cd ${quotePath(wd)}")
            lastWorkingDir = wd
        }

        // Execute the given shell command (appears and runs in the IDE’s terminal).
        shell.executeCommand(command)
    }

    /**
     * Ensures that the Sequelize terminal widget exists and is valid.
     * If it has not been created yet or was previously disposed, a new one is created.
     *
     * @param initialWorkingDir The directory to initialize the shell in.
     * @return A valid ShellTerminalWidget instance ready for command execution.
     */
    private fun ensureWidget(initialWorkingDir: String): ShellTerminalWidget {
        val existing = widget

        // Reuse an existing terminal if it is still active and not disposed.
        @Suppress("DEPRECATION")
        if (existing != null && !Disposer.isDisposed(existing)) {
            return existing
        }

        // Create a new local shell widget using the TerminalToolWindowManager API.
        val manager = TerminalToolWindowManager.getInstance(project)
        val shell: ShellTerminalWidget = manager.createLocalShellWidget(initialWorkingDir, TAB_TITLE)

        widget = shell
        lastWorkingDir = initialWorkingDir
        return shell
    }

    /**
     * Safely quotes a given file path for shell execution.
     * Handles paths with spaces or special characters.
     */
    private fun quotePath(path: String): String {
        val p: Path = Paths.get(path)
        return "\"${p.toString()}\""
    }

    /**
     * Disposes of the terminal widget when the project is closed or the service is unloaded.
     * Ensures that no background processes or terminal instances remain active.
     */
    override fun dispose() {
        try {
            widget?.let { Disposer.dispose(it) }
        } catch (_: Throwable) {
            // Suppress disposal exceptions to avoid polluting logs.
        } finally {
            widget = null
        }
    }

    companion object {
        /**
         * Retrieves the singleton SequelizeTerminalService instance for the given project.
         */
        fun getInstance(project: Project): SequelizeTerminalService = project.service()
    }
}
