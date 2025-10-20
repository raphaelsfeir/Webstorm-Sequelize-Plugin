/**
 * CreateMigrationAction.kt
 * -------------------------
 * IDE-native action to scaffold a Sequelize migration with automatic
 * ESM/CommonJS detection and template rendering from resource files.
 *
 * Trigger path:
 *  Tools → Create Migration (or any action bound to this class).
 *
 * What it does:
 *  1) Prompts the user for a migration name.
 *  2) Detects the project module type (ESM/CommonJS) via [ModuleKindDetector].
 *  3) Renders migration content from external templates via [MigrationScaffolder].
 *  4) Writes the file to `<project>/migrations/<timestamp>-<slug>.<ext>`.
 *  5) Opens the file in the editor and shows a success notification.
 *
 * Notes:
 *  - Templates are loaded from `/resources/templates` (handled by [MigrationScaffolder]).
 *  - The operation uses IntelliJ's VFS write API ([WriteCommandAction]) to create the file,
 *    ensuring the IDE immediately sees the new file without manual refresh.
 *  - If the `migrations/` directory does not exist, it is created.
 *
 * Responsibilities:
 *  - Gather user input for the migration name.
 *  - Orchestrate detection (ESM/CJS), template rendering, and file creation.
 *  - Surface meaningful logs and user notifications via [Notif].
 *
 * Example:
 *  - Input: "Create users"
 *  - Output file: `migrations/20251020161230-create-users.mjs` (if ESM historical usage prefers `.mjs`)
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.1.0 — Switched from inline strings to resource-based templates with auto ESM/CJS detection.
 *
 * @license
 *   MIT License
 */
package actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import core.ModuleKindDetector
import core.MigrationScaffolder
import core.Notif
import i18n.Labels
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Action implementation for creating a migration with native templates.
 */
class CreateMigrationAction : AnAction(), DumbAware {

    private val log = Logger.getInstance(CreateMigrationAction::class.java)

    /** Runs safely on a background thread to avoid UI freezes. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Main workflow:
     *  - Ask for migration name
     *  - Detect module kind (ESM/CJS)
     *  - Render template
     *  - Create file in `migrations/`
     *  - Open and notify
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            log.warn("[CreateMigrationAction] e.project is null")
            return
        }
        val basePath = project.basePath ?: run {
            log.warn("[CreateMigrationAction] project.basePath is null")
            return
        }
        log.info("[CreateMigrationAction] basePath=$basePath")

        // Step 1 — Prompt for migration name
        val rawName = Messages.showInputDialog(
            project,
            Labels.t("askMigrationName"),
            Labels.t("appTitle"),
            null
        )?.trim() ?: return

        if (rawName.isEmpty()) {
            log.info("[CreateMigrationAction] aborted: empty name")
            return
        }
        log.info("[CreateMigrationAction] requested migration name=\"$rawName\"")

        // Step 2 — Detect module kind (refresh to ensure VFS state is up-to-date)
        val rootVf: VirtualFile? =
            LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
        if (rootVf == null) {
            log.warn("[CreateMigrationAction] refreshAndFindFileByPath returned null for basePath=$basePath")
        } else {
            log.debug("[CreateMigrationAction] rootVf=${rootVf.path}")
        }

        val moduleInfo = ModuleKindDetector.detect(rootVf)
        val kind = moduleInfo.kind
        val ext = moduleInfo.preferredExt
        log.info("[CreateMigrationAction] detected kind=$kind ext=$ext reason=${moduleInfo.reason}")

        // Step 3 — Render template content from resources
        val content = try {
            val c = MigrationScaffolder.renderBlankTemplate(kind)
            log.debug("[CreateMigrationAction] loaded template from resources (length=${c.length})")
            c
        } catch (t: Throwable) {
            log.warn("[CreateMigrationAction] failed to renderBlankTemplate: ${t.message}", t)
            "// Template load failed: ${t.message ?: "unknown error"}"
        }

        // Step 4 — Ensure migrations directory exists
        val migrationsDir = ensureMigrationsDir(project, rootVf)
        if (migrationsDir == null) {
            Notif.warning(project, "Unable to create/find 'migrations' directory.")
            log.warn("[CreateMigrationAction] migrations dir is null")
            return
        } else {
            log.debug("[CreateMigrationAction] migrations dir=${migrationsDir.path}")
        }

        // Step 5 — Create the migration file
        val fileName = buildFilename(rawName, ext)
        log.info("[CreateMigrationAction] creating file=$fileName")

        try {
            val vf = WriteCommandAction.writeCommandAction(project).compute<VirtualFile, IOException> {
                val existing = migrationsDir.findChild(fileName)
                val file = existing ?: migrationsDir.createChildData(this, fileName)
                VfsUtil.saveText(file, content)
                file
            }

            log.info("[CreateMigrationAction] migration file created at=${vf.path}")

            // Optional: mark dirty & refresh to be extra-safe (usually not needed with VFS writes)
            VfsUtil.markDirtyAndRefresh(true, false, false, vf)

            // Step 6 — Open the new file and notify user
            FileEditorManager.getInstance(project).openFile(vf, true)
            Notif.success(project, Labels.t("notifMigrationCreated", "name" to vf.name))
        } catch (ex: Throwable) {
            log.warn("[CreateMigrationAction] Failed to create migration file: ${ex.message}", ex)
            Notif.warning(project, "Failed to create migration: ${ex.message ?: "Unknown error"}")
        }
    }

    /**
     * Builds a filename in the form:
     * `<timestamp>-<slug>.<ext>`
     *
     * Example:
     *  20251020162311-create-users.mjs
     */
    private fun buildFilename(name: String, ext: String): String {
        val ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "$ts-$slug$ext"
    }

    /**
     * Ensures `<project>/migrations` exists and returns its [VirtualFile].
     * Creates the directory if it does not exist.
     */
    private fun ensureMigrationsDir(
        project: com.intellij.openapi.project.Project,
        rootVf: VirtualFile?
    ): VirtualFile? {
        if (rootVf == null) return null
        val existing = rootVf.findChild("migrations")
        if (existing != null && existing.isDirectory) return existing

        return try {
            WriteCommandAction.writeCommandAction(project).compute<VirtualFile, IOException> {
                rootVf.createChildDirectory(this, "migrations")
            }.also {
                log.info("[CreateMigrationAction] created migrations dir at ${it.path}")
            }
        } catch (t: Throwable) {
            log.warn("[CreateMigrationAction] cannot create migrations dir: ${t.message}")
            null
        }
    }
}
