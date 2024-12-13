package gr.atc.modapto.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.UserRole;
import gr.atc.modapto.exception.CustomExceptions;
import gr.atc.modapto.service.IEventService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private IEventService eventService;

    private static List<EventDto> events;
    private static EventDto testEvent;
    private static EventMappingsDto testEventMapping;

    @BeforeAll
    static void setup() {
        testEvent = EventDto.builder()
                .eventType("Test")
                .smartService("Test Smart Service")
                .productionModule("Test Production Module")
                .eventType(null)
                .timestamp(LocalDateTime.now())
                .description(null)
                .priority(MessagePriority.HIGH)
                .build();

        events = List.of(testEvent);

        testEventMapping = EventMappingsDto.builder()
                .smartService("Test SSI")
                .productionModule("Test Production Module")
                .eventType("Test Event")
                .userRoles(List.of(UserRole.OPERATOR))
                .build();
    }

    @DisplayName("Get All Events: Success")
    @Test
    void givenValidRequest_whenGetAllEvents_thenReturnEventList() throws Exception {
        // Given
        given(eventService.retrieveAllEvents()).willReturn(events);

        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Events retrieved successfully!")))
                .andExpect(jsonPath("$.data[0].productionModule", is("Test Production Module")));
    }

    @DisplayName("Get All Events: Empty List")
    @Test
    void givenNoEvents_whenGetAllEvents_thenReturnEmptyList() throws Exception {
        // Given
        given(eventService.retrieveAllEvents()).willReturn(Collections.emptyList());

        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("Get All Events: Exception Handling")
    @Test
    void givenException_whenGetAllEvents_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(eventService).retrieveAllEvents();

        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    @DisplayName("Create Event Mapping: Success")
    @Test
    void givenValidEventMapping_whenStoreNewEventMapping_thenReturnSuccess() throws Exception {
        // Given
        given(eventService.storeEventMapping(any(EventMappingsDto.class))).willReturn("123");

        // When
        mockMvc.perform(post("/api/events/mappings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEventMapping)))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event mapping created successfully!")))
                .andExpect(jsonPath("$.data", is("123")));
    }

    @DisplayName("Create Event Mapping: Failure")
    @Test
    void givenInvalidEventMapping_whenStoreNewEventMapping_thenReturnServerError() throws Exception {
        // Given
        given(eventService.storeEventMapping(any(EventMappingsDto.class))).willReturn(null);

        // When
        mockMvc.perform(post("/api/events/mappings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceComponent\":\"Test Component\"}"))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @DisplayName("Delete Event Mapping by ID: Success")
    @Test
    void givenValidEventMappingId_whenDeleteEventMappingById_thenReturnSuccess() throws Exception {
        given(eventService.deleteEventMappingById("mappingId")).willReturn(true);

        mockMvc.perform(delete("/api/events/mappings/mappingId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event mapping deleted successfully!")));
    }

    @DisplayName("Delete Event Mapping by ID: Not Found message")
    @Test
    void givenInvalidEventMappingId_whenDeleteEventMappingById_thenReturnNotFoundMessage() throws Exception {
        given(eventService.deleteEventMappingById(any())).willThrow(CustomExceptions.DataNotFoundException.class);

        mockMvc.perform(delete("/api/events/mappings/invalidId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Get All Event Mappings: Success")
    @Test
    void givenValidRequest_whenGetAllEventMappings_thenReturnEventMappingList() throws Exception {
        // Given
        given(eventService.retrieveAllEventMappings()).willReturn(List.of(testEventMapping));

        // When
        mockMvc.perform(get("/api/events/mappings")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event Mappings retrieved successfully!")))
                .andExpect(jsonPath("$.data[0].smartService", is("Test SSI")));
    }

    @DisplayName("Get All Event Mappings: Exception Handling")
    @Test
    void givenException_whenGetAllEventMappings_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(eventService).retrieveAllEventMappings();

        // When
        mockMvc.perform(get("/api/events/mappings")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }
}
