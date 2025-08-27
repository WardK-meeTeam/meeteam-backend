package com.wardk.meeteam_backend.global.auth.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.MemberSkillRepository;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.util.FileUtil;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.MemberSubCategoryRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final FileUtil fileUtil;
    private final SubCategoryRepository subCategoryRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final MemberSkillRepository memberSkillRepository;
    private final SkillRepository skillRepository;


    @Transactional
    public String register(RegisterRequest registerRequest, MultipartFile filed) {

        // 이메일 중복 불가능
        memberRepository.findOptionByEmail(registerRequest.getEmail())
                .ifPresent(email -> {
                    throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
                });

        String storeFileName = null;
        MultipartFile file = filed;
        if (file != null && !file.isEmpty()) {
            storeFileName = fileUtil.storeFile(file).getStoreFileName();
        }

        Member member = Member.builder()
                .realName(registerRequest.getName())
                .age(registerRequest.getAge())
                .gender(registerRequest.getGender())
                .email(registerRequest.getEmail())
                .password(bCryptPasswordEncoder.encode(registerRequest.getPassword()))
                .storeFileName(storeFileName)
                .introduction(registerRequest.getIntroduce())
                .isParticipating(true)
                .role(UserRole.USER)
                .build();


        memberRepository.save(member);

        registerRequest.getSubCategories().stream().
                forEach(e -> {
                    SubCategory subCategory = subCategoryRepository.findByName(e.getSubcategory())
                            .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));
                    member.addSubCategory(subCategory);
                });


        registerRequest.getSkills().stream()
                .forEach(e -> {
                    Skill skill = skillRepository.findBySkillName(e.getSkillName())
                            .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUND));
                    member.addMemberSkill(skill);
                });

        return registerRequest.getName();

    }


}
