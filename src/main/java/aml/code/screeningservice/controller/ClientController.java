package aml.code.screeningservice.controller;


import aml.code.screeningservice.dto.request.ClientRequest;
import aml.code.screeningservice.service.ClientService;
import aml.code.screeningservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody @Valid ClientRequest request) {
        return ResponseEntity.ok(clientService.create(request));
    }
    @GetMapping
    public ResponseEntity<?> getAllClients() {
        return ResponseEntity.ok(clientService.getAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getById(id));
    }
//    @GetMapping("/{id}/transactions")
//    public ResponseEntity<?> getClientTransactions(@PathVariable Long id, @RequestParam Integer page,
//                                                   @RequestParam Integer limit,
//                                                   @RequestParam(required = false) List<String> status) {
//        return ResponseEntity.ok(transactionService.getClientTransactions(new ClientTransactionFilter(id, page, limit, status)));
//    }
}
