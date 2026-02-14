package com.wardk.meeteam_backend.web.member.controller;

import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;



import com.wardk.meeteam_backend.domain.member.service.MemberProfileService;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관련 API")
public class MemberController {


    private final MemberProfileService memberProfileService;

    /**
     * 나의 프로필보기에서 나의 memberId;
     */

    @Operation(summary = "나의 프로필 보기", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("api/members")
    public SuccessResponse<MemberProfileResponse> getMember(
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
            ) {

        MemberProfileResponse profile = memberProfileService.profile(userDetails.getMemberId());

        return SuccessResponse.onSuccess(profile);
    }

    @PutMapping(value = "/api/members", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "나의 프로필 수정", description = "현재 로그인한 회원의 프로필 정보를 수정합니다.")
    public SuccessResponse<MemberProfileUpdateResponse> updateMember(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomSecurityUserDetails userDetails,
            @RequestPart("memberInfo") @Valid MemberProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        MemberProfileUpdateResponse response = memberProfileService.updateProfile(
                userDetails.getMemberId(),
                request,
                profileImage
        );
        return SuccessResponse.onSuccess(response);
    }



    @Operation(summary = "특정 사용자 프로필 보기" , description = "특정 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("api/members/{memberId}")
    public SuccessResponse<MemberProfileResponse> getMember(
            @PathVariable Long memberId
    ) {

        MemberProfileResponse profile = memberProfileService.profile(memberId);

        return SuccessResponse.onSuccess(profile);
    }

    @Operation(summary = "메인 페이지 사용자 카드 조회 " , description = "메인 페이지에서 팀을 구해요 에서 전체 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("api/members/all")
    public SuccessResponse<List<MemberCardResponse>> getAllMembers() {
        List<MemberCardResponse> cards = memberProfileService.getAllMemberCards();

        return SuccessResponse.onSuccess(cards);
    }


    @Operation(summary = "사용자 조건 검색", description = "사용자 이름, 플랫폼, 분야, 기술 스택 등 다양한 조건으로 사용자를 검색합니다.")
    @GetMapping("api/members/search")
    public SuccessResponse<Page<MemberCardResponse>> searchMembers (
            @RequestParam(value = "jobFieldIds", required = false) List<Long> jobFieldIds,
            @RequestParam(value = "skillIds", required = false) List<Long> skillIds,
            @ParameterObject Pageable pageable
            ) {

        MemberSearchRequest searchRequest = new MemberSearchRequest();
        searchRequest.setJobFieldIds(jobFieldIds);
        searchRequest.setSkillIds(skillIds);

        Page<MemberCardResponse> searchResults = memberProfileService.searchMembers(searchRequest, pageable);
        return SuccessResponse.onSuccess(searchResults);
    }
}
