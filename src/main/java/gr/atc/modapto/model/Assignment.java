package gr.atc.modapto.model;

import java.time.LocalDateTime;
import java.util.List;

import gr.atc.modapto.dto.AssignmentDto;
import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "assignments")
public class Assignment extends Message {

    @Id
    private String id;
    
    @Field(type = FieldType.Keyword, name ="sourceUserId")
    private String sourceUserId;

    @Field(type = FieldType.Keyword, name ="targetUserId")
    private String targetUserId;

    @Field(type = FieldType.Keyword, name = "status")
    private String status;

    @Field(type = FieldType.Nested, name = "comments")
    private List<AssignmentComment> comments;

    @Field(type = FieldType.Date, name = "timestampUpdated", format = DateFormat.date_hour_minute_second)
    private LocalDateTime timestampUpdated;

    /**
     * Update specific fields of an existing Assignment
     * @param assignment : Existing assignment in DB
     * @param assignmentDto : Fields of assignment to be updated
     * @return Updated assignment
     */
    public static Assignment updateExistingAssignment(Assignment assignment, AssignmentDto assignmentDto) {
        // Update the existing assignment with the new values except the comments
        if (assignmentDto.getSourceUserId() != null)
            assignment.setSourceUserId(assignmentDto.getSourceUserId());

        if (assignmentDto.getTargetUserId() != null)
            assignment.setTargetUserId(assignmentDto.getTargetUserId());

        if (assignmentDto.getStatus() != null)
            assignment.setStatus(assignmentDto.getStatus());

        if (assignmentDto.getDescription() != null)
            assignment.setDescription(assignmentDto.getDescription());

        if (assignmentDto.getPriority() != null)
            assignment.setPriority(assignmentDto.getPriority());

        if (assignmentDto.getProductionModule() != null)
            assignment.setProductionModule(assignmentDto.getProductionModule());

        if (assignmentDto.getComments() != null)
            assignmentDto.getComments().stream()
                .map(AssignmentComment::convertToAssignmentComment)
                .forEach(comment -> assignment.getComments().add(comment));

        return assignment;
    }
}