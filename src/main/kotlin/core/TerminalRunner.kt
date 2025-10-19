/**
 * TerminalRunner.kt
 * -----------------
 * Provides a safe and convenient entry point for executing shell commands
 * inside the shared **"Sequelize Runner"** terminal tab managed by
 * [SequelizeTerminalService].
 *
 * This utility ensures that all terminal operations are executed
 * on the **Swing Event Dispatch Thread (EDT)** — the only thread
 * allowed to interact with the IDE’s UI safely.
 *
 * Responsibilities:
 *  - Forward commands to the persistent terminal session.
 *  - Guarantee thread safety by always running on the EDT.
 *  - Create the terminal automatically if it does not yet exist.
 *
 * Example usage:
 * ```kotlin
 * TerminalRunner.runInTerminal(project, "npx sequelize-cli db:migrate")
 * TerminalRunner.runInTerminal(project, "npm run seed:all", "server/")
 * ```
 *
 * Dependencies:
 *  - [SequelizeTerminalService]: Manages the persistent terminal widget.
 *  - IntelliJ Platform’s [ApplicationManager]: Used to schedule EDT operations.
 *
 * @see SequelizeTerminalService
 * @see com.intellij.openapi.application.ApplicationManager
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of the terminal command runner.
 *
 * @license
 *   MIT License
 */

package core

import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager

/**
 * Utility object that provides a thread-safe way to execute shell commands
 * in the shared "Sequelize Runner" terminal tab.
 *
 * Ensures the terminal is created if missing and that all operations
 * run on the Event Dispatch Thread for UI safety.
 */
object TerminalRunner {

    /**
     * Executes a shell command in the shared "Sequelize Runner" terminal tab.
     *
     * If the terminal does not exist yet, it will be created automatically.
     *
     * @param project The current IntelliJ project context.
     * @param command The shell command to execute
     *   (for example, `"npx sequelize-cli db:migrate"`).
     * @param workingDir Optional working directory.
     *   Defaults to the project’s root path if not specified.
     */
    fun runInTerminal(project: Project, command: String, workingDir: String? = null) {
        // Always execute terminal operations on the EDT for thread safety.
        val app = ApplicationManager.getApplication()
        app.invokeLater {
            SequelizeTerminalService.getInstance(project).run(command, workingDir)
        }
    }
}
