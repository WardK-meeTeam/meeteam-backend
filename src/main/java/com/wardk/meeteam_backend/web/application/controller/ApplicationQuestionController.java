package com.wardk.meeteam_backend.web.application.controller;

import com.wardk.meeteam_backend.domain.application.service.ApplicationQuestionService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.application.dto.request.ApplicationQuestionSaveRequest;
import com.wardk.meeteam_backend.web.application.dto.response.ApplicationQuestionResponse;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "지원서 양식", description = "리더가 정의하는 동적 지원서 양식(질문) 관련 API")
@RestController
@RequiredArgsConstructor
public class ApplicationQuestionController {

    private final ApplicationQuestionService applicationQuestionService;

    @Operation(summary = "지원서 양식 저장", description = "프로젝트 리더가 지원서 질문 목록을 저장(전체 교체)합니다.")
    @PutMapping("/api/v1/projects/{projectId}/application/questions")
    public SuccessResponse<List<ApplicationQuestionResponse>> saveQuestions(
            @PathVariable Long projectId,
            @RequestBody @Validated ApplicationQuestionSaveRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<ApplicationQuestionResponse> response =
                applicationQuestionService.saveQuestions(projectId, userDetails.getUsername(), request);

        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "지원서 양식 조회", description = "지원 페이지 렌더링을 위해 프로젝트의 지원서 질문 목록을 조회합니다.")
    @GetMapping("/api/v1/projects/{projectId}/application/questions")
    public SuccessResponse<List<ApplicationQuestionResponse>> getQuestions(
            @PathVariable Long projectId) {

        List<ApplicationQuestionResponse> response = applicationQuestionService.getQuestions(projectId);

        return SuccessResponse.onSuccess(response);
    }
}