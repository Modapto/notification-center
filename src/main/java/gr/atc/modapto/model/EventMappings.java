package gr.atc.modapto.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "event_mappings")
public class EventMappings {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "topic")
    private String topic;

    @Field(type = FieldType.Keyword, name = "description")
    private String description;

    @Field(type = FieldType.Keyword, name = "userRoles")
    private List<String> userRoles;
}
