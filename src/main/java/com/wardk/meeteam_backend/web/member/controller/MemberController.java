package com.wardk.meeteam_backend.web.member.controller;

import io.swagger.v3.oas.annotations.Operation;



import com.wardk.meeteam_backend.domain.member.service.MemberProfileService;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {


    private final MemberProfileService memberProfileService;



    @Operation(summary = "나의 프로필 보기", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("api/members")
    public SuccessResponse<MemberProfileResponse> getMember(
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
            ) {

        MemberProfileResponse profile = memberProfileService.profile(userDetails.getMemberId());

        return SuccessResponse.onSuccess(profile);
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
