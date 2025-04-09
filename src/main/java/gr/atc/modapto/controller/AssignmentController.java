package gr.atc.modapto.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gr.atc.modapto.dto.AssignmentCommentDto;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.dto.PaginatedResultsDto;
import gr.atc.modapto.service.interfaces.IAssignmentService;
import gr.atc.modapto.util.JwtUtils;
import gr.atc.modapto.validation.ValidAssignmentStatus;
import gr.atc.modapto.validation.ValidAssignmentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/assignments")
@AllArgsConstructor
@Validated
@Slf4j
@Tag(name="Assignment Controller", description = "Manage assignments of users")
public class AssignmentController {

    private final IAssignmentService assignmentService;

    private static final String ASSIGNMENT_SUCCESS = "Assignments retrieved successfully!";

    /**
     * Retrieve all Assignments
     *
     * @param page: Page of results
     * @param size: Results per page
     * @param sortAttribute : Sort attribute
     * @param isAscending : Sort order
     * @return Page<AssignmentDto> : Assignments
     */
    @Operation(summary = "Retrieve all Assignments", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = ASSIGNMENT_SUCCESS),
            @ApiResponse(responseCode = "400", description = "Invalid sort attribute."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping
    public ResponseEntity<BaseAppResponse<PaginatedResultsDto<AssignmentDto>>> getAllAssignments(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "timestampUpdated") String sortAttribute,
            @RequestParam(required = false, defaultValue = "false") boolean isAscending){

        // Fix the pagination parameters
        Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
        if (pageable == null) {
            return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attribute"), HttpStatus.BAD_REQUEST);
        }

        // Retrieve stored results in pages
        Page<AssignmentDto> resultsPage = assignmentService.retrieveAllAssignments(pageable);

        // Fix the pagination class object
        PaginatedResultsDto<AssignmentDto> results = new PaginatedResultsDto<>(
                resultsPage.getContent(),
                resultsPage.getTotalPages(),
                (int) resultsPage.getTotalElements(),
                resultsPage.isLast());

        return new ResponseEntity<>(BaseAppResponse.success(results, ASSIGNMENT_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve all Assignment per UserID and Status (optional) and Assignment Type (Source or Received) (optional)
     *
     * @param userId: Id of user
     * @param status: Assignment Status
     * @param page: Page of results
     * @param size: Results per page
     * @param sortAttribute : Sort attribute
     * @param isAscending : Sort order
     * @return Page<AssignmentDto> : Assignments
     */
    @Operation(summary = "Retrieve all Assignment per UserID and Status (optional) and Assignment Type (Source or Received) (optional)" , security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = ASSIGNMENT_SUCCESS),
            @ApiResponse(responseCode = "400", description = "Invalid assignment status. Only OPEN, ACCEPTED, IN_PROGRESS and COMPLETED are allowed."),
            @ApiResponse(responseCode = "400", description = "Invalid assignment type. Only 'requested' or 'received' are allowed."),
            @ApiResponse(responseCode = "400", description = "Invalid sort attribute."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseAppResponse<PaginatedResultsDto<AssignmentDto>>> getAllAssignmentPerUser(
                @PathVariable String userId,
                @ValidAssignmentStatus @RequestParam(required = false) String status,
                @ValidAssignmentType @RequestParam(required = false) String type,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size,
                @RequestParam(required = false, defaultValue = "timestampUpdated") String sortAttribute,
                @RequestParam(required = false, defaultValue = "false") boolean isAscending) {

        // Fix the pagination parameters
        Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
        if (pageable == null) {
            return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attribute"), HttpStatus.BAD_REQUEST);
        }

        // Return results according to whether status was inserted
        Page<AssignmentDto> resultsPage;
        if (status != null)
            resultsPage = assignmentService.retrieveAssignmentsPerUserIdAndStatus(userId, type != null ? type.toUpperCase() : null, status.toUpperCase(), pageable);
        else
            resultsPage = assignmentService.retrieveAssignmentsPerUserId(userId, type != null ? type.toUpperCase() : null, pageable);

        // Fix the pagination class object
        PaginatedResultsDto<AssignmentDto> results = new PaginatedResultsDto<>();
        if (resultsPage != null) {
            results.setLastPage(resultsPage.isLast());
            results.setResults(resultsPage.getContent());
            results.setTotalElements((int) resultsPage.getTotalElements());
            results.setTotalPages(resultsPage.getTotalPages());
        }

        return new ResponseEntity<>(BaseAppResponse.success(results, ASSIGNMENT_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve an Assignment given its ID
     *
     * @param assignmentId: Id of assignment
     * @return AssignmentDto : Assignment requested if found in DB
     */
    @Operation(summary = "Retrieve an Assignment given its ID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Assignment with id [ID] not found in DB")
    })
    @GetMapping("/{assignmentId}")
    public ResponseEntity<BaseAppResponse<AssignmentDto>> getAssignmentById(@PathVariable String assignmentId) {
        return new ResponseEntity<>(BaseAppResponse.success(assignmentService.retrieveAssignmentById(assignmentId), "Assignment retrieved successfully"), HttpStatus.OK);
    }

    /**
     * Create a new assignment and asynchronously create a new notification and send it to destination user
     *
     * @param assignmentDto: DTO of assignment
     * @return AssignmentId : Id of assignment
     */
    @Operation(summary = "Create a new assignment", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment created successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "401", description = "Invalid JWT token inserted"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "500", description = "Unable to create and store assignment")
    })
    @PostMapping("/create")
    public ResponseEntity<BaseAppResponse<String>> createAssignment(@RequestBody AssignmentDto assignmentDto, @AuthenticationPrincipal Jwt jwt) {
        // Add current user's ID in message
        assignmentDto.setSourceUserId(JwtUtils.extractUserId(jwt));

        // Generate System Message
        AssignmentCommentDto systemComment = new AssignmentCommentDto();
        systemComment.setComment("Task assignment has been initiated");
        assignmentDto.setComments(List.of(systemComment));

        // Store Assignment in DB
        String assignmentId = assignmentService.storeAssignment(assignmentDto);
        if (assignmentId == null)
            return new ResponseEntity<>(BaseAppResponse.error("Unable to create and store assignment"), HttpStatus.INTERNAL_SERVER_ERROR);

        assignmentDto.setId(assignmentId);
        // Create Notification and Notify relevant user asynchronously
        CompletableFuture<Void> notificationCreationAsync = assignmentService.createNotificationAndNotifyUser(assignmentDto);

        // Log the notification creation process
        CompletableFuture.allOf(notificationCreationAsync)
                .thenRun(() -> log.info("Notification creation process completed"))
                .exceptionally(ex -> {
                    log.error("Notification creation process failed", ex);
                    return null;
                });

        // Return success message
        return new ResponseEntity<>(BaseAppResponse.success(assignmentId, "Assignment created successfully"), HttpStatus.OK);

    }

    /**
     * Update an assignment
     *
     * @param assignmentId: Id of assignment
     * @param assignmentDto: DTO of assignment
     * @return Message of success or error
     */
    @Operation(summary = "Update an assignment", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment updated successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "500", description = "Unable to create and store assignment")
    })
    @PutMapping("/{assignmentId}")
    public ResponseEntity<BaseAppResponse<String>> updateAssignment(@PathVariable String assignmentId, @RequestBody AssignmentDto assignmentDto) {
        // Check whether assignment status has been changed
        assignmentDto.setId(assignmentId);
        AssignmentDto updatedAssignment = generateSystemComments(assignmentDto);
        assignmentService.updateAssignment(updatedAssignment);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Assignment updated successfully!"), HttpStatus.OK);
    }

    /**
     * Generate system comments for assignment status and priority changes
     *
     * @param assignmentDto: DTO of assignment
     * @return AssignmentDto : Updated assignment with system comments
     */
    private AssignmentDto generateSystemComments(AssignmentDto assignmentDto) {
        AssignmentCommentDto systemComment = AssignmentCommentDto.builder().build();
        List<AssignmentCommentDto> comments = assignmentDto.getComments() != null ? assignmentDto.getComments() : new ArrayList<>();
        // Check whether assignment status has been changed
        if (assignmentDto.getStatus() != null){
            systemComment.setComment("Task status updated to '" + assignmentDto.getStatus() + "'");
            comments.add(systemComment);
        }

        // Check whether assignment priority has been changed
        if (assignmentDto.getPriority() != null) {
            systemComment.setComment("Task prioity updated to '" + assignmentDto.getPriority() + "'");
            comments.add(systemComment);
        }

        assignmentDto.setComments(comments);
        return assignmentDto;
    }

    /**
     * Add a comment to an assignment depending on the user (Source or Target)
     *
     * @param assignmentId: Id of assignment
     * @param assignmentComment: Assignment Comment Dto depending on the user who commented
     * @return Message of success or error
     */
    @Operation(summary = "Add a comment to an assignment depending on the user (Source or Target)", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment comments updated successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "500", description = "Unable to create and store assignment")
    })
    @PutMapping("/{assignmentId}/comments")
    public ResponseEntity<BaseAppResponse<String>> updateAssignmentComments(@PathVariable String assignmentId, @RequestBody @Valid AssignmentCommentDto assignmentComment) {
        assignmentComment.setDatetime(LocalDateTime.now().withNano(0));
        assignmentService.updateAssignmentComments(assignmentId, assignmentComment);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Assignment comments updated successfully!"), HttpStatus.OK);
    }

    /**
     * Delete an assignment by ID
     *
     * @param assignmentId: Id of assignment
     * @return Message of success or error
     */
    @Operation(summary = "Delete an assignment by ID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment deleted successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Assignment with id [ID] not found in DB")
    })
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<BaseAppResponse<String>> deleteAssignmentById(@PathVariable String assignmentId) {
        assignmentService.deleteAssignmentById(assignmentId);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Assignment deleted successfully!"), HttpStatus.OK);
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
        boolean isValidField = Arrays.stream(AssignmentDto.class.getDeclaredFields())
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
