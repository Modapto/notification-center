package gr.atc.modapto.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.service.IEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;

/**
 * EventController class to manage REST API Requests
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final IEventService eventService;

    /**
     * Retrieve all events
     * @return List of Events
     */
    @Operation(summary = "Retrieve all events")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping
    public ResponseEntity<BaseAppResponse<List<EventDto>>> getAllEvents() {
        return new ResponseEntity<>(BaseAppResponse.success(eventService.retrieveAllEvents(), "Events retrieved successfully!"), HttpStatus.OK);
    }

    /**
     * Create a new event Mapping
     *
     * @param eventMapping: Event Mapping Dto
     * @return Event Mapping ID
     */
    @Operation(summary = "Create a new event Mapping")
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
    @Operation(summary = "Get all Event Mappings")
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
}