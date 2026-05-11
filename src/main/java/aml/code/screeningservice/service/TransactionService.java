package aml.code.screeningservice.service;


import aml.code.screeningservice.dto.request.TransactionRequest;
import aml.code.screeningservice.dto.response.TransactionResponse;
import aml.code.screeningservice.entity.Transaction;
import aml.code.screeningservice.entity.enums.TransactionStatus;
import aml.code.screeningservice.mapper.TransactionMapper;
import aml.code.screeningservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

//    public List<Transaction> getClientTransactions(ClientTransactionFilter filter) {
//        ClientSpecification spec = new ClientSpecification(filter);
//
//        return null;
//    }

    public Long createTransaction(TransactionRequest request) {
        Transaction transaction = transactionMapper.toEntity(request);
        return transactionRepository.save(transaction).getId();
    }

    public Page<TransactionResponse> getAllTransactions(TransactionStatus status, Pageable pageable) {
        if (status != null) {
            Page<Transaction> allByStatus = transactionRepository.findAllByStatus(status, pageable);
            return allByStatus.map(transactionMapper::toResponse);
        }
        return transactionRepository.findAll(pageable).map(transactionMapper::toResponse);
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(
                () -> new RuntimeException("transaction.not.found")
        );
        return transactionMapper.toResponse(transaction);
    }
}
