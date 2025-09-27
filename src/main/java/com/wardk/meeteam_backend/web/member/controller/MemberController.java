package com.wardk.meeteam_backend.web.member.controller;

import io.swagger.v3.oas.annotations.Operation;



import com.wardk.meeteam_backend.domain.member.service.MemberProfileService;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileUpdateRequest;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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




}
