package aml.code.screeningservice.entity;

import aml.code.screeningservice.entity.enums.MatchResult;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_entry_id")
    private BlacklistEntry matchedEntry;    //null если CLEAR

    @Column(nullable = false)
    private Double matchScore;              // степень совпадения

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchResult result;

    @Column(nullable = false)
    private Double threshold;               // порог, который используется

    @Column(nullable = false)
    private LocalDateTime checkDate;

    private String algorothm;                   // "LEVENSHTEIN" / "JARO_WINKLER"
}
