package gr.atc.modapto.repository;

import gr.atc.modapto.config.SetupTestContainersEnvironment;
import gr.atc.modapto.model.EventMappings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class EventMappingsRepositoryTests extends SetupTestContainersEnvironment {

    @Autowired
    private EventMappingsRepository eventMappingsRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "event_mappings";
    private static final String TOPIC_1 = "modapto-module-created";
    private static final String TOPIC_2 = "smart-service-invoked";
    private static final String TOPIC_3 = "predictive-maintenance";

    @BeforeEach
    void setup() {
        // Clear the index before each test
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).create();

        // Insert test data
        List<EventMappings> eventMappingsList = createTestEventMappings();
        insertEventMappings(eventMappingsList);

        // Refresh the index to make sure all data is searchable
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
    }

    private List<EventMappings> createTestEventMappings() {
        EventMappings mapping1 = createEventMapping(TOPIC_1, "modapto-module-created", List.of("OPERATOR"));
        EventMappings mapping2 = createEventMapping(TOPIC_2, "smart-service-invoked", List.of("ALL"));
        EventMappings mapping3 = createEventMapping(TOPIC_3, "predictive-maintenance", List.of("PLANT_MANAGER"));

        return Arrays.asList(mapping1, mapping2, mapping3);
    }

    private EventMappings createEventMapping(String topic, String description, List<String> userRoles) {
        EventMappings eventMappings = new EventMappings();
        eventMappings.setId(UUID.randomUUID().toString());
        eventMappings.setTopic(topic);
        eventMappings.setDescription(description);
        eventMappings.setUserRoles(userRoles);
        return eventMappings;
    }

    private void insertEventMappings(List<EventMappings> eventMappingsList) {
        for (EventMappings eventMappings : eventMappingsList) {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(eventMappings.getId())
                    .withObject(eventMappings)
                    .build();
            elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
        }
    }

    @DisplayName("Find event mapping by topic")
    @Test
    void givenTopic_whenFindByTopic_thenReturnEventMapping() {
        // Given
        String topic = TOPIC_1;

        // When
        Optional<EventMappings> result = eventMappingsRepository.findByTopic(topic);

        // Then
        assertTrue(result.isPresent());
        assertEquals(topic, result.get().getTopic());
        assertNotNull(result.get().getTopic());
        assertNotNull(result.get().getDescription());
    }

    @DisplayName("Find event mapping by non-existent topic")
    @Test
    void givenNonExistentTopic_whenFindByTopic_thenReturnEmpty() {
        // Given
        String nonExistentTopic = "non.existent.topic";

        // When
        Optional<EventMappings> result = eventMappingsRepository.findByTopic(nonExistentTopic);

        // Then
        assertFalse(result.isPresent());
    }

    @DisplayName("Save and retrieve event mapping")
    @Test
    void givenNewEventMapping_whenSave_thenShouldBeRetrievable() {
        // Given
        String newTopic = "new.event.topic";
        EventMappings newMapping = createEventMapping(newTopic, "New Event", List.of("ALL"));

        // When
        EventMappings savedMapping = eventMappingsRepository.save(newMapping);
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
        Optional<EventMappings> retrievedMapping = eventMappingsRepository.findByTopic(newTopic);

        // Then
        assertTrue(retrievedMapping.isPresent());
        assertEquals(savedMapping.getId(), retrievedMapping.get().getId());
        assertEquals(newTopic, retrievedMapping.get().getTopic());
        assertEquals("New Event", retrievedMapping.get().getDescription());
    }
}