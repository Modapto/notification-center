package gr.atc.modapto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import gr.atc.modapto.model.ModaptoModule;
import gr.atc.modapto.repository.ModaptoModuleRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
class ModaptoModuleServiceTests {

    @Mock
    private ModaptoModuleRepository modaptoModuleRepository;

    @InjectMocks
    private ModaptoModuleService modaptoModuleService;

    private ModaptoModule testModule;
    private ModaptoModule.SmartService testSmartService;

    @BeforeEach
    void setup() {
        testSmartService = new ModaptoModule.SmartService(
                "TestService",
                "catalogue-123",
                "service-456",
                "http://test-endpoint.com"
        );

        testModule = new ModaptoModule(
                "module-id-123",
                "MODULE-001",
                "Test Module",
                "http://module-endpoint.com",
                List.of(testSmartService),
                LocalDateTime.now(),
                Instant.now()
        );

        // Clear mock interactions
        reset(modaptoModuleRepository);
    }

    @DisplayName("Retrieve Modapto Module Name: Success")
    @Test
    void givenValidModuleId_whenRetrieveModaptoModuleName_thenReturnModuleName() {
        // Given
        when(modaptoModuleRepository.findByModuleId("MODULE-001"))
                .thenReturn(Optional.of(testModule));

        // When
        String result = modaptoModuleService.retrieveModaptoModuleName("MODULE-001");

        // Then
        assertEquals("Test Module", result);
    }

    @DisplayName("Retrieve Modapto Module Name: Module Not Found")
    @Test
    void givenInvalidModuleId_whenRetrieveModaptoModuleName_thenReturnNull() {
        // Given
        when(modaptoModuleRepository.findByModuleId("INVALID-MODULE"))
                .thenReturn(Optional.empty());

        // When
        String result = modaptoModuleService.retrieveModaptoModuleName("INVALID-MODULE");

        // Then
        assertNull(result);
    }

    @DisplayName("Retrieve Modapto Module Name: Null Module ID")
    @Test
    void givenNullModuleId_whenRetrieveModaptoModuleName_thenReturnNull() {
        // Given
        when(modaptoModuleRepository.findByModuleId(null))
                .thenReturn(Optional.empty());

        // When
        String result = modaptoModuleService.retrieveModaptoModuleName(null);

        // Then
        assertNull(result);
    }
}