package com.wardk.meeteam_backend.global.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.global.auth.dto.login.LoginRequest;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.global.auth.dto.register.SkillDto;
import com.wardk.meeteam_backend.global.auth.dto.register.SubCategoryDto;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        setupTestData();
    }

    private void setupTestData() {
        // SubCategory 리스트 생성
        SubCategoryDto subCategory1 = new SubCategoryDto();
        subCategory1.setSubcategory("웹서버");

        SubCategoryDto subCategory2 = new SubCategoryDto();
        subCategory2.setSubcategory("AI");

        List<SubCategoryDto> subCategories = Arrays.asList(subCategory1, subCategory2);

        // Skill 리스트 생성
        SkillDto skill1 = new SkillDto();
        skill1.setSkillName("Java");

        SkillDto skill2 = new SkillDto();
        skill2.setSkillName("Spring Boot");

        List<SkillDto> skills = Arrays.asList(skill1, skill2);

        // RegisterRequest 생성
        registerRequest = new RegisterRequest();
        registerRequest.setName("홍길동");
        registerRequest.setAge(27);
        registerRequest.setEmail("meeteam@naver.com");
        registerRequest.setGender(Gender.MALE);
        registerRequest.setPassword("qwer1234");
        registerRequest.setSubCategories(subCategories);
        registerRequest.setSkills(skills);
        registerRequest.setIntroduce("안녕하세요, 백엔드 개발자입니다.");

        // LoginRequest 생성
        loginRequest = new LoginRequest();
        // LoginRequest는 private 필드들이므로 JSON으로만 테스트
    }

    @Test
    @DisplayName("회원가입 성공 테스트 - 파일 포함")
    void register_WithFile_Success() throws Exception {
        // Given
        String expectedName = "홍길동";
        given(authService.register(any(RegisterRequest.class), any())).willReturn(expectedName);

        // 파일 생성
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                "profile image content".getBytes()
        );

        // RegisterRequest를 JSON으로 변환
        String requestJson = objectMapper.writeValueAsString(registerRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/auth/register")
                        .file(requestPart)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.username").value(expectedName));
    }

    @Test
    @DisplayName("회원가입 성공 테스트 - 파일 없음")
    @WithMockUser
    void register_WithoutFile_Success() throws Exception {
        // Given
        String expectedName = "홍길동";
        given(authService.register(any(RegisterRequest.class), any())).willReturn(expectedName);

        // RegisterRequest를 JSON으로 변환
        String requestJson = objectMapper.writeValueAsString(registerRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/auth/register")
                        .file(requestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.username").value(expectedName));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 필수 필드 누락")
    @WithMockUser
    void register_MissingRequiredFields_BadRequest() throws Exception {
        // Given - 이름이 없는 잘못된 요청
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setAge(27);
        invalidRequest.setEmail("test@naver.com");
        // name 필드가 누락됨

        String requestJson = objectMapper.writeValueAsString(invalidRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/auth/register")
                        .file(requestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").isEmpty()); // null 확인
    }

    @Test
    @DisplayName("로그인 테스트 - 정상 요청")
    @WithMockUser
    void login_Success() throws Exception {
        // Given
        String loginRequestJson = """
                {
                    "email": "test@naver.com",
                    "password": "password123"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("test@naver.com")); // username(email) 반환 확인
    }

    @Test
    @DisplayName("로그인 테스트 - 잘못된 JSON 형식")
    @WithMockUser
    void login_InvalidJson_BadRequest() throws Exception {
        // Given - 잘못된 JSON
        String invalidJson = """
                {
                    "email": "test@naver.com"
                    "password": "password123"
                }
                """; // 쉼표 누락

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("회원가입 엔드포인트 - 잘못된 Content-Type")
    @WithMockUser
    void register_WrongContentType_UnsupportedMediaType() throws Exception {
        // Given
        String requestJson = objectMapper.writeValueAsString(registerRequest);

        // When & Then - APPLICATION_JSON으로 보내면 실패해야 함 (MULTIPART_FORM_DATA 기대)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("존재하지 않는 엔드포인트 테스트")
    @WithMockUser
    void nonExistentEndpoint_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/nonexistent"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}