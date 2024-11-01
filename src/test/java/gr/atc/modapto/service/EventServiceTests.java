package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gr.atc.modapto.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.UserRole;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Event;
import gr.atc.modapto.model.EventMappings;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
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
                .timestamp(LocalDateTime.now())
                .description(null)
                .priority(MessagePriority.HIGH)
                .build();
        
        testEvent = new Event();
        testEvent.setId("1");
        testEvent.setEventType("Test");
        testEvent.setSmartService("Test Smart Service");
        testEvent.setProductionModule("Test Production Module");
        testEvent.setEventType(null);
        testEvent.setTimestamp(LocalDateTime.now());
        testEvent.setDescription(null);


        testEventMappingDto = EventMappingsDto.builder()
                .id("1")
                .smartService("Test SSI")
                .productionModule("Test Production Module")
                .eventType("Test Event")
                .userRoles(List.of(UserRole.OPERATOR))
                .build();
        
        testEventMapping = EventMappings.builder()
                .id("1")
                .smartService("Test SSI")
                .productionModule("Test Production Module")
                .eventType("Test Event")
                .userRoles(List.of(UserRole.OPERATOR))
                .build();

        // Clear mock interactions
        reset(eventRepository, eventMappingsRepository, modelMapper);
    }

    @DisplayName("Store Incoming Event: Success")
    @Test
    void givenValidtestEventDto_whenStoreIncomingEvent_thenReturnEventId() {
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
    void givenInvalidtestEventDto_whenStoreIncomingEvent_thenThrowModelMappingException() {
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
        List<EventDto> result = eventService.retrieveAllEvents();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
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
            eventService.retrieveAllEvents();
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
        assertEquals("1", result.get(0).getId());
    }

    @DisplayName("Retrieve User Roles per Event: Success")
    @Test
    void givenValidEventDetails_whenRetrieveUserRolesPerEvent_thenReturnUserRoleList() {
        // Given
        when(eventMappingsRepository.findByEventTypeAndProductionModuleAndSmartService(any(), any(), any()))
                .thenReturn(Optional.of(testEventMapping));

        // When
        List<UserRole> result = eventService.retrieveUserRolesPerEventType("type", "module", "service");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(UserRole.OPERATOR, result.get(0));
    }

    @DisplayName("Retrieve User Roles per Event: No Mappings Found")
    @Test
    void givenInvalidEventDetails_whenRetrieveUserRolesPerEvent_thenReturnEmptyList() {
        // Given
        when(eventMappingsRepository.findByEventTypeAndProductionModuleAndSmartService(any(), any(), any()))
                .thenReturn(Optional.empty());

        // When
        List<UserRole> result = eventService.retrieveUserRolesPerEventType("type", "module", "service");

        // Then
        assertTrue(result.isEmpty());
    }
}
