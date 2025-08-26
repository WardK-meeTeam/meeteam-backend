package com.wardk.meeteam_backend.domain.mainpage.service;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.mainpage.dto.MainPageProjectDto;
import com.wardk.meeteam_backend.web.mainpage.dto.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainPageServiceImpl implements MainPageService {

    private final ProjectRepository projectRepository;

    @Override
    public SliceResponse<MainPageProjectDto> getRecruitingProjectsByCategory(List<Long> bigCategoryIds, Pageable pageable) {
        // 대분류 파라미터 필수 검증
        if (bigCategoryIds == null || bigCategoryIds.isEmpty()) {
            throw new CustomException(ErrorCode.MAIN_PAGE_CATEGORY_NOT_FOUND);
        }

        // 대분류별 + 모집중 상태 프로젝트 조회
        Slice<Project> projectSlice = projectRepository.findRecruitingProjectsByBigCategories(
                bigCategoryIds,
                Recruitment.RECRUITING,  // Enum 직접 전달
                pageable
        );

        // DTO 변환 (기존 ProjectListResponse 방식 참고)
        List<MainPageProjectDto> dtoList = projectSlice.getContent().stream()
                .map(MainPageProjectDto::responseDto)  // 정적 메서드 사용
                .collect(Collectors.toList());

        return SliceResponse.of(dtoList, projectSlice.hasNext(), projectSlice.getNumber());
    }
}
