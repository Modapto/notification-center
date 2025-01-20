package gr.atc.modapto.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.UserRole;

public interface IEventService {

    String storeIncomingEvent(EventDto event);

    String storeEventMapping(EventMappingsDto eventMapping);

    EventDto retrieveEventById(String eventId);

    Page<EventDto> retrieveAllEvents(Pageable pageable);

    List<EventMappingsDto> retrieveAllEventMappings();

    List<UserRole> retrieveUserRolesPerEventType(String eventType, String productionModule, String smartService);

    boolean deleteEventMappingById(String mappingId);
}
