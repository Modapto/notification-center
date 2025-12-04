package gr.atc.modapto.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewNotificationMappingsEvent extends ApplicationEvent {
    private final String topic;

    public NewNotificationMappingsEvent(Object source, String topic) {
        super(source);
        this.topic = topic;
    }
}
