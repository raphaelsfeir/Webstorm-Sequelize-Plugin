// actions/CreateMigrationAction.kt
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
 * CreateMigrationAction.kt
 * -------------------------
 * Génère une migration Sequelize native avec détection automatique ESM/CJS.
 */
class CreateMigrationAction : AnAction(), DumbAware {

    private val log = Logger.getInstance(CreateMigrationAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

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

        // 1) Demande du nom
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

        // 2) Détection du module kind (IMPORTANT: refresh + find)
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
        log.info("[CreateMigrationAction] detection result: kind=$kind ext=$ext reason=${moduleInfo.reason}")

        // 3) Génération du contenu (blank par défaut)
        val content = try {
            val c = MigrationScaffolder.renderBlankTemplate(kind)
            log.debug("[CreateMigrationAction] template generated (blank) length=${c.length}")
            c
        } catch (t: Throwable) {
            log.warn("[CreateMigrationAction] renderBlankTemplate failed: ${t.message}")
            // Fallback dur : CJS
            "module.exports = { async up(){}, async down(){} };"
        }

        // 4) Écriture du fichier
        val migrationsDir = ensureMigrationsDir(project, rootVf)
        if (migrationsDir == null) {
            Notif.warning(project, "Unable to create/find 'migrations' directory.")
            log.warn("[CreateMigrationAction] migrations dir is null")
            return
        } else {
            log.debug("[CreateMigrationAction] migrations dir=${migrationsDir.path}")
        }

        val fileName = buildFilename(rawName, ext)
        log.info("[CreateMigrationAction] final filename=$fileName")

        try {
            val vf = WriteCommandAction.writeCommandAction(project).compute<VirtualFile, IOException> {
                val existing = migrationsDir.findChild(fileName)
                val file = existing ?: migrationsDir.createChildData(this, fileName)
                VfsUtil.saveText(file, content)
                file
            }
            log.info("[CreateMigrationAction] file written at=${vf.path} (size=${vf.length}B)")

            FileEditorManager.getInstance(project).openFile(vf, true)
            Notif.success(project, Labels.t("notifMigrationCreated", "name" to vf.name))
        } catch (ex: Throwable) {
            log.warn("[CreateMigrationAction] Failed to create migration file: ${ex.message}", ex)
            Notif.warning(project, "Failed to create migration: ${ex.message ?: "Unknown error"}")
        }
    }

    /** Builds `<timestamp>-<slug>.<ext>` */
    private fun buildFilename(name: String, ext: String): String {
        val ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "$ts-$slug$ext"
    }

    /** Ensures `<project>/migrations` exists and returns its VirtualFile. */
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
