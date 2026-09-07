package com.jobsight.company.resume;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.resume.dto.ResumeCopyRequest;
import com.jobsight.company.resume.dto.ResumeRequest;
import com.jobsight.company.resume.dto.ResumeResponse;
import com.jobsight.company.resume.dto.ResumeRowRequest;
import com.jobsight.company.resume.dto.ResumeSummary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** 이력서는 문서 통째로 읽고 쓴다. 행 단위 API 는 없다 — docs/resumes.md. 모든 조회는 소유자로 한정. */
@Service
@Transactional(readOnly = true)
public class ResumeService {
    private final ResumeRepository repository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ResumeService(ResumeRepository repository, CurrentUser currentUser, ObjectMapper objectMapper,
                         Validator validator) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /** 목록엔 content 를 싣지 않는다. 카드는 이름·수정일만 그린다. */
    public List<ResumeSummary> findAll() {
        return repository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .map(ResumeSummary::of)
                .toList();
    }

    public ResumeResponse findById(UUID id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public ResumeResponse create(ResumeRequest request) {
        Resume resume = new Resume(currentUser.id(), request.name().trim(), write(request.content()));
        return toResponse(repository.save(resume));
    }

    @Transactional
    public ResumeResponse update(UUID id, ResumeRequest request) {
        Resume resume = findOwned(id);
        resume.update(request.name().trim(), write(request.content()));
        return toResponse(repository.save(resume));
    }

    /** 같은 내용의 새 버전. 원본은 건드리지 않는다. */
    @Transactional
    public ResumeResponse copy(UUID id, ResumeCopyRequest request) {
        Resume source = findOwned(id);
        Resume copy = new Resume(currentUser.id(), request.name().trim(), source.getContent());
        return toResponse(repository.save(copy));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    // ── 행 단위 도구 (MCP 전용) ───────────────────────────────────────────
    // REST 에는 행 API 가 없다 — 화면은 문서 통째 PUT 이다. MCP 는 문서 전체를 되돌려 보내기 어려우니
    // 서버가 조회 → 한 군데 수정 → 저장을 한 트랜잭션으로 묶어 준다. 나머지 섹션은 건드리지 않는다.

    @Transactional
    public ResumeResponse updateBasic(UUID id, ResumeContent.BasicInfo basic) {
        Resume resume = findOwned(id);
        ResumeContent current = read(resume.getContent());
        ResumeContent next = new ResumeContent(basic == null ? ResumeContent.BasicInfo.empty() : basic,
                current.educations(), current.trainings(), current.activities(), current.experiences(),
                current.awards(), current.certificates(), current.skills(), current.projects());
        resume.update(resume.getName(), objectMapper.writeValueAsString(next.normalized()));
        return toResponse(repository.save(resume));
    }

    /** 섹션 맨 뒤에 붙인다. */
    @Transactional
    public ResumeResponse addRow(UUID id, ResumeSection section, ResumeRowRequest row) {
        return mutateRows(id, section, rows -> rows.add(toNode(section, row)));
    }

    /** index(0-based) 행을 통째로 바꾼다. 다른 행은 그대로. */
    @Transactional
    public ResumeResponse updateRow(UUID id, ResumeSection section, int index, ResumeRowRequest row) {
        return mutateRows(id, section, rows -> {
            requireIndex(rows, index);
            rows.set(index, toNode(section, row));
        });
    }

    /** index(0-based) 행을 지운다. 뒤 행이 앞으로 당겨진다. */
    @Transactional
    public ResumeResponse deleteRow(UUID id, ResumeSection section, int index) {
        return mutateRows(id, section, rows -> {
            requireIndex(rows, index);
            rows.remove(index);
        });
    }

    private ResumeResponse mutateRows(UUID id, ResumeSection section, Consumer<ArrayNode> change) {
        Resume resume = findOwned(id);
        ObjectNode root = (ObjectNode) objectMapper.readTree(resume.getContent());
        JsonNode existing = root.get(section.key());
        ArrayNode rows = existing instanceof ArrayNode array ? array : root.putArray(section.key());
        change.accept(rows);
        ResumeContent next = objectMapper.treeToValue(root, ResumeContent.class).normalized();
        resume.update(resume.getName(), objectMapper.writeValueAsString(next));
        return toResponse(repository.save(resume));
    }

    /**
     * 합집합 record 를 이 섹션의 record 로 엄격 변환한다. 다른 섹션 필드가 섞이면 거부한다 —
     * Jackson 이 조용히 버리면 값이 사라진 채 저장되고, 도구 사용자는 성공으로 알기 때문이다.
     */
    private JsonNode toNode(ResumeSection section, ResumeRowRequest row) {
        Map<String, Object> present = new LinkedHashMap<>();
        Map<?, ?> all = objectMapper.convertValue(row, Map.class);
        all.forEach((key, value) -> {
            if (value != null) present.put(String.valueOf(key), value);
        });
        Object typed;
        try {
            typed = objectMapper.readerFor(section.rowType())
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(objectMapper.writeValueAsString(present));
        } catch (JacksonException e) {
            throw new ApiRuleException(HttpStatus.BAD_REQUEST, "UNKNOWN_ROW_FIELD",
                    section.key() + " 섹션의 필드는 " + section.fields() + " 입니다.");
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(typed);
        if (!violations.isEmpty()) {
            ConstraintViolation<Object> first = violations.iterator().next();
            throw new ApiRuleException(HttpStatus.BAD_REQUEST, "INVALID_ROW",
                    first.getPropertyPath() + ": " + first.getMessage());
        }
        return objectMapper.valueToTree(typed);
    }

    private static void requireIndex(ArrayNode rows, int index) {
        if (index < 0 || index >= rows.size()) {
            throw new ApiRuleException(HttpStatus.NOT_FOUND, "ROW_NOT_FOUND",
                    "행 번호가 범위를 벗어났습니다: " + index + " (0부터, 현재 " + rows.size() + "개)");
        }
    }

    /** 남의 데이터는 존재 자체를 알리지 않도록 403 이 아니라 404. */
    private Resume findOwned(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private ResumeResponse toResponse(Resume resume) {
        return ResumeResponse.of(resume, read(resume.getContent()));
    }

    /** null 은 빈 문서로. 저장되는 JSON 이 항상 같은 모양이어야 읽는 쪽이 편하다. */
    private String write(ResumeContent content) {
        ResumeContent normalized = (content == null ? ResumeContent.empty() : content).normalized();
        return objectMapper.writeValueAsString(normalized);
    }

    private ResumeContent read(String json) {
        return objectMapper.readValue(json, ResumeContent.class).normalized();
    }
}
