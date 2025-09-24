package core

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalView

object TerminalRunner {
    fun runInTerminal(project: Project, command: String) {
        val basePath = project.basePath ?: "."
        val terminalView = TerminalView.getInstance(project)
        val widget = terminalView.createLocalShellWidget(basePath, "Sequelize") as ShellTerminalWidget
        widget.executeCommand(command)
    }
}
