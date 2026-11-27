package com.wardk.meeteam_backend.domain.member.search;

import com.wardk.meeteam_backend.web.mainpage.dto.response.MemberCardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 기술스택 기반 팀원 검색 서비스.
 */
public interface MemberTechStackSearchService {

    /**
     * 요청한 기술스택 이름 중 하나라도 보유한 회원을 검색한다 (OR 매칭).
     * 정렬은 projectCount 내림차순으로 고정이다 (검색 투영 테이블이 그 순서로만 존재).
     *
     * @param techStackNames 기술스택 이름 목록 (예: React, Spring) — 비어 있으면 안 됨
     */
    Page<MemberCardResponse> searchMembersHavingAnyTechStack(List<String> techStackNames, Pageable pageable);
}