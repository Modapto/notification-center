package gr.atc.modapto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "modapto-modules")
public class ModaptoModule {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String moduleId;

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Text)
    private String endpoint;

    @Field(type = FieldType.Object)
    private List<SmartService> smartServices;

    @Field(name = "timestamp_dt", type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime timestampDt;

    @Field(name = "timestamp_elastic", type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant timestampElastic;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SmartService{
        @Field(type = FieldType.Keyword)
        private String name;

        @Field(type = FieldType.Keyword)
        private String catalogueId;

        @Field(type = FieldType.Keyword)
        private String serviceId;

        @Field(type = FieldType.Text)
        private String endpoint;
    }

}
