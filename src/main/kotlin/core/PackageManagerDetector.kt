/**
 * PackageManagerDetector.kt
 * --------------------------
 * Detects the active Node.js package manager (npm, yarn, or pnpm) used in the project,
 * and constructs compatible shell commands for executing Sequelize CLI operations.
 *
 * The detection logic is based on the presence of well-known lockfiles in the project root:
 *  - `pnpm-lock.yaml` → [PackageManager.PNPM]
 *  - `yarn.lock` → [PackageManager.YARN]
 *  - Otherwise → [PackageManager.NPM] (default)
 *
 * Responsibilities:
 *  - Identify the appropriate [PackageManager] based on project files.
 *  - Generate OS-safe, shell-ready command strings for execution in the IDE terminal.
 *  - Properly quote arguments for Windows and UNIX shells.
 *
 * Example usage:
 * ```kotlin
 * val pm = PackageManagerDetector.detect(projectBaseDir)
 * val command = PackageManagerDetector.command(pm, listOf("sequelize-cli", "db:migrate"))
 * TerminalRunner.runInTerminal(project, command)
 * ```
 *
 * Result (on Windows with npm):
 * ```
 * npx.cmd sequelize-cli db:migrate
 * ```
 *
 * Result (on macOS with pnpm):
 * ```
 * pnpm dlx sequelize-cli db:migrate
 * ```
 *
 * Dependencies:
 *  - [PackageManager]: Enum defining supported Node.js package managers.
 *  - [TerminalRunner]: Executes commands built by this class in the IDE terminal.
 *
 * @see PackageManager
 * @see TerminalRunner
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of package manager detection and shell command builder.
 *
 * @license
 *   MIT License
 */

package core

import com.intellij.openapi.vfs.VirtualFile

/**
 * Utility object that detects the project's package manager and constructs
 * compatible Sequelize CLI commands for execution in the IDE terminal.
 */
object PackageManagerDetector {

    /**
     * Detects which Node.js package manager is being used by inspecting
     * common lockfiles in the provided project root directory.
     *
     * @param root The project’s root [VirtualFile].
     * @return The detected [PackageManager]. Defaults to [PackageManager.NPM] if unknown.
     */
    fun detect(root: VirtualFile?): PackageManager =
        when {
            root?.findChild("pnpm-lock.yaml") != null -> PackageManager.PNPM
            root?.findChild("yarn.lock") != null -> PackageManager.YARN
            else -> PackageManager.NPM
        }

    /**
     * Builds a complete, shell-ready command string for running Sequelize CLI
     * commands according to the detected package manager and operating system.
     *
     * On Windows, the `.cmd` suffix is added automatically for Node.js binaries.
     * For pnpm, the command is prefixed with `dlx` to ensure CLI execution.
     *
     * @param pm The active [PackageManager].
     * @param args The list of arguments to pass to the CLI (e.g., `["sequelize-cli", "db:migrate"]`).
     * @return A string representing the complete, executable shell command.
     */
    fun command(pm: PackageManager, args: List<String>): String {
        val isWin = System.getProperty("os.name").lowercase().contains("win")

        // Determine the correct executable name per OS
        val bin = when (pm) {
            PackageManager.NPM -> if (isWin) "npx.cmd" else "npx"
            PackageManager.YARN -> if (isWin) "yarn.cmd" else "yarn"
            PackageManager.PNPM -> if (isWin) "pnpm.cmd" else "pnpm"
        }

        // pnpm requires the `dlx` prefix for executing local binaries
        val body = when (pm) {
            PackageManager.PNPM -> "dlx " + args.joinToString(" ") { shellQuote(it, isWin) }
            else -> args.joinToString(" ") { shellQuote(it, isWin) }
        }

        return "$bin $body"
    }

    /**
     * Quotes a string argument safely for the target shell environment.
     * Ensures that arguments with spaces or special characters are escaped properly.
     *
     * @param s The raw string argument.
     * @param isWin `true` if the current OS is Windows, otherwise `false`.
     * @return A safely quoted string for shell execution.
     */
    private fun shellQuote(s: String, isWin: Boolean): String {
        val needsQuoting = s.any { it.isWhitespace() || "\"'&|><".contains(it) }
        return if (!needsQuoting) {
            s
        } else if (isWin) {
            "\"${s.replace("\"", "\\\"")}\""
        } else {
            "'${s.replace("'", "'\\''")}'"
        }
    }
}
