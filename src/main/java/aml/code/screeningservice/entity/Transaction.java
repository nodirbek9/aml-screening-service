package aml.code.screeningservice.entity;

import aml.code.screeningservice.entity.enums.TransactionStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String recipientName;

    private String recipientPassport;

    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;        //RUB USD EUR

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String reviewedBy;      //username офицера, принявшего решение
    private String reviewComment;       // коментарии к решению
}
