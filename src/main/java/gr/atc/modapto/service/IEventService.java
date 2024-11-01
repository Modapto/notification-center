package gr.atc.modapto.service;

import gr.atc.modapto.dto.EventDto;
import gr.atc.modapto.dto.EventMappingsDto;
import gr.atc.modapto.enums.UserRole;

import java.util.List;

public interface IEventService {

    String storeIncomingEvent(EventDto event);

    String storeEventMapping(EventMappingsDto eventMapping);

    EventDto retrieveEventById(String eventId);

    List<EventDto> retrieveAllEvents();

    List<EventMappingsDto> retrieveAllEventMappings();

    List<UserRole> retrieveUserRolesPerEventType(String eventType, String productionModule, String smartService);
}
