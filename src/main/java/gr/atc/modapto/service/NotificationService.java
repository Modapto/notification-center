package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.dto.UserDto;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.service.interfaces.INotificationService;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gr.atc.modapto.controller.BaseAppResponse;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Notification;
import gr.atc.modapto.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;

    private final RestTemplate restTemplate;

    private final ModelMapper modelMapper;

    private final WebSocketService webSocketService;

    private final ObjectMapper objectMapper;

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    @Value("${keycloak.token-uri}")
    private String tokenUri;

    @Value("${user.manager.component.url}")
    private String userManagerUrl;

    @Value("${keycloak.client}")
    private String client;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    private static final String TOKEN = "access_token";

    private static final String JWT_ERROR = "Unable to retrieve Component's JWT Token - Client credentials";

    private static final String MAPPING_ERROR = "Error mapping Notifications to Dto - Error: ";

    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper, WebSocketService webSocketService, ObjectMapper objectMapper){
        this.notificationRepository = notificationRepository;
        this.restTemplate = new RestTemplate();
        this.modelMapper = modelMapper;
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
    }

    /**
     * Store notification in DB
     *
     * @param notification: Notification Dto
     * @return String: Notification ID
     */
    @Override
    public String storeNotification(NotificationDto notification) {
       try{
           return notificationRepository.save(modelMapper.map(notification, Notification.class)).getId();
       } catch (MappingException e){
           throw new ModelMappingException("Unable to map NotificationDto to Notification - " + e.getMessage());
       }
    }

    /**
     * Delete notification by Id
     *
     * @param notificationId: Id of notification
     */
    @Override
    public void deleteNotificationById(String notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty())
            throw new DataNotFoundException("Notification with id: " + notificationId + " not found in DB");

        notificationRepository.delete(optionalNotification.get());
    }

    /**
     * Retrieve all notifications for Super-Admins
     *
     * @param pageable : Pagination Attributes
     * @return List<NotificationDto>
     */
    @Override
    public Page<NotificationDto> retrieveAllNotifications(Pageable pageable) {
        try {
            Page<Notification> notificationPage = notificationRepository.findByUserId(SUPER_ADMIN_ROLE, pageable);
            return notificationPage.map(notification -> modelMapper.map(notification, NotificationDto.class));
        } catch (MappingException e) {
            throw new ModelMappingException(MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve all notifications for a specific user
     *
     * @param userId : Id of user
     * @param pageable : Pagination Attributes
     * @return List<NotificationDto>
     */
    @Override
    public Page<NotificationDto> retrieveAllNotificationsPerUserId(String userId, Pageable pageable){
        try{
            Page<Notification> notificationPage = notificationRepository.findByUserId(userId, pageable);
            return notificationPage.map(notification -> modelMapper.map(notification, NotificationDto.class));
        } catch (MappingException e) {
            throw new ModelMappingException(MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve all unread notifications for a specific user
     *
     * @param userId: Id of user
     * @return List<NotificationDto>
     */
    @Override
    public List<NotificationDto> retrieveUnreadNotificationsPerUserId(String userId) {
        try{
            Page<Notification> notificationsPage = notificationRepository.findByUserIdAndNotificationStatus(userId, NotificationStatus.UNREAD.toString(), Pageable.unpaged());
            List<Notification> notifications = notificationsPage.getContent();
            return notifications.stream().map(notification -> modelMapper.map(notification, NotificationDto.class)).toList();
        } catch (MappingException e) {
            throw new ModelMappingException(MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve all notifications per notification status
     *
     * @param notificationType : Notification Type
     * @param pageable : Pagination Attributes
     * @return Page<NotificationDto>
     */
    @Override
    public Page<NotificationDto> retrieveAllNotificationsPerNotificationType(String notificationType, Pageable pageable) {
        try{
            Page<Notification> notificationsPage = notificationRepository.findByNotificationType(notificationType, pageable);
            return notificationsPage.map(notification -> modelMapper.map(notification, NotificationDto.class));
        } catch (MappingException e) {
            throw new ModelMappingException(MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve all notifications per notification status and userId
     *
     * @param notificationType : Notification Type
     * @param userId : User ID
     * @param pageable : Pagination Attributes
     * @return Page<NotificationDto>
     */
    @Override
    public Page<NotificationDto> retrieveAllNotificationsPerNotificationTypeAndUserId(String notificationType, String userId, Pageable pageable) {
        try{
            Page<Notification> notificationsPage = notificationRepository.findByNotificationTypeAndUserId(notificationType, userId, pageable);
            return notificationsPage.map(notification -> modelMapper.map(notification, NotificationDto.class));
        } catch (MappingException e) {
            throw new ModelMappingException(MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve a notification given a notification Id
     *
     * @param notificationId: Id of notification
     * @return NotificationDto
     */
    @Override
    public NotificationDto retrieveNotificationById(String notificationId){
        try{
            Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isEmpty())
                throw new DataNotFoundException("Notification with id: " + notificationId + " not found in DB");
            return modelMapper.map(optionalNotification.get(), NotificationDto.class);
        } catch (MappingException e) {
            throw new ModelMappingException("Error mapping Notification to Dto - Error: " + e.getMessage());
        }
    }

    /**
     * Update notification status (From Unread to Read) for a specific notification
     *
     * @param notificationId : ID of notification
     */
    @Override
    public void updateNotificationStatusToRead(String notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty())
            throw new DataNotFoundException("Notification with id: " + notificationId + " not found in DB");

        Notification notification = optionalNotification.get();
        notification.setNotificationStatus(NotificationStatus.READ.toString());
        notificationRepository.save(notification);
    }

    /**
     * Retrieve all user ids for a specific Pilot from User Manager Service
     *
     * @return List<String> : List with user Ids
     */
    @Override
    public List<String> retrieveUserIdsPerPilot(String pilot) {
        // Retrieve Component's JWT Token - Client credentials
        String token = retrieveComponentJwtToken();
        if (token == null){
            log.error(JWT_ERROR);
            return Collections.emptyList();
        }

        // Retrieve User Ids
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<BaseAppResponse<List<UserDto>>> response = restTemplate.exchange(
                    userManagerUrl.concat("/api/users/pilot/").concat(pilot),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return Optional.of(response)
                    .filter(resp -> resp.getStatusCode().is2xxSuccessful())
                    .map(ResponseEntity::getBody)
                    .map(BaseAppResponse::getData)
                    .map(dataList -> dataList.stream()
                            .map(UserDto::getUserId)
                            .filter(Objects::nonNull)
                            .toList())
                    .orElse(Collections.emptyList());
        } catch (RestClientException e) {
            log.error("Unable to retrieve user IDs for pilot {} -  Error: {}", pilot, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve user ids per role
     *
     * @param roles: User Roles correlated with an event
     * @return List<String> : List with user Ids
     */
    @Override
    public List<String> retrieveUserIdsPerRoles(List<String> roles){
        // Retrieve Component's JWT Token - Client credentials
        String token = retrieveComponentJwtToken();
        if (token == null){
            return Collections.emptyList();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        // Retrieve User Ids per role
        List<String> allUserIds = new ArrayList<>();
        roles.forEach(role -> {
            try {
                ResponseEntity<BaseAppResponse<List<UserDto>>> response = restTemplate.exchange(
                        userManagerUrl.concat("/api/users/role/").concat(role),
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {}
                );

                // Parse response and retrieve user Ids
                List<String> userIds = Optional.of(response)
                        .filter(resp -> resp.getStatusCode().is2xxSuccessful())
                        .map(ResponseEntity::getBody)
                        .map(BaseAppResponse::getData)
                        .map(dataList -> dataList.stream()
                                .map(UserDto::getUserId)
                                .filter(Objects::nonNull)
                                .toList())
                        .orElse(Collections.emptyList());

                allUserIds.addAll(userIds);
            } catch (RestClientException e) {
                log.error("Unable to locate User IDs for Role: {} - Error: {}", role, e.getMessage());
            }
        });
        return allUserIds;
    }

    /**
     * Generate a JWT Token to access Keycloak resources
     *
     * @return Token
     */
    public String retrieveComponentJwtToken(){
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", client);
            map.add("client_secret", clientSecret);
            map.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return Optional.of(response)
                    .filter(resp -> resp.getStatusCode().is2xxSuccessful())
                    .map(ResponseEntity::getBody)
                    .filter(body -> body.get(TOKEN) != null)
                    .map(body -> body.get(TOKEN).toString())
                    .orElse(null);
        }  catch (RestClientException e) {
            log.error("Rest Client error during authenticating the client: Error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create async a notification connected to an assignment and notify user through WebSockets
     *
     * @param assignment: Assignment DTO
     */
    @Async("asyncPoolTaskExecutor")
    @Override
    public CompletableFuture<Void> createNotificationAndNotifyUser(AssignmentDto assignment) {
        return CompletableFuture.runAsync(() -> {
                // Create the notification
                NotificationDto assignmentNotification = NotificationDto.builder()
                        .notificationType(NotificationType.ASSIGNMENT.toString())
                        .notificationStatus(NotificationStatus.UNREAD.toString())
                        .messageStatus(assignment.getStatus())
                        .productionModule(assignment.getProductionModule())
                        .userId(null)
                        .user(null)
                        .smartService(null)
                        .relatedAssignment(assignment.getId())
                        .relatedEvent(null)
                        .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC))
                        .priority(assignment.getPriority())
                        .description(assignment.getDescription())
                        .build();

                // Store Notification for Associated User
                storeNotificationForEachAssociatedUserAndNotifyUser(assignment.getTargetUserId(), assignment.getTargetUser(), assignmentNotification);

                // Store Notification for Super Admin
                storeNotificationForEachAssociatedUserAndNotifyUser(SUPER_ADMIN_ROLE, SUPER_ADMIN_ROLE, assignmentNotification);
        });
    }

    /*
     * Helper method to Store Assignment Notification per Each User and Notify User
     */
    private void storeNotificationForEachAssociatedUserAndNotifyUser(String userId, String userFullName, NotificationDto notification){
        try {
            // Update Notification with the Associated User
            notification.setUserId(userId);
            notification.setUser(userFullName);

            String notificationId = storeNotification(notification);
            log.info("Notification for user {} stored in DB with ID: {}", userId, notificationId);
            if (notificationId == null){
                log.error("Notification could not be stored in DB");
                return;
            }

            notification.setId(notificationId);
            String assignmentMessage = objectMapper.writeValueAsString(notification);
            // Send notification through WebSocket
            webSocketService.notifyUsersAndRolesViaWebSocket(assignmentMessage, userId);
        } catch (JsonProcessingException e){
            log.error("Error processing Notification Dto to JSON - Error: {}", e.getMessage());
        }
    }
}
