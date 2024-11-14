package gr.atc.modapto.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.service.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationService notificationService;

    private static final String NOTIFICATION_SUCCESS = "Notifications retrieved successfully!";

    /**
     * Retrieve all Notifications
     *
     * @return List<NotificationDto> : Notifications
     */
    @Operation(summary = "Retrieve all Notifications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping
    public ResponseEntity<BaseAppResponse<List<NotificationDto>>> getAllNotifications() {
        return new ResponseEntity<>(BaseAppResponse.success(notificationService.retrieveAllNotifications(), NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve all notification per UserID
     *
     * @param userId: Id of user
     * @return List<NotificationDto> : Notifications
     */
    @Operation(summary = "Retrieve all notification per UserID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseAppResponse<List<NotificationDto>>> getAllNotificationPerUser(@PathVariable String userId) {
        return new ResponseEntity<>(BaseAppResponse.success(notificationService.retrieveNotificationPerUserId(userId), NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve all unread notifications for a specific user
     *
     * @param userId: Id of user
     * @return List<NotificationDto> : Unread notifications
     */
    @Operation(summary = "Retrieve all unread notifications for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<BaseAppResponse<List<NotificationDto>>> getAllUnreadNotificationPerUser(@PathVariable String userId) {
        return new ResponseEntity<>(BaseAppResponse.success(notificationService.retrieveUnreadNotificationsPerUserId(userId), "Unread notifications retrieved successfully!"), HttpStatus.OK);
    }

    /**
     * Retrieve Notification By Id
     *
     * @return NotificationDto : Notification if exists
     */
    @Operation(summary = "Retrieve Notification By Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "417", description = "Notification with id [ID] not found in DB")
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<BaseAppResponse<NotificationDto>> getNotificationById(@PathVariable String notificationId) {
        return new ResponseEntity<>(BaseAppResponse.success(notificationService.retrieveNotificationById(notificationId), NOTIFICATION_SUCCESS), HttpStatus.OK);
    }
}
