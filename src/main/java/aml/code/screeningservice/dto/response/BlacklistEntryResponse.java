package aml.code.screeningservice.dto.response;

import lombok.Data;

@Data
public class BlacklistEntryResponse {
    private String id;
    private String fullName;
    private String birthDate;
    private String passportNumber;
    private String inn;
    private String listType;
    private String status;
    private String addedAt;
    private String updatedAt;
    private String addedBy;
}
