package gr.atc.modapto.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import gr.atc.modapto.dto.AssignmentCommentDto;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentComment {

    @Field(type = FieldType.Date, name = "datetime", format = DateFormat.strict_date_optional_time)
    private OffsetDateTime datetime;

    @Field(type = FieldType.Keyword, name ="comment")
    private String comment;

    @Field(type = FieldType.Keyword, name ="origin")
    private String origin;

    @Field(type = FieldType.Keyword, name ="originName")
    private String originName;

    /**
     * Convert AssignmentCommentDto to AssignmentComment
     * 
     * @param commentDto : Comment DTO to be converted
     * @return Converted AssignmentComment entity
     */
    public static AssignmentComment convertToAssignmentComment(AssignmentCommentDto commentDto) {
        AssignmentComment comment = new AssignmentComment();
        comment.setDatetime(commentDto.getDatetime());
        comment.setOrigin(commentDto.getOrigin());
        comment.setComment(commentDto.getComment());
        comment.setOriginName(commentDto.getOriginName());
        return comment;
    }
}
