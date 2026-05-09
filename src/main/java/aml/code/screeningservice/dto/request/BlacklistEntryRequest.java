package aml.code.screeningservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlacklistEntryRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String  fullName;
    @NotNull(message = "List type is required")
    private String listType;
    @Past(message = "Birth date must be in the past")
    private String birthDate;
}
