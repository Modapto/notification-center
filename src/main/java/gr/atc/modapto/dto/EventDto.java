package gr.atc.modapto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gr.atc.modapto.util.UtcOffsetDateTimeDeserializer;
import gr.atc.modapto.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event Object Representation", title = "Event")
public class EventDto {
    @JsonProperty("eventId")
    private String id;

    @JsonProperty("description")
    private String description;

    @NotEmpty(message = "Production Module cannot be empty")
    @JsonProperty("module")
    private String module;

    @JsonProperty("moduleName")
    private String moduleName;

    @JsonDeserialize(using = UtcOffsetDateTimeDeserializer.class)
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @NotNull(message = "Priority cannot be empty")
    @ValidPriority
    @JsonProperty("priority")
    private String priority;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @JsonProperty("smartService")
    private String smartService;

    @NotNull(message = "Topic cannot be empty")
    @JsonProperty("topic")
    private String topic;

    @JsonProperty("results")
    private JsonNode results;
}