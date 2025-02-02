package gr.atc.modapto.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gr.atc.modapto.controller.BaseAppResponse;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.UserRole;
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

    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper){
        this.notificationRepository = notificationRepository;
        this.restTemplate = new RestTemplate();
        this.modelMapper = modelMapper;
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
     * Retrieve all notifications
     * 
     * @param pageable : Pagination Attributes
     * @return List<NotificationDto>
     */
    @Override
    public Page<NotificationDto> retrieveAllNotifications(Pageable pageable) {
        try {
            Page<Notification> notificationPage = notificationRepository.findAll(pageable);
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
            Page<Notification> notificationsPage = notificationRepository.findByUserIdAndNotificationStatus(userId, NotificationStatus.NOT_VIEWED.toString(), Pageable.unpaged());
            List<Notification> notifications = notificationsPage.getContent();
            return notifications.stream().map(notification -> modelMapper.map(notification, NotificationDto.class)).toList();
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

            HttpEntity<BaseAppResponse<List<String>>> entity = new HttpEntity<>(headers);
            ResponseEntity<BaseAppResponse<List<String>>> response = restTemplate.exchange(
                    userManagerUrl.concat("/api/users/ids/pilot/").concat(pilot.toUpperCase()),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return Objects.requireNonNull(response.getBody()).getData();
            }
            return Collections.emptyList();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error during retrieving user ids for specific pilot: Error: {}", e.getMessage());
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Rest Client error during retrieving user ids for specific pilot: Error: {}", e.getMessage());
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
    public List<String> retrieveUserIdsPerRoles(List<UserRole> roles){
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
        List<String> userIds = new ArrayList<>();
        roles.forEach(role -> {
            try {
                ResponseEntity<BaseAppResponse<List<String>>> response = restTemplate.exchange(
                        userManagerUrl.concat("/api/users/ids/role/").concat(role.toString()),
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {
                        }
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    userIds.addAll(Objects.requireNonNull(Objects.requireNonNull(response.getBody()).getData()));
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("HTTP error during retrieving user for role {}: Error: {}", role, e.getMessage());
            } catch (RestClientException e) {
                log.error("Rest Client error during retrieving user role {}: Error: {}", role, e.getMessage());
            }
        });
        return userIds;
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
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody == null || responseBody.get(TOKEN) == null) {
                    return null;
                }
                return responseBody.get(TOKEN).toString();
            }
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error during authenticating the client: Error: {}", e.getMessage());
            return null;
        } catch (RestClientException e) {
            log.error("Rest Client error during authenticating the client: Error: {}", e.getMessage());
            return null;
        }
    }
}
