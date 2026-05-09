package aml.code.screeningservice.dto.response;

import lombok.Data;

@Data
public class ClientResponse {
    private String id;
    private String fullName;
    private String birthDate;
    private String passportNumber;
    private String inn;
    private String email;
    private String phone;
    private String status;
    private String createdAt;
}
