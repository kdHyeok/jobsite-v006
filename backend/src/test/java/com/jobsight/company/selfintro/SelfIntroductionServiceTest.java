package com.jobsight.company.selfintro;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.resume.Resume;
import com.jobsight.company.resume.ResumeRepository;
import com.jobsight.company.selfintro.dto.SelfIntroductionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class SelfIntroductionServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESUME_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Mock SelfIntroductionRepository repository;
    @Mock ResumeRepository resumeRepository;
    @Mock CurrentUser currentUser;
    SelfIntroductionService service;

    @BeforeEach
    void setUp() {
        service = new SelfIntroductionService(repository, resumeRepository, currentUser);
    }

    @Test
    void bm25SearchesQuestionAndAnswerAndRanksFrequency() {
        SelfIntroduction frequent = value("협업 경험", "협업 과정에서 협업 규칙을 정했습니다.");
        SelfIntroduction once = value("문제 해결", "협업으로 해결했습니다.");
        SelfIntroduction absent = value("성장 과정", "꾸준히 학습했습니다.");

        assertThat(SelfIntroductionService.rank(List.of(once, absent, frequent), "협업"))
                .containsExactly(frequent, once);
    }

    @Test
    void createRequiresOwnedResumeAndNormalizesAnswer() {
        given(currentUser.id()).willReturn(OWNER_ID);
        Resume resume = new Resume(OWNER_ID, "지원용", "{}");
        given(resumeRepository.findAllByIdInAndOwnerId(anyList(), org.mockito.ArgumentMatchers.eq(OWNER_ID)))
                .willReturn(List.of(resume));
        given(repository.save(any())).willAnswer(invocation -> {
            SelfIntroduction value = invocation.getArgument(0);
            value.onCreate();
            return value;
        });

        var created = service.create(new SelfIntroductionRequest(List.of(RESUME_ID), "  지원 동기는? ", "  답변  "));

        assertThat(created.question()).isEqualTo("지원 동기는?");
        assertThat(created.answer()).isEqualTo("답변");
    }

    private static SelfIntroduction value(String question, String answer) {
        SelfIntroduction value = new SelfIntroduction(OWNER_ID, Set.of(RESUME_ID), question, answer);
        value.onCreate();
        return value;
    }
}
