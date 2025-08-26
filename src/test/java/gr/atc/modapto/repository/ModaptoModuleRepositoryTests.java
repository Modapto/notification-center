package gr.atc.modapto.repository;

import gr.atc.modapto.config.SetupTestContainersEnvironment;
import gr.atc.modapto.model.ModaptoModule;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class ModaptoModuleRepositoryTests extends SetupTestContainersEnvironment {

    @Autowired
    private ModaptoModuleRepository modaptoModuleRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "modapto-modules";
    private static final String MODULE_ID_1 = "MODULE-001";
    private static final String MODULE_ID_2 = "MODULE-002";

    @BeforeEach
    void setup() {
        // Clear the index before each test
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).create();

        // Insert test data
        List<ModaptoModule> modulesList = createTestModules();
        insertModules(modulesList);

        // Refresh the index to make sure all data is searchable
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
    }

    private List<ModaptoModule> createTestModules() {
        ModaptoModule.SmartService smartService1 = new ModaptoModule.SmartService(
                "SelfAwareness",
                "catalogue-123", 
                "service-456", 
                "http://self-awareness.com"
        );

        ModaptoModule.SmartService smartService2 = new ModaptoModule.SmartService(
                "PredictiveMaintenance", 
                "catalogue-789", 
                "service-012", 
                "http://maintenance-service.com"
        );

        ModaptoModule module1 = createModule(
                MODULE_ID_1, 
                "Manufacturing Module", 
                "http://manufacturing.com", 
                List.of(smartService1)
        );

        ModaptoModule module2 = createModule(
                MODULE_ID_2, 
                "Quality Control Module", 
                "http://quality-control.com", 
                List.of(smartService2)
        );

        return Arrays.asList(module1, module2);
    }

    private ModaptoModule createModule(String moduleId, String name, String endpoint, List<ModaptoModule.SmartService> smartServices) {
        ModaptoModule module = new ModaptoModule();
        module.setId(UUID.randomUUID().toString());
        module.setModuleId(moduleId);
        module.setName(name);
        module.setEndpoint(endpoint);
        module.setSmartServices(smartServices);
        module.setTimestampDt(LocalDateTime.now());
        module.setTimestampElastic(Instant.now());
        return module;
    }

    private void insertModules(List<ModaptoModule> modulesList) {
        for (ModaptoModule module : modulesList) {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(module.getId())
                    .withObject(module)
                    .build();
            elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
        }
    }

    @DisplayName("Find module by module ID")
    @Test
    void givenModuleId_whenFindByModuleId_thenReturnModule() {
        // Given
        String moduleId = MODULE_ID_1;

        // When
        Optional<ModaptoModule> result = modaptoModuleRepository.findByModuleId(moduleId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(moduleId, result.get().getModuleId());
        assertEquals("Manufacturing Module", result.get().getName());
        assertNotNull(result.get().getEndpoint());
        assertNotNull(result.get().getSmartServices());
        assertFalse(result.get().getSmartServices().isEmpty());
    }

    @DisplayName("Find module by non-existent module ID")
    @Test
    void givenNonExistentModuleId_whenFindByModuleId_thenReturnEmpty() {
        // Given
        String nonExistentModuleId = "NON-EXISTENT-MODULE";

        // When
        Optional<ModaptoModule> result = modaptoModuleRepository.findByModuleId(nonExistentModuleId);

        // Then
        assertFalse(result.isPresent());
    }

    @DisplayName("Save and retrieve module")
    @Test
    void givenNewModule_whenSave_thenShouldBeRetrievable() {
        // Given
        String newModuleId = "MODULE-NEW";
        ModaptoModule.SmartService newSmartService = new ModaptoModule.SmartService(
                "NewService", 
                "new-catalogue", 
                "new-service", 
                "http://new-service.com"
        );
        ModaptoModule newModule = createModule(
                newModuleId, 
                "New Test Module", 
                "http://new-module.com", 
                List.of(newSmartService)
        );

        // When
        ModaptoModule savedModule = modaptoModuleRepository.save(newModule);
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
        Optional<ModaptoModule> retrievedModule = modaptoModuleRepository.findByModuleId(newModuleId);

        // Then
        assertTrue(retrievedModule.isPresent());
        assertEquals(savedModule.getId(), retrievedModule.get().getId());
        assertEquals(newModuleId, retrievedModule.get().getModuleId());
        assertEquals("New Test Module", retrievedModule.get().getName());
        assertEquals("http://new-module.com", retrievedModule.get().getEndpoint());
    }
}