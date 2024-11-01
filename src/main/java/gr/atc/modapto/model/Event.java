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
@Document(indexName = "events")
public class Event extends Message {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "eventType")
    private String eventType;

    @Field(type = FieldType.Keyword, name = "sourceComponent")
    private String sourceComponent;

    @Field(type = FieldType.Keyword, name = "smartService")
    private String smartService;

    @Field(type = FieldType.Text, name = "topic")
    private String topic;
}
