package gr.atc.modapto.integration;


import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.UserRole;

import gr.atc.modapto.model.Event;
import gr.atc.modapto.model.EventMappings;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EventControllerIntegrationTests extends SetupTestContainersEnvironment {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMappingsRepository eventMappingsRepository;

    private EventDto testEvent;
    private EventMappingsDto testEventMapping;

    @BeforeEach
    void setup() {
        eventRepository.deleteAll();

        testEvent = EventDto.builder()
                .eventType("Test")
                .smartService("Test Smart Service")
                .productionModule("Test Production Module")
                .eventType(null)
                .timestamp(LocalDateTime.now())
                .description(null)
                .priority(MessagePriority.HIGH)
                .build();

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
        eventRepository.save(modelMapper.map(testEvent, Event.class));

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
        // When
        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }


    @DisplayName("Create Event Mapping: Success")
    @Test
    void givenValidEventMapping_whenStoreNewEventMapping_thenReturnSuccess() throws Exception {
        // When
        mockMvc.perform(post("/api/events/mappings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEventMapping)))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event mapping created successfully!")));
    }


    @DisplayName("Get All Event Mappings: Success")
    @Test
    void givenValidRequest_whenGetAllEventMappings_thenReturnEventMappingList() throws Exception {
        // Given
        eventMappingsRepository.save(modelMapper.map(testEventMapping, EventMappings.class));

        // When
        mockMvc.perform(get("/api/events/mappings")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Event Mappings retrieved successfully!")))
                .andExpect(jsonPath("$.data[0].smartService", is("Test SSI")));
    }
}
