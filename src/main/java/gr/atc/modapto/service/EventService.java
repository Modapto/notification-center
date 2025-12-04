package gr.atc.modapto.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import gr.atc.modapto.service.interfaces.IEventService;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Event;
import gr.atc.modapto.model.EventMappings;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EventService class to manage transactions with MongoDB about Event collection
 *
*/
@Service
@AllArgsConstructor
@Slf4j
public class EventService implements IEventService {

    private final ModelMapper modelMapper;

    private final EventRepository eventRepository;

    private final EventMappingsRepository eventMappingsRepository;

    private static final String EVENT_MAPPER_ERROR  = "Error mapping Event to Dto - Error: ";
    private static final String EVENT_MAPPING_MAPPER_ERROR  = "Error mapping Event Mapping to Dto - Error: ";

    /**
     * Create a new event in DB when received from Kafka
     *
     * @param eventDto: DTO of Event
     * @return String: ID of the created Event
     */
    @Override
    public String storeIncomingEvent(EventDto eventDto) {
        try {
            Event event = modelMapper.map(eventDto, Event.class);

            return eventRepository.save(event).getId();
        } catch (MappingException e){
            throw new ModelMappingException(EVENT_MAPPER_ERROR + e.getMessage());
        }
    }

    /**
     * Create a new event mapping in DB
     *
     * @param eventMapping: Event Mapping with Event and User Roles
     * @return String: ID of the created Event Mapping
     */
    @Override
    public String storeEventMapping(EventMappingsDto eventMapping) {
        try {
            EventMappings eventMappings = modelMapper.map(eventMapping, EventMappings.class);
            return eventMappingsRepository.save(eventMappings).getId();
        } catch (MappingException e){
            throw new ModelMappingException(EVENT_MAPPING_MAPPER_ERROR + e.getMessage());
        }
    }

    /**
     * Search an Event in DB by ID
     *
     * @param eventId : ID of Event
     * @return EventDto: EventDTO if exists, otherwise null
     */
    @Override
    public EventDto retrieveEventById(String eventId) {
        try {
            Optional<Event> optionalEvent = eventRepository.findById(eventId);
            if (optionalEvent.isEmpty())
                throw new DataNotFoundException("Event with id: " + eventId + " not found in DB");
            return modelMapper.map(optionalEvent.get(), EventDto.class);
        } catch (MappingException e) {
            throw new ModelMappingException(EVENT_MAPPER_ERROR + e.getMessage());
        }
    }

    /**
     * Fetch all the available Events from DB
     *
     * @param pageable : Pagination Options
     * @return List<EventDto>: list of Events
     */
    @Override
    public Page<EventDto> retrieveAllEvents(Pageable pageable) {
        try {
            Page<Event> eventPage = eventRepository.findAll(pageable);
            return eventPage.map(event -> modelMapper.map(event, EventDto.class));
        } catch (MappingException e) {
            throw new ModelMappingException("Error mapping Events to Dto - Error: " + e.getMessage());
        }
    }

    /**
     * Fetch all the available Event Mappings from DB
     *
     * @return List<EventMappingsDto>: list of Event Mappings
     */
    @Override
    public List<EventMappingsDto> retrieveAllEventMappings() {
        try {
            Page<EventMappings> eventMappingsPage = eventMappingsRepository.findAll(Pageable.unpaged());
            List<EventMappings> eventMappings = eventMappingsPage.getContent();
            return eventMappings.stream().map(eventMapping -> modelMapper.map(eventMapping, EventMappingsDto.class)).toList();
        } catch (MappingException e) {
            throw new ModelMappingException("Error mapping Event Mappings to Dto - Error: " + e.getMessage());
        }
    }

    public void deleteAll(){
        eventRepository.deleteAll();
    }

    /**
     * Fetch correlated User Roles for a specific Event from DB by Topic Name
     *
     * @param topic: Topic Name
     * @return List<UserRole>: list of UserRoles
     */
    @Override
    public List<String> retrieveUserRolesPerTopic(String topic) {
        Optional<EventMappings> existingEventMappings = eventMappingsRepository.findByTopic(topic);
        if (existingEventMappings.isPresent()) {
            return existingEventMappings.get().getUserRoles();
        }
        return Collections.emptyList();
    }

    /**
     * Delete an Event Mapping in DB if exists
     * 
     * @param mappingId : Event Mapping ID
     */
    @Override
    public void deleteEventMappingById(String mappingId) {
        // Try to locate if event mapping exists
        Optional<EventMappings> existingEventMapping = eventMappingsRepository.findById(mappingId);
        if (existingEventMapping.isEmpty())
            throw new DataNotFoundException("Event Mapping with id: " + mappingId + " not found in DB");

        // Delete the event mapping
        eventMappingsRepository.deleteById(mappingId);
    }

    /**
     * Update an Event Mapping in DB if exists
     *
     * @param eventMapping : Event Mapping
     */
    @Override
    public void updateEventMappingById(EventMappingsDto eventMapping) {
        // Try to locate if event mapping exists
        Optional<EventMappings> existingEventMapping = eventMappingsRepository.findById(eventMapping.getId());
        if (existingEventMapping.isEmpty())
            throw new DataNotFoundException("Event Mapping with id: " + eventMapping.getId() + " not found in DB");

        // Update User Roles
        EventMappings updatedEventMapping = updateUserRolesOfEventMapping(existingEventMapping.get(), eventMapping);

        // Delete the event mapping
        eventMappingsRepository.save(updatedEventMapping);
    }

    private EventMappings updateUserRolesOfEventMapping(EventMappings existingEventMapping, EventMappingsDto eventMapping) {
        // Check whether "ALL" is included inside updated eventMapping and keep only this selection
        if (eventMapping.getUserRoles().contains("ALL"))
            existingEventMapping.setUserRoles(List.of("ALL"));
        // Otherwise update the UserRoles
        else
            existingEventMapping.setUserRoles(eventMapping.getUserRoles());

        return existingEventMapping;
    }

}
