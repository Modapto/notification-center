package gr.atc.modapto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gr.atc.modapto.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventMappingsDto {

    @JsonProperty("eventMappingsId")
    private String id;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonProperty("smartService")
    private String smartService;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("userRoles")
    private List<UserRole> userRoles;
}
