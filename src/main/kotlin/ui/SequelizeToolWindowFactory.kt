/**
 * SequelizeToolWindowFactory.kt
 * ------------------------------
 * Defines the factory responsible for creating the **Sequelize** tool window
 * within the IDE.
 *
 * The tool window provides a dedicated UI panel for Sequelize-related operations,
 * such as running migrations, managing seeds, or viewing project environments.
 * It is registered in `plugin.xml` under the `<toolWindow>` extension point.
 *
 * Responsibilities:
 *  - Instantiate the main [SequelizeToolWindowPanel].
 *  - Wrap the panel in a [com.intellij.ui.content.Content] object.
 *  - Add the content to the provided [ToolWindow] instance.
 *
 * Example registration (`plugin.xml`):
 * ```xml
 * <toolWindow id="Sequelize"
 *             anchor="right"
 *             factoryClass="ui.SequelizeToolWindowFactory"
 *             icon="icons/sequelize.svg"
 *             canCloseContents="false"/>
 * ```
 *
 * Example usage:
 * The tool window is created automatically when the project loads.
 * You can open it from the IDE’s right-hand sidebar under the **Sequelize** tab.
 *
 * Dependencies:
 *  - [SequelizeToolWindowPanel]: The main UI panel created by this factory.
 *  - IntelliJ Platform’s [ToolWindow] and [ContentFactory] APIs.
 *
 * @see SequelizeToolWindowPanel
 * @see com.intellij.openapi.wm.ToolWindowFactory
 *
 * @autor
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of the Sequelize tool window factory.
 *
 * @license
 *   MIT License
 */

package ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory class responsible for creating and populating the **Sequelize** tool window
 * when a project is opened in the IDE.
 *
 * This factory is automatically called by the IntelliJ Platform during plugin initialization.
 */
class SequelizeToolWindowFactory : ToolWindowFactory {

    /**
     * Creates and attaches the Sequelize tool window content.
     *
     * @param project The current IntelliJ [Project].
     * @param toolWindow The [ToolWindow] instance to populate with UI content.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SequelizeToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
