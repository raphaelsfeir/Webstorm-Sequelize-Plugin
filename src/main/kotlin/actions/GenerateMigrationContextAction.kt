package actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import core.EnvManager
import core.Notif
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels

/**
 * Context action to generate a migration when right-clicking inside /migrations.
 */
class GenerateMigrationContextAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf.isInMigrationsDir()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val name = Messages.showInputDialog(
            project,
            Labels.t("askMigrationName"),
            Labels.t("appTitle"),
            null
        )?.trim() ?: return
        if (name.isEmpty()) return

        val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
        val pm = PackageManagerDetector.detect(rootVf)
        val env = project.getService(EnvManager::class.java)?.get() ?: "development"

        val cmd = PackageManagerDetector.command(
            pm, listOf("sequelize-cli", "migration:generate", "--name", name, "--env", env)
        )
        TerminalRunner.runInTerminal(project, cmd)
        Notif.success(project, Labels.t("notifMigrationCreated", "name" to name))
    }

    private fun VirtualFile?.isInMigrationsDir(): Boolean {
        if (this == null) return false
        val p = path.replace("\\", "/")
        return p.contains("/migrations")
    }
}
