package aml.code.screeningservice.dto.request;

import lombok.Data;

@Data
public class ClientRequest {
    private String  fullName;
    private String email;
    private String phone;
}
