package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gr.atc.modapto.model.AssignmentComment;
import gr.atc.modapto.service.interfaces.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gr.atc.modapto.dto.AssignmentCommentDto;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.enums.AssignmentType;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.model.Assignment;
import gr.atc.modapto.repository.AssignmentRepository;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
class AssignmentServiceTests {

    @Mock
    private INotificationService notificationService;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AssignmentService assignmentService;

    private AssignmentDto assignmentDto;
    private Assignment assignment;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        AssignmentCommentDto assignmentCommentDto = AssignmentCommentDto.builder().comment("System Comment").build();

        AssignmentCommentDto oldAssignmentCommentDto = AssignmentCommentDto.builder().comment("Old System Comment").datetime(LocalDateTime.now().minusDays(1)).build();

        AssignmentComment assignmentComment = AssignmentComment.convertToAssignmentComment(assignmentCommentDto);
        AssignmentComment oldAssignmentComment = AssignmentComment.convertToAssignmentComment(oldAssignmentCommentDto);

        List<AssignmentCommentDto> commentsDto = new ArrayList<>();
        commentsDto.add(assignmentCommentDto);
        commentsDto.add(oldAssignmentCommentDto);

        List<AssignmentComment> comments = new ArrayList<>();
        comments.add(oldAssignmentComment);
        comments.add(assignmentComment);

        assignmentDto = new AssignmentDto();
        assignmentDto.setId("1");
        assignmentDto.setDescription("Test Assignment");
        assignmentDto.setProductionModule("TestModule");
        assignmentDto.setTargetUserId("testUser");
        assignmentDto.setComments(commentsDto);
        assignmentDto.setPriority(MessagePriority.HIGH.toString());
        assignmentDto.setStatus(AssignmentStatus.OPEN.toString());

