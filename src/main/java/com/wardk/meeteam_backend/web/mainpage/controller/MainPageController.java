package com.wardk.meeteam_backend.web.mainpage.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.mainpage.dto.MainPageProjectDto;
import com.wardk.meeteam_backend.web.mainpage.dto.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "mainpage-controller", description = "메인페이지 API")
@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainPageController {

    private final ProjectService projectService;

    // 대분류별(sub-category 별)
    @Operation(summary = "대분류별 프로젝트 목록 조회", description = "대분류 필터링을 통한 모집중인 프로젝트 목록을 무한 스크롤로 조회합니다.")
    @GetMapping("/projects")
    public ResponseEntity<SliceResponse<MainPageProjectDto>> getMainPageProjects(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            // 한 번에 가져올 데이터 개수
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,

            // 정렬할 필드명 (기본값: "createdAt" - 생성일시)
            @Parameter(description = "정렬 기준", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,

            // 정렬 방향 (기본값: "desc" - 내림차순)
            @Parameter(description = "정렬 방향", example = "desc")
            @RequestParam(defaultValue = "desc") String direction,

            @Parameter(description = "대분류 ID 목록 (필수)", example = "[3, 4]", required = true)
            @RequestParam List<Long> bigCategoryIds) {

        // 파라미터 검증
        if (page < 0 || size <= 0 || size > 50) {
            throw new CustomException(ErrorCode.MAIN_PAGE_INVALID_PAGINATION);
        }

        // createdAt 기준으로 정렬 파라미터 검증 수정
        if (!Arrays.asList("createdAt", "name").contains(sort)) {
            throw new CustomException(ErrorCode.MAIN_PAGE_SORT_PARAMETER_INVALID);
        }

        // 유효하지 않은 ID(0이하) 또는 빈 목록/과다 목록 방어
        if (bigCategoryIds == null || bigCategoryIds.isEmpty()
                || bigCategoryIds.size() > 50
                || bigCategoryIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new CustomException(ErrorCode.MAIN_PAGE_CATEGORY_NOT_FOUND);
        }

        // direction이 "desc"이면 DESC, 아니면 ASC , equalsIgnoreCase(): 대소문자 구분 없이 비교
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        SliceResponse<MainPageProjectDto> response = projectService.getRecruitingProjectsByCategory(bigCategoryIds, pageable);

        return ResponseEntity.ok(response);
    }
}