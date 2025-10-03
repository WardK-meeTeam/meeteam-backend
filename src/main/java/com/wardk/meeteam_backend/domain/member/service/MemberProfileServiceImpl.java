package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.member.dto.*;
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
    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
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

//        List<ReviewResponse> reviewResponses = reviewRepositoryCustom.getReview(member.getId());
//        memberProfileResponse.setReviewList(reviewResponses);
//        memberProfileResponse.setReviewCount(reviewResponses.size());

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
        updateMemberSubCategories(member, request.getSubCategories());

        // 기존 기술 스택 삭제 후 새로 추가
        updateMemberSkills(member, request.getSkills());

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
     * 회원 검색 (조건에 따라 적절한 쿼리 선택)
     */
    @Override
    @Transactional(readOnly = true)
    public List<MemberCardResponse> searchMembers(MemberSearchRequest searchRequest, Pageable pageable) {

        Page<Member> memberPage;

        // 조건에 따라 적절한 쿼리 선택
        boolean hasBigCategories = searchRequest.getBigCategories() != null && !searchRequest.getBigCategories().isEmpty();
        boolean hasSkills = searchRequest.getSkillList() != null && !searchRequest.getSkillList().isEmpty();

        if (hasBigCategories && hasSkills) {
            log.info("대분류와 기술스택 모두로 검색");
            memberPage = memberRepository.findByBigCategoriesAndSkills(
                    searchRequest.getBigCategories(),
                    searchRequest.getSkillList(),
                    pageable
            );
        } else if (hasBigCategories) {
            log.info("대분류로만 검색");
            memberPage = memberRepository.findByBigCategories(
                    searchRequest.getBigCategories(),
                    pageable
            );
        } else if (hasSkills) {
            log.info("기술스택으로만 검색");
            memberPage = memberRepository.findBySkills(
                    searchRequest.getSkillList(),
                    pageable
            );
        } else {
            log.info("조건 없음 - 전체 회원 조회");
            memberPage = memberRepository.findAll(pageable);
        }

        List<Member> members = memberPage.getContent();

        log.info("검색 결과 - 총 {}개 회원 조회됨", members.size());
        log.info("페이지 정보 - 총 {}페이지 중 {}페이지, 총 {}개 회원",
                memberPage.getTotalPages(), memberPage.getNumber() + 1, memberPage.getTotalElements());

        return members.stream()
                .map(MemberCardResponse::responseToDto)
                .toList();
    }
    /**
     * 회원 관심 분야 업데이트
     */
    private void updateMemberSubCategories(Member member, List<String> subCategoryNames) {
        member.getSubCategories().clear();

        List<SubCategory> subCategories = subCategoryRepository.findByNameIn(subCategoryNames);

        if (subCategories.size() != subCategoryNames.size()) {
            throw new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND);
        }

        for (SubCategory subCategory : subCategories) {
            member.addSubCategory(subCategory);
        }
    }

    /**
     * 회원 기술 스택 업데이트
     */
    private void updateMemberSkills(Member member, List<String> skillNames) {
        member.getMemberSkills().clear();

        List<Skill> skills = skillRepository.findBySkillNameIn(skillNames);

        if (skills.size() != skillNames.size()) {
            throw new CustomException(ErrorCode.SKILL_NOT_FOUND);
        }

        for (Skill skill : skills) {
            member.addMemberSkill(skill);
        }
    }


}
