package gr.atc.modapto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Paginated Result Data Transfer Object", title = "Paginated Results")
public class PaginatedResultsDto<T> {

    @JsonProperty("results")
    private List<T> results;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("lastPage")
    private Boolean lastPage;
}
