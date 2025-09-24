package ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import core.EnvManager
import core.Notif
import core.PackageManager
import core.PackageManagerDetector
import core.TerminalRunner
import i18n.Labels
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * ToolWindow panel for Sequelize quick actions.
 * - Two sections: Migrations & Seeds
 * - Uses integrated terminal for commands
 * - Success notifications when a command is sent
 * - Production environment warning below the title
 */
class SequelizeToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    // --- Services / state ---
    private val envManager = project.getService(EnvManager::class.java)
    private val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
    private val pm: PackageManager = PackageManagerDetector.detect(rootVf)
    private var currentEnv: String = envManager?.get() ?: "development"

    // --- UI components ---
    private val footer = JBLabel()
    private val prodWarning = JBLabel(Labels.t("productionWarning")).apply {
        foreground = UIManager.getColor("Notification.errorForeground")
            ?: UIManager.getColor("Label.errorForeground")
        font = font.deriveFont(Font.BOLD, 11f)
        isVisible = currentEnv.equals("production", ignoreCase = true)
        border = JBUI.Borders.emptyTop(4)
    }

    init {
        border = JBUI.Borders.empty(10)

        // Header
        val header = buildHeader()

        // Content
        val content = JPanel(GridBagLayout()).apply {
            val c = GridBagConstraints().apply {
                gridx = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.NORTHWEST
                insets = JBUI.insets(0, 0, 10, 0)
            }
            c.gridy = 0; add(buildMigrationsSection(), c)
            c.gridy = 1; add(buildSeedsSection(), c)
            c.gridy = 2; c.weighty = 1.0; add(Box.createVerticalGlue(), c)
        }

        // Footer
        footer.text = footerText()
        footer.border = JBUI.Borders.emptyTop(6)
        footer.foreground = UIManager.getColor("Label.disabledForeground")
        footer.font = footer.font.deriveFont(Font.PLAIN, 11f)

        add(header, BorderLayout.NORTH)
        add(JScrollPane(content).apply { border = null; verticalScrollBar.unitIncrement = 16 }, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    // Header
    private fun buildHeader(): JPanel {
        val title = JBLabel(Labels.t("appTitle"), AllIcons.General.Settings, SwingConstants.LEADING).apply {
            font = JBFont.label().asBold().deriveFont(15f)
        }

        val envBox = JComboBox(arrayOf("development", "test", "production")).apply {
            selectedItem = currentEnv
            toolTipText = Labels.t("envLabel")
            addActionListener {
                currentEnv = selectedItem as String
                envManager?.set(currentEnv)
                footer.text = footerText()
                prodWarning.isVisible = currentEnv.equals("production", ignoreCase = true)
                if (prodWarning.isVisible) {
                    Notif.warning(project, Labels.t("notifProductionWarning"))
                }
            }
        }

        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            add(JBLabel(Labels.t("envLabel")))
            add(envBox)
            add(ActionLink(Labels.t("docsLink")) {
                // Open terminal with CLI help for quick reference
                TerminalRunner.runInTerminal(project, "npx sequelize-cli help")
            })
        }

        val center = JPanel(BorderLayout()).apply {
            add(title, BorderLayout.NORTH)
            add(prodWarning, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
            add(center, BorderLayout.WEST)
            add(right, BorderLayout.EAST)
        }
    }

    // Sections (cards)
    private fun buildMigrationsSection(): JComponent {
        val wrapper = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(UIManager.getColor("Separator.foreground"), 1)
        }
        val inner = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(10) }

        inner.add(TitledSeparator(Labels.t("sectionMigrations")), BorderLayout.NORTH)
        inner.add(makeToolbar(listOf(
            action(Labels.t("btnMigrate"), AllIcons.Actions.Execute) {
                runAndNotify(listOf("sequelize-cli", "db:migrate", "--env", currentEnv), Labels.t("notifMigrationsStarted"))
            },
            action(Labels.t("btnUndoLast"), AllIcons.Actions.Undo) {
                runAndNotify(listOf("sequelize-cli", "db:migrate:undo", "--env", currentEnv), Labels.t("notifUndoLastDone"))
            },
            action(Labels.t("btnStatus"), AllIcons.Actions.ListFiles) {
                runAndNotify(listOf("sequelize-cli", "db:migrate:status", "--env", currentEnv), Labels.t("notifStatusRequested"))
            },
            action(Labels.t("btnGenerate"), AllIcons.General.Add) {
                val name = ask(Labels.t("askMigrationName")) ?: return@action
                runAndNotify(
                    listOf("sequelize-cli", "migration:generate", "--name", name, "--env", currentEnv),
                    Labels.t("notifMigrationCreated", "name" to name)
                )
            },
        )), BorderLayout.CENTER)

        inner.add(tips(Labels.t("tipMigrations")), BorderLayout.SOUTH)
        wrapper.add(inner, BorderLayout.CENTER)
        return wrapper
    }

    private fun buildSeedsSection(): JComponent {
        val wrapper = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(UIManager.getColor("Separator.foreground"), 1)
        }
        val inner = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(10) }

        inner.add(TitledSeparator(Labels.t("sectionSeeds")), BorderLayout.NORTH)
        inner.add(makeToolbar(listOf(
            action(Labels.t("btnRunAllSeeds"), AllIcons.Actions.Execute) {
                runAndNotify(listOf("sequelize-cli", "db:seed:all", "--env", currentEnv), Labels.t("notifRunAllSeeds"))
            },
            action(Labels.t("btnUndoAllSeeds"), AllIcons.Actions.Undo) {
                runAndNotify(listOf("sequelize-cli", "db:seed:undo:all", "--env", currentEnv), Labels.t("notifUndoAllSeeds"))
            },
            action(Labels.t("btnGenerate"), AllIcons.General.Add) {
                val name = ask(Labels.t("askSeedName")) ?: return@action
                runAndNotify(
                    listOf("sequelize-cli", "seed:generate", "--name", name, "--env", currentEnv),
                    Labels.t("notifSeedCreated", "name" to name)
                )
            },
        )), BorderLayout.CENTER)

        inner.add(tips(Labels.t("tipSeeds")), BorderLayout.SOUTH)
        wrapper.add(inner, BorderLayout.CENTER)
        return wrapper
    }

    // Helpers
    private fun makeToolbar(actions: List<AnAction>): JComponent {
        val group = DefaultActionGroup().apply { actions.forEach { add(it) } }
        return ActionManager.getInstance()
            .createActionToolbar("SequelizeToolbar", group, true)
            .apply {
                targetComponent = this.component
                component.border = JBUI.Borders.empty(4, 2)
            }.component
    }

    /** update on BGT. */
    private fun action(text: String, icon: javax.swing.Icon, onClick: () -> Unit): AnAction =
        object : AnAction(text, null, icon), DumbAware {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
            override fun actionPerformed(e: AnActionEvent) = onClick()
        }

    private fun tips(text: String): JComponent =
        JBLabel(text).apply {
            border = JBUI.Borders.emptyTop(8)
            foreground = UIManager.getColor("Label.disabledForeground")
            font = font.deriveFont(Font.PLAIN, 11f)
        }

    private fun ask(title: String): String? =
        com.intellij.openapi.ui.Messages.showInputDialog(project, title, "Sequelize", null)?.trim()

    /** Join command via PackageManagerDetector and show success notification. */
    private fun runAndNotify(args: List<String>, successMsg: String) {
        val cmd = PackageManagerDetector.command(pm, args)
        TerminalRunner.runInTerminal(project, cmd)
        Notif.success(project, successMsg)
    }

    private fun footerText(): String =
        Labels.t("footerStatusTemplate", "pm" to pm.name, "env" to currentEnv)
}
