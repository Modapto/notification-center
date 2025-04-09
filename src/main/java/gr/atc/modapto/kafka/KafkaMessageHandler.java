package gr.atc.modapto.kafka;

import java.time.LocalDateTime;
import java.util.List;

import gr.atc.modapto.events.NewNotificationEvent;
import gr.atc.modapto.events.NewNotificationMappingsEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.service.interfaces.IEventService;
import gr.atc.modapto.service.interfaces.INotificationService;
import gr.atc.modapto.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaMessageHandler {

    @Value("${kafka.topics}")
    @SuppressWarnings("unused")
    private List<String> kafkaTopics;

    @Value("${use-case.pilot}")
    private String pilot;

    private static final String MQTT_KAFKA_TOPIC = "modapto-mqtt-topics";

    private final KafkaAdmin kafkaAdmin;

    private final IEventService eventService;

    private final INotificationService notificationService;

    private final WebSocketService webSocketService;

    private final ObjectMapper objectMapper;

    private final ApplicationEventPublisher eventPublisher;

    public KafkaMessageHandler(KafkaAdmin kafkaAdmin, IEventService eventService, INotificationService notificationService, WebSocketService webSocketService, ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        kafkaAdmin.setAutoCreate(true);
        this.kafkaAdmin = kafkaAdmin;
        this.eventService = eventService;
        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
        this.eventPublisher=eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Kafka consumer method to receive a JSON Event message - From Kafka Producers
     *
     * @param event: Event occurred in MODAPTO
     */
    @KafkaListener(topics = "#{'${kafka.topics}'.split(',')}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey){
        // Validate that same essential variables are present
        if (!isValidEvent(event)){
            log.error("Kafka message error - Either priority or production module or Topic are missing from the event. Message is discarded! Data: {}",event);
            return;
        }

        // Handle cases where Topic is the MODAPTO's MQTT Topics
        if (topic.equals(MQTT_KAFKA_TOPIC)){
            event.setTopic(messageKey);
        }

        log.info("Event Received: {}", event);
        // Store incoming Event
        try{
            // Store incoming event
            String eventId = eventService.storeIncomingEvent(event);
            if (eventId == null){
                log.error("Event could not be stored in DB. Message is discarded!");
                return;
            }
            event.setId(eventId);

            // Retrieve the user roles that will receive event notification and the userIds correlated with that roles by the EventMappings for that specific Topic
            List<String> userRolesPerEventType = eventService.retrieveUserRolesPerTopic(event.getTopic());
            List<String> userIds = determineRecipientsOfNotification(event, userRolesPerEventType);

            // Create and store the notification
            NotificationDto eventNotification = generateNotificationFromEvent(event);
            log.info("Notification created: {}", eventNotification);
            
            // Store notifications per each User
            createNotificationForUsers(eventNotification, userIds);

            // Remove userId from notification and convert it to String
            eventNotification.setUserId(null);
            String notificationMessage = objectMapper.writeValueAsString(eventNotification);
            if (userRolesPerEventType.isEmpty() || userIds.isEmpty())
                // Send notification globally to pilot users
                webSocketService.notifyRolesWebSocket(notificationMessage, pilot.toUpperCase());
            else 
                // Send notification through WebSockets to all user roles in the plant
                userRolesPerEventType.forEach(role -> webSocketService.notifyRolesWebSocket(notificationMessage, role));
        } catch (JsonProcessingException e){
            log.error("Unable to convert Notification to string message - {}", e.getMessage());
        } catch (ModelMappingException  e){
            log.error("ModelMapping exception occurred when trying to store incoming event / retrieve current event mapping - {}", e.getMessage());
        }
    }


    /*
     * Helper method to locate the UserIDs that will receive the Notification
     */
    private List<String> determineRecipientsOfNotification(EventDto event, List<String> userRolesPerEventType) {
        // Handle empty mappings case
        if (userRolesPerEventType.isEmpty()) {
            log.info("No mappings exist for topic '{}'. All users in {} plant will be informed!",
                    event.getTopic(), pilot.toUpperCase());

            // Request creation of mapping
            createDefaultNotificationMapping(event.getTopic());

            return notificationService.retrieveUserIdsPerPilot(pilot.toUpperCase());
        }

        // Handle "ALL" role mapping
        if (userRolesPerEventType.contains("ALL")) {
            return notificationService.retrieveUserIdsPerPilot(pilot.toUpperCase());
        }

        // Handle specific roles
        return notificationService.retrieveUserIdsPerRoles(userRolesPerEventType);
    }

    /*
     * Method to publish Application event to create a new Event Mapping
     */
    private void createDefaultNotificationMapping(String topic) {
        NewNotificationMappingsEvent appEvent = new NewNotificationMappingsEvent(this, topic);
        log.info("Publishing event to create a new Event Mapping for topic: {}", topic);
        eventPublisher.publishEvent(appEvent);
    }

    /*
     * Method to publish Application event to create a new Notification for specific UserIDs
     */
    private void createNotificationForUsers(NotificationDto notification, List<String> userIds) {
        NewNotificationEvent appEvent = new NewNotificationEvent(this, notification, userIds);
        log.info("Publishing event to create Notifications for all Users");
        eventPublisher.publishEvent(appEvent);
    }

    /*
     * Helper method to generate a Notification from Event
     */
    private NotificationDto generateNotificationFromEvent(EventDto event){
        return  NotificationDto.builder()
                .notificationType(NotificationType.Event.toString())
                .notificationStatus(NotificationStatus.Unread.toString())
                .sourceComponent(event.getSourceComponent())
                .productionModule(event.getProductionModule())
                .smartService(event.getSmartService())
                .relatedEvent(event.getId())
                .relatedAssignment(null)
                .timestamp(LocalDateTime.now().withNano(0))
                .priority(event.getPriority())
                .description(event.getDescription())
                .build();
    }

    private boolean isValidEvent(EventDto event) {
        return event.getPriority() != null &&
                event.getProductionModule() != null &&
                event.getTopic() != null;
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
