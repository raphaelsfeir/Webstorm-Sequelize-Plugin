/**
 * SequelizeTerminalService.kt
 * ---------------------------
 * Provides a persistent terminal session within the IDE for running Sequelize commands.
 *
 * This service manages a single shared terminal tab called **"Sequelize Runner"**
 * for each IntelliJ project. All Sequelize-related CLI operations are executed
 * through this terminal to keep output centralized and consistent.
 *
 * Responsibilities:
 *  - Create and manage one persistent terminal widget per project.
 *  - Execute shell commands safely within the Event Dispatch Thread (EDT).
 *  - Automatically switch the working directory for monorepo compatibility.
 *  - Dispose of resources cleanly when the project is closed.
 *
 * Typical usage:
 *  ```kotlin
 *  TerminalRunner.runInTerminal(project, "npx sequelize-cli db:migrate")
 *  ```
 *
 * This class is used internally by [TerminalRunner] and other plugin components.
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
 * dedicated to executing Sequelize commands.
 *
 * Key responsibilities:
 *  - Guarantees only one terminal tab exists per project.
 *  - Reuses the same terminal session for all commands.
 *  - Allows changing the working directory dynamically (useful for monorepos).
 *
 * This class abstracts away all terminal handling logic so other parts of the plugin
 * can simply call [run] to execute commands safely within the IDE terminal.
 */
@Service(Service.Level.PROJECT)
class SequelizeTerminalService(private val project: Project) : Disposable {

    /** Reference to the active terminal widget, if any. */
    @Volatile
    private var widget: ShellTerminalWidget? = null

    /**
     * Executes the given shell command inside the shared “Sequelize Runner” terminal tab.
     *
     * If the terminal does not exist yet, it is created automatically.
     * The working directory defaults to the project root unless otherwise specified.
     *
     * @param command The shell command to run (for example, `"npx sequelize-cli db:migrate"`).
     * @param workingDir Optional working directory. Defaults to the project base path.
     */
    fun run(command: String, workingDir: String? = null) {
        val wd = workingDir ?: (project.basePath ?: ".")
        val shell = ensureWidget(wd)

        // Always switch to the correct directory before executing the command.
        // This is important for monorepo projects where context changes frequently.
        shell.executeCommand("cd ${quotePath(wd)}")
        shell.executeCommand(command)
    }

    /**
     * Ensures that the Sequelize terminal widget exists and is usable.
     * If it has not been created yet or has been disposed, a new one is created.
     *
     * @param initialWorkingDir The initial working directory to create the terminal in.
     * @return A valid [ShellTerminalWidget] instance ready for command execution.
     */
    private fun ensureWidget(initialWorkingDir: String): ShellTerminalWidget {
        val existing = widget

        // Verify that the current widget is still alive and usable.
        @Suppress("DEPRECATION")
        if (existing != null && !Disposer.isDisposed(existing)) {
            return existing
        }

        // Create a new local shell widget using the modern API.
        // (TerminalView is deprecated and should not be used anymore.)
        val manager = TerminalToolWindowManager.getInstance(project)
        val shell: ShellTerminalWidget = manager.createLocalShellWidget(initialWorkingDir, TAB_TITLE)

        widget = shell
        return shell
    }

    /**
     * Wraps the given path in quotes so it can safely handle spaces or
     * special characters when passed to a shell command.
     *
     * @param path The raw file system path.
     * @return A shell-safe quoted path.
     */
    private fun quotePath(path: String): String {
        val p: Path = Paths.get(path)
        return "\"${p.toString()}\""
    }

    /**
     * Disposes of the terminal widget when the service is unloaded or the project closes.
     * Prevents any lingering shell processes from staying alive after disposal.
     */
    override fun dispose() {
        try {
            widget?.let { Disposer.dispose(it) }
        } catch (_: Throwable) {
            // Suppress any disposal exceptions to avoid noisy logs.
        } finally {
            widget = null
        }
    }

    companion object {
        /**
         * Returns the [SequelizeTerminalService] instance for the given project.
         *
         * @param project The current IntelliJ project.
         * @return The singleton service instance for that project.
         */
        fun getInstance(project: Project): SequelizeTerminalService = project.service()
    }
}
