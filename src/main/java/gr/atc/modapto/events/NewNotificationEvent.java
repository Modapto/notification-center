package gr.atc.modapto.events;

import gr.atc.modapto.dto.NotificationDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class NewNotificationEvent extends ApplicationEvent {
    private final NotificationDto notification;
    private final List<String> userIds;

    public NewNotificationEvent(Object source, NotificationDto notification, List<String> userIds) {
        super(source);
        this.notification = notification;
        this.userIds = userIds;
    }
}
