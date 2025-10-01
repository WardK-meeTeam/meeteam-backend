package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.web.member.dto.MemberCardResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileUpdateRequest;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileUpdateResponse;
import com.wardk.meeteam_backend.web.member.dto.MemberSearchRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberProfileService {

    MemberProfileResponse profile(Long memberId);

    MemberProfileUpdateResponse updateProfile(Long memberId, MemberProfileUpdateRequest request, MultipartFile profileImage);

    List<MemberCardResponse> getAllMemberCards();

    List<MemberCardResponse> searchMembers(MemberSearchRequest searchRequest, Pageable pageable);
}
