package aml.code.screeningservice.dto.response;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String active;
    private String createdAt;
}
