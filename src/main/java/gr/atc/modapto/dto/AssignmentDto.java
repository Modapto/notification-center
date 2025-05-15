package gr.atc.modapto.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.modapto.validation.ValidAssignmentStatus;
import gr.atc.modapto.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assignment Object Representation", title = "Assignment")
public class AssignmentDto {

    @JsonProperty("assignmentId")
    private String id;

    @JsonProperty("sourceUserId")
    private String sourceUserId;

    @JsonProperty("sourceUser")
    private String sourceUser;

    @JsonProperty("targetUserId")
    private String targetUserId;

    @JsonProperty("targetUser")
    private String targetUser;

    @ValidAssignmentStatus
    @JsonProperty("status")
    private String status;

    @JsonProperty("comments")
    private List<AssignmentCommentDto> comments;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    @JsonProperty("timestampUpdated")
    private OffsetDateTime timestampUpdated;

    @ValidPriority
    @JsonProperty("priority")
    private String priority;
}
