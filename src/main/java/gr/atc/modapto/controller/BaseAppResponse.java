package gr.atc.modapto.controller;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "BaseAppResponse", description = "Standard application response wrapper")
public class BaseAppResponse<T> {
    @Schema(description = "Response data", type = "object")
    private T data;

    @Schema(description = "Error details", type = "object")
    private Object errors;

    @Schema(description = "Response message", type = "string")
    private String message;

    @Schema(description = "Success/Failure Flag", type = "boolean")
    private boolean success;

    @Schema(description = "Timestamp", type = "string", format = "date-time", example = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default  
    private String timestamp = OffsetDateTime.now(ZoneOffset.UTC).withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

    public static <T> BaseAppResponse<T> success(T data) {
        return BaseAppResponse.<T>builder()
                .success(true)
                .message("Operation successful")
                .data(data)
                .build();
    }

    public static <T> BaseAppResponse<T> success(T data, String message) {
        return BaseAppResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseAppResponse<T> error(String message) {
        return BaseAppResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> BaseAppResponse<T> error(String message, Object errors) {
        return BaseAppResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
}
