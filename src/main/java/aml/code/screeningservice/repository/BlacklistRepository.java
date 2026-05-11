package aml.code.screeningservice.repository;

import aml.code.screeningservice.entity.BlacklistEntry;
import aml.code.screeningservice.entity.enums.EntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BlacklistRepository extends JpaRepository<BlacklistEntry, Long> {

    Page<BlacklistEntry> findByStatus(EntryStatus status, Pageable pageable);

    @Query("select b from BlacklistEntry b where b.status = :entryStatus")
    List<BlacklistEntry> findAllByStatus(EntryStatus entryStatus);
}
