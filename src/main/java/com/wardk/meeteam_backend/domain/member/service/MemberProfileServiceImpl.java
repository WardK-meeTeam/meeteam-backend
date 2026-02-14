package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import com.wardk.meeteam_backend.domain.job.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.job.repository.TechStackRepository;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.member.dto.request.*;
import com.wardk.meeteam_backend.web.member.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepository memberRepository;
    private final JobPositionRepository jobPositionRepository;
    private final TechStackRepository techStackRepository;
    private final S3FileService s3FileService;

    /**
     * 나의 프로필 조회 (프로필 사진 분기처리 포함)
     */
    @Override
    @Transactional(readOnly = true)
    public MemberProfileResponse profile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        MemberProfileResponse memberProfileResponse = new MemberProfileResponse(member, memberId);

        // 프로필 사진 처리
        if (member.getStoreFileName() != null && !member.getStoreFileName().trim().isEmpty()) {
            memberProfileResponse.setProfileImageUrl(member.getStoreFileName());
            memberProfileResponse.setProfileImageName("profile.jpg");
            log.debug("프로필 사진 있음: {}", member.getStoreFileName());
        } else {
            memberProfileResponse.setProfileImageUrl(null);
            memberProfileResponse.setProfileImageName(null);
            log.debug("프로필 사진 없음 - 회원 ID: {}", memberId);
        }



        return memberProfileResponse;
    }

    /**
     * 회원 프로필 수정 (프로필 이미지 포함)
     */
    @Override
    @Transactional
    public MemberProfileUpdateResponse updateProfile(Long memberId, MemberProfileUpdateRequest request, MultipartFile profileImage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 프로필 사진 처리 (새 사진이 업로드된 경우에만)
        if (profileImage != null && !profileImage.isEmpty()) {
            log.info("새 프로필 사진 업로드 요청 - 파일명: {}, 크기: {}",
                    profileImage.getOriginalFilename(), profileImage.getSize());

            // 기존 프로필 사진 삭제 (있다면)
            if (member.getStoreFileName() != null && !member.getStoreFileName().trim().isEmpty()) {
                try {
                    s3FileService.deleteFile(member.getStoreFileName());
                    log.info("기존 프로필 사진 삭제 완료: {}", member.getStoreFileName());
                } catch (Exception e) {
                    log.warn("기존 프로필 사진 삭제 실패: {}", e.getMessage());
                }
            }

            // 새 프로필 사진 S3 업로드
            String profileImageUrl = s3FileService.uploadFile(profileImage, "profiles", memberId);
            member.setStoreFileName(profileImageUrl);
            log.info("새 프로필 사진 업로드 완료: {}", profileImageUrl);
        } else {
            log.debug("프로필 사진 변경 없음 - 기존 사진 유지");
        }

        // 기본 정보 수정
        member.setRealName(request.getName());
        member.setAge(request.getAge());
        member.setGender(request.getGender());
        member.setIsParticipating(request.getIsParticipating());
        member.setIntroduction(request.getIntroduction());

        // 기존 관심 분야 삭제 후 새로 추가
        updateMemberJobPositions(member, request.getJobPositionIds());

        // 기존 기술 스택 삭제 후 새로 추가
        updateMemberTechStacks(member, request.getTechStackIds());

        // 변경 사항 저장 (더티 체킹으로 자동 업데이트)
        Member savedMember = memberRepository.save(member);

        return new MemberProfileUpdateResponse(savedMember);
    }


    @Override
    @Transactional(readOnly = true)
    public List<MemberCardResponse> getAllMemberCards() {

        List<Member> members = memberRepository.findAll();

        if (members.isEmpty()) {
            log.info("=== 조회된 회원이 없습니다. ===");
            return List.of();
        }

        return members.stream()
                .map(MemberCardResponse::responseToDto)
                .toList();
    }

    /**
     * 회원 검색 (QueryDSL을 사용한 동적 쿼리 및 정렬)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MemberCardResponse> searchMembers(MemberSearchRequest searchRequest, Pageable pageable) {
        // QueryDSL로 조회
        Page<Member> memberPage = memberRepository.searchMembers(
                searchRequest.getJobFieldIds(),
                searchRequest.getSkillIds(),
                pageable
        );

        log.info("검색 결과 - 총 {}개 회원 조회됨", memberPage.getTotalElements());
        log.info("페이지 정보 - 총 {}페이지 중 {}페이지, 총 {}개 회원",
                memberPage.getTotalPages(), memberPage.getNumber() + 1, memberPage.getTotalElements());

        return memberPage.map(MemberCardResponse::responseToDto);
    }
    /**
     * 회원 관심 분야 업데이트
     */
    private void updateMemberJobPositions(Member member, List<Long> jobPositionIds) {
        member.getJobPositions().clear();

        if (jobPositionIds == null || jobPositionIds.isEmpty()) {
            return;
        }

        List<JobPosition> jobPositions = jobPositionRepository.findAllById(jobPositionIds);
        if (jobPositions.size() != jobPositionIds.size()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        for (JobPosition jobPosition : jobPositions) {
            member.addJobPosition(jobPosition);
        }
    }

    /**
     * 회원 기술 스택 업데이트
     */
    private void updateMemberTechStacks(Member member, List<Long> techStackIds) {
        member.getMemberTechStacks().clear();

        List<TechStack> techStacks = techStackRepository.findAllById(techStackIds);
        if (techStacks.size() != techStackIds.size()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        for (TechStack techStack : techStacks) {
            member.addMemberTechStack(techStack);
        }
    }


}
