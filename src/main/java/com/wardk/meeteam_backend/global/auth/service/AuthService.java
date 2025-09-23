package com.wardk.meeteam_backend.global.auth.service;


import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.MemberSkillRepository;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.auth.dto.EmailDuplicateResponse;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterDescriptionRequest;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterResponse;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.MemberSubCategoryRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
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
//    private final FileUtil fileUtil;
    private final S3FileService s3FileService;
    private final SubCategoryRepository subCategoryRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final MemberSkillRepository memberSkillRepository;
    private final SkillRepository skillRepository;
    private final JwtUtil jwtUtil;


    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest, MultipartFile filed) {

        // 이메일 중복 불가능
        memberRepository.findOptionByEmail(registerRequest.getEmail())
                .ifPresent(email -> {
                    throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
                });

        String storeFileName = null;
        if (filed != null && !filed.isEmpty()) {
            storeFileName = s3FileService.uploadFile(filed, "images");
        }

        Member member = Member.builder()
                .realName(registerRequest.getName())
                .age(registerRequest.getAge())
                .gender(registerRequest.getGender())
                .email(registerRequest.getEmail())
                .password(bCryptPasswordEncoder.encode(registerRequest.getPassword()))
                .storeFileName(storeFileName)
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

        return new RegisterResponse(registerRequest.getName(), member.getId());

    }


    @Transactional
    public EmailDuplicateResponse checkDuplicateEmail(String email) {

        Boolean exists = memberRepository.existsByEmail(email);

        String message = exists ? "이미 존재하는 이메일 입니다" : "사용 가능한 이메일 입니다.";

        return new EmailDuplicateResponse(exists, message);
    }

    @Transactional
    public RegisterResponse registDesciption(Long memberId, RegisterDescriptionRequest introduction) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));


        member.setIntroduction(introduction.getIntroduce());

        return new RegisterResponse(member.getRealName(), member.getId());
    }

    /**
     * Refresh Token을 이용하여 새로운 Access Token을 발급받는 메서드
     * Access Token이 만료된 상황에서만 새로운 토큰을 발급합니다.
     *
     * @param request HttpServletRequest (헤더에서 accessToken 확인, 쿠키에서 refreshToken 추출)
     * @return 새로운 Access Token 또는 기존 유효한 Access Token
     */
    @Transactional
    public String refreshAccessToken(HttpServletRequest request) {

        // 헤더에서 기존 Access Token 확인
        String existingAccessToken = extractAccessTokenFromHeader(request);

        // 기존 Access Token이 있고 유효하다면 새로 발급하지 않음
        if (existingAccessToken != null) {
            try {
                if (!jwtUtil.isExpired(existingAccessToken)) {
                    String category = jwtUtil.getCategory(existingAccessToken);
                    if ("access".equals(category)) {
                        // 기존 Access Token이 아직 유효하므로 그대로 반환
                        return existingAccessToken;
                    }
                }
            } catch (Exception e) {
                // 토큰 파싱 오류 시 새로 발급 시도
            }
        }

        // Access Token이 없거나 만료되었을 때만 Refresh Token으로 새로 발급
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // Refresh Token 유효성 검증
        try {
            // 만료 시간 검증
            if (jwtUtil.isExpired(refreshToken)) {
                throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            }

            // 토큰 카테고리 확인 (refresh 토큰인지 확인)
            String category = jwtUtil.getCategory(refreshToken);
            if (!"refresh".equals(category)) {
                throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            // 토큰에서 사용자 정보 추출
            String email = jwtUtil.getUsername(refreshToken);

            // DB에서 사용자 존재 여부 확인 (계정 삭제/비활성화 체크)
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // 새로운 Access Token 생성
            CustomSecurityUserDetails userDetails = new CustomSecurityUserDetails(member);
            return jwtUtil.createAccessToken(userDetails);

        } catch (Exception e) {
            // JWT 파싱 오류나 기타 오류 시
            if (e instanceof CustomException) {
                throw e;
            }
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * 헤더에서 Access Token을 추출하는 메서드
     *
     * @param request HttpServletRequest
     * @return Access Token 값 또는 null
     */
    private String extractAccessTokenFromHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7); // "Bearer " 제거
        }

        return null;
    }

    /**
     * 쿠키에서 refreshToken을 추출하는 메서드
     *
     * @param request HttpServletRequest
     * @return refreshToken 값 또는 null
     */
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
