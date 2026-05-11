package aml.code.screeningservice.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionResponse {
    private Long id;
    private Long clientId;
    private String clientName;           // Client'ning fullName'i
    private String recipientName;
    private String recipientPassport;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String reviewedBy;
    private String reviewComment;
    private String checkResult;
}
