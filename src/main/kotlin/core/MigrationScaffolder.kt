package core

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object MigrationScaffolder {
    private val log = Logger.getInstance(MigrationScaffolder::class.java)

    private fun loadTemplate(name: String): String {
        val resourcePath = "/templates/$name"
        log.debug("[MigrationScaffolder] Loading template: $resourcePath")

        val stream = MigrationScaffolder::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Template not found: $resourcePath")

        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    /** Template vide */
    fun renderBlankTemplate(kind: ModuleKind): String {
        val file = when (kind) {
            ModuleKind.ESM -> "migration_blank_esm.tpl"
            ModuleKind.CJS -> "migration_blank_cjs.tpl"
        }
        return loadTemplate(file)
    }

    /** Template createTable */
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

    fun timestampedFilename(baseDir: String, ext: String, name: String): java.nio.file.Path {
        val ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .format(java.time.LocalDateTime.now())
        val p = java.nio.file.Paths.get(baseDir, "migrations", "${ts}-${slug(name)}$ext")
        log.debug("[MigrationScaffolder] timestampedFilename -> $p")
        return p
    }

    private fun slug(s: String) =
        s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
