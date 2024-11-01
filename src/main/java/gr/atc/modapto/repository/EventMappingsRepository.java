package gr.atc.modapto.repository;

import gr.atc.modapto.model.EventMappings;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface EventMappingsRepository extends ElasticsearchRepository<EventMappings, String> {
    Optional<EventMappings> findByEventTypeAndProductionModuleAndSmartService(String eventType, String productionModule, String smartService);
}
