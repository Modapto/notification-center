package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Notification;
import gr.atc.modapto.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
class NotificationServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationDto notificationDto;
    private NotificationDto superAdminNotificationDto;
    private Notification notification;
    private Notification superAdminNotification;

    private static final String USER_MANAGER_URL = "http://localhost:8080/api/keycloak";
    private static final String TOKEN_URL = "http://localhost:9080/api/token";
    private static final String CLIENT = "client";
    private static final String CLIENT_SECRET = "secret";

    @BeforeEach
    void setup() {
        // Configure service properties
        ReflectionTestUtils.setField(notificationService, "userManagerUrl", USER_MANAGER_URL);
        ReflectionTestUtils.setField(notificationService, "tokenUri", TOKEN_URL);
        ReflectionTestUtils.setField(notificationService, "client", CLIENT);
        ReflectionTestUtils.setField(notificationService, "clientSecret", CLIENT_SECRET);

        // Initialize data
        notificationDto = NotificationDto.builder()
            .id("1")
            .timestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC))
            .userId("user1")
                .user("user1")
            .notificationStatus(NotificationStatus.UNREAD.toString())
            .description("Test Notification")
                .module("test-module-1")
            .moduleName("Test Module Name")
            .build();

        notification = new Notification();
        notification.setNotificationStatus(NotificationStatus.UNREAD.toString());
        notification.setId("1");
        notification.setUserId("user1");
        notification.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));
        notification.setDescription("Test Notification");


        superAdminNotificationDto = NotificationDto.builder()
                .id("2")
                .timestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC))
                .userId("SUPER_ADMIN")
                .notificationStatus(NotificationStatus.UNREAD.toString())
                .description("Test Notification")
                .build();

        superAdminNotification = new Notification();
        superAdminNotification.setNotificationStatus(NotificationStatus.UNREAD.toString());
        superAdminNotification.setId("2");
        superAdminNotification.setUserId("SUPER_ADMIN");
        superAdminNotification.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));
        superAdminNotification.setDescription("Test Notification");

        // Clear mock interactions
        reset(notificationRepository, modelMapper, restTemplate);
    }

    @DisplayName("Store Notification: Success")
    @Test
    void givenValidNotificationDto_whenStoreNotification_thenReturnNotificationId() {
        // Given
        when(modelMapper.map(notificationDto, Notification.class)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        // When
        String result = notificationService.storeNotification(notificationDto);

        // Then
        assertEquals("1", result);
    }

    @DisplayName("Store Notification: Mapping Exception")
    @Test
    void givenInvalidNotificationDto_whenStoreNotification_thenThrowModelMappingException() {
        // Given
        when(modelMapper.map(notificationDto, Notification.class)).thenThrow(ModelMappingException.class);

        // When - Then
        assertThrows(ModelMappingException.class, () -> {
            notificationService.storeNotification(notificationDto);
        });
    }

    @DisplayName("Retrieve All Notifications: Success")
    @Test
    void whenRetrieveAllNotifications_thenReturnListOfNotificationDtos() {
        // Given
        Page<Notification> notifications = new PageImpl<>(List.of(superAdminNotification));
        when(notificationRepository.findByUserId(anyString(), any(Pageable.class))).thenReturn(notifications);
        when(modelMapper.map(superAdminNotification, NotificationDto.class)).thenReturn(superAdminNotificationDto);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotifications(Pageable.ofSize(10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("2", result.getContent().getFirst().getId());
    }

    @DisplayName("Create Notification and Notify User: Success")
    @Test
    void whenCreateNotificationAndNotifyUser_thenNotificationSent() throws ExecutionException, InterruptedException, JsonProcessingException {
        // Given
        Notification mockNotification = new Notification();
        mockNotification.setId("1");

        when(modelMapper.map(any(NotificationDto.class), eq(Notification.class))).thenReturn(mockNotification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(objectMapper.writeValueAsString(any(NotificationDto.class))).thenReturn("{\"id\":\"1\"}");
        doNothing().when(webSocketService).notifyUsersAndRolesViaWebSocket(anyString(), anyString());

        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setTargetUserId("testUser");

        // When
        CompletableFuture<Void> result = notificationService.createNotificationAndNotifyUser(assignmentDto);
        result.get();

        // Then
        assertNotNull(result);
        verify(webSocketService, times(1))
                .notifyUsersAndRolesViaWebSocket(anyString(), eq("testUser"));

        verify(webSocketService, times(1))
                .notifyUsersAndRolesViaWebSocket(anyString(), eq("SUPER_ADMIN"));
    }

    @DisplayName("Retrieve All Notifications: Mapping Exception")
    @Test
    void whenRetrieveAllNotifications_thenThrowModelMappingException() {
        // Given
        Page<Notification> notifications = new PageImpl<>(List.of(superAdminNotification));
        when(notificationRepository.findByUserId(anyString(), any(Pageable.class))).thenReturn(notifications);
        when(modelMapper.map(superAdminNotification, NotificationDto.class)).thenThrow(ModelMappingException.class);

        // When - Then
        assertThrows(ModelMappingException.class, () -> {
            notificationService.retrieveAllNotifications(Pageable.ofSize(10));
        });
    }

    @DisplayName("Retrieve Notification by User ID: Success")
    @Test
    void givenValidUserId_whenRetrieveNotificationPerUserId_thenReturnNotificationList() {
        // Given
        Page<Notification> notifications = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserId(anyString(), any(Pageable.class))).thenReturn(notifications);
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotificationsPerUserId("user1", Pageable.ofSize(10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("1", result.getContent().getFirst().getId());
    }

    @DisplayName("Retrieve Unread Notifications by User ID: Success")
    @Test
    void givenValidUserId_whenRetrieveUnreadNotificationsPerUserId_thenReturnUnreadNotifications() {
        // Given
        Page<Notification> notifications = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdAndNotificationStatus(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(notifications);
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        // When
        List<NotificationDto> result = notificationService.retrieveUnreadNotificationsPerUserId("1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.getFirst().getId());
    }

    @DisplayName("Retrieve Notification by ID: Success")
    @Test
    void givenValidNotificationId_whenRetrieveNotificationById_thenReturnNotificationDto() {
        // Given
        when(notificationRepository.findById(anyString())).thenReturn(Optional.of(notification));
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        // When
        NotificationDto result = notificationService.retrieveNotificationById("1");

        // Then
        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @DisplayName("Retrieve Notification by ID: Not Found")
    @Test
    void givenInvalidNotificationId_whenRetrieveNotificationById_thenThrowDataNotFoundException() {
        // Given
        when(notificationRepository.findById(anyString())).thenReturn(Optional.empty());

        // When - Then
        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            notificationService.retrieveNotificationById("invalid");
        });

        assertEquals("Notification with id: invalid not found in DB", exception.getMessage());
    }

    @DisplayName("Update Notification Status by ID: Success")
    @Test
    void givenValidNotificationId_whenUpdateNotificationStatus_thenReturnSuccess() {
        // Given
        when(notificationRepository.findById(anyString())).thenReturn(Optional.of(notification));

        // When
        notificationService.updateNotificationStatusToRead("1");

        // Then
        verify(notificationRepository, times(1)).save(notification);
    }

    @DisplayName("Update Notification Status by ID: Not Found")
    @Test
    void givenInvalidNotificationId_whenUpdateNotificationStatus_thenThrowDataNotFoundException() {
        // Given
        when(notificationRepository.findById(anyString())).thenReturn(Optional.empty());

        // When - Then
        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            notificationService.retrieveNotificationById("invalid");
        });

        assertEquals("Notification with id: invalid not found in DB", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    @DisplayName("Retrieve Component JWT Token: Failure")
    @Test
    void givenMockToken_whenRetrieveComponentJwtTokenFails_thenReturnNull() {
        // Given
        lenient().when(restTemplate.exchange(eq(TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class),
            any(ParameterizedTypeReference.class))).thenThrow(RestClientException.class);

        // When
        String result = notificationService.retrieveComponentJwtToken();

        // Then
        assertNull(result);
    }

    @DisplayName("Delete Notification By ID: Success")
    @Test
    void givenExistingNotificationId_whenDeleteNotificationById_thenNotificationIsDeleted() {
        // Given
        when(notificationRepository.findById(anyString())).thenReturn(Optional.of(notification));

        // When
        notificationService.deleteNotificationById("1");

        // Then
        verify(notificationRepository, times(1)).delete(notification);
    }

    @DisplayName("Delete Notification by ID: Not Found")
    @Test
    void givenInvalidNotificationId_whenDeleteNotification_thenThrowDataNotFoundException() {
        // Given
        when(notificationRepository.findById(anyString())).thenReturn(Optional.empty());

        // When - Then
        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            notificationService.deleteNotificationById("invalid");
        });

        assertEquals("Notification with id: invalid not found in DB", exception.getMessage());
    }

    @DisplayName("Retrieve All Notifications By Type: Success")
    @Test
    void givenNotificationType_whenRetrieveAllNotifications_thenReturnMappedPage() {
        // Given
        String notificationType = NotificationType.ASSIGNMENT.toString();
        Pageable pageable = PageRequest.of(0, 5);
        List<Notification> notifications = List.of(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByNotificationType(notificationType, pageable))
                .thenReturn(notificationPage);
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotificationsPerNotificationType(notificationType, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(notificationDto, result.getContent().getFirst());
    }

    @DisplayName("Retrieve Notifications By Type And UserId: Success")
    @Test
    void givenNotificationTypeAndUserId_whenRetrieveNotifications_thenReturnMappedPage() {
        // Given
        String notificationType = NotificationType.ASSIGNMENT.toString();
        String userId = "user123";
        Pageable pageable = PageRequest.of(0, 5);
        List<Notification> notifications = List.of(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByNotificationTypeAndUserId(notificationType, userId, pageable))
                .thenReturn(notificationPage);
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        // When
        Page<NotificationDto> result = notificationService
                .retrieveAllNotificationsPerNotificationTypeAndUserId(notificationType, userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(notificationDto, result.getContent().getFirst());
    }
}
