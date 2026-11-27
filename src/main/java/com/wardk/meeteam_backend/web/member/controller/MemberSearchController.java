package com.wardk.meeteam_backend.web.member.controller;

import com.wardk.meeteam_backend.domain.member.search.MemberTechStackSearchService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.mainpage.dto.response.MemberCardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 기술스택 기반 팀원 검색 API.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관련 API")
public class MemberSearchController {

    private final MemberTechStackSearchService memberTechStackSearchService;

    @Operation(
            summary = "기술스택으로 팀원 찾기 (OR 매칭)",
            description = "요청한 기술스택 중 하나라도 보유한 팀원을 검색합니다. " +
                    "모든 스택을 보유해야 하는 기존 팀원 찾기(/api/v1/members/search)와 달리 OR 조건으로 매칭합니다. " +
                    "정렬은 프로젝트 경험 많은 순(projectCount desc)으로 고정이며 sort 파라미터는 무시됩니다."
    )
    @GetMapping("/api/v1/members/search/tech-stacks")
    public SuccessResponse<Page<MemberCardResponse>> searchMembersHavingAnyTechStack(
            @Parameter(description = "기술스택 이름 목록 (예: React,Spring) — 하나라도 보유하면 매칭")
            @RequestParam("techStackNames") List<String> techStackNames,

            @ParameterObject Pageable pageable
    ) {
        Page<MemberCardResponse> results =
                memberTechStackSearchService.searchMembersHavingAnyTechStack(techStackNames, pageable);
        return SuccessResponse.onSuccess(results);
    }
}