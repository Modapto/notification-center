package gr.atc.modapto.controller;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.exception.CustomExceptions;
import gr.atc.modapto.repository.AssignmentRepository;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import gr.atc.modapto.repository.NotificationRepository;
import gr.atc.modapto.service.interfaces.INotificationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
class NotificationControllerTests {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ElasticsearchTemplate elasticsearchTemplate;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private AssignmentRepository assignmentRepository;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private EventMappingsRepository eventMappingsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private INotificationService notificationService;

    private static List<NotificationDto> notifications;
    private static NotificationDto testNotification;
    private static Page<NotificationDto> paginatedResults;

    @BeforeEach
    void setupInit(){
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @BeforeAll
    static void setup(){
        testNotification = NotificationDto.builder()
                .notificationType(NotificationType.Event.toString())
                .notificationStatus(NotificationStatus.Unread.toString())
                .sourceComponent("Test Component")
                .productionModule("Test Production Module")
                .timestamp(LocalDateTime.now())
                .priority(MessagePriority.Mid.toString())
                .description("Test")
                .build();

        notifications = List.of(testNotification);

        paginatedResults = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);
    }

    @DisplayName("Get All Notifications: Success")
    @WithMockUser
    @Test
    void givenValidRequest_whenGetAllNotifications_thenReturnNotificationList() throws Exception {
        // Given
        given(notificationService.retrieveAllNotifications(any(Pageable.class))).willReturn(paginatedResults);

        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Notifications retrieved successfully!")));
    }

    @DisplayName("Get All Notifications: Empty List")
    @WithMockUser
    @Test
    void givenNoNotifications_whenGetAllNotifications_thenReturnEmptyList() throws Exception {
        // Given
        given(notificationService.retrieveAllNotifications(any(Pageable.class))).willReturn(Page.empty());

        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.results").isEmpty());
    }

    @DisplayName("Get All Notifications: Exception Handling")
    @WithMockUser
    @Test
    void givenException_whenGetAllNotifications_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(notificationService).retrieveAllNotifications(any(Pageable.class));

        // When
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    @DisplayName("Get Notification by ID: Success")
    @WithMockUser
    @Test
    void givenNotificationID_whenGetNotificationById_thenReturnNotification() throws Exception {
        // Given
        when(notificationService.retrieveNotificationById(anyString())).thenReturn(notifications.getFirst());

        // When
        mockMvc.perform(get("/api/notifications/notificationId")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productionModule", is("Test Production Module")));
    }

    @DisplayName("Get Notification by ID: Not Found Exception")
    @WithMockUser
    @Test
    void givenNotificationID_whenGetNotificationById_thenReturnNotFound() throws Exception {
        // Given
        doThrow(new CustomExceptions.DataNotFoundException("Requested resource not found in DB"))
                .when(notificationService).retrieveNotificationById(any());

        // When
        mockMvc.perform(get("/api/notifications/notificationId")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Get Notification by ID: Mapping Exception")
    @WithMockUser
    @Test
    void givenNotificationID_whenGetNotificationById_thenReturnMappingException() throws Exception {
        // Given
        doThrow(new CustomExceptions.ModelMappingException("Model Mapping Exception"))
                .when(notificationService).retrieveNotificationById(any());

        // When
        mockMvc.perform(get("/api/notifications/notificationId")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Internal error in mapping process")));
    }

    @DisplayName("Get Notifications by User ID: Success")
    @WithMockUser
    @Test
    void givenValidUserId_whenGetNotificationsByUserId_thenReturnNotifications() throws Exception {
        // Given
        given(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class))).willReturn(paginatedResults);

        // When
        mockMvc.perform(get("/api/notifications/user/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Notifications retrieved successfully!")));
    }

    @DisplayName("Get Unread Notifications by User ID: Success")
    @WithMockUser
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
    @WithMockUser
    @Test
    void givenException_whenGetNotificationsByUserId_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(notificationService).retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class));

        // When
        mockMvc.perform(get("/api/notifications/user/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    @DisplayName("Get Unread Notifications by User ID: Exception Handling")
    @WithMockUser
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