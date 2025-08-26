package gr.atc.modapto.events;

import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.service.interfaces.IEventService;
import gr.atc.modapto.service.interfaces.INotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Handles application events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppEventListener {

    private final IEventService eventService;

    private final INotificationService notificationService;

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    @EventListener
    @Async(value = "asyncPoolTaskExecutor")
    public void handleNewUserMappingsEvent(NewNotificationMappingsEvent appEvent) {
        String topic = appEvent.getTopic();
        log.debug("Creating new Event-Mapping for Topic: {}", topic);
        try {
            EventMappingsDto mapping = EventMappingsDto.builder()
                    .userRoles(List.of("ALL"))
                    .topic(topic)
                    .description("Mapping for events of '".concat(topic).concat("' topic"))
                    .build();
            eventService.storeEventMapping(mapping);
        } catch (ModelMappingException e) {
            log.error("Failed to create Event-Mapping for Topic: {}", topic, e);
        }
    }

    @EventListener
    @Async(value = "asyncPoolTaskExecutor")
    public void handleNewNotificationEvent(NewNotificationEvent appEvent) {
        NotificationDto eventNotification = appEvent.getNotification();
        try {
            for (String userId : appEvent.getUserIds()) {
                eventNotification.setUserId(userId);
                if (userId.equals(SUPER_ADMIN_ROLE)) {
                    eventNotification.setUser(userId);
                } else {
                    String fullName = notificationService.retrieveUserFullName(userId);
                    eventNotification.setUser(fullName != null ? fullName : userId);
                }

                String notificationId = notificationService.storeNotification(eventNotification);
                log.debug("Notification stored successfully for User: {}", eventNotification.getUser());
                if (notificationId == null) {
                    log.error("Notification could not be stored in DB for User with ID: {}", userId);
                    return;
                }
            }
        } catch (ModelMappingException e) {
            log.error("Failed to create new Notification for selected Users");
        }
    }
}
