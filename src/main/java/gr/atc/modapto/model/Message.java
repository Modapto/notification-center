package gr.atc.modapto.model;

import java.time.LocalDateTime;

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

    @Field(type = FieldType.Keyword, name = "productionModule")
    private String productionModule;

    @Field(type = FieldType.Date, name = "timestamp", format = DateFormat.date_hour_minute_second)
    private LocalDateTime timestamp;

    @Field(type = FieldType.Keyword, name ="priority")
    private String priority;
}