        assignment = new Assignment();
        assignment.setId("1");
        assignment.setDescription("Test Assignment");
        assignment.setProductionModule("TestModule");
        assignment.setComments(comments);
        assignment.setTargetUserId("testUser");
        assignment.setStatus(AssignmentStatus.OPEN.toString());
        assignmentDto.setPriority(MessagePriority.HIGH.toString());
        assignment.setTimestamp(LocalDateTime.now());
    }

    @DisplayName("Retrieve All Assignments: Success")
    @Test
    void whenRetrieveAllAssignments_thenReturnListOfAssignmentDtos() {
        // Given
        Page<Assignment> assignments = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findAll(any(Pageable.class))).thenReturn(assignments);
        when(modelMapper.map(any(Assignment.class), eq(AssignmentDto.class))).thenReturn(assignmentDto);

        Page<AssignmentDto> result = assignmentService.retrieveAllAssignments(Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getSize());
        assertEquals("1", result.getContent().getFirst().getId());
    }

    @DisplayName("Retrieve Assignments Per User ID: Success")
    @Test
    void whenRetrieveAssignmentsPerUserId_thenReturnAssignments() {
        // Given
        Page<Assignment> assignments = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findBySourceUserIdOrTargetUserId(anyString(), anyString(), any(Pageable.class))).thenReturn(assignments);
        when(modelMapper.map(any(Assignment.class), eq(AssignmentDto.class))).thenReturn(assignmentDto);

        // When
        Page<AssignmentDto> result = assignmentService.retrieveAssignmentsPerUserId("testUser", null, Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSize());
        assertEquals("1", result.getContent().getFirst().getId());
    }

    @DisplayName("Retrieve Assignments Per User ID and Status: Success")
    @Test
    void whenRetrieveAssignmentsPerUserIdAndStatus_thenReturnAssignments() {
        // Given
        Page<Assignment> assignments = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findByTargetUserIdAndStatus(anyString(), anyString(), any(Pageable.class))).thenReturn(assignments);
        when(modelMapper.map(any(Assignment.class), eq(AssignmentDto.class))).thenReturn(assignmentDto);

        // When
        Page<AssignmentDto> result = assignmentService.retrieveAssignmentsPerUserIdAndStatus("testUser", AssignmentType.RECEIVED.toString(), AssignmentStatus.OPEN.toString(), Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSize());
        assertEquals("1", result.getContent().getFirst().getId());
    }

    @DisplayName("Retrieve Assignment by ID: Success")
    @Test
    void givenValidAssignmentId_whenRetrieveAssignmentById_thenReturnAssignmentDto() {
        // Given
        when(assignmentRepository.findById("1")).thenReturn(Optional.of(assignment));
        when(modelMapper.map(any(Assignment.class), eq(AssignmentDto.class))).thenReturn(assignmentDto);

        // When
        AssignmentDto result = assignmentService.retrieveAssignmentById("1");

        // Then
        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("System Comment", result.getComments().getFirst().getComment());
        assertEquals("Old System Comment", result.getComments().getLast().getComment());
    }

    @DisplayName("Retrieve Assignment by ID: Not Found")
    @Test
    void givenInvalidAssignmentId_whenRetrieveAssignmentById_thenThrowDataNotFoundException() {
        // Given
        when(assignmentRepository.findById("invalid")).thenReturn(Optional.empty());

        // When
        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            assignmentService.retrieveAssignmentById("invalid");
        });

        // Then
        assertEquals("Assignment with id: invalid not found in DB", exception.getMessage());
    }

    @DisplayName("Store Assignment: Success")
    @Test
    void givenValidAssignmentDto_whenStoreAssignment_thenReturnAssignmentId() {
        // Given
        when(modelMapper.map(any(AssignmentDto.class), eq(Assignment.class))).thenReturn(assignment);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        // When
        String result = assignmentService.storeAssignment(assignmentDto);

        // Then
        assertEquals("1", result);
    }

    @DisplayName("Update Assignment: Success")
    @Test
    void givenExistingAssignment_whenUpdateAssignment_thenSaveUpdatedAssignment() {
        // Given
        when(assignmentRepository.findById(anyString())).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        // When
        assignmentService.updateAssignment(assignmentDto);

        // Then
        verify(assignmentRepository, times(1)).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Update Assignment: Not Found")
    void givenNonExistentAssignment_whenUpdateAssignment_thenThrowDataNotFoundException() {
        // When
        when(assignmentRepository.findById(anyString())).thenReturn(Optional.empty());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            assignmentService.updateAssignment(assignmentDto);
        });

        // Then
        assertEquals("Assignment with id: 1 not found in DB", exception.getMessage());
    }

    @DisplayName("Update Assignment Comments: Success")
    @Test
    void givenExistingAssignment_whenUpdateAssignmentComments_thenSaveUpdatedComments() {
        // Given
        AssignmentCommentDto commentDto = AssignmentCommentDto.builder().comment("Test comment").build();

        // When
        when(assignmentRepository.findById(anyString())).thenReturn(Optional.of(assignment));

        assignmentService.updateAssignmentComments("1", commentDto);

        // Then
        verify(assignmentRepository, times(1)).save(any(Assignment.class));;
    }

    @DisplayName("Update Assignment Comments: Assignment Not Found")
    @Test
    void givenNonExistentAssignment_whenUpdateAssignmentComments_thenThrowDataNotFoundException() {
        // Given
        AssignmentCommentDto commentDto = AssignmentCommentDto.builder().comment("Test comment").build();

        // When
        when(assignmentRepository.findById("invalid")).thenReturn(Optional.empty());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            assignmentService.updateAssignmentComments("invalid", commentDto);
        });

        // Then
        assertEquals("Assignment with id: invalid not found in DB", exception.getMessage());
    }

    @DisplayName("Create Notification and Notify User: Success")
    @Test
    void whenCreateNotificationAndNotifyUser_thenNotificationSent() throws JsonProcessingException, ExecutionException, InterruptedException {
        when(notificationService.storeNotification(any(NotificationDto.class))).thenReturn("1");
        doNothing().when(webSocketService).notifyUsersAndRolesViaWebSocket(anyString(), anyString());

        // When
        CompletableFuture<Void> result = assignmentService.createNotificationAndNotifyUser(assignmentDto);

        // Wait for the async operation to complete
        result.get();

        // Then
        assertNotNull(result);
        verify(webSocketService, times(1)).notifyUsersAndRolesViaWebSocket(anyString(), eq("testUser"));
    }

    @DisplayName("Delete Assignment: Success")
    @Test
    void givenExistingAssignment_whenDeleteAssignment_thenDeleteFromDB() {
        // Given
        Assignment assignment = new Assignment();
        assignment.setId("1");

        // When
        when(assignmentRepository.findById("1")).thenReturn(Optional.of(assignment));

        assignmentService.deleteAssignmentById("1");

        // Then
        verify(assignmentRepository, times(1)).deleteById(anyString());
    }

    @DisplayName("Delete Assignment: Not Found")
    @Test
    void givenNonExistingAssignment_whenDeleteAssignment_thenThrowException() {
        // When
        when(assignmentRepository.findById(anyString())).thenReturn(Optional.empty());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> {
            assignmentService.deleteAssignmentById("invalid");
        });

        // Then
        assertEquals("Assignment with id: invalid not found in DB", exception.getMessage());
    }
}
