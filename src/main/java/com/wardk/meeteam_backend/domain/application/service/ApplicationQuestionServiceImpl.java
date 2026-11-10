package com.wardk.meeteam_backend.domain.application.service;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationQuestion;
import com.wardk.meeteam_backend.domain.application.repository.ApplicationQuestionRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.application.dto.request.ApplicationQuestionSaveRequest;
import com.wardk.meeteam_backend.web.application.dto.response.ApplicationQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 지원서 양식(질문) 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationQuestionServiceImpl implements ApplicationQuestionService {

    private final ProjectRepository projectRepository;
    private final ApplicationQuestionRepository applicationQuestionRepository;

    @Override
    public List<ApplicationQuestionResponse> saveQuestions(Long projectId, String requesterEmail,
                                                           ApplicationQuestionSaveRequest request) {
        // 1단계: 프로젝트 조회 및 리더 권한 검증
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.APPLICATION_QUESTION_FORBIDDEN);
        }

        // 2단계: 기존 양식 전체 교체
        applicationQuestionRepository.deleteByProjectId(projectId);

        // 3단계: 배열 순서를 displayOrder 로 하여 저장
        List<ApplicationQuestion> questions = IntStream.range(0, request.questions().size())
                .mapToObj(index -> {
                    ApplicationQuestionSaveRequest.QuestionItem item = request.questions().get(index);
                    return ApplicationQuestion.of(
                            project,
                            item.label(),
                            item.inputType(),
                            item.required(),
                            item.options(),
                            index
                    );
                })
                .toList();

        List<ApplicationQuestion> saved = applicationQuestionRepository.saveAll(questions);

        log.info("지원서 양식 저장 완료 - projectId: {}, questionCount: {}", projectId, saved.size());

        return saved.stream()
                .map(ApplicationQuestionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationQuestionResponse> getQuestions(Long projectId) {
        return applicationQuestionRepository.findByProjectIdOrderByDisplayOrderAsc(projectId).stream()
                .map(ApplicationQuestionResponse::from)
                .toList();
    }
}