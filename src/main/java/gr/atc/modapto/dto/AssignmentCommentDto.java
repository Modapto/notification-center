package gr.atc.modapto.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gr.atc.modapto.enums.AssignmentOrigin;
import gr.atc.modapto.validation.ValidAssignmentOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assignment Comment Object Representation", title = "Assignment Comment")
public class AssignmentCommentDto {

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime datetime = LocalDateTime.now();

    @NotEmpty
    @JsonProperty("comment")
    private String comment;

    @Builder.Default
    @ValidAssignmentOrigin
    @JsonProperty("origin")
    private String origin = AssignmentOrigin.System.toString().toString();

    @Builder.Default
    @NotEmpty
    private String originName = "MODAPTO System";
}
