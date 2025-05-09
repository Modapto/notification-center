package gr.atc.modapto.service.interfaces;

import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import gr.atc.modapto.dto.AssignmentCommentDto;
import gr.atc.modapto.dto.AssignmentDto;

public interface IAssignmentService {
    Page<AssignmentDto> retrieveAllAssignments(Pageable pageable);

    Page<AssignmentDto> retrieveAssignmentsPerUserId(String userId, String assignmentType, Pageable pageable);

    Page<AssignmentDto> retrieveAssignmentsPerUserIdAndStatus(String userId, String assignmentType, String status, Pageable pageable);

    AssignmentDto retrieveAssignmentById(String assignmentId);

    void updateAssignment(AssignmentDto assignmentDto, String userId);

    String storeAssignment(AssignmentDto assignmentDto);

    void updateAssignmentComments(String assignmentId, AssignmentCommentDto assignmentComment, String userId);

    boolean deleteAssignmentById(String assignmentId);
}
