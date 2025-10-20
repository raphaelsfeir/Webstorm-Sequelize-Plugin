/**
 * SequelizeToolWindowPanel.kt
 * ----------------------------
 * Main UI panel for the **Sequelize** tool window.
 *
 * Provides one-click actions to manage migrations and seeds directly from the IDE.
 * The "Generate" action for migrations uses the plugin’s native generator
 * (ESM/CommonJS aware via [ModuleKindDetector]) and renders files from resource
 * templates via [MigrationScaffolder]. Seed generation still uses sequelize-cli.
 *
 * What you get:
 *  - Migrations:
 *      - Migrate          → `sequelize-cli db:migrate`
 *      - Undo Last        → `sequelize-cli db:migrate:undo`
 *      - Status           → `sequelize-cli db:migrate:status`
 *      - Generate (native)→ Template-based generator with automatic ESM/CJS detection
 *  - Seeds:
 *      - Run All          → `sequelize-cli db:seed:all`
 *      - Undo All         → `sequelize-cli db:seed:undo:all`
 *      - Generate         → `sequelize-cli seed:generate --name <name>`
 *
 * UX details:
 *  - The selected Sequelize environment is persisted via [EnvManager].
 *  - After generating a migration natively, the VFS is refreshed so the file
 *    appears immediately in the Project tool window (no manual refresh required).
 *  - A small production warning is displayed when the selected environment is "production".
 *
 * Dependencies:
 *  - [EnvManager]                → persists current environment
 *  - [PackageManagerDetector]    → builds terminal commands for npm/yarn/pnpm
 *  - [TerminalRunner]            → executes CLI commands in the IDE terminal
 *  - [ModuleKindDetector]        → detects ESM/CJS and extension preference
 *  - [MigrationScaffolder]       → loads and renders migration templates
 *  - [Notif] & [Labels]          → notifications and localized strings
 *
 * Typical usage:
 *  - Open the **Sequelize** tool window → use toolbar buttons to run actions
 *  - Click **Generate** under *Migrations* to create a new migration file natively
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.1.0 — Switched migration generation to native, template-based flow.
 *
 * @license
 *   MIT License
 */
package ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import core.*
import i18n.Labels
import java.awt.*
import java.nio.file.Files
import javax.swing.*

/**
 * Swing panel composing the full tool window UI and wiring button actions.
 */
class SequelizeToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    // Services and runtime state
    private val envManager = project.getService(EnvManager::class.java)
    private val rootVf = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ".")
    private val pm: PackageManager = PackageManagerDetector.detect(rootVf)
    private var currentEnv: String = envManager?.get() ?: "development"
    private val log = Logger.getInstance(SequelizeToolWindowPanel::class.java)

    // UI components
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
     * Builds the top header with:
     *  - Title and small gear icon
     *  - Environment selector (development/test/production)
     *  - Quick docs link opening sequelize-cli help in terminal
     */
    private fun buildHeader(): JPanel {
        val title = JBLabel(Labels.t("appTitle"), AllIcons.General.Settings, SwingConstants.LEADING).apply {
            font = JBFont.label().asBold().deriveFont(15f)
        }

        val envBox = JComboBox(arrayOf("development", "test", "production")).apply {
            selectedItem = currentEnv
            addActionListener {
                currentEnv = selectedItem as String
                envManager?.set(currentEnv)
                footer.text = footerText()
                prodWarning.isVisible = currentEnv.equals("production", ignoreCase = true)
                if (prodWarning.isVisible) Notif.warning(project, Labels.t("notifProductionWarning"))
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
     * Builds the scrollable content area containing the Migrations and Seeds sections.
     */
    private fun buildContent(): JComponent = JPanel(GridBagLayout()).apply {
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

    /**
     * Migrations section.
     * Contains four actions:
     *  - Migrate
     *  - Undo Last
     *  - Status
     *  - Generate (native template-based)
     */
    private fun buildMigrationsSection(): JComponent {
        val wrapper = borderedPanel()
        val inner = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(10) }

        inner.add(TitledSeparator(Labels.t("sectionMigrations")), BorderLayout.NORTH)
        inner.add(makeToolbar(listOf(
            // Run migrations via CLI in terminal
            action(Labels.t("btnMigrate"), AllIcons.Actions.Execute) {
                runAndNotify(listOf("sequelize-cli", "db:migrate", "--env", currentEnv), Labels.t("notifMigrationsStarted"))
            },
            // Undo last migration
            action(Labels.t("btnUndoLast"), AllIcons.Actions.Undo) {
                runAndNotify(listOf("sequelize-cli", "db:migrate:undo", "--env", currentEnv), Labels.t("notifUndoLastDone"))
            },
            // Show migration status
            action(Labels.t("btnStatus"), AllIcons.Actions.ListFiles) {
                runAndNotify(listOf("sequelize-cli", "db:migrate:status", "--env", currentEnv), Labels.t("notifStatusRequested"))
            },
            // Generate migration natively (ESM/CJS aware, template-based)
            action(Labels.t("btnGenerate"), AllIcons.General.Add) {
                val name = ask(Labels.t("askMigrationName")) ?: return@action
                generateNativeMigration(name)
            }
        )), BorderLayout.CENTER)

        inner.add(tips(Labels.t("tipMigrations")), BorderLayout.SOUTH)
        wrapper.add(inner, BorderLayout.CENTER)
        return wrapper
    }

    /**
     * Seeds section.
     * Contains three actions:
     *  - Run All
     *  - Undo All
     *  - Generate (via sequelize-cli)
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
            action(Labels.t("btnGenerateSeed"), AllIcons.General.Add) {
                val name = ask(Labels.t("askSeedName")) ?: return@action
                runAndNotify(
                    listOf("sequelize-cli", "seed:generate", "--name", name, "--env", currentEnv),
                    Labels.t("notifSeedCreated", "name" to name)
                )
            }
        )), BorderLayout.CENTER)

        inner.add(tips(Labels.t("tipSeeds")), BorderLayout.SOUTH)
        wrapper.add(inner, BorderLayout.CENTER)
        return wrapper
    }

    // Helpers

    /** Thin bordered container for visually grouping sections. */
    private fun borderedPanel(): JPanel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(UIManager.getColor("Separator.foreground"), 1)
        }

    /** Builds a horizontal toolbar from a list of actions. */
    private fun makeToolbar(actions: List<AnAction>): JComponent {
        val group = DefaultActionGroup().apply { actions.forEach { add(it) } }
        return ActionManager.getInstance()
            .createActionToolbar("SequelizeToolbar", group, true)
            .apply {
                targetComponent = component
                component.border = JBUI.Borders.empty(4, 2)
            }.component
    }

    /** Utility to build an action with icon and click handler. */
    private fun action(text: String, icon: Icon, onClick: () -> Unit): AnAction =
        object : AnAction(text, null, icon), DumbAware {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
            override fun actionPerformed(e: AnActionEvent) = onClick()
        }

    /** Small gray hint below each section. */
    private fun tips(text: String): JComponent =
        JBLabel(text).apply {
            border = JBUI.Borders.emptyTop(8)
            foreground = UIManager.getColor("Label.disabledForeground")
            font = font.deriveFont(Font.PLAIN, 11f)
        }

    /** Simple input prompt used for names. */
    private fun ask(title: String): String? =
        com.intellij.openapi.ui.Messages.showInputDialog(project, title, "Sequelize", null)?.trim()

    /**
     * Generates a migration natively using templates and automatic ESM/CJS detection.
     * After writing the file to disk, refreshes the Virtual File System so the new file
     * appears immediately in the Project tool window.
     */
    private fun generateNativeMigration(name: String) {
        try {
            val basePath = project.basePath ?: return
            val rootVf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
            val moduleInfo = ModuleKindDetector.detect(rootVf)
            val kind = moduleInfo.kind
            val ext = moduleInfo.preferredExt

            log.info("[SequelizeToolWindowPanel] Generating native migration: kind=$kind ext=$ext")

            val content = MigrationScaffolder.renderBlankTemplate(kind)
            val path = MigrationScaffolder.timestampedFilename(basePath, ext, name)
            Files.createDirectories(path.parent)
            Files.writeString(path, content)

            // Make new file visible instantly
            val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile())
            if (vf != null) {
                VfsUtil.markDirtyAndRefresh(true, false, false, vf)
            } else {
                // Fallback: refresh the entire migrations directory
                rootVf?.findChild("migrations")?.let {
                    VfsUtil.markDirtyAndRefresh(true, true, true, it)
                }
            }

            Notif.success(project, Labels.t("notifMigrationCreated", "name" to path.fileName.toString()))
        } catch (ex: Throwable) {
            Notif.warning(project, "Failed to create native migration: ${ex.message}")
            log.warn("[SequelizeToolWindowPanel] Failed to create native migration: ${ex.message}", ex)
        }
    }

    /**
     * Executes a CLI command in the IDE terminal using the detected package manager
     * (npm, yarn, or pnpm) and shows a success notification.
     * Still used for migrate/undo/status and all seed actions.
     */
    private fun runAndNotify(args: List<String>, successMsg: String) {
        val cmd = PackageManagerDetector.command(pm, args)
        TerminalRunner.runInTerminal(project, cmd)
        Notif.success(project, successMsg)
    }

    /** Footer text displaying current package manager and environment. */
    private fun footerText(): String =
        Labels.t("footerStatusTemplate", "pm" to pm.name, "env" to currentEnv)
}
