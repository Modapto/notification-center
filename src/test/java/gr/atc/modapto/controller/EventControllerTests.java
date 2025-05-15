package gr.atc.modapto.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

import gr.atc.modapto.repository.AssignmentRepository;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import gr.atc.modapto.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.exception.CustomExceptions;
import gr.atc.modapto.service.interfaces.IEventService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(EventController.class)
@ActiveProfiles("test")
class EventControllerTests {

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

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IEventService eventService;

    private static List<EventDto> events;
    private static EventDto testEvent;
    private static EventMappingsDto testEventMapping;
    private static Page<EventDto> paginatedResults;

    @BeforeEach
    void setupInit(){
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @BeforeAll
    static void setup() {
        testEvent = EventDto.builder()
                .eventType("Test")
                .smartService("Test Smart Service")
                .productionModule("Test Production Module")
                .eventType(null)
                .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC))
                .description(null)
                .priority(MessagePriority.HIGH.toString())
                .build();

        events = List.of(testEvent);

        testEventMapping = EventMappingsDto.builder()
                .topic("Test Topic")
                .description("Test Description")
                .userRoles(List.of("OPERATOR"))
                .build();

        paginatedResults = new PageImpl<>(events, PageRequest.of(0, 10), 1);

    }

    @DisplayName("Get All Events: Success")
    @WithMockUser
    @Test
    void givenValidRequest_whenGetAllEvents_thenReturnEventList() throws Exception {
        // Given
        given(eventService.retrieveAllEvents(any(Pageable.class))).willReturn(paginatedResults);

        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Events retrieved successfully!")));
    }

    @DisplayName("Get All Events: Empty List")
    @WithMockUser
    @Test
    void givenNoEvents_whenGetAllEvents_thenReturnEmptyList() throws Exception {
        // Given
        given(eventService.retrieveAllEvents(any(Pageable.class))).willReturn(Page.empty());

        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.results").isEmpty());
    }

    @DisplayName("Get All Events: Exception Handling")
    @WithMockUser
    @Test
    void givenException_whenGetAllEvents_thenReturnServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Server error")).when(eventService).retrieveAllEvents(any(Pageable.class));

        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    @DisplayName("Create Event Mapping: Success")
    @WithMockUser
    @Test
    void givenValidEventMapping_whenStoreNewEventMapping_thenReturnSuccess() throws Exception {
        // Given
        given(eventService.storeEventMapping(any(EventMappingsDto.class))).willReturn("123");

        // When
        mockMvc.perform(post("/api/events/mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEventMapping)))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event mapping created successfully!")))
                .andExpect(jsonPath("$.data", is("123")));
    }

    @DisplayName("Create Event Mapping: Failure")
    @WithMockUser
    @Test
    void givenInvalidEventMapping_whenStoreNewEventMapping_thenReturnServerError() throws Exception {
        // Given
        given(eventService.storeEventMapping(any(EventMappingsDto.class))).willReturn(null);

        // When
        mockMvc.perform(post("/api/events/mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceComponent\":\"Test Component\"}"))
                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @DisplayName("Delete Event Mapping by ID: Success")
    @WithMockUser
    @Test
    void givenValidEventMappingId_whenDeleteEventMappingById_thenReturnSuccess() throws Exception {

        mockMvc.perform(delete("/api/events/mappings/mappingId")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event mapping deleted successfully")));
    }

    @DisplayName("Delete Event Mapping by ID: Not Found message")
    @WithMockUser
    @Test
    void givenInvalidEventMappingId_whenDeleteEventMappingById_thenReturnNotFoundMessage() throws Exception {
        doThrow(new CustomExceptions.DataNotFoundException("Requested resource not found in DB"))
                .when(eventService).deleteEventMappingById(any());

        mockMvc.perform(delete("/api/events/mappings/invalidId")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Get All Event Mappings: Success")
    @WithMockUser
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
                .andExpect(jsonPath("$.data[0].topic", is("Test Topic")));
    }

    @DisplayName("Get All Event Mappings: Exception Handling")
    @WithMockUser
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

    @DisplayName("Update Event Mapping by ID: Success")
    @WithMockUser
    @Test
    void givenValidEventMappingId_whenUpdateEventMappingById_thenReturnSuccess() throws Exception {

        mockMvc.perform(put("/api/events/mappings/mappingId")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(testEventMapping))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event mapping updated successfully")));
    }

    @DisplayName("Update Event Mapping by ID: Not Found message")
    @WithMockUser
    @Test
    void givenInvalidEventMappingId_whenUpdateEventMappingById_thenReturnNotFoundMessage() throws Exception {
        doThrow(new CustomExceptions.DataNotFoundException("Requested resource not found in DB"))
                .when(eventService).updateEventMappingById(any());

        mockMvc.perform(put("/api/events/mappings/invalidId")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(testEventMapping))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Update Event Mapping by ID: Validation Error")
    @WithMockUser
    @Test
    void givenInvalidEventMappingId_whenUpdateEventMappingById_thenReturnValidationError() throws Exception {

        EventMappingsDto badEventMapping = new EventMappingsDto();

        mockMvc.perform(put("/api/events/mappings/mappingId")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(badEventMapping))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }
}
