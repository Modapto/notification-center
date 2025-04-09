package gr.atc.modapto.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.service.WebSocketService;
import gr.atc.modapto.service.interfaces.IEventService;
import gr.atc.modapto.service.interfaces.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableKafka
@SpringJUnitConfig(classes = {
        KafkaMessageHandler.class,
        KafkaAutoConfiguration.class,
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
    private KafkaAdmin kafkaAdmin;

    @InjectMocks
    @Autowired
    private KafkaMessageHandler kafkaMessageHandler;

    @BeforeEach
    void setup(){
        ReflectionTestUtils.setField(kafkaMessageHandler,"kafkaTopics", List.of("test-topic"));
        ReflectionTestUtils.setField(kafkaMessageHandler,"pilot", "TEST");
    }

    @Test
    @DisplayName("Kafka Consumer: Valid Event Consumed Successfully")
    void givenValidEvent_whenConsumed_thenProcessSuccessfully() throws Exception {
        // Arrange
        EventDto event = new EventDto();
        event.setPriority(MessagePriority.High.toString());
        event.setProductionModule("Test Module");
        event.setTopic("test-topic");
        event.setDescription("Test Description");
        event.setSourceComponent("Test Source Component");
        event.setSmartService("Test Smart Service");

        when(eventService.storeIncomingEvent(any(EventDto.class))).thenReturn("event-123");
        when(eventService.retrieveUserRolesPerTopic(anyString())).thenReturn(List.of("ADMIN"));
        when(notificationService.retrieveUserIdsPerRoles(anyList())).thenReturn(List.of("user1", "user2"));

        // Act
        kafkaMessageHandler.consume(event, "test-topic", null);

        // Assert
        verify(eventService, times(1)).storeIncomingEvent(any(EventDto.class));
        verify(notificationService, times(1)).retrieveUserIdsPerRoles(anyList());
        verify(webSocketService, atLeastOnce()).notifyRolesWebSocket(any(), eq("ADMIN"));
    }
}
