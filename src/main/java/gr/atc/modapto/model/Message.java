package gr.atc.modapto.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @Field(type = FieldType.Text, name = "description")
    private String description;

    @Field(type = FieldType.Keyword, name = "module")
    private String module;

    @Field(type = FieldType.Date, name = "timestamp", format = DateFormat.strict_date_optional_time)
    private OffsetDateTime timestamp;

    @Field(type = FieldType.Keyword, name = "moduleName")
    private String moduleName;

    @Field(type = FieldType.Keyword, name ="priority")
    private String priority;
}
