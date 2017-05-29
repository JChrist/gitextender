package gr.jchrist.gitextender;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class NotificationUtilTest {
    @Test
    public void showErrorNotification(@Mocked final Notifications.Bus bus) throws Exception {
        final String title = "test title";
        final String content = "test content";
        NotificationUtil.showErrorNotification(title, content);
        new Verifications() {{
            Notification notif;
            Notifications.Bus.notify(notif = withCapture());
            assertThat(notif.getTitle()).as("unexpected notification title").isEqualTo(title);
            assertThat(notif.getContent()).as("unexpected notification content").isEqualTo(content);
            assertThat(notif.getType()).as("unexpected notification type").isEqualTo(NotificationType.ERROR);
        }};
    }

    @Test
    public void showInfoNotification(@Mocked final Notifications.Bus bus) throws Exception {
        final String title = "test title";
        final String content = "test content";
        NotificationUtil.showInfoNotification(title, content);
        new Verifications() {{
            Notification notif;
            Notifications.Bus.notify(notif = withCapture());
            assertThat(notif.getTitle()).as("unexpected notification title").isEqualTo(title);
            assertThat(notif.getContent()).as("unexpected notification content").isEqualTo(content);
            assertThat(notif.getType()).as("unexpected notification type").isEqualTo(NotificationType.INFORMATION);
        }};
    }

    @Test
    public void newNotificationUtil() throws Exception {
        Constructor<NotificationUtil> constructor = NotificationUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}