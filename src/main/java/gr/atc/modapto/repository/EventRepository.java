package gr.atc.modapto.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import gr.atc.modapto.model.Event;

@Repository
public interface EventRepository extends ElasticsearchRepository<Event, String> {
}
