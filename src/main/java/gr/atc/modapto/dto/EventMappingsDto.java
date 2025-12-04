package gr.atc.modapto.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event Mapping Object Representation", title = "Event Mapping")
public class EventMappingsDto {

    @JsonProperty("eventMappingId")
    private String id;

    @NotEmpty(message = "Topic cannot be empty")
    @JsonProperty("topic")
    private String topic;

    @JsonProperty("description")
    private String description;

    @NotEmpty(message = "User Roles cannot be empty")
    @JsonProperty("userRoles")
    private List<String> userRoles;
}
