package gr.atc.modapto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "message_topics")
public class MessageTopic {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "topicName")
    private String topicName;

    @Field(type = FieldType.Text, name = "topicDescription")
    private String topicDescription;

    @Field(type = FieldType.Keyword, name = "smartServiceType")
    private String smartServiceType;

    @Field(type = FieldType.Keyword, name = "registeredComponents")
    private List<String> registeredComponents;
}
