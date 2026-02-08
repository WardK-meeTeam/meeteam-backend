package com.wardk.meeteam_backend.global.auth.service;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.auth.repository.OAuthCodeRepository;
import com.wardk.meeteam_backend.global.auth.repository.TokenBlacklistRepository;
import com.wardk.meeteam_backend.global.auth.service.dto.RegisterMemberCommand;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthLoginInfo;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthRegisterInfo;
import com.wardk.meeteam_backend.global.auth.service.dto.TokenExchangeResult;
import com.wardk.meeteam_backend.web.auth.dto.EmailDuplicateResponse;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterResult;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterDescriptionRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterResponse;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    public static final int PASSWORD_LIMIT_LENGTH = 8;
    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final S3FileService s3FileService;
    private final SkillRepository skillRepository;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final OAuthTokenRevokeService oAuthTokenRevokeService;
    private final OAuthCodeRepository oAuthCodeRepository;


    @Transactional
    public RegisterResponse register(RegisterMemberCommand command, MultipartFile file) {
        validateEmailNotDuplicated(command.email());
        validatePassword(command.password());

        List<Skill> skills = fetchSkillsByNames(command.skills());
        String imageUrl = uploadFile(file);

        Member member = Member.createMember(
                command,
                bCryptPasswordEncoder.encode(command.password()),
                imageUrl
        );
        member.initializeDetails(command.jobPositions(), skills);
        memberRepository.save(member);

        return new RegisterResponse(member.getRealName(), member.getId());
    }

    @Transactional
    public OAuth2RegisterResult oauth2Register(OAuth2RegisterRequest registerRequest, MultipartFile file) {
        OAuthRegisterInfo registerInfo = oAuthCodeRepository.consumeRegisterInfo(registerRequest.getCode())
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_OAUTH_CODE));

        validateOAuthNotDuplicated(registerInfo.getProvider(), registerInfo.getProviderId());

        List<Skill> skills = fetchSkillsByNames(registerRequest.getSkills());
        String imageUrl = uploadFile(file);

        Member member = Member.createOAuthMember(
            registerRequest,
            registerInfo,
            bCryptPasswordEncoder.encode(UUID.randomUUID().toString()),
            imageUrl
        );
        member.initializeDetails(registerRequest.getJobPositions(), skills);

        if (registerInfo.getOauthAccessToken() != null) {
            member.setOauthAccessToken(registerInfo.getOauthAccessToken());
        }
        memberRepository.save(member);

        String accessToken = jwtUtil.createAccessToken(member);
        String refreshToken = jwtUtil.createRefreshToken(member);
        return new OAuth2RegisterResult(member, accessToken, refreshToken);
    }

    /**
     * OAuth 일회용 코드를 사용하여 기존 회원의 토큰을 교환하는 메서드
     *
     * @param code 일회용 UUID 코드
     * @return 토큰 교환 결과 (accessToken, refreshToken)
     */
    @Transactional
    public TokenExchangeResult exchangeToken(String code) {
        OAuthLoginInfo loginInfo = oAuthCodeRepository.consumeLoginInfo(code)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_OAUTH_CODE));

        Member member = memberRepository.findById(loginInfo.getMemberId())
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // OAuth Access Token 저장 (로그아웃 시 토큰 철회용)
        if (loginInfo.getOauthAccessToken() != null) {
            member.setOauthAccessToken(loginInfo.getOauthAccessToken());
            memberRepository.save(member);
            log.info("OAuth Access Token 저장 완료");
        }

        String accessToken = jwtUtil.createAccessToken(member);
        String refreshToken = jwtUtil.createRefreshToken(member);

        return new TokenExchangeResult(accessToken, refreshToken);
    }

    private void validateEmailNotDuplicated(String email) {
        memberRepository.findOptionByEmail(email)
            .ifPresent(e -> { throw new CustomException(ErrorCode.DUPLICATE_MEMBER); });
    }

    private void validateOAuthNotDuplicated(String provider, String providerId) {
        memberRepository.findByProviderAndProviderId(provider, providerId)
            .ifPresent(m -> { throw new CustomException(ErrorCode.DUPLICATE_MEMBER); });
    }

    private void validatePassword(String password) {
        if (password.length() < PASSWORD_LIMIT_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_PATTERN);
        }
    }

    private List<Skill> fetchSkillsByNames(List<String> skillNames) {
        List<Skill> skills = skillRepository.findBySkillNameIn(skillNames);
        if (skills.size() != skillNames.size()) {
            throw new CustomException(ErrorCode.SKILL_NOT_FOUND);
        }
        return skills;
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
                    if (category.equals(JwtUtil.ACCESS_CATEGORY)) {
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
            if (!category.equals(JwtUtil.REFRESH_CATEGORY)) {
                throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            // 토큰에서 사용자 정보 추출
            String email = jwtUtil.getUsername(refreshToken);

            // DB에서 사용자 존재 여부 확인 (계정 삭제/비활성화 체크)
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // 새로운 Access Token 생성
            return jwtUtil.createAccessToken(member);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
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
            if (JwtUtil.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private String uploadFile(MultipartFile file) {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = s3FileService.uploadFile(file, "images");
        }
        return imageUrl;
    }

    @Transactional
    public void deleteByEmail(String email) {
        memberRepository.deleteByEmail(email);
    }

    /**
     * 로그아웃 처리 - AccessToken을 블랙리스트에 추가하고, OAuth 토큰을 철회하고, 만료된 쿠키 반환
     *
     * @param accessToken 블랙리스트에 추가할 AccessToken (null 가능)
     * @return 만료된 Refresh Token 쿠키
     */
    @Transactional
    public ResponseCookie logout(String accessToken) {
        // AccessToken이 있으면 블랙리스트에 추가 및 OAuth 토큰 철회
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                // 토큰이 유효한 경우에만 처리
                if (!jwtUtil.isExpired(accessToken)) {
                    String jti = jwtUtil.getJti(accessToken);
                    long remainingTime = jwtUtil.getAccessTokenExpirationTime(accessToken);

                    if (jti != null) {
                        tokenBlacklistRepository.addToBlacklist(jti, remainingTime);
                        log.info("AccessToken이 블랙리스트에 추가되었습니다. JTI: {}", jti);
                    } else {
                        // JTI가 없는 구버전 토큰의 경우 (호환성)
                        log.warn("토큰에 JTI가 없습니다. 블랙리스트에 추가하지 않습니다.");
                    }

                    // OAuth 토큰 철회 처리
                    revokeOAuthToken(accessToken);
                }
            } catch (Exception e) {
                // 토큰 파싱 실패해도 로그아웃은 진행
                log.warn("토큰 블랙리스트 추가 중 오류 발생: {}", e.getMessage());
            }
        }

        // Refresh Token 쿠키 삭제
        return createExpiredRefreshTokenCookie();
    }

    /**
     * OAuth 토큰 철회 처리
     * JWT에서 사용자 정보를 추출하여 저장된 OAuth 토큰을 철회하고 삭제
     *
     * @param accessToken JWT Access Token
     */
    private void revokeOAuthToken(String accessToken) {
        try {
            String email = jwtUtil.getUsername(accessToken);
            Member member = memberRepository.findByEmail(email).orElse(null);

            if (member == null) {
                log.debug("사용자를 찾을 수 없어 OAuth 토큰 철회를 건너뜁니다.");
                return;
            }

            // OAuth 사용자가 아닌 경우 건너뜀
            if (member.getProvider() == null || member.getOauthAccessToken() == null) {
                log.debug("일반 로그인 사용자이므로 OAuth 토큰 철회를 건너뜁니다.");
                return;
            }

            // OAuth 토큰 철회
            boolean revoked = oAuthTokenRevokeService.revokeToken(
                member.getProvider(),
                member.getOauthAccessToken()
            );

            if (revoked) {
                log.info("OAuth 토큰 철회 성공 - provider: {}", member.getProvider());
            }

            // 저장된 OAuth 토큰 삭제
            member.setOauthAccessToken(null);
            memberRepository.save(member);
            log.info("저장된 OAuth 토큰 삭제 완료");

        } catch (Exception e) {
            // OAuth 토큰 철회 실패해도 로그아웃은 계속 진행
            log.warn("OAuth 토큰 철회 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 만료된 Refresh Token 쿠키 생성
     *
     * @return 만료된 Refresh Token 쿠키
     */
    private ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from(JwtUtil.REFRESH_COOKIE_NAME, "")
                .path("/")                                 // 경로 동일
                .domain(".meeteam.alom-sejong.com")        // 도메인 동일
                .httpOnly(true)
                .secure(true)                              // OAuth2 회원가입과 동일하게 true로 설정
                .sameSite("None")
                .maxAge(0)                                 // 즉시 만료
                .build();
    }
}
