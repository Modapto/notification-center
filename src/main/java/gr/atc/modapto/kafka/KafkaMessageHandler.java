package gr.atc.modapto.kafka;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.events.NewNotificationEvent;
import gr.atc.modapto.events.NewNotificationMappingsEvent;
import org.apache.commons.lang3.EnumUtils;
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

    private static final String GLOBAL_EVENT_MAPPINGS = "ALL";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

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
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Kafka consumer method to receive a JSON Event message - From Kafka Producers
     *
     * @param event: Event occurred in MODAPTO
     */
    @KafkaListener(topics = "#{'${kafka.topics}'.split(',')}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        // Validate that same essential variables are present
        if (!isValidEvent(event)) {
            log.error("Kafka message error - Either priority or production module or Topic are missing / invalid from the event. Message is discarded! Data: {}", event);
            return;
        }

        // Refactor and complete incoming event
        refactorAndCompleteEvent(event, topic, messageKey);

        log.info("Event Received: {}", event);
        // Store incoming Event
        try {
            // Store incoming event
            String eventId = eventService.storeIncomingEvent(event);
            if (eventId == null) {
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

            // Store notifications per each User - Async
            createNotificationForUsers(eventNotification, userIds);

            // Remove userId from notification and convert Object to JSON message
            eventNotification.setUserId(null);
            String notificationMessage = objectMapper.writeValueAsString(eventNotification);
            if (userRolesPerEventType.isEmpty() || userRolesPerEventType.contains(GLOBAL_EVENT_MAPPINGS))
                // Send notification globally to pilot users
                webSocketService.notifyUsersAndRolesViaWebSocket(notificationMessage, pilot.toUpperCase());
            else
                // Send notification through WebSockets to all user roles in the plant
                userRolesPerEventType.forEach(role -> webSocketService.notifyUsersAndRolesViaWebSocket(notificationMessage, role));

            // Send notification through WebSockets for Super-Admins
            webSocketService.notifyUsersAndRolesViaWebSocket(notificationMessage, SUPER_ADMIN_ROLE);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert Notification to string message - {}", e.getMessage());
        } catch (ModelMappingException e) {
            log.error("ModelMapping exception occurred when trying to store incoming event / retrieve current event mapping - {}", e.getMessage());
        }
    }

    /*
     * Helper method to refactor Event Fields in not in proper format and add any potential missing fields
     */
    private void refactorAndCompleteEvent(EventDto incomingEvent, String kafkaTopic, String messageKey) {
        // Format Priority Status - Uniform Case
        incomingEvent.setPriority(MessagePriority.valueOf(incomingEvent.getPriority().toUpperCase()).toString());

        // Handle cases where Topic is the MODAPTO's MQTT Topics
        if (kafkaTopic.equals(MQTT_KAFKA_TOPIC))
            incomingEvent.setTopic(messageKey);

        // Generate Description if not available
        if (incomingEvent.getDescription() == null && incomingEvent.getEventType() != null)
            incomingEvent.setDescription("New system event: " + incomingEvent.getEventType());

        // Generate Timestamp if missing
        if (incomingEvent.getTimestamp() == null)
            incomingEvent.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
    }


    /*
     * Helper method to locate the UserIDs that will receive the Notification
     */
    private List<String> determineRecipientsOfNotification(EventDto event, List<String> userRolesPerEventType) {
        List<String> relatedUserIds = new ArrayList<>();

        // Include Super-Admin UserID
        relatedUserIds.add(SUPER_ADMIN_ROLE);

        // Handle empty mappings case - Creating mapping and retrieve all pilot users
        if (userRolesPerEventType.isEmpty()) {
            log.info("No mappings exist for topic '{}'. All users in {} plant will be informed!",
                    event.getTopic(), pilot.toUpperCase());

            // Request creation of mapping - Async
            createDefaultNotificationMapping(event.getTopic());

            relatedUserIds.addAll(notificationService.retrieveUserIdsPerPilot(pilot.toUpperCase()));
        } else if (userRolesPerEventType.contains(GLOBAL_EVENT_MAPPINGS)) { // Handle "ALL" role mapping - Send notification globally to pilot users
            relatedUserIds.addAll(notificationService.retrieveUserIdsPerPilot(pilot.toUpperCase()));
        } else { // Handle specific roles
            relatedUserIds.addAll(notificationService.retrieveUserIdsPerRoles(userRolesPerEventType));
        }

        return relatedUserIds;
    }

    /*
     * Method to publish Application event to create a new Event Mapping
     */
    private void createDefaultNotificationMapping(String topic) {
        Thread.startVirtualThread(() -> {
            try {
                NewNotificationMappingsEvent appEvent = new NewNotificationMappingsEvent(this, topic);
                log.info("Publishing event to create a new Event Mapping for topic: {}", topic);
                eventPublisher.publishEvent(appEvent);
            } catch (Exception e) {
                log.error("Error while creating a new event mapping: {}", e.getMessage(), e);
            }
        });
    }

    /*
     * Method to publish Application event to create a new Notification for specific UserIDs
     */
    private void createNotificationForUsers(NotificationDto notification, List<String> userIds) {
        Thread.startVirtualThread(() -> {
            try {
                NewNotificationEvent appEvent = new NewNotificationEvent(this, notification, userIds);
                log.info("Publishing event to create Notifications for all Users");
                eventPublisher.publishEvent(appEvent);
            } catch (Exception e) {
                log.error("Error while publishing notification event: {}", e.getMessage(), e);
            }
        });
    }

    /*
     * Helper method to generate a Notification from Event
     */
    private NotificationDto generateNotificationFromEvent(EventDto event) {
        return NotificationDto.builder()
                .notificationType(NotificationType.EVENT.toString())
                .notificationStatus(NotificationStatus.UNREAD.toString())
                .messageStatus(event.getEventType())
                .sourceComponent(event.getSourceComponent())
                .productionModule(event.getProductionModule())
                .smartService(event.getSmartService())
                .relatedEvent(event.getId())
                .relatedAssignment(null)
                .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC))
                .priority(event.getPriority())
                .description(event.getDescription())
                .build();
    }

    private boolean isValidEvent(EventDto event) {
        return (event.getPriority() != null && EnumUtils.isValidEnumIgnoreCase(MessagePriority.class, event.getPriority())) &&
                event.getProductionModule() != null &&
                event.getTopic() != null;
    }

    /**
     * Dynamic creation of new topics in Kafka with Kafka Admin
     *
     * @param topicName  : String value of newly created topic
     * @param partitions : Number of partitions for the specified topic
     * @param replicas   : Number of replicas for the specified topic
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
