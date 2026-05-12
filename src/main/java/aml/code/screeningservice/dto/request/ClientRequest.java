package aml.code.screeningservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClientRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 255, message = "Full name must be between 3 and 255 characters")
    private String fullName;

    private LocalDate birthDate;

    private String passportNumber;  // ← QO'SHILDI

    private String inn;  // ← QO'SHILDI

    private String phone;

    @Email(message = "Email should be valid")
    private String email;
}
