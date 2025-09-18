package com.wardk.meeteam_backend.web.member.controller;



import com.wardk.meeteam_backend.domain.member.service.MemberProfileService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {


    private final MemberProfileService memberProfileService;

    @GetMapping("api/members/{memberId}")
    public SuccessResponse<MemberProfileResponse> getMember(@PathVariable Long memberId) {

        MemberProfileResponse profile = memberProfileService.profile(memberId);

        return SuccessResponse.onSuccess(profile);
    }
}
