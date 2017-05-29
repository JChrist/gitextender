package gr.jchrist.gitextender;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.jetbrains.annotations.NotNull;

public class NotificationUtil {
    private NotificationUtil() {
        //no instances should be created
    }

    public static final String NOTIFICATION_GROUP_ID = "Git Extender";

    public static void showErrorNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.ERROR);
    }

    public static void showInfoNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.INFORMATION);
    }

    public static void showNotification(@NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notifications.Bus.notify(new Notification(NOTIFICATION_GROUP_ID, title, content, type));
    }
}
