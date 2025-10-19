package core

import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager

/**
 * Utility object that provides a safe entry point to execute shell commands
 * inside the shared "Sequelize Runner" terminal tab.
 *
 * This ensures that:
 *  - Commands are always executed on the Swing Event Dispatch Thread (EDT),
 *    which is required for all UI interactions in IntelliJ-based IDEs.
 *  - The command is sent to the single persistent terminal managed by
 *    [SequelizeTerminalService].
 */
object TerminalRunner {

    /**
     * Executes a shell command in the shared "Sequelize Runner" terminal tab.
     *
     * If the terminal does not exist yet, it will be created automatically.
     *
     * @param project The current IntelliJ project context.
     * @param command The shell command to execute (e.g., "npx sequelize-cli db:migrate").
     * @param workingDir Optional working directory. Defaults to the project root.
     */
    fun runInTerminal(project: Project, command: String, workingDir: String? = null) {
        // Always execute terminal operations on the EDT for thread-safety.
        val app = ApplicationManager.getApplication()
        app.invokeLater {
            SequelizeTerminalService.getInstance(project).run(command, workingDir)
        }
    }
}
