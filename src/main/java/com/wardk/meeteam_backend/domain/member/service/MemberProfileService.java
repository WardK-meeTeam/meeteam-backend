package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;

public interface MemberProfileService {

    MemberProfileResponse profile(Long memberId);
}
