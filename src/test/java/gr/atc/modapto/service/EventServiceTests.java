package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Event;
import gr.atc.modapto.model.EventMappings;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
class EventServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMappingsRepository eventMappingsRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private EventService eventService;

    private EventDto testEventDto;
    private Event testEvent;
    private EventMappingsDto testEventMappingDto;
    private EventMappings testEventMapping;

    @BeforeEach
    void setup() {
        testEventDto = EventDto.builder()
                .id("1")
                .eventType("Test")
                .smartService("Test Smart Service")
                .productionModule("Test Production Module")
                .eventType(null)
                .timestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC))
                .description(null)
                .priority(MessagePriority.HIGH.toString())
                .build();
        
        testEvent = new Event();
        testEvent.setId("1");
        testEvent.setEventType("Test");
        testEvent.setSmartService("Test Smart Service");
        testEvent.setProductionModule("Test Production Module");
        testEvent.setEventType(null);
        testEvent.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));
        testEvent.setDescription(null);


        testEventMappingDto = EventMappingsDto.builder()
                .id("1")
                .description(null)
                .topic("Test Topic")
                .userRoles(List.of("OPERATOR"))
                .build();
        
        testEventMapping = EventMappings.builder()
                .id("1")
                .description(null)
                .topic("Test Topic")
                .userRoles(List.of("OPERATOR"))
                .build();

        // Clear mock interactions
        reset(eventRepository, eventMappingsRepository, modelMapper);
    }

    @DisplayName("Store Incoming Event: Success")
    @Test
    void givenValidEventDto_whenStoreIncomingEvent_thenReturnEventId() {
        // Given
        when(modelMapper.map(testEventDto, Event.class)).thenReturn(testEvent);
        when(eventRepository.save(testEvent)).thenReturn(testEvent);

        // When
        String result = eventService.storeIncomingEvent(testEventDto);

        // Then
        assertEquals("1", result);
    }

    @DisplayName("Store Incoming Event: Mapping Exception")
    @Test
    void givenInvalidEventDto_whenStoreIncomingEvent_thenThrowModelMappingException() {
        // Given
        when(modelMapper.map(testEventDto, Event.class)).thenThrow(ModelMappingException.class);

        // When - Then
        assertThrows(ModelMappingException.class, () -> {
            eventService.storeIncomingEvent(testEventDto);
        });
    }

    @DisplayName("Store Event Mapping: Success")
    @Test
    void givenValidEventMappingsDto_whenStoreEventMapping_thenReturnMappingId() {
        // Given
        when(modelMapper.map(testEventMappingDto, EventMappings.class)).thenReturn(testEventMapping);
        when(eventMappingsRepository.save(testEventMapping)).thenReturn(testEventMapping);

        // When
        String result = eventService.storeEventMapping(testEventMappingDto);

        // Then
        assertEquals("1", result);
    }

    @DisplayName("Store Event Mapping: Mapping Exception")
    @Test
    void givenInvalidEventMappingsDto_whenStoreEventMapping_thenThrowModelMappingException() {
        // Given
        when(modelMapper.map(testEventMappingDto, EventMappings.class)).thenThrow(ModelMappingException.class);

        // When - Then
        assertThrows(ModelMappingException.class, () -> {
            eventService.storeEventMapping(testEventMappingDto);
        });
    }

    @DisplayName("Retrieve Event by ID: Success")
    @Test
    void givenValidEventId_whenRetrieveEventById_thenReturntestEventDto() {
        // Given
        when(eventRepository.findById("1")).thenReturn(Optional.of(testEvent));
        when(modelMapper.map(testEvent, EventDto.class)).thenReturn(testEventDto);

        // When
        EventDto result = eventService.retrieveEventById("1");

        // Then
        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @DisplayName("Retrieve Event by ID: Not Found")
    @Test
    void givenInvalidEventId_whenRetrieveEventById_thenThrowDataNotFoundException() {
        // Given
        when(eventRepository.findById("invalid")).thenReturn(Optional.empty());

        // When - Then
        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            eventService.retrieveEventById("invalid");
        });

        assertEquals("Event with id: invalid not found in DB", exception.getMessage());
    }

    @DisplayName("Retrieve All Events: Success")
    @Test
    void whenRetrieveAllEvents_thenReturnListOftestEventDtos() {
        // Given
        Page<Event> events = new PageImpl<>(List.of(testEvent));
        when(eventRepository.findAll(any(Pageable.class))).thenReturn(events);
        when(modelMapper.map(testEvent, EventDto.class)).thenReturn(testEventDto);

        // When
        Page<EventDto> result = eventService.retrieveAllEvents(Pageable.ofSize(10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("1", result.getContent().getFirst().getId());
    }

    @DisplayName("Retrieve All Events: Mapping Exception")
    @Test
    void whenRetrieveAllEvents_thenThrowModelMappingException() {
        // Given
        Page<Event> events = new PageImpl<>(List.of(testEvent));
        when(eventRepository.findAll(any(Pageable.class))).thenReturn(events);
        when(modelMapper.map(testEvent, EventDto.class)).thenThrow(ModelMappingException.class);

        // When - Then
        assertThrows(ModelMappingException.class, () -> {
            eventService.retrieveAllEvents(Pageable.ofSize(10));
        });
    }

    @DisplayName("Retrieve All Event Mappings: Success")
    @Test
    void whenRetrieveAllEventMappings_thenReturnListOfEventMappingsDtos() {
        // Given
        Page<EventMappings> page = new PageImpl<>(List.of(testEventMapping));
        when(eventMappingsRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(testEventMapping, EventMappingsDto.class)).thenReturn(testEventMappingDto);

        // When
        List<EventMappingsDto> result = eventService.retrieveAllEventMappings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.getFirst().getId());
    }

    @DisplayName("Retrieve User Roles per Event: Success")
    @Test
    void givenValidEventDetails_whenRetrieveUserRolesPerEvent_thenReturnUserRoleList() {
        // Given
        when(eventMappingsRepository.findByTopic(any()))
                .thenReturn(Optional.of(testEventMapping));

        // When
        List<String> result = eventService.retrieveUserRolesPerTopic("Test Topic");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("OPERATOR", result.getFirst());
    }

    @DisplayName("Retrieve User Roles per Event: No Mappings Found")
    @Test
    void givenInvalidEventDetails_whenRetrieveUserRolesPerEvent_thenReturnEmptyList() {
        // Given
        when(eventMappingsRepository.findByTopic(any()))
                .thenReturn(Optional.empty());

        // When
        List<String> result = eventService.retrieveUserRolesPerTopic("Mock Topic");

        // Then
        assertTrue(result.isEmpty());
    }

    @DisplayName("Delete Event Mapping: Success")
    @Test
    void givenExistingEventMapping_whenDeleteEventMapping_thenDeleteFromDB() {
        // Given
        EventMappings mapping = new EventMappings();
        mapping.setId("1");

        // When
        when(eventMappingsRepository.findById("1")).thenReturn(Optional.of(mapping));

        eventService.deleteEventMappingById("1");

        // Then
        verify(eventMappingsRepository, times(1)).deleteById(anyString());
    }

    @DisplayName("Delete Event Mapping: Not Found")
    @Test
    void givenNonExistingEventMapping_whenDeleteEventMapping_thenThrowException() {
        // When
        when(eventMappingsRepository.findById(anyString())).thenReturn(Optional.empty());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            eventService.deleteEventMappingById("invalid");
        });

        // Then
        assertEquals("Event Mapping with id: invalid not found in DB", exception.getMessage());
    }

    @DisplayName("Update Event Mapping: Success")
    @Test
    void givenExistingEventMappingAndNewData_whenUpdateEventMapping_thenUpdateEventMapping() {
        // Given
        EventMappings mapping = new EventMappings();
        mapping.setId("1");

        EventMappingsDto newMapping = new EventMappingsDto();
        newMapping.setId("1");
        newMapping.setTopic("Test Topic");
        newMapping.setUserRoles(List.of("ALL"));

        // When
        when(eventMappingsRepository.findById("1")).thenReturn(Optional.of(mapping));

        eventService.updateEventMappingById(newMapping);

        // Then
        verify(eventMappingsRepository, times(1)).save(any(EventMappings.class));
    }

    @DisplayName("Update Event Mapping: Not Found")
    @Test
    void givenNonExistingEventMapping_whenUpdateEventMapping_thenThrowException() {
        EventMappingsDto newMapping = new EventMappingsDto();
        newMapping.setId("InvalidID");
        newMapping.setTopic("Test Topic");
        newMapping.setUserRoles(List.of("ALL"));

        // When
        when(eventMappingsRepository.findById(anyString())).thenReturn(Optional.empty());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            eventService.updateEventMappingById(newMapping);
        });

        // Then
        assertEquals("Event Mapping with id: InvalidID not found in DB", exception.getMessage());
    }
}
