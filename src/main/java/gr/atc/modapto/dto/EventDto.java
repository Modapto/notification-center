package gr.atc.modapto.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.validation.ValidPriority;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDto {
    @JsonProperty("eventId")
    private String id;

    @JsonProperty("description")
    private String description;

    @NotEmpty(message = "Production Module cannot be empty")
    @JsonProperty("productionModule")
    private String productionModule;

    @NotEmpty(message = "Pilot cannot be empty")
    @JsonProperty("pilot")
    private String pilot;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @NotNull(message = "Priority cannot be empty")
    @ValidPriority
    @JsonProperty("priority")
    private MessagePriority priority;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @JsonProperty("smartService")
    private String smartService;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("results")
    private JsonNode results;
}