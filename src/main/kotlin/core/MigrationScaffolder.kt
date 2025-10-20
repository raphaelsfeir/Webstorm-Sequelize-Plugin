/**
 * MigrationScaffolder.kt
 * -----------------------
 * Generates Sequelize migration files using resource-based templates.
 *
 * The scaffolder reads template files located under `/resources/templates`
 * and injects the appropriate placeholders depending on the migration type
 * (blank or createTable). It automatically selects the correct syntax and
 * extension according to the detected module kind (ESM or CommonJS).
 *
 * Responsibilities:
 *  - Load migration templates from the plugin’s classpath.
 *  - Replace dynamic placeholders such as `${TABLE_NAME}` and `${COLUMNS}`.
 *  - Generate timestamped migration filenames (e.g., `20251020153245-create-users.mjs`).
 *  - Produce clean, consistent, and IDE-native migration files.
 *
 * Typical usage:
 * ```kotlin
 * val kind = ModuleKind.ESM
 * val content = MigrationScaffolder.renderCreateTableTemplate(
 *     kind,
 *     table = "users",
 *     columns = """
 *         id: { type: Sequelize.UUID, defaultValue: Sequelize.literal("gen_random_uuid()"), primaryKey: true },
 *         created_at: { type: Sequelize.DATE, allowNull: false, defaultValue: Sequelize.NOW },
 *         updated_at: { type: Sequelize.DATE, allowNull: false, defaultValue: Sequelize.NOW }
 *     """.trimIndent()
 * )
 *
 * val path = MigrationScaffolder.timestampedFilename("/my/project", ".mjs", "create-users")
 * Files.writeString(path, content)
 * ```
 *
 * Notes:
 *  - Templates must be stored under `/resources/templates/` in UTF-8.
 *  - Supported template names:
 *      - `migration_blank_esm.tpl`
 *      - `migration_blank_cjs.tpl`
 *      - `migration_create_table_esm.tpl`
 *      - `migration_create_table_cjs.tpl`
 *  - The class is stateless and thread-safe.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.1.0 — Switched from inline strings to external template files.
 *
 * @license
 *   MIT License
 */
package core

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** Provides helper methods for loading and rendering Sequelize migration templates. */
object MigrationScaffolder {

    private val log = Logger.getInstance(MigrationScaffolder::class.java)

    /**
     * Loads a template file from the `/resources/templates` directory.
     *
     * @param name The name of the template file (e.g., `migration_blank_esm.tpl`).
     * @return The file content as a UTF-8 string.
     * @throws IllegalStateException If the template cannot be found in the classpath.
     */
    private fun loadTemplate(name: String): String {
        val resourcePath = "/templates/$name"
        log.debug("[MigrationScaffolder] Loading template: $resourcePath")

        val stream = MigrationScaffolder::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Template not found: $resourcePath")

        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    /**
     * Returns a blank migration template (empty `up` and `down` methods).
     *
     * @param kind The module kind (ESM or CJS).
     * @return A string containing the formatted migration source code.
     */
    fun renderBlankTemplate(kind: ModuleKind): String {
        val file = when (kind) {
            ModuleKind.ESM -> "migration_blank_esm.tpl"
            ModuleKind.CJS -> "migration_blank_cjs.tpl"
        }
        return loadTemplate(file)
    }

    /**
     * Returns a `createTable` migration template with substituted placeholders.
     *
     * @param kind The module kind (ESM or CJS).
     * @param table The name of the table to be created.
     * @param columns The raw column definitions to insert into the template.
     * @return A rendered Sequelize migration file as a string.
     */
    fun renderCreateTableTemplate(kind: ModuleKind, table: String, columns: String): String {
        val file = when (kind) {
            ModuleKind.ESM -> "migration_create_table_esm.tpl"
            ModuleKind.CJS -> "migration_create_table_cjs.tpl"
        }
        val raw = loadTemplate(file)
        return raw
            .replace("\${TABLE_NAME}", table)
            .replace("\${COLUMNS}", columns)
    }

    /**
     * Builds a timestamped migration filename using the format:
     * `migrations/YYYYMMDDHHMMSS-name.ext`.
     *
     * @param baseDir The project root or migrations directory path.
     * @param ext The file extension (e.g., `.js`, `.mjs`, `.cjs`).
     * @param name The migration name, which will be slugified.
     * @return The generated file path as a [java.nio.file.Path].
     */
    fun timestampedFilename(baseDir: String, ext: String, name: String): java.nio.file.Path {
        val ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .format(java.time.LocalDateTime.now())
        val p = java.nio.file.Paths.get(baseDir, "migrations", "${ts}-${slug(name)}$ext")
        log.debug("[MigrationScaffolder] timestampedFilename -> $p")
        return p
    }

    /**
     * Converts a string into a lowercase, hyphen-safe slug.
     *
     * @param s The string to slugify.
     * @return A sanitized, filesystem-safe slug.
     */
    private fun slug(s: String): String =
        s.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
