package aml.code.screeningservice.repository;

import aml.code.screeningservice.entity.BlacklistEntry;
import aml.code.screeningservice.entity.enums.EntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlacklistRepository extends JpaRepository<BlacklistEntry, Long> {

    Page<BlacklistEntry> findByStatus(EntryStatus status, Pageable pageable);

    List<BlacklistEntry> findAllByStatus(EntryStatus status);

    boolean existsByPassportNumber(String passportNumber);
}
