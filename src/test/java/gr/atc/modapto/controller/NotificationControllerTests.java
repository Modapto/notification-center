package gr.atc.modapto.controller;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.service.INotificationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private INotificationService notificationService;

    private static List<NotificationDto> notifications;
    private static NotificationDto testNotification;

    @BeforeAll
    static void setup(){
        testNotification = NotificationDto.builder()
                .notificationType(NotificationType.EVENT)
                .notificationStatus(NotificationStatus.NOT_VIEWED)
                .sourceComponent("Test Component")
                .productionModule("Test Production Module")
                .timestamp(LocalDateTime.now())
                .priority(MessagePriority.MEDIUM)
                .description("Test")
                .build();

        notifications = List.of(testNotification);
    }

    @DisplayName("Get All Notifications: Success")
    @Test
    void givenValidRequest_whenGetAllNotifications_thenReturnNotificationList() throws Exception {
        // Given
        given(notificationService.retrieveAllNotifications()).willReturn(notifications);

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
        // Given
        given(notificationService.retrieveAllNotifications()).willReturn(Collections.emptyList());

        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("Get All Notifications: Exception Handling")
    @Test
    void givenException_whenGetAllNotifications_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(notificationService).retrieveAllNotifications();

        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    @DisplayName("Get Notifications by User ID: Success")
    @Test
    void givenValidUserId_whenGetNotificationsByUserId_thenReturnNotifications() throws Exception {
        // Given
        given(notificationService.retrieveNotificationPerUserId("12345")).willReturn(notifications);

        // When
        mockMvc.perform(get("/api/notifications/user/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Notifications retrieved successfully!")))
                .andExpect(jsonPath("$.data[0].productionModule", is("Test Production Module")));
    }

    @DisplayName("Get Unread Notifications by User ID: Success")
    @Test
    void givenValidUserId_whenGetUnreadNotificationsByUserId_thenReturnUnreadNotifications() throws Exception {
        // Given
        given(notificationService.retrieveUnreadNotificationsPerUserId("12345")).willReturn(notifications);

        // When
        mockMvc.perform(get("/api/notifications/user/12345/unread")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Unread notifications retrieved successfully!")))
                .andExpect(jsonPath("$.data[0].productionModule", is("Test Production Module")));
    }

    @DisplayName("Get Notifications by User ID: Exception Handling")
    @Test
    void givenException_whenGetNotificationsByUserId_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(notificationService).retrieveNotificationPerUserId("12345");

        // When
        mockMvc.perform(get("/api/notifications/user/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    @DisplayName("Get Unread Notifications by User ID: Exception Handling")
    @Test
    void givenException_whenGetUnreadNotificationsByUserId_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(notificationService).retrieveUnreadNotificationsPerUserId("12345");

        // When
        mockMvc.perform(get("/api/notifications/user/12345/unread")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

}