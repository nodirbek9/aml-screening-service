package aml.code.screeningservice.dto.request;

import lombok.Data;

@Data
public class ReviewRequest {

    // Комментарий необязателен для submit-review
    // Желательен для approve и reject
    private String comment;
}
