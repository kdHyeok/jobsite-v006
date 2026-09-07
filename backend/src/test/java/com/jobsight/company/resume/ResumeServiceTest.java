package com.jobsight.company.resume;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.resume.dto.ResumeCopyRequest;
import com.jobsight.company.resume.dto.ResumeRequest;
import com.jobsight.company.resume.dto.ResumeResponse;
import com.jobsight.company.resume.dto.ResumeRowRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private ResumeRepository repository;
    @Mock private CurrentUser currentUser;

    private final JsonMapper mapper = JsonMapper.builder().build();
    private ResumeService service;

    @BeforeEach
    void setUp() {
        service = new ResumeService(repository, currentUser, mapper, Validation.buildDefaultValidatorFactory().getValidator());
        given(currentUser.id()).willReturn(OWNER_ID);
    }

    private ResumeRowRequest row(Map<String, String> fields) {
        return mapper.convertValue(fields, ResumeRowRequest.class);
    }

    private void saveReturnsArgument() {
        given(repository.save(any())).willAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.onCreate();
            return r;
        });
    }

    /** content 없이 만들어도 저장 JSON 은 항상 같은 모양이다 — 읽는 쪽이 null 검사를 안 한다. */
    @Test
    void createWithoutContentStoresEmptyDocument() {
        saveReturnsArgument();

        ResumeResponse created = service.create(new ResumeRequest("  새 이력서 ", null));

        ArgumentCaptor<Resume> saved = ArgumentCaptor.forClass(Resume.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("새 이력서");
        assertThat(saved.getValue().getContent()).contains("\"educations\":[]").contains("\"projects\":[]");
        assertThat(created.content().basic()).isNotNull();
        assertThat(created.content().educations()).isEmpty();
    }

    /** 행 배열은 순서 그대로 왕복한다. 연월은 문자열이라 "현재" 같은 표현이 살아남는다. */
    @Test
    void contentRoundTripsThroughJson() {
        saveReturnsArgument();
        ResumeContent content = new ResumeContent(
                new ResumeContent.BasicInfo("테스트", null, null, null, null, null, "https://example.com/gh"),
                List.of(new ResumeContent.Education("2020.03", "현재", "테스트 대학교", "컴퓨터공학과", "4.0 / 4.5")),
                null, null, null, null, null, null, null);

        ResumeResponse created = service.create(new ResumeRequest("v1", content));

        assertThat(created.content().educations()).hasSize(1);
        assertThat(created.content().educations().get(0).endYm()).isEqualTo("현재");
        assertThat(created.content().basic().githubUrl()).isEqualTo("https://example.com/gh");
        // null 로 보낸 섹션은 빈 배열로 정규화된다.
        assertThat(created.content().trainings()).isEmpty();
    }

    /** 복제는 새 id 에 같은 content. 원본은 건드리지 않는다. */
    @Test
    void copyCreatesNewVersionWithSameContent() {
        saveReturnsArgument();
        Resume source = new Resume(OWNER_ID, "v1", "{\"basic\":{\"name\":\"테스트\"},\"educations\":[]}");
        source.onCreate();
        given(repository.findByIdAndOwnerId(source.getId(), OWNER_ID)).willReturn(Optional.of(source));

        ResumeResponse copy = service.copy(source.getId(), new ResumeCopyRequest("v2"));

        assertThat(copy.id()).isNotEqualTo(source.getId());
        assertThat(copy.name()).isEqualTo("v2");
        assertThat(copy.content().basic().name()).isEqualTo("테스트");
        assertThat(source.getName()).isEqualTo("v1");
    }

    /** 행 도구: 그 섹션 필드만 받고, 다른 섹션 필드·범위 밖 index 는 거부한다. 나머지 섹션은 그대로. */
    @Test
    void rowToolsEditOneSectionAndRejectForeignFields() {
        saveReturnsArgument();
        Resume resume = new Resume(OWNER_ID, "v1",
                "{\"basic\":{\"name\":\"테스트\"},\"educations\":[{\"school\":\"테스트 대학교\"}],\"certificates\":[]}");
        resume.onCreate();
        given(repository.findByIdAndOwnerId(resume.getId(), OWNER_ID)).willReturn(Optional.of(resume));

        ResumeResponse added = service.addRow(resume.getId(), ResumeSection.certificates,
                row(Map.of("name", "SQLD", "issuer", "기관 A", "acquiredYm", "2026.03")));
        assertThat(added.content().certificates()).extracting(ResumeContent.Certificate::name).containsExactly("SQLD");
        assertThat(added.content().educations()).extracting(ResumeContent.Education::school).containsExactly("테스트 대학교");
        assertThat(added.content().basic().name()).isEqualTo("테스트");

        ResumeResponse replaced = service.updateRow(resume.getId(), ResumeSection.certificates, 0,
                row(Map.of("name", "ADsP", "issuer", "기관 A", "acquiredYm", "2025.09")));
        assertThat(replaced.content().certificates()).extracting(ResumeContent.Certificate::name).containsExactly("ADsP");

        // 학력 필드(school)를 자격증 섹션에 넣으면 조용히 버리지 않고 거부한다.
        assertThatThrownBy(() -> service.addRow(resume.getId(), ResumeSection.certificates, row(Map.of("school", "x"))))
                .isInstanceOf(ApiRuleException.class).hasMessageContaining("certificates");
        assertThatThrownBy(() -> service.deleteRow(resume.getId(), ResumeSection.certificates, 5))
                .isInstanceOf(ApiRuleException.class);

        ResumeResponse removed = service.deleteRow(resume.getId(), ResumeSection.certificates, 0);
        assertThat(removed.content().certificates()).isEmpty();
    }

    @Test
    void updateBasicKeepsOtherSections() {
        saveReturnsArgument();
        Resume resume = new Resume(OWNER_ID, "v1", "{\"basic\":{},\"skills\":[{\"name\":\"Python\"}]}");
        resume.onCreate();
        given(repository.findByIdAndOwnerId(resume.getId(), OWNER_ID)).willReturn(Optional.of(resume));

        ResumeResponse updated = service.updateBasic(resume.getId(),
                new ResumeContent.BasicInfo("테스트", null, null, null, null, null, "https://example.com/gh"));

        assertThat(updated.content().basic().name()).isEqualTo("테스트");
        assertThat(updated.content().skills()).extracting(ResumeContent.Skill::name).containsExactly("Python");
    }

    /** 다른 계정 소유의 이력서는 존재를 알리지 않고 404. */
    @Test
    void otherOwnersResumeIsNotReachable() {
        UUID id = UUID.randomUUID();
        given(repository.findByIdAndOwnerId(id, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.copy(id, new ResumeCopyRequest("x"))).isInstanceOf(ResourceNotFoundException.class);
    }
}
