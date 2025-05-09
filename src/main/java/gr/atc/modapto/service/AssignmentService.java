package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import gr.atc.modapto.exception.CustomExceptions;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import gr.atc.modapto.dto.AssignmentCommentDto;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.enums.AssignmentType;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Assignment;
import gr.atc.modapto.model.AssignmentComment;
import gr.atc.modapto.repository.AssignmentRepository;
import gr.atc.modapto.service.interfaces.IAssignmentService;
import gr.atc.modapto.service.interfaces.INotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class AssignmentService implements IAssignmentService {

    private final INotificationService notificationService;

    private final AssignmentRepository assignmentRepository;

    private final ModelMapper modelMapper;

    private static final String ASSIGNMENTS_MAPPING_ERROR = "Error mapping Assignments to Dto - Error: ";

    private static final String ASSIGNMENT_MAPPING_ERROR = "Error mapping Assignment to Dto - Error: ";

    private static final String USER_NOT_INVOLVED_IN_ASSIGNMENT_ERROR = "User not involved in assignment";

    /**
     * Retrieve all Assignments
     *
     * @param pageable : Pagination parameters
     * @return Page<AssignmentDto> : Requested page of AssignmentDto
     */
    @Override
    public Page<AssignmentDto> retrieveAllAssignments(Pageable pageable) {
        try{
            Page<Assignment> existingAssignments = assignmentRepository.findAll(pageable);
            return existingAssignments.map(assignment -> modelMapper.map(assignment, AssignmentDto.class));
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENTS_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve all Assignments per UserID and Assignment Type (optional)
     *
     * @param userId: Source or Target User ID
     * @param assignmentType: Type of Assignment (Requested or Received or null)
     * @param pageable: Pagination parameters
     * @return Page<AssignmentDto> : Requested page of AssignmentDto
     */
    @Override
    public Page<AssignmentDto> retrieveAssignmentsPerUserId(String userId, String assignmentType, Pageable pageable) {
        try{
            // Retrieve results according to the assignment type (Received, Requested or null)
            if (assignmentType == null)
                // Null assignmentType
                return assignmentRepository.findBySourceUserIdOrTargetUserId(userId, userId, pageable).map(assignment -> modelMapper.map(assignment, AssignmentDto.class));
            else if (assignmentType.equals(AssignmentType.RECEIVED.toString()))
                // Received assignments case
                return assignmentRepository.findByTargetUserId(userId, pageable).map(assignment -> modelMapper.map(assignment, AssignmentDto.class));
            else
                // Requested assignments case
                return assignmentRepository.findBySourceUserId(userId, pageable).map(assignment -> modelMapper.map(assignment, AssignmentDto.class));

        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENTS_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve all Assignments per UserID, Assignment Type (optional) and Assignment Status
     *
     * @param userId : Source or Target User ID
     * @param assignmentType : Type of Assignment (Requested or Received or null)
     * @param status : Assignment Status
     * @param pageable : Pagination parameters
     * @return Page<AssignmentDto> : Requested page of AssignmentDto
     */
    @Override
    public Page<AssignmentDto> retrieveAssignmentsPerUserIdAndStatus(String userId, String assignmentType, String status, Pageable pageable) {
        try{
            // Retrieve results according to the assignment type (Received, Requested or null)
            if (assignmentType == null)
                // Null assignmentType
                return assignmentRepository.findByStatusAndSourceUserIdOrTargetUserId(status, userId, userId, pageable).map(assignment -> modelMapper.map(assignment, AssignmentDto.class));
            else if (assignmentType.equals(AssignmentType.RECEIVED.toString()))
                // Received assignments case
                return assignmentRepository.findByTargetUserIdAndStatus(userId, status, pageable).map(assignment -> modelMapper.map(assignment, AssignmentDto.class));
            else
                // Requested assignments case
                return assignmentRepository.findBySourceUserIdAndStatus(userId, status, pageable).map(assignment -> modelMapper.map(assignment, AssignmentDto.class));

        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENTS_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Retrieve an Assignment by ID
     *
     * @param assignmentId : ID of assignment
     * @return AssignmentDto
     */
    @Override
    public AssignmentDto retrieveAssignmentById(String assignmentId) {
        try{
            Optional<Assignment> existingAssignment = assignmentRepository.findById(assignmentId);
            if (existingAssignment.isEmpty())
                throw new DataNotFoundException("Assignment with id: " + assignmentId + " not found in DB");

            // Sort Comments by Datetime
            List<AssignmentComment> sortedComments = new ArrayList<>(existingAssignment.get().getComments());
            sortedComments.sort(Comparator.comparing(AssignmentComment::getDatetime).reversed());
            existingAssignment.get().setComments(sortedComments);

            return modelMapper.map(existingAssignment.get(), AssignmentDto.class);
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENT_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Update an Assignment in DB and generate a Notification (if applicable)
     *
     * @param assignmentDto : Assignment information to be updated\
     * @param userId : UserID made the request
     */
    @Override
    public void updateAssignment(AssignmentDto assignmentDto, String userId) {
        try{
            // Generate System Comments and Update Description
            AssignmentDto updatedAssignment = generateSystemCommentsAndUpdateDescription(assignmentDto);

            // Try to locate if assignment exists
            Optional<Assignment> existingAssignment = assignmentRepository.findById(updatedAssignment.getId());
            if (existingAssignment.isEmpty())
                throw new DataNotFoundException("Assignment with id: " + updatedAssignment.getId() + " not found in DB");

            // Validate the user is involved in the Assignment
            if (userIsNotInvolvedInAssignment(existingAssignment.get(), userId))
                throw new CustomExceptions.UnauthorizedAssignmentUpdateException(USER_NOT_INVOLVED_IN_ASSIGNMENT_ERROR);

            // Update assignment and save it to repository
            Assignment newAssignment = Assignment.updateExistingAssignment(existingAssignment.get(), updatedAssignment);
            assignmentRepository.save(newAssignment);

            // Create Notification and Notify relevant user asynchronously
            generateNotificationFromAssignment(assignmentDto);
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENT_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Generate system comments for assignment status and priority changes
     *
     * @param assignmentDto: DTO of assignment
     * @return AssignmentDto : Updated assignment with system comments
     */
    private AssignmentDto generateSystemCommentsAndUpdateDescription(AssignmentDto assignmentDto) {
        List<AssignmentCommentDto> comments = assignmentDto.getComments() != null
                ? new ArrayList<>(assignmentDto.getComments())
                : new ArrayList<>();
        String statusDescription = null;

        // Check whether assignment status has been changed
        if (assignmentDto.getStatus() != null){
            statusDescription = "Task status changed to '" + assignmentDto.getStatus() + "'";
            AssignmentCommentDto statusComment = AssignmentCommentDto.builder()
                    .comment("Task status updated to '" + assignmentDto.getStatus() + "'")
                    .build();
            comments.add(statusComment);
        }

        // Check whether assignment priority has been changed
        if (assignmentDto.getPriority() != null) {
            if (statusDescription == null)
                statusDescription = "Task priority change to" + "'" + assignmentDto.getPriority() + "'";
            else
                statusDescription += " and priority changed to '" + assignmentDto.getPriority() + "'";

            AssignmentCommentDto priorityComment = AssignmentCommentDto.builder()
                    .comment("Task priority updated to '" + assignmentDto.getPriority() + "'")
                    .build();
            comments.add(priorityComment);
        }

        // Update description and comments
        assignmentDto.setDescription(statusDescription);
        assignmentDto.setComments(comments);
        return assignmentDto;
    }

    /**
     * Update comments of an Assignment
     *
     * @param assignmentId : ID of assignment
     * @param assignmentComment : Assignment Comment Dto depending on the user who commented
     * @param userId : UserID made the request
     */
    @Override
    public void updateAssignmentComments(String assignmentId, AssignmentCommentDto assignmentComment, String userId) {
        try{
            // Try to locate if assignment exists
            Optional<Assignment> existingAssignment = assignmentRepository.findById(assignmentId);
            if (existingAssignment.isEmpty())
                throw new DataNotFoundException("Assignment with id: " + assignmentId + " not found in DB");

            // Validate the user is involved in the Assignment
            if (userIsNotInvolvedInAssignment(existingAssignment.get(), userId))
                throw new CustomExceptions.UnauthorizedAssignmentUpdateException(USER_NOT_INVOLVED_IN_ASSIGNMENT_ERROR);

            // Update assignment comments
            Assignment updatedAssignment = existingAssignment.get();

            LocalDateTime timeOfLastUpdate = LocalDateTime.now();
            updatedAssignment.getComments().add(AssignmentComment.convertToAssignmentComment(assignmentComment));
            updatedAssignment.setTimestampUpdated(timeOfLastUpdate);
            assignmentRepository.save(updatedAssignment);
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENT_MAPPING_ERROR + e.getMessage());
        }
    }

    /*
     * Helper method to ensure if a user requesting an assignment update is involved in the assignment
     */
    private boolean userIsNotInvolvedInAssignment(Assignment assignment, String userId) {
        return !assignment.getSourceUserId().equals(userId) && !assignment.getTargetUserId().equals(userId);
    }

    /**
     * Create a new Assignment in DB
     *
     * @param assignmentDto : Assignment Information
     * @return String of assignmentId
     */
    @Override
    public String storeAssignment(AssignmentDto assignmentDto) {
        try {
            // Create Assignment and set the initial fields
            Assignment assignment = modelMapper.map(assignmentDto, Assignment.class);
            assignment.setStatus(AssignmentStatus.OPEN.toString());
            assignment.setTimestamp(LocalDateTime.now());
            assignment.setTimestampUpdated(LocalDateTime.now());

            String assignmentId = assignmentRepository.save(assignment).getId();
            if (assignmentId != null)
                // Create Notification and Notify relevant user asynchronously
                generateNotificationFromAssignment(assignmentDto);

            return assignmentId;
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENT_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Helper method to async create a notification from a created or updated assignment
     *
     * @param assignmentDto : Assignment
     */
    private void generateNotificationFromAssignment(AssignmentDto assignmentDto) {
        // Create Notification and Notify relevant user asynchronously
        CompletableFuture<Void> notificationCreationAsync = notificationService.createNotificationAndNotifyUser(assignmentDto);

        // Log the notification creation process
        CompletableFuture.allOf(notificationCreationAsync)
                .thenRun(() -> log.info("Notification creation process completed"))
                .exceptionally(ex -> {
                    log.error("Notification creation process failed", ex);
                    return null;
                });
    }

    /**
     * Delete an Assignment in DB by ID
     * 
     * @param assignmentId : ID of assignment
     * @return True on Success, False on Error
     */
    @Override
    public boolean deleteAssignmentById(String assignmentId) {
        // Try to locate if assignment exists
        Optional<Assignment> existingAssignment = assignmentRepository.findById(assignmentId);
        if (existingAssignment.isEmpty())
            throw new DataNotFoundException("Assignment with id: " + assignmentId + " not found in DB");

        // Delete the assignment
        assignmentRepository.deleteById(assignmentId);
        return true;
    }
}
