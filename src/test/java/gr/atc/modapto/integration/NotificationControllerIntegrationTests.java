package gr.atc.modapto.integration;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;

import gr.atc.modapto.model.Notification;
import gr.atc.modapto.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NotificationControllerIntegrationTests extends SetupTestContainersEnvironment {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    private NotificationDto testNotification;
    private NotificationDto testNotificationRead;

    @BeforeEach
    void setup() {
        notificationRepository.deleteAll();

        testNotification = NotificationDto.builder()
                .notificationType(NotificationType.EVENT)
                .userId("12345")
                .notificationStatus(NotificationStatus.NOT_VIEWED)
                .sourceComponent("Test Component")
                .productionModule("Test Production Module")
                .timestamp(LocalDateTime.now())
                .priority(MessagePriority.MEDIUM)
                .description("Test")
                .build();

        testNotificationRead = NotificationDto.builder()
                .notificationType(NotificationType.EVENT)
                .userId("12345")
                .notificationStatus(NotificationStatus.VIEWED)
                .sourceComponent("Test Component")
                .productionModule("Test Production Module")
                .timestamp(LocalDateTime.now())
                .priority(MessagePriority.MEDIUM)
                .description("Test")
                .build();
    }

    @DisplayName("Get All Notifications: Success")
    @Test
    void givenValidRequest_whenGetAllNotifications_thenReturnNotificationList() throws Exception {
        // Given
        notificationRepository.save(modelMapper.map(testNotification, Notification.class));

        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Notifications retrieved successfully!")))
                .andExpect(jsonPath("$.data[0].productionModule", is("Test Production Module")));
    }

    @DisplayName("Get All Notifications: Empty List")
    @Test
    void givenNoNotifications_whenGetAllNotifications_thenReturnEmptyList() throws Exception {
        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("Get Notifications by User ID: Success")
    @Test
    void givenValidUserId_whenGetNotificationsByUserId_thenReturnNotifications() throws Exception {
        // Given
        notificationRepository.save(modelMapper.map(testNotification, Notification.class));
        notificationRepository.save(modelMapper.map(testNotificationRead, Notification.class));

        // When
        mockMvc.perform(get("/api/notifications/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Notifications retrieved successfully!")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].productionModule", is("Test Production Module")));
    }

    @DisplayName("Get Unread Notifications by User ID: Success")
    @Test
    void givenValidUserId_whenGetUnreadNotificationsByUserId_thenReturnUnreadNotifications() throws Exception {
        // Given
        notificationRepository.save(modelMapper.map(testNotification, Notification.class));
        notificationRepository.save(modelMapper.map(testNotificationRead, Notification.class));

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/notifications/12345/unread")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Unread notifications retrieved successfully!")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productionModule", is("Test Production Module")));

    }
}
