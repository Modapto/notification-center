package gr.atc.modapto.model;

import java.time.LocalDateTime;

import gr.atc.modapto.enums.MessagePriority;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message{

    @Field(type = FieldType.Text, name = "description")
    private String description;

    @Field(type = FieldType.Keyword, name = "productionModule")
    private String productionModule;

    @Field(type = FieldType.Date, name = "timestamp", format = DateFormat.date_hour_minute_second)
    private LocalDateTime timestamp;

    @Field(type = FieldType.Keyword, name ="priority")
    private MessagePriority priority;

    @Field(type = FieldType.Keyword, name ="pilot")
    private String pilot;

}
