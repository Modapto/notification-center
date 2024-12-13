package gr.atc.modapto.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.validation.ValidPriority;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDto<T> {
    @JsonProperty("eventId")
    private String id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

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
    private T results;
}