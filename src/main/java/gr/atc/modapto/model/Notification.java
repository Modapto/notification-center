package gr.atc.modapto.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "notifications")
public class Notification extends Message {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name ="notificationType")
    private String notificationType;

    @Field(type = FieldType.Keyword, name = "sourceComponent")
    private String sourceComponent;

    @Field(type = FieldType.Keyword, name = "smartService")
    private String smartService;

    @Field(type = FieldType.Keyword, name = "userId")
    private String userId;

    @Field(type = FieldType.Keyword, name = "notificationStatus")
    private String notificationStatus;

    @Field(type = FieldType.Keyword, name = "relatedEvent")
    private String relatedEvent;

    @Field(type = FieldType.Keyword, name = "relatedAssignment")
    private String relatedAssignment;

    @Field(type = FieldType.Keyword, name = "messageStatus")
    private String messageStatus;
}
