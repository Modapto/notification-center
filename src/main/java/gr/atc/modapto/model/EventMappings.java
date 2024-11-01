package gr.atc.modapto.model;

import gr.atc.modapto.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "event_mappings")
public class EventMappings {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "productionModule")
    private String productionModule;

    @Field(type = FieldType.Keyword, name = "smartService")
    private String smartService;

    @Field(type = FieldType.Keyword, name = "eventType")
    private String eventType;

    @Field(type = FieldType.Keyword, name = "userRoles")
    private List<UserRole> userRoles;
}
