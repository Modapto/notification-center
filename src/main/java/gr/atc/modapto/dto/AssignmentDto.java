package gr.atc.modapto.dto;

import java.time.LocalDateTime;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.validation.ValidAssignmentStatus;
import gr.atc.modapto.validation.ValidPriority;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentDto {

    @JsonProperty("assignmentId")
    private String id;

    @JsonProperty("sourceUserId")
    private String sourceUserId;

    @JsonProperty("targetUserId")
    private String targetUserId;

    @ValidAssignmentStatus
    @JsonProperty("status")
    private AssignmentStatus status;

    @JsonProperty("sourceUserComments")
    private HashMap<LocalDateTime, String> sourceUserComments;

    @JsonProperty("targetUserComments")
    private HashMap<LocalDateTime, String> targetUserComments;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("timestampUpdated")
    private LocalDateTime timestampUpdated;

    @ValidPriority
    @JsonProperty("priority")
    private MessagePriority priority;
}
