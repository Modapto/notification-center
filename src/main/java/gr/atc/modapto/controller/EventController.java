package gr.atc.modapto.controller;

import java.util.Arrays;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.dto.PaginatedResultsDto;
import gr.atc.modapto.service.interfaces.IEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

/**
 * EventController class to manage REST API Requests
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/events")
@Tag(name="Event Controller", description = "Manage events and event mappings for MODAPTO system")
public class EventController {

    private final IEventService eventService;

    /**
     * Retrieve all events
     * 
     * @param page: Number of Page
     * @param size: Size of Page Elements
     * @param sortAttribute: Sort Based on Variable field
     * @param isAscending: ASC or DESC
     * @return List of Events (Paginated)
     */
    @Operation(summary = "Retrieve all events" , security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Invalid sort attributes")
    })
    @GetMapping
    public ResponseEntity<BaseAppResponse<PaginatedResultsDto<EventDto>>> getAllEvents(
                @RequestParam(required = false, defaultValue = "0") int page,
                @RequestParam(required = false, defaultValue = "10") int size,
                @RequestParam(required = false, defaultValue = "timestamp") String sortAttribute,
                @RequestParam(required = false, defaultValue = "false") boolean isAscending) {

        // Fix the pagination parameters
        Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
        if (pageable == null) {
            return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attributes"), HttpStatus.BAD_REQUEST);
        }

        // Retrieve stored results in pages
        Page<EventDto> resultsPage = eventService.retrieveAllEvents(pageable);

        // Fix the pagination class object
        PaginatedResultsDto<EventDto> results = new PaginatedResultsDto<>(
                resultsPage.getContent(),
                resultsPage.getTotalPages(),
                (int) resultsPage.getTotalElements(),
                resultsPage.isLast());

        return new ResponseEntity<>(BaseAppResponse.success(results, "Events retrieved successfully!"), HttpStatus.OK);
    }

    /**
     * Create a new event Mapping
     *
     * @param eventMapping: Event Mapping Dto
     * @return Event Mapping ID
     */
    @Operation(summary = "Create a new event Mapping" , security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping created successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "500", description = "Error storing event mapping!")
    })
    @PostMapping("/mappings/create")
    public ResponseEntity<BaseAppResponse<String>> storeNewEventMapping(@RequestBody EventMappingsDto eventMapping) {
        String eventMappingId = eventService.storeEventMapping(eventMapping);
        if (eventMappingId == null)
            return new ResponseEntity<>(BaseAppResponse.error("Error storing event mapping!"), HttpStatus.INTERNAL_SERVER_ERROR);

        return new ResponseEntity<>(BaseAppResponse.success(eventMappingId, "Event mapping created successfully!"), HttpStatus.OK);
    }

    /**
     * Get all Event Mappings
     *
     * @return List<EventMappingsDto>
     */
    @Operation(summary = "Get all Event Mappings" , security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping retrieved successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "500", description = "Error storing event mapping!")
    })
    @GetMapping("/mappings")
    public ResponseEntity<BaseAppResponse<List<EventMappingsDto>>> getAllEventMappings() {
        return new ResponseEntity<>(BaseAppResponse.success(eventService.retrieveAllEventMappings(), "Event Mappings retrieved successfully!"), HttpStatus.OK);
    }

    /**
     * Delete an event mapping by ID
     *
     * @param mappingId: Id of event mapping
     * @return Message of success or error
     */
    @Operation(summary = "Delete an assignment by ID" , security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Event mapping with id [ID] not found in DB")
    })
    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<BaseAppResponse<String>> deleteEventMappingById(@PathVariable String mappingId) {
        eventService.deleteEventMappingById(mappingId);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Event mapping deleted successfully"), HttpStatus.OK);
    }

    /**
     * Update an Event Mapping
     *
     * @param mappingId: Id of event mapping
     * @return Message of success or error
     */
    @Operation(summary = "Update an Event Mapping" , security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation Error"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Event mapping with id [ID] not found in DB")
    })
    @PutMapping("/mappings/{mappingId}")
    public ResponseEntity<BaseAppResponse<String>> updateEventMappingById(@PathVariable String mappingId, @RequestBody @Valid EventMappingsDto eventMapping) {
        eventMapping.setId(mappingId);
        eventService.updateEventMappingById(eventMapping);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Event mapping updated successfully"), HttpStatus.OK);
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
        boolean isValidField = Arrays.stream(EventDto.class.getDeclaredFields())
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