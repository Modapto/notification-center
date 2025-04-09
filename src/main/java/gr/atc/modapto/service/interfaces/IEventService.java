package gr.atc.modapto.service.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;

public interface IEventService {

    String storeIncomingEvent(EventDto event);

    String storeEventMapping(EventMappingsDto eventMapping);

    EventDto retrieveEventById(String eventId);

    Page<EventDto> retrieveAllEvents(Pageable pageable);

    List<EventMappingsDto> retrieveAllEventMappings();

    List<String> retrieveUserRolesPerTopic(String topic);

    void deleteEventMappingById(String mappingId);

    void updateEventMappingById(EventMappingsDto eventMapping);
}
