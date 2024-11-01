package gr.atc.modapto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentCommentDto {

    @JsonProperty("sourceUserComment")
    private String sourceUserComment;

    @JsonProperty("targetUserComment")
    private String targetUserComment;
}
