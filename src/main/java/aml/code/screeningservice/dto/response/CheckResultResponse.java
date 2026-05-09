package aml.code.screeningservice.dto.response;

import lombok.Data;

@Data
public class CheckResultResponse {
    private String matchScore;
    private String result;
    private String threshold;
    private String checkedDate;
    private String algorithm;
}
