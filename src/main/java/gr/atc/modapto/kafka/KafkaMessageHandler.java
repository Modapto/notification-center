package gr.atc.modapto.kafka;

import java.time.LocalDateTime;
import java.util.List;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.service.IEventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.enums.UserRole;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.service.INotificationService;
import gr.atc.modapto.service.WebSocketService;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaMessageHandler {

    private final KafkaAdmin kafkaAdmin;

    private final IEventService eventService;

    private final INotificationService notificationService;

    private final WebSocketService webSocketService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Kafka consumer method to receive a JSON Event message
     *
     * @param event: Event occurred in MODAPTO
     */
    @KafkaListener(topics = { "self-awareness" }, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventDto event){
        // Store incoming Event
        try{
            // Store incoming event
            String eventId = eventService.storeIncomingEvent(event);
            if (eventId == null){
                log.error("Event could not be stored in DB");
                return;
            }
            event.setId(eventId);


            // Retrieve the user roles that will receive event notification and the userIds correlated with that roles
            List<UserRole> userRolesPerEventType = eventService.retrieveUserRolesPerEventType(event.getEventType(), event.getProductionModule(), event.getSmartService());
            List<String> userIds;
            if (userRolesPerEventType.isEmpty()){
                log.warn("Unable to locate user roles for this event type. All users will be informed!");
                userIds = notificationService.retrieveAllUserIds();
            } else {
                userIds = notificationService.retrieveUserIdsPerRoles(userRolesPerEventType);
            }

            // Create and store the notification
            NotificationDto eventNotification = NotificationDto.builder()
                .notificationType(NotificationType.EVENT)
                .notificationStatus(NotificationStatus.NOT_VIEWED)
                .sourceComponent(event.getSourceComponent())
                .productionModule(event.getProductionModule())
                    .smartService(event.getSmartService())
                    .relatedEvent(eventId)
                    .relatedAssignment(null)
                .timestamp(LocalDateTime.now())
                .priority(event.getPriority())
                .description(event.getDescription())
                .build();
            
            // Store notification per each User
            for (String userId : userIds) {
                eventNotification.setUserId(userId);
            
                String notificationId = notificationService.storeNotification(eventNotification);
                if (notificationId == null){
                    log.error("Notification could not be stored in DB");
                    return;
                }
            }

            String message = objectMapper.writeValueAsString(eventNotification);
            if (userRolesPerEventType.isEmpty())
                webSocketService.notifyRolesWebSocket(message, UserRole.GLOBAL.toString());
            else
                // Send notification through WebSockets to all user roles
                userRolesPerEventType.forEach(role -> webSocketService.notifyRolesWebSocket(message, role.toString()));
        } catch (JsonProcessingException e){
            log.error("Unable to convert Notification to string message - {}", e.getMessage());
        } catch (ModelMappingException  e){
            log.error("ModelMapping exception occurred when trying to store incoming event / retrieve current event mapping - {}", e.getMessage());
        }
    }

    /**
     * Dynamic creation of new topics in Kafka with Kafka Admin
     *
     * @param topicName : String value of newly created topic
     * @param partitions : Number of partitions for the specified topic
     * @param replicas : Number of replicas for the specified topic
     */
    public void createTopic(String topicName, int partitions, int replicas) {
        NewTopic topic = TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();

        kafkaAdmin.createOrModifyTopics(topic);
        log.info("Topic created: {}", topicName);
    }
}
