package gr.atc.modapto.kafka;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.enums.UserRole;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.service.IEventService;
import gr.atc.modapto.service.INotificationService;
import gr.atc.modapto.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaMessageHandler {

    @Value("${kafka.topics}")
    @SuppressWarnings("unused")
    private List<String> kafkaTopics;

    private final KafkaAdmin kafkaAdmin;

    private final IEventService eventService;

    private final INotificationService notificationService;

    private final WebSocketService webSocketService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public KafkaMessageHandler(KafkaAdmin kafkaAdmin, IEventService eventService, INotificationService notificationService, WebSocketService webSocketService) {
        kafkaAdmin.setAutoCreate(true);
        this.kafkaAdmin = kafkaAdmin;
        this.eventService = eventService;
        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
    }

    /**
     * Kafka consumer method to receive a JSON Event message - From Kafka Producers
     *
     * @param event: Event occurred in MODAPTO
     */
    @KafkaListener(topics = "#{'${kafka.topics}'.split(',')}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic){
        // Validate that same essential variables are present
        if (event.getPriority() == null || event.getProductionModule() == null || event.getPilot() == null){
            log.error("Either priority or production module or pilot code is missing from the event. Message is discarded!");
            return;
        }

        // Store incoming Event
        try{
            // Store incoming event
            String eventId = eventService.storeIncomingEvent(event);
            if (eventId == null){
                log.error("Event could not be stored in DB. Message is discarded!");
                return;
            }
            event.setId(eventId);


            // Retrieve the user roles that will receive event notification and the userIds correlated with that roles
            List<UserRole> userRolesPerEventType = eventService.retrieveUserRolesPerEventType(event.getEventType(), event.getProductionModule(), event.getSmartService());
            List<String> userIds;
            if (userRolesPerEventType.isEmpty()){
                log.info("Unable to locate user roles for this event type. All users in pilot will be informed!");
                userIds = notificationService.retrieveUserIdsPerPilot(event.getPilot());
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

            // Remove userId from notification and convert it to String
            eventNotification.setUserId(null);
            String message = objectMapper.writeValueAsString(eventNotification);
            if (userRolesPerEventType.isEmpty())
                // Send notification globally to pilot users
                webSocketService.notifyRolesWebSocket(message, event.getPilot());
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
