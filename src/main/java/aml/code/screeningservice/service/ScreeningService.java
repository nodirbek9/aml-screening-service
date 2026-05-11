package aml.code.screeningservice.service;

import aml.code.screeningservice.entity.BlacklistEntry;
import aml.code.screeningservice.entity.CheckResult;
import aml.code.screeningservice.entity.Transaction;
import aml.code.screeningservice.entity.enums.EntryStatus;
import aml.code.screeningservice.entity.enums.MatchResult;
import aml.code.screeningservice.entity.enums.TransactionStatus;
import aml.code.screeningservice.repository.BlacklistRepository;
import aml.code.screeningservice.repository.CheckResultRepository;
import aml.code.screeningservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningService {

    private final BlacklistRepository blacklistRepository;
    private final TransactionRepository transactionRepository;
    private final CheckResultRepository checkResultRepository;

    @Value("${screening.threshold:0.80}")
    private double threshold;

    public void screen(Transaction transaction) {
        log.info("Starting screening for transaction ID: {}", transaction.getId());

        transaction.setStatus(TransactionStatus.CHECKING);
        transactionRepository.save(transaction);
        log.debug("Transaction status changed to CHECKING");

        String recipientName = transaction.getRecipientName();
        String normalizedRecipient = normalizeText(recipientName);
        log.debug("Normalized recipient name: {}", normalizedRecipient);

        List<BlacklistEntry> activeEntries = blacklistRepository.findAllByStatus(EntryStatus.ACTIVE);
        log.debug("Found {} active blacklist entries", activeEntries.size());

        if (activeEntries.isEmpty()) {
            log.info("Blacklist is empty, marking transaction as CLEAR");
            createClearResult(transaction);
            return;
        }

        double maxScore = 0.0;
        BlacklistEntry bestMatch = null;

        for (BlacklistEntry entry : activeEntries) {
            String normalizedEntry = normalizeText(entry.getFullName());

            // Levenshtein va Jaro-Winkler score'larni hisoblash
            double levScore = calcLevenshtein(normalizedRecipient, normalizedEntry);
            double jaroScore = calcJaroWinkler(normalizedRecipient, normalizedEntry);

            // Eng yaxshi score'ni tanlash
            double bestScore = Math.max(levScore, jaroScore);

            log.debug("Comparing with '{}': Levenshtein={}, JaroWinkler={}, Best={}",
                    entry.getFullName(), levScore, jaroScore, bestScore);

            // Maksimal score'ni yangilash
            if (bestScore > maxScore) {
                maxScore = bestScore;
                bestMatch = entry;
            }
        }

        log.info("Best match score: {} (threshold: {})", maxScore, threshold);

        if (maxScore >= threshold) {
            // HIT - совпадение найдено
            log.warn("HIT detected! Match score {} >= threshold {}", maxScore, threshold);
            createHitResult(transaction, bestMatch, maxScore);
        } else {
            // CLEAR - совпадений нет
            log.info("CLEAR - no match found. Score {} < threshold {}", maxScore, threshold);
            createClearResultWithScore(transaction, maxScore);
        }
    }
    /**
     * CLEAR result yaratish (blacklist bo'sh bo'lganda)
     */
    private void createClearResult(Transaction transaction) {
        CheckResult checkResult = new CheckResult();
        checkResult.setTransaction(transaction);
        checkResult.setMatchScore(0.0);
        checkResult.setResult(MatchResult.CLEAR);
        checkResult.setThreshold(threshold);
        checkResult.setCheckDate(LocalDateTime.now());
        checkResult.setAlgorithm("NONE");
        checkResult.setMatchedEntry(null);

        checkResultRepository.save(checkResult);

        transaction.setStatus(TransactionStatus.CLEAR);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        log.info("Transaction {} marked as CLEAR (empty blacklist)", transaction.getId());
    }

    /**
     * CLEAR result yaratish (score past bo'lganda)
     */
    private void createClearResultWithScore(Transaction transaction, double matchScore) {
        CheckResult checkResult = new CheckResult();
        checkResult.setTransaction(transaction);
        checkResult.setMatchScore(matchScore);
        checkResult.setResult(MatchResult.CLEAR);
        checkResult.setThreshold(threshold);
        checkResult.setCheckDate(LocalDateTime.now());
        checkResult.setAlgorithm("JARO_WINKLER");
        checkResult.setMatchedEntry(null);

        checkResultRepository.save(checkResult);

        transaction.setStatus(TransactionStatus.CLEAR);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        log.info("Transaction {} marked as CLEAR (score: {})", transaction.getId(), matchScore);
    }

    /**
     * HIT result yaratish (совпадение найдено)
     */
    private void createHitResult(Transaction transaction, BlacklistEntry matchedEntry, double
            matchScore) {
        CheckResult checkResult = new CheckResult();
        checkResult.setTransaction(transaction);
        checkResult.setMatchScore(matchScore);
        checkResult.setResult(MatchResult.HIT);
        checkResult.setThreshold(threshold);
        checkResult.setCheckDate(LocalDateTime.now());
        checkResult.setAlgorithm("JARO_WINKLER");
        checkResult.setMatchedEntry(matchedEntry);

        checkResultRepository.save(checkResult);

        transaction.setStatus(TransactionStatus.BLOCKED_AUTO);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        log.warn("Transaction {} BLOCKED_AUTO! Matched with blacklist entry ID: {} (score: {})",
                transaction.getId(), matchedEntry.getId(), matchScore);
    }

    private String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text.toLowerCase()
                .trim()
                .replaceAll("[^a-zа-яё\\s]", "")  // Faqat harflar va bo'sh joy
                .replaceAll("\\s+", " ");          // Ko'p bo'sh joylarni bitta qilish
    }

    /**
     * Levenshtein Distance алгоритм
     * Вощврашает: 0.0 (если нет вапше совпадений) от 1.0 (полностю одинаково) до
     */
    private double calcLevenshtein(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        LevenshteinDistance levenshtein = new LevenshteinDistance();
        int distance = levenshtein.apply(a, b);
        int maxLength = Math.max(a.length(), b.length());

        // Normalizatsiya: 1.0 - (distance / maxLength)
        // distance = 0 → score = 1.0 (to'liq bir xil)
        // distance = maxLength → score = 0.0 (umuman o'xshamaydi)
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Jaro-Winkler Similarity алгоритм
     * Вощврашает: 0.0 (если нет вапше совпадений) от 1.0 (полностю одинаково) до
     */
    private double calcJaroWinkler(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
        return jaroWinkler.apply(a, b);
    }
}
