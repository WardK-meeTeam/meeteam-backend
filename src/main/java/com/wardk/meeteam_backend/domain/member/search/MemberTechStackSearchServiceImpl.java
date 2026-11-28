package com.wardk.meeteam_backend.domain.member.search;

import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import com.wardk.meeteam_backend.domain.job.repository.TechStackRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.mainpage.dto.response.MemberCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 기술스택 기반 팀원 검색 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberTechStackSearchServiceImpl implements MemberTechStackSearchService {

    private final TechStackRepository techStackRepository;
    private final MemberTechStackSearchRepository memberTechStackSearchRepository;

    /**
     * 요청한 기술스택 이름 중 하나라도 보유한 회원을 검색한다 (OR 매칭).
     * <p>
     * 기술스택 이름은 ID로 선변환하여 검색 쿼리에서 tech_stack 조인을 제거한다 (v1 검색과 동일한 방침).
     * 존재하지 않는 이름의 처리는 AND 검색과 다르다 — AND에서는 하나라도 없으면 결과가
     * 있을 수 없어 빈 페이지를 반환하지만, OR에서는 없는 이름이 결과를 좁히지 않으므로
     * 무시하고 나머지 이름으로 검색한다. 전부 존재하지 않을 때만 빈 페이지를 반환한다.
     */
    @Override
    public Page<MemberCardResponse> searchMembersHavingAnyTechStack(List<String> techStackNames, Pageable pageable) {
        if (techStackNames == null || techStackNames.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        List<Long> techStackIds = resolveExistingTechStackIds(techStackNames);
        if (techStackIds.isEmpty()) {
            log.info("기술스택 OR 검색 - 존재하는 기술스택 없음, 빈 결과 반환. 요청: {}", techStackNames);
            return Page.empty(pageable);
        }

        Page<Member> memberPage = memberTechStackSearchRepository
                .findMembersHavingAnyTechStack(techStackIds, pageable);

        log.info("기술스택 OR 검색 - techStacks: {}, 총 {}명", techStackNames, memberPage.getTotalElements());

        return memberPage.map(MemberCardResponse::from);
    }

    /**
     * 기술스택 이름 목록에서 실제 존재하는 기술스택의 ID만 추려낸다. 이름 중복은 제거한다.
     */
    private List<Long> resolveExistingTechStackIds(List<String> techStackNames) {
        Set<String> distinctNames = Set.copyOf(techStackNames);
        return techStackRepository.findByNameIn(distinctNames).stream()
                .map(TechStack::getId)
                .toList();
    }
}