package aml.code.screeningservice.entity;

import aml.code.screeningservice.entity.enums.EntryStatus;
import jakarta.persistence.*;
import aml.code.screeningservice.entity.enums.ListType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blacklist_entries")
public class BlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private LocalDate birthday;

    private String passportNumber;

    private String inn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListType listType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryStatus status;

    @Column(nullable = false)
    private LocalDateTime addedAt;

    private LocalDateTime updatedAt;

    private String addedBy;
}
