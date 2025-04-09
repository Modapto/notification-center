package gr.atc.modapto.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import gr.atc.modapto.service.interfaces.IAssignmentService;
import gr.atc.modapto.service.interfaces.INotificationService;

import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gr.atc.modapto.dto.AssignmentCommentDto;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.enums.AssignmentType;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.exception.CustomExceptions.DataNotFoundException;
import gr.atc.modapto.exception.CustomExceptions.ModelMappingException;
import gr.atc.modapto.model.Assignment;
import gr.atc.modapto.model.AssignmentComment;
import gr.atc.modapto.repository.AssignmentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class AssignmentService implements IAssignmentService {

    private final INotificationService notificationService;

    private final WebSocketService webSocketService;

    private final AssignmentRepository assignmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ModelMapper modelMapper;

    private static final String ASSIGNMENTS_MAPPING_ERROR = "Error mapping Assignments to Dto - Error: ";

    private static final String ASSIGNMENT_MAPPING_ERROR = "Error mapping Assignment to Dto - Error: ";

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
            else if (assignmentType.equals(AssignmentType.Received.toString()))
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
            else if (assignmentType.equals(AssignmentType.Received.toString()))
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

    @Override
    public void updateAssignment(AssignmentDto assignmentDto) {
        try{
            // Try to locate if assignment exists
            Optional<Assignment> existingAssignment = assignmentRepository.findById(assignmentDto.getId());
            if (existingAssignment.isEmpty())
                throw new DataNotFoundException("Assignment with id: " + assignmentDto.getId() + " not found in DB");

            // Update assignment and save it to repository
            Assignment newAssignment = Assignment.updateExistingAssignment(existingAssignment.get(), assignmentDto);
            assignmentRepository.save(newAssignment);
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENT_MAPPING_ERROR + e.getMessage());
        }

    }

    /**
     * Update comments of an Assignment
     *
     * @param assignmentId : ID of assignment
     * @param assignmentComment : Assignment Comment Dto depending on the user who commented
     */
    @Override
    public void updateAssignmentComments(String assignmentId, AssignmentCommentDto assignmentComment) {
        try{
            // Try to locate if assignment exists
            Optional<Assignment> existingAssignment = assignmentRepository.findById(assignmentId);
            if (existingAssignment.isEmpty())
                throw new DataNotFoundException("Assignment with id: " + assignmentId + " not found in DB");

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
            assignment.setStatus(AssignmentStatus.Open.toString().toString());
            assignment.setTimestamp(LocalDateTime.now());
            assignment.setTimestampUpdated(LocalDateTime.now());
            return assignmentRepository.save(assignment).getId();
        } catch (MappingException e) {
            throw new ModelMappingException(ASSIGNMENT_MAPPING_ERROR + e.getMessage());
        }
    }

    /**
     * Create async a notification connected to an assignment and notify user through WebSockets
     *
     * @param assignment: Assignment DTO
     */
    @Async("asyncPoolTaskExecutor")
    @Override
    public CompletableFuture<Void> createNotificationAndNotifyUser(AssignmentDto assignment) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create the notification
                NotificationDto assignmentNotification = NotificationDto.builder()
                        .notificationType(NotificationType.Assignment.toString())
                        .notificationStatus(NotificationStatus.Unread.toString().toString())
                        .productionModule(assignment.getProductionModule())
                        .userId(assignment.getTargetUserId())
                        .smartService(null)
                        .relatedAssignment(assignment.getId())
                        .relatedEvent(null)
                        .timestamp(LocalDateTime.now())
                        .priority(assignment.getPriority())
                        .description(assignment.getDescription())
                        .build();

                // Store Notification
                String notificationId = notificationService.storeNotification(assignmentNotification);
                if (notificationId == null){
                    log.error("Notification could not be stored in DB");
                    return;
                }

                assignmentNotification.setId(notificationId);
                // Send notification through WebSocket
                webSocketService.notifyUserWebSocket(assignmentNotification.getUserId(), objectMapper.writeValueAsString(assignmentNotification));
            } catch (JsonProcessingException e){
                log.error("Error processing Notification Dto to JSON - Error: {}", e.getMessage());
            }
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
