package gr.atc.modapto.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.service.ModaptoModuleService;
import gr.atc.modapto.service.WebSocketService;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;
import gr.atc.modapto.service.interfaces.IEventService;
import gr.atc.modapto.service.interfaces.INotificationService;

@EnableKafka
@SpringJUnitConfig(classes = {
        KafkaMessageHandler.class,
        KafkaAutoConfiguration.class,
})
@TestPropertySource(properties = {
        "kafka.topics=topic1,topic2",
        "spring.kafka.consumer.group-id=test-group"
})
@EmbeddedKafka(partitions = 1, topics = "test-topic", brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092", "port=9092"
})
@ActiveProfiles(profiles = "test")
class KafkaMessageHandlerTests {

    @MockitoBean
    private IEventService eventService;

    @MockitoBean
    private INotificationService notificationService;

    @MockitoBean
    private WebSocketService webSocketService;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private ObjectMapper objectMapper;

    @MockitoBean
    private ModaptoModuleService modaptoModuleService;

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @InjectMocks
    @Autowired
    private KafkaMessageHandler kafkaMessageHandler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(kafkaMessageHandler, "pilot", "TEST");
    }

    @AfterEach
    void cleanup() {
        try {
            AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());

            // Delete the topic
            adminClient.deleteTopics(Collections.singleton("test-topic"));
            adminClient.close();
        } catch (Exception e) {
            System.err.println("Failed to delete topic: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Kafka Consumer: Valid Event Consumed Successfully")
    void givenValidEvent_whenConsumed_thenProcessSuccessfully() throws Exception {
        // Arrange
        EventDto event = new EventDto();
        event.setPriority(MessagePriority.HIGH.toString());
        event.setModule("Test Module");
        event.setTopic("test-topic");
        event.setDescription("Test Description");
        event.setSourceComponent("Test Source Component");
        event.setSmartService("Test Smart Service");

        when(modaptoModuleService.retrieveModaptoModuleName(anyString())).thenReturn("Test Module Name");
        when(eventService.storeIncomingEvent(any(EventDto.class))).thenReturn("event-123");
        when(eventService.retrieveUserRolesPerTopic(anyString())).thenReturn(List.of("ADMIN"));
        when(notificationService.retrieveUserIdsPerRoles(anyList())).thenReturn(new ArrayList<>(List.of("user1", "user2")));

        // Act
        kafkaMessageHandler.consume(event, "test-topic", null);

        // Assert
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(eventService).storeIncomingEvent(any(EventDto.class));
            verify(notificationService).retrieveUserIdsPerRoles(anyList());
            verify(webSocketService).notifyUsersAndRolesViaWebSocket(any(), eq("ADMIN"));
        });
    }

    @Test
    @DisplayName("Kafka Consumer: No roles triggers default mapping and pilot broadcast")
    void givenNoRoles_whenConsumed_thenCreateMappingAndNotifyPilot() throws Exception {
        EventDto event = new EventDto();
        event.setPriority(MessagePriority.HIGH.toString());
        event.setModule("Test Module");
        event.setTopic("unmapped-topic");
        event.setDescription("Test Description");
        event.setSourceComponent("Test Source Component");
        event.setSmartService("Test Smart Service");

        when(modaptoModuleService.retrieveModaptoModuleName(anyString())).thenReturn("Test Module Name");
        when(eventService.storeIncomingEvent(any())).thenReturn("event-456");
        when(eventService.retrieveUserRolesPerTopic(anyString())).thenReturn(List.of());
        when(notificationService.retrieveUserIdsPerPilot(anyString())).thenReturn(new ArrayList<>(List.of("test-user")));

        kafkaMessageHandler.consume(event, "unmapped-topic", null);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(webSocketService).notifyUsersAndRolesViaWebSocket(any(), eq("TEST"));
        });
    }

    @Test
    @DisplayName("Kafka Consumer: Module Creation Event with Name Parsing")
    void givenModuleCreationEvent_whenConsumed_thenParseNameFromResults() throws Exception {
        // Given
        EventDto event = new EventDto();
        event.setPriority(MessagePriority.HIGH.toString());
        event.setModule("MODULE-001");
        event.setTopic("modapto-module-creation");
        event.setDescription("Module Created");
        event.setSourceComponent("PKB");
        event.setSmartService("Module Manager");

        // Mock JSON results
        com.fasterxml.jackson.databind.JsonNode jsonNode = mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(jsonNode.has("name")).thenReturn(true);
        when(jsonNode.get("name")).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
        when(jsonNode.get("name").isNull()).thenReturn(false);
        when(jsonNode.get("name").asText()).thenReturn("Test Module Name");
        event.setResults(jsonNode);

        when(eventService.storeIncomingEvent(any(EventDto.class))).thenReturn("event-789");
        when(eventService.retrieveUserRolesPerTopic(anyString())).thenReturn(List.of("OPERATOR"));
        when(notificationService.retrieveUserIdsPerRoles(anyList())).thenReturn(new ArrayList<>(List.of("user1")));

        // When
        kafkaMessageHandler.consume(event, "modapto-module-creation", null);

        // Then
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(eventService).storeIncomingEvent(any(EventDto.class));
            verify(modaptoModuleService, never()).retrieveModaptoModuleName(anyString());
        });
    }

    @Test
    @DisplayName("Kafka Consumer: Module Name Returns Null")
    void givenUnresolvedModuleName_whenConsumed_thenSkipProcessing() throws Exception {
        // Given
        EventDto event = new EventDto();
        event.setPriority(MessagePriority.HIGH.toString());
        event.setModule("INVALID-MODULE");
        event.setTopic("test-topic");
        event.setDescription("Test Description");

        when(modaptoModuleService.retrieveModaptoModuleName("INVALID-MODULE")).thenReturn(null);

        // When
        kafkaMessageHandler.consume(event, "test-topic", null);

        // Then
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(modaptoModuleService).retrieveModaptoModuleName("INVALID-MODULE");
            // No event is stored
            verify(eventService, never()).storeIncomingEvent(any(EventDto.class));
        });
    }
}