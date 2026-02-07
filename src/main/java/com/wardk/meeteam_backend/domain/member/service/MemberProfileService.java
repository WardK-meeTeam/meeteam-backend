package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberProfileService {

    MemberProfileResponse profile(Long memberId);

    MemberProfileUpdateResponse updateProfile(Long memberId, MemberProfileUpdateRequest request, MultipartFile profileImage);

    List<MemberCardResponse> getAllMemberCards();

    Page<MemberCardResponse> searchMembers(MemberSearchRequest searchRequest, Pageable pageable);
}
