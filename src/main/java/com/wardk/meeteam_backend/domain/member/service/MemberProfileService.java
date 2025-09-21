package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileUpdateRequest;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MemberProfileService {

    MemberProfileResponse profile(Long memberId);

    MemberProfileUpdateResponse updateProfile(Long memberId, MemberProfileUpdateRequest request, MultipartFile profileImage);
}
