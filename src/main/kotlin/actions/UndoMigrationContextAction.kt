package actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.LocalFileSystem
import core.EnvManager
import core.Notif
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels

/**
 * Context action: undo only the clicked migration (by name)
 */
class UndoMigrationContextAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null &&
                    vf.path.replace("\\", "/").contains("/migrations/") &&
                    (vf.name.endsWith(".js") || vf.name.endsWith(".ts"))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)
        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "db:migrate:undo", "--name", vf.name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)
        Notif.success(project, Labels.t("notifMigrationUndo", "file" to vf.name))
    }
}
