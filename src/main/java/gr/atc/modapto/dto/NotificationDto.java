package gr.atc.modapto.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.modapto.validation.ValidPriority;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto{

    @JsonProperty("notificationId")
    private String id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("notificationType")
    private NotificationType notificationType;

    @JsonProperty("relatedEvent")
    private String relatedEvent;

    @JsonProperty("relatedAssignment")
    private String relatedAssignment;

    @JsonProperty("notificationStatus")
    private NotificationStatus notificationStatus;

    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonProperty("smartService")
    private String smartService;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @ValidPriority
    @JsonProperty("priority")
    private MessagePriority priority;
}
