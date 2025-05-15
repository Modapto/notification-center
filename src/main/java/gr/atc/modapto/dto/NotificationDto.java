package gr.atc.modapto.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.modapto.validation.ValidNotificationStatus;
import gr.atc.modapto.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification Object Representation", title = "Notification")
public class NotificationDto{

    @JsonProperty("notificationId")
    private String id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("user")
    private String user;

    @JsonProperty("notificationType")
    private String notificationType;

    @JsonProperty("relatedEvent")
    private String relatedEvent;

    @JsonProperty("relatedAssignment")
    private String relatedAssignment;

    @ValidNotificationStatus
    @JsonProperty("notificationStatus")
    private String notificationStatus;

    @JsonProperty("messageStatus")
    private String messageStatus;

    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonProperty("smartService")
    private String smartService;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @ValidPriority
    @JsonProperty("priority")
    private String priority;
}
