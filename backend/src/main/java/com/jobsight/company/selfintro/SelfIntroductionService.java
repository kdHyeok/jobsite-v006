package com.jobsight.company.selfintro;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.resume.ResumeRepository;
import com.jobsight.company.selfintro.dto.SelfIntroductionRequest;
import com.jobsight.company.selfintro.dto.SelfIntroductionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class SelfIntroductionService {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final SelfIntroductionRepository repository;
    private final ResumeRepository resumeRepository;
    private final CurrentUser currentUser;

    public SelfIntroductionService(SelfIntroductionRepository repository, ResumeRepository resumeRepository,
                                   CurrentUser currentUser) {
        this.repository = repository;
        this.resumeRepository = resumeRepository;
        this.currentUser = currentUser;
    }

    public List<SelfIntroductionResponse> findAll(String query, UUID resumeId) {
        List<SelfIntroduction> values = repository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .filter(value -> resumeId == null || value.getResumeId().equals(resumeId))
                .toList();
        return rank(values, query).stream().map(SelfIntroductionResponse::of).toList();
    }

    public SelfIntroductionResponse findById(UUID id) {
        return SelfIntroductionResponse.of(findOwned(id));
    }

    @Transactional
    public SelfIntroductionResponse create(SelfIntroductionRequest request) {
        requireOwnedResume(request.resumeId());
        SelfIntroduction value = new SelfIntroduction(currentUser.id(), request.resumeId(), request.question().trim(),
                normalize(request.answer()));
        return SelfIntroductionResponse.of(repository.save(value));
    }

    @Transactional
    public SelfIntroductionResponse update(UUID id, SelfIntroductionRequest request) {
        SelfIntroduction value = findOwned(id);
        requireOwnedResume(request.resumeId());
        value.update(request.resumeId(), request.question().trim(), normalize(request.answer()));
        return SelfIntroductionResponse.of(repository.save(value));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    static List<SelfIntroduction> rank(List<SelfIntroduction> values, String query) {
        List<String> terms = tokenize(query).stream().distinct().toList();
        if (terms.isEmpty() || values.isEmpty()) return values;

        Map<SelfIntroduction, List<String>> documents = new HashMap<>();
        values.forEach(value -> documents.put(value, tokenize(value.getQuestion() + " " +
                (value.getAnswer() == null ? "" : value.getAnswer()))));
        double averageLength = documents.values().stream().mapToInt(List::size).average().orElse(1);
        Map<SelfIntroduction, Double> scores = new HashMap<>();

        for (SelfIntroduction value : values) {
            List<String> words = documents.get(value);
            double score = 0;
            for (String term : terms) {
                long frequency = words.stream().filter(word -> word.contains(term)).count();
                if (frequency == 0) continue;
                long documentFrequency = documents.values().stream()
                        .filter(doc -> doc.stream().anyMatch(word -> word.contains(term)))
                        .count();
                double idf = Math.log(1 + (values.size() - documentFrequency + 0.5) / (documentFrequency + 0.5));
                double denominator = frequency + K1 * (1 - B + B * words.size() / averageLength);
                score += idf * frequency * (K1 + 1) / denominator;
            }
            if (score > 0) scores.put(value, score);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<SelfIntroduction, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getUpdatedAt(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static List<String> tokenize(String value) {
        if (value == null || value.isBlank()) return List.of();
        Matcher matcher = WORD.matcher(value.toLowerCase(Locale.ROOT));
        List<String> words = new ArrayList<>();
        while (matcher.find()) words.add(matcher.group());
        return words;
    }

    private SelfIntroduction findOwned(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private void requireOwnedResume(UUID id) {
        resumeRepository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
