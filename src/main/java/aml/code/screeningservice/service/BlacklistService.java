package aml.code.screeningservice.service;

import aml.code.screeningservice.dto.request.BlacklistEntryRequest;
import aml.code.screeningservice.dto.response.BlacklistEntryResponse;
import aml.code.screeningservice.entity.BlacklistEntry;
import aml.code.screeningservice.entity.enums.EntryStatus;
import aml.code.screeningservice.mapper.BlackListMapper;
import aml.code.screeningservice.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final BlackListMapper blackListMapper;
    private static final String EXCEPTION_MESSAGE = "client.not.found";

    public Long create(BlacklistEntryRequest request, String addedBy) {
        BlacklistEntry entry = blackListMapper.toEntity(request);
        entry.setStatus(EntryStatus.ACTIVE);
        entry.setAddedAt(LocalDateTime.now());
        entry.setAddedBy(addedBy);
        return blacklistRepository.save(entry).getId();



    }

    public Page<BlacklistEntryResponse> getAll(EntryStatus status, Pageable pageable) {
        Page<BlacklistEntry> entries = blacklistRepository.findByStatus(status, pageable);
        return entries.map(blackListMapper::toResponse);
    }

    public BlacklistEntryResponse getById(Long id) {
        BlacklistEntry entry = blacklistRepository.findById(id).orElseThrow(
                () -> new RuntimeException(EXCEPTION_MESSAGE)
        );
        return blackListMapper.toResponse(entry);
    }

    public Boolean update(Long id, BlacklistEntryRequest request) {
        BlacklistEntry entry = blacklistRepository.findById(id).orElseThrow(
                () -> new RuntimeException(EXCEPTION_MESSAGE)
        );
        blackListMapper.updateFromRequest(request, entry);
        entry.setUpdatedAt(LocalDateTime.now());
        blacklistRepository.save(entry);
        return true;
    }

    public Boolean delete(Long id) {
        BlacklistEntry entry = blacklistRepository.findById(id).orElseThrow(
                () -> new RuntimeException(EXCEPTION_MESSAGE)
        );
        if (entry.getStatus() == EntryStatus.INACTIVE) {
            throw new RuntimeException("entry.already.deleted");
        }else {
            entry.setStatus(EntryStatus.INACTIVE);
            entry.setUpdatedAt(LocalDateTime.now());
            blacklistRepository.save(entry);

        }
        return true;
    }
}
