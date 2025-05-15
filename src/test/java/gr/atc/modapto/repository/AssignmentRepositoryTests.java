package gr.atc.modapto.repository;

import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.config.SetupTestContainersEnvironment;
import gr.atc.modapto.model.Assignment;
import gr.atc.modapto.model.AssignmentComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class AssignmentRepositoryTests extends SetupTestContainersEnvironment {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "assignments";
    private static final String SOURCE_USER_ID_1 = "user1";
    private static final String SOURCE_USER_ID_2 = "user2";
    private static final String TARGET_USER_ID_1 = "target1";
    private static final String TARGET_USER_ID_2 = "target2";
    private static final String STATUS_OPEN = "Open";

    @BeforeEach
    void setup() {
        // Clear the index before each test
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).create();

        // Insert test data
        List<Assignment> assignments = createTestAssignments();
        insertAssignments(assignments);

        // Refresh the index to make sure all data is searchable
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
    }

    private List<Assignment> createTestAssignments() {
        List<Assignment> assignments = new ArrayList<>();

        // Assignments for sourceUser1
        assignments.add(createAssignment(SOURCE_USER_ID_1, TARGET_USER_ID_1, AssignmentStatus.OPEN.toString()));
        assignments.add(createAssignment(SOURCE_USER_ID_1, TARGET_USER_ID_2, AssignmentStatus.OPEN.toString()));
        assignments.add(createAssignment(SOURCE_USER_ID_1, TARGET_USER_ID_1, AssignmentStatus.DONE.toString()));

        // Assignments for sourceUser2
        assignments.add(createAssignment(SOURCE_USER_ID_2, TARGET_USER_ID_1, AssignmentStatus.OPEN.toString()));
        assignments.add(createAssignment(SOURCE_USER_ID_2, TARGET_USER_ID_2, AssignmentStatus.DONE.toString()));

        return assignments;
    }

    private Assignment createAssignment(String sourceUserId, String targetUserId, String status) {
        AssignmentComment comment = new AssignmentComment();
        comment.setComment("Test Comment");

        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID().toString());
        assignment.setSourceUserId(sourceUserId);
        assignment.setTargetUserId(targetUserId);
        assignment.setStatus(status);
        assignment.setComments(List.of(comment));
        assignment.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        assignment.setTimestampUpdated(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        assignment.setDescription("Test Assignment");
        assignment.setDescription("Test Description");
        return assignment;
    }

    private void insertAssignments(List<Assignment> assignments) {
        for (Assignment assignment : assignments) {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(assignment.getId())
                    .withObject(assignment)
                    .build();
            elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
        }
    }

    @DisplayName("Find assignments by source user ID")
    @Test
    void givenPaginationAndSourceUserId_whenFindBySourceUserId_thenReturnAssignment() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Assignment> result = assignmentRepository.findBySourceUserId(SOURCE_USER_ID_1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        result.getContent().forEach(assignment ->
                assertEquals(SOURCE_USER_ID_1, assignment.getSourceUserId())
        );
    }

    @DisplayName("Find assignments by source user ID and status")
    @Test
    void givenPaginationAndSourceUserIdAndStatus_whenFindBySourceUserIdAndStatus_thenReturnAssignment() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Assignment> result = assignmentRepository.findBySourceUserIdAndStatus(SOURCE_USER_ID_1, STATUS_OPEN, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(assignment -> {
            assertEquals(SOURCE_USER_ID_1, assignment.getSourceUserId());
        });
    }

    @DisplayName("Find assignments by target user ID")
    @Test
    void givenPaginationAndTargetUserId_whenFindByTargetUserId_thenReturnAssignment() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Assignment> result = assignmentRepository.findByTargetUserId(TARGET_USER_ID_1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        result.getContent().forEach(assignment ->
                assertEquals(TARGET_USER_ID_1, assignment.getTargetUserId())
        );
    }

    @DisplayName("Find assignments by target user ID and status")
    @Test
    void givenPaginationAndTargetUserIdAndStatus_whenFindByTargetUserIdAndStatus_thenReturnAssignment() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Assignment> result = assignmentRepository.findByTargetUserIdAndStatus(TARGET_USER_ID_1, STATUS_OPEN, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(assignment -> {
            assertEquals(TARGET_USER_ID_1, assignment.getTargetUserId());
        });
    }

    @DisplayName("Find assignments by source user ID or target user ID")
    @Test
    void givenPaginationAndSourceAndTargetID_whenFindBySourceUserIdOrTargetUserId_thenReturnAssignment() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Assignment> result = assignmentRepository.findBySourceUserIdOrTargetUserId(SOURCE_USER_ID_1, TARGET_USER_ID_2, pageable);

        // Then
        assertNotNull(result);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(4);
        result.getContent().forEach(assignment ->
                assertThat(assignment.getSourceUserId().equals(SOURCE_USER_ID_1) ||
                        assignment.getTargetUserId().equals(TARGET_USER_ID_2)).isTrue()
        );
    }

    @DisplayName("Find assignments by status and (source user ID or target user ID)")
    @Test
    void givenPaginationAndStatusAndSourceAndTargetID_whenFindByStatusAndSourceUserIdOrTargetUserId_thenReturnAssignment() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Assignment> result = assignmentRepository.findByStatusAndSourceUserIdOrTargetUserId(
                STATUS_OPEN, SOURCE_USER_ID_1, TARGET_USER_ID_1, pageable);

        // Then
        assertNotNull(result);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
        result.getContent().forEach(assignment -> {
            assertThat(assignment.getSourceUserId().equals(SOURCE_USER_ID_1) ||
                    assignment.getTargetUserId().equals(TARGET_USER_ID_1)).isTrue();
        });
    }
}