package core

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object Notif {
    fun success(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Sequelize")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun warning(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Sequelize")
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
}
