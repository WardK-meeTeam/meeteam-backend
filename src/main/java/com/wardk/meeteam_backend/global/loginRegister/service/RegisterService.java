package com.wardk.meeteam_backend.global.loginRegister.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.MemberSubCategory;
import com.wardk.meeteam_backend.domain.member.entity.SignupSubCategory;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.skill.entity.MemberSkill;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.MemberSkillRepository;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.FileStore;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.RegisterRequestDto;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.SubCategoryDto;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.MemberSubCategoryRepository;
import com.wardk.meeteam_backend.domain.member.repository.SignupSubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final FileStore fileStore;
    private final SignupSubCategoryRepository signupSubCategoryRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final MemberSkillRepository memberSkillRepository;
    private final SkillRepository skillRepository;


    @Transactional
    public String register(RegisterRequestDto registerRequestDto, MultipartFile filed) {

        // 이메일 중복 불가능
        memberRepository.findOptionByEmail(registerRequestDto.getEmail())
                .ifPresent(email -> {
                    throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
                });

        String storeFileName = null;
        MultipartFile file = filed;
        if (file != null && !file.isEmpty()) {
            storeFileName = fileStore.storeFile(file).getStoreFileName();
        }

        Member member = Member.builder()
                .realName(registerRequestDto.getName())
                .age(registerRequestDto.getAge())
                .gender(registerRequestDto.getGender())
                .email(registerRequestDto.getEmail())
                .password(bCryptPasswordEncoder.encode(registerRequestDto.getPassword()))
                .storeFileName(storeFileName)
                .role(UserRole.USER)
                .build();

        memberRepository.save(member);

        registerRequestDto.getSubCategories().stream().
                forEach(e -> {
                    SignupSubCategory subCategory = signupSubCategoryRepository.findBySubCategory(e.getSubcategory())
                            .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));
                    member.addSubCategory(subCategory);
                });


        registerRequestDto.getSkills().stream()
                .forEach(e -> {
                    Skill skill = skillRepository.findBySkillName(e.getSkillName())
                            .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUNT));
                    member.addMemberSkill(skill);
                });

        return registerRequestDto.getName();

    }


}
