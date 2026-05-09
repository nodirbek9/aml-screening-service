package aml.code.screeningservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

@Data
public class TransactionRequest {
    @NonNull
    private Long clientId;
    @NotBlank
    @Size(min = 2, max = 255)
    private String recipientName;
    @NonNull
    @Positive(message = "Amount must be positive")
    private String amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a three-upper case letters")
    private String currency;
}
