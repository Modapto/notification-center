package gr.atc.modapto.controller;

import java.util.Arrays;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.dto.PaginatedResultsDto;
import gr.atc.modapto.service.interfaces.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name="Notification Controller", description = "Manage notifications for MODAPTO system")
public class NotificationController {

    private final INotificationService notificationService;

    private static final String NOTIFICATION_SUCCESS = "Notifications retrieved successfully!";

    /**
     * Retrieve all Notifications
     *
     * @param page: Number of Page
     * @param size: Size of Page Elements
     * @param sortAttribute: Sort Based on Variable field
     * @param isAscending: ASC or DESC
     * @return PaginatedResultsDto<NotificationDto> : Notifications with pagination
     */
    @Operation(summary = "Retrieve all Notifications", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BaseAppResponse.class,
                            subTypes = {PaginatedResultsDto.class, NotificationDto.class},
                            description = "BaseAppResponse containing PaginatedResultDto of NotificationDto"))),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Invalid sort attributes")
    })
    @GetMapping
    public ResponseEntity<BaseAppResponse<PaginatedResultsDto<NotificationDto>>> getAllNotifications(
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "10") int size,
        @RequestParam(required = false, defaultValue = "timestamp") String sortAttribute,
        @RequestParam(required = false, defaultValue = "false") boolean isAscending) {
    
        // Fix the pagination parameters
        Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
        if (pageable == null) 
            return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attributes"), HttpStatus.BAD_REQUEST);
        
        // Retrieve stored results in pages
        Page<NotificationDto> resultsPage = notificationService.retrieveAllNotifications(pageable);
        
        // Fix the pagination class object
        PaginatedResultsDto<NotificationDto> results = new PaginatedResultsDto<>(
            resultsPage.getContent(),
            resultsPage.getTotalPages(),
            (int) resultsPage.getTotalElements(),
            resultsPage.isLast());

        return new ResponseEntity<>(BaseAppResponse.success(results, NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve all notification per UserID
     *
     * @param userId: Id of user
     * @return List<NotificationDto> : Notifications
     */
    @Operation(summary = "Retrieve all notification per UserID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseAppResponse<PaginatedResultsDto<NotificationDto>>> getAllNotificationPerUser(
        @PathVariable String userId,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "10") int size,
        @RequestParam(required = false, defaultValue = "timestamp") String sortAttribute,
        @RequestParam(required = false, defaultValue = "false") boolean isAscending) {

        // Fix the pagination parameters
        Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
        if (pageable == null) 
            return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attributes"), HttpStatus.BAD_REQUEST);
        
        // Retrieve stored results in pages
        Page<NotificationDto> resultsPage = notificationService.retrieveAllNotificationsPerUserId(userId, pageable);
        
        // Fix the pagination class object
        PaginatedResultsDto<NotificationDto> results = new PaginatedResultsDto<>(
            resultsPage.getContent(),
            resultsPage.getTotalPages(),
            (int) resultsPage.getTotalElements(),
            resultsPage.isLast());

        return new ResponseEntity<>(BaseAppResponse.success(results, NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve all unread notifications for a specific user
     *
     * @param userId: Id of user
     * @return List<NotificationDto> : Unread notifications
     */
    @Operation(summary = "Retrieve all unread notifications for a specific user", security = @SecurityRequirement(name = "bearerToken"))
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
    @Operation(summary = "Retrieve Notification By ID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Notification with id [ID] not found in DB")
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<BaseAppResponse<NotificationDto>> getNotificationById(@PathVariable String notificationId) {
        return new ResponseEntity<>(BaseAppResponse.success(notificationService.retrieveNotificationById(notificationId), NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Create pagination parameters
     *
     * @param page : Page of results
     * @param size : Results per page
     * @param sortAttribute : Sort attribute
     * @param isAscending : Sort order
     * @return pageable : Pagination Object
     */
    private Pageable createPaginationParameters(int page, int size, String sortAttribute, boolean isAscending){
        // Check if sort attribute is valid
        boolean isValidField = Arrays.stream(NotificationDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals(sortAttribute));

        // If not valid, return null
        if (!isValidField) {
            return null;
        }

        // Create pagination parameters
        return isAscending
                ? PageRequest.of(page, size, Sort.by(sortAttribute).ascending())
                : PageRequest.of(page, size, Sort.by(sortAttribute).descending());
    }

}
