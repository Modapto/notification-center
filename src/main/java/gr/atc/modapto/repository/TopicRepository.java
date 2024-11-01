package gr.atc.modapto.repository;

import gr.atc.modapto.model.MessageTopic;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TopicRepository extends ElasticsearchRepository<MessageTopic, String> {
}
