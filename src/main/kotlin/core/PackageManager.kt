/**
 * PackageManager.kt
 * ------------------
 * Defines the supported Node.js package managers recognized by the plugin.
 *
 * The enum is used by [PackageManagerDetector] to identify which command prefix
 * should be used when executing Sequelize CLI commands (e.g., `npx`, `yarn`, or `pnpm`).
 *
 * Example usage:
 * ```kotlin
 * val pm = PackageManager.NPM
 * val cmd = when (pm) {
 *     PackageManager.NPM -> "npx sequelize-cli db:migrate"
 *     PackageManager.YARN -> "yarn sequelize-cli db:migrate"
 *     PackageManager.PNPM -> "pnpm sequelize-cli db:migrate"
 * }
 * ```
 *
 * Responsibilities:
 *  - Represent the available package managers supported by the plugin.
 *  - Provide a type-safe way to switch behavior based on the detected manager.
 *
 * Dependencies:
 *  - Used directly by [PackageManagerDetector].
 *  - Referenced indirectly by actions that execute Sequelize commands via [TerminalRunner].
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial definition of supported package managers.
 *
 * @license
 *   MIT License
 */

package core

/**
 * Enum representing the Node.js package managers supported by the plugin.
 */
enum class PackageManager {
    /** Standard Node.js package manager (uses `npx` for CLI execution). */
    NPM,

    /** Yarn package manager (uses `yarn` for command execution). */
    YARN,

    /** pnpm package manager (uses `pnpm exec` for command execution). */
    PNPM
}
