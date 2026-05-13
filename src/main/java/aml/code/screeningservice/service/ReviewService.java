package aml.code.screeningservice.service;

import aml.code.screeningservice.dto.response.TransactionResponse;
import aml.code.screeningservice.entity.Transaction;
import aml.code.screeningservice.entity.enums.TransactionStatus;
import aml.code.screeningservice.exception.InvalidStatusTransitionException;
import aml.code.screeningservice.exception.ResourceNotFoundException;
import aml.code.screeningservice.mapper.TransactionMapper;
import aml.code.screeningservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

        public TransactionResponse submitForReview(Long transactionId) {
            Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                    () -> new ResourceNotFoundException("transaction.not.found"));

            if (transaction.getStatus() == TransactionStatus.BLOCKED_AUTO) {
                transaction.setStatus(TransactionStatus.UNDER_REVIEW);
                transaction.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
                return transactionMapper.toResponse(transaction);
            }
            throw new InvalidStatusTransitionException("invalid.status.transition");
        }

        public TransactionResponse approve(Long transactionId, String comment, String officerUsername ) {
            Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                    () -> new ResourceNotFoundException("transaction.not.found"));
            if (!(transaction.getStatus() == TransactionStatus.UNDER_REVIEW)) {
                throw new ResourceNotFoundException("invalid.status.transition");
            }
            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transaction.setReviewedBy(officerUsername);
            transaction.setReviewComment(comment);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            return transactionMapper.toResponse(transaction);
        }

        public TransactionResponse reject(Long transactionId, String comment, String officerUsername) {
            Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                    () -> new ResourceNotFoundException("transaction.not.found"));
            if (!(transaction.getStatus() == TransactionStatus.UNDER_REVIEW)) {
                throw new ResourceNotFoundException("invalid.status.transition");
            }
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transaction.setReviewedBy(officerUsername);
            transaction.setReviewComment(comment);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            return transactionMapper.toResponse(transaction);
        }
}
