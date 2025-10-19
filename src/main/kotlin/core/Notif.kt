/**
 * Notif.kt
 * ---------
 * Provides a simple utility object for displaying in-IDE notifications
 * related to Sequelize operations.
 *
 * Notifications are displayed through IntelliJ’s built-in
 * [NotificationGroupManager] API and appear in the IDE’s event log
 * and notification popups.
 *
 * Responsibilities:
 *  - Display success messages for completed actions (e.g., migration created, seed executed).
 *  - Display warning messages when a non-critical issue occurs.
 *
 * Example usage:
 * ```kotlin
 * Notif.success(project, "Migration created successfully.")
 * Notif.warning(project, "No migration files found.")
 * ```
 *
 * Configuration:
 *  - Uses the notification group **"Sequelize"**, which must be declared
 *    in `plugin.xml` (e.g. `<notificationGroup id="Sequelize" displayType="BALLOON"/>`).
 *
 * Dependencies:
 *  - IntelliJ Platform’s [NotificationGroupManager] for creating notifications.
 *  - IntelliJ Platform’s [NotificationType] for message severity.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of notification helper.
 *
 * @license
 *   MIT License
 */

package core

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Centralized helper for displaying success and warning notifications
 * within the IDE under the "Sequelize" notification group.
 *
 * This object allows consistent, reusable notification behavior
 * across all plugin actions and services.
 */
object Notif {

    /**
     * Displays a success (information-level) notification in the IDE.
     *
     * @param project The current IntelliJ project context.
     * @param message The message content to display.
     */
    fun success(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Sequelize")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    /**
     * Displays a warning-level notification in the IDE.
     *
     * @param project The current IntelliJ project context.
     * @param message The message content to display.
     */
    fun warning(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Sequelize")
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
}
