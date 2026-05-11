package aml.code.screeningservice.dto.response;

import aml.code.screeningservice.entity.enums.MatchResult;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckResultResponse {
    private Double matchScore;
    private MatchResult result;
    private Double threshold;
    private Long matchedEntryId;
    private LocalDateTime checkDate;
    private String algorithm;
}
