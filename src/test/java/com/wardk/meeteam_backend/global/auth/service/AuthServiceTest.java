package com.wardk.meeteam_backend.global.auth.service;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.MemberSubCategoryRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.MemberSkillRepository;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.global.auth.dto.register.SkillDto;
import com.wardk.meeteam_backend.global.auth.dto.register.SubCategoryDto;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.util.FileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    BCryptPasswordEncoder passwordEncoder;

    // storeFile().getStoreFileName() 때문에 RETURNS_DEEP_STUBS 사용
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    FileUtil fileUtil;

    @Mock
    SubCategoryRepository subCategoryRepository;
    @Mock
    MemberSubCategoryRepository memberSubCategoryRepository;
    @Mock
    MemberSkillRepository memberSkillRepository;
    @Mock
    SkillRepository skillRepository;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("중복 이메일 오류 발생")
    void register_duplicateEmail() {
        // given
        RegisterRequest req = buildRequest(
                "홍길동", 20, "test@naver.com", Gender.MALE, "password", "introduce", List.of(), List.of()
        );

        Mockito.when(memberRepository.findOptionByEmail(req.getEmail()))
                .thenReturn(Optional.of(Mockito.mock(Member.class)));

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.register(req, null));

        // then
        assertEquals(ErrorCode.DUPLICATE_MEMBER, ex.getErrorCode());

        Mockito.verify(memberRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(fileUtil, Mockito.never()).storeFile(Mockito.any());
        Mockito.verify(subCategoryRepository, Mockito.never()).findByName(Mockito.anyString());
        Mockito.verify(skillRepository, Mockito.never()).findBySkillName(Mockito.anyString());
    }

    @Test
    @DisplayName("회원가입 성공 - 이미지X")
    void register_success() {
        // given
        RegisterRequest req = buildRequest(
                "홍길동", 20, "test@naver.com", Gender.MALE, "password", "introduce",
                List.of("웹서버"), List.of("Java")
        );

        // 이메일 중복 없음
        Mockito.when(memberRepository.findOptionByEmail(req.getEmail()))
                .thenReturn(Optional.empty());

        Mockito.when(passwordEncoder.encode(req.getPassword()))
                .thenReturn("encodedPassword");

        Mockito.when(memberRepository.save(Mockito.any(Member.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubCategory subCategory = Mockito.mock(SubCategory.class);
        Mockito.when(subCategory.getName())
                .thenReturn("웹서버");

        Mockito.when(subCategoryRepository.findByName("웹서버"))
                .thenReturn(Optional.of(subCategory));

        Skill skill = Mockito.mock(Skill.class);
        Mockito.when(skill.getSkillName())
                .thenReturn("Java");

        Mockito.when(skillRepository.findBySkillName("Java"))
                .thenReturn(Optional.of(skill));

        // when
        String result = authService.register(req, null);

        // then
        assertEquals(req.getName(), result);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        Mockito.verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();

        assertEquals(req.getEmail(), saved.getEmail());
        assertEquals("encodedPassword", saved.getPassword());
        assertNull(saved.getStoreFileName());
        assertEquals(1, saved.getSubCategories().size());
        assertEquals(1, saved.getMemberSkills().size());
        assertEquals("웹서버", saved.getSubCategories().get(0).getSubCategory().getName());
        assertEquals("Java", saved.getMemberSkills().get(0).getSkill().getSkillName());

        Mockito.verify(memberRepository).findOptionByEmail(req.getEmail());
        Mockito.verify(passwordEncoder).encode(req.getPassword());
        Mockito.verify(subCategoryRepository).findByName("웹서버");
        Mockito.verify(skillRepository).findBySkillName("Java");
        
        Mockito.verify(fileUtil, Mockito.never()).storeFile(Mockito.any());
    }

    private RegisterRequest buildRequest(
            String name, Integer age, String email, Gender gender, String password, String introduce,
            List<String> subCategoryNames, List<String> skillNames
    ) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setAge(age);
        req.setEmail(email);
        req.setGender(gender);
        req.setPassword(password);
        req.setIntroduce(introduce);

        List<SubCategoryDto> subCategories = subCategoryNames.stream()
                .map(n -> {
                    SubCategoryDto dto = new SubCategoryDto();
                    dto.setSubcategory(n);
                    return dto;
                }).toList();

        List<SkillDto> skills = skillNames.stream()
                .map(n -> {
                    SkillDto dto = new SkillDto();
                    dto.setSkillName(n);
                    return dto;
                }).toList();

        req.setSubCategories(subCategories);
        req.setSkills(skills);

        return req;
    }
}