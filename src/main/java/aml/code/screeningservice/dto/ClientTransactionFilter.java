package aml.code.screeningservice.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ClientTransactionFilter {
    private Long clientId;
    private Integer page;
    private Integer limit;
    private List<String> status;

    public ClientTransactionFilter(Long clientId, Integer page, Integer limit, List<String> status) {
        this.clientId = clientId;
        this.page = page;
        this.limit = limit;
        this.status = status;
    }
}
