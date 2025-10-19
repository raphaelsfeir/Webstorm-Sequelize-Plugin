/**
 * SequelizeToolWindowPanel.kt
 * ----------------------------
 * Defines the main UI panel displayed inside the **Sequelize** tool window.
 *
 * This panel provides quick-access buttons for common Sequelize CLI operations
 * such as running migrations, generating seeds, or undoing database changes —
 * all integrated with the IDE’s terminal and notification systems.
 *
 * The panel is split into two main sections:
 *  1. **Migrations** — Run, undo, or generate migration scripts.
 *  2. **Seeds** — Run, undo, or generate seed files.
 *
 * Key Features:
 *  - Executes commands via the shared “Sequelize Runner” terminal tab.
 *  - Automatically detects the project’s package manager (npm, yarn, pnpm).
 *  - Allows switching environments (“development”, “test”, “production”).
 *  - Displays warnings and notifications when running in production.
 *
 * Example layout:
 * ```
 * [ Sequelize ⚙  | Environment: [development ▼] | Docs link ]
 * ------------------------------------------------------------
 * |  Migrations  |   Run Migration   |   Undo Last   |   Status   |   Generate   |
 * |  Seeds       |   Run All         |   Undo All    |   Generate  |
 * ------------------------------------------------------------
 *   PM: NPM • Env: development
 * ```
 *
 * Dependencies:
 *  - [EnvManager]: Manages and persists the current Sequelize environment.
 *  - [PackageManagerDetector]: Detects the Node.js package manager.
 *  - [TerminalRunner]: Executes commands in the IDE terminal.
 *  - [Notif]: Displays success and warning notifications.
 *  - [Labels]: Provides localized UI text.
 *
 * Registered via [SequelizeToolWindowFactory] in `plugin.xml`.
 *
 * @see core.TerminalRunner
 * @see core.PackageManagerDetector
 * @see core.EnvManager
 * @see ui.SequelizeToolWindowFactory
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of the main Sequelize tool window panel.
 *
 * @license
 *   MIT License
 */

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
import java.awt.*
import javax.swing.*

/**
 * The main UI panel of the **Sequelize** tool window.
 *
 * Provides buttons and quick actions for migrations and seeds management,
 * along with environment controls and live status information.
 */
class SequelizeToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    // --- Services and runtime state ---
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

        // Build layout: header, content, footer
        val header = buildHeader()
        val content = buildContent()
        footer.text = footerText()
        footer.border = JBUI.Borders.emptyTop(6)
        footer.foreground = UIManager.getColor("Label.disabledForeground")
        footer.font = footer.font.deriveFont(Font.PLAIN, 11f)

        add(header, BorderLayout.NORTH)
        add(JScrollPane(content).apply { border = null; verticalScrollBar.unitIncrement = 16 }, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    /**
     * Builds the top header section with:
     *  - Title and icon
     *  - Environment dropdown selector
     *  - Link to Sequelize documentation/help
     */
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

    /**
     * Builds the scrollable content area with two sections:
     * Migrations and Seeds.
     */
    private fun buildContent(): JComponent {
        return JPanel(GridBagLayout()).apply {
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
    }

    /**
     * Builds the "Migrations" section with action buttons.
     */
    private fun buildMigrationsSection(): JComponent {
        val wrapper = borderedPanel()
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

    /**
     * Builds the "Seeds" section with action buttons.
     */
    private fun buildSeedsSection(): JComponent {
        val wrapper = borderedPanel()
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

    /** Builds a thin bordered container for section grouping. */
    private fun borderedPanel(): JPanel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(UIManager.getColor("Separator.foreground"), 1)
        }

    /** Creates an action toolbar for a list of buttons. */
    private fun makeToolbar(actions: List<AnAction>): JComponent {
        val group = DefaultActionGroup().apply { actions.forEach { add(it) } }
        return ActionManager.getInstance()
            .createActionToolbar("SequelizeToolbar", group, true)
            .apply {
                targetComponent = this.component
                component.border = JBUI.Borders.empty(4, 2)
            }.component
    }

    /** Creates a simple text-based button for toolbar actions. */
    private fun action(text: String, icon: Icon, onClick: () -> Unit): AnAction =
        object : AnAction(text, null, icon), DumbAware {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
            override fun actionPerformed(e: AnActionEvent) = onClick()
        }

    /** Builds a small gray informational text label below sections. */
    private fun tips(text: String): JComponent =
        JBLabel(text).apply {
            border = JBUI.Borders.emptyTop(8)
            foreground = UIManager.getColor("Label.disabledForeground")
            font = font.deriveFont(Font.PLAIN, 11f)
        }

    /** Prompts the user for input with a small dialog box. */
    private fun ask(title: String): String? =
        com.intellij.openapi.ui.Messages.showInputDialog(project, title, "Sequelize", null)?.trim()

    /**
     * Joins and runs the Sequelize CLI command based on the detected package manager,
     * then displays a success notification.
     */
    private fun runAndNotify(args: List<String>, successMsg: String) {
        val cmd = PackageManagerDetector.command(pm, args)
        TerminalRunner.runInTerminal(project, cmd)
        Notif.success(project, successMsg)
    }

    /** Builds the footer text showing current environment and package manager. */
    private fun footerText(): String =
        Labels.t("footerStatusTemplate", "pm" to pm.name, "env" to currentEnv)
}
