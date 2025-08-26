package com.wardk.meeteam_backend.domain.mainpage.service;

import com.wardk.meeteam_backend.web.mainpage.dto.MainPageProjectDto;
import com.wardk.meeteam_backend.web.mainpage.dto.SliceResponse;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface MainPageService {

    /*
      대분류별 모집중인 프로젝트 조회
      @param bigCategoryIds 대분류 ID 리스트 (필수)
      @param pageable 페이징 정보
      @return 프로젝트 목록 응답
     */
    SliceResponse<MainPageProjectDto> getRecruitingProjectsByCategory(List<Long> bigCategoryIds, Pageable pageable);
}