package com.wardk.meeteam_backend.web.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.*;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProjectController.class)
@Import(ProjectControllerTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"
})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectPostRequest projectPostRequest;
    private ProjectUpdateRequest projectUpdateRequest;

    // 각 테스트 실행 전 데이터 초기화
    @BeforeEach
    void setUp() {
        setupTestData();
    }

    // 테스트 데이터 설정, 완전한 ProjectPostRequest 생성, Validation 요구사항 모두 충족
    private void setupTestData() {
        // 완전한 ProjectPostRequest 설정
        projectPostRequest = new ProjectPostRequest();
        projectPostRequest.setProjectName("테스트 프로젝트");
        projectPostRequest.setDescription("프로젝트 설명입니다.");
        projectPostRequest.setProjectCategory(ProjectCategory.EDUCATION);
        projectPostRequest.setPlatformCategory(PlatformCategory.WEB);
        projectPostRequest.setOfflineRequired(true);
        projectPostRequest.setSubCategory("웹서버");
        projectPostRequest.setEndDate(LocalDate.now().plusMonths(3));

        // 모집 분야 설정
        ProjectRecruitDto recruitDto = new ProjectRecruitDto();
        recruitDto.setSubCategory("웹서버");
        recruitDto.setRecruitmentCount(2);
        projectPostRequest.setRecruitments(Arrays.asList(recruitDto));

        // 기술 스택 설정
        ProjectSkillDto skillDto = new ProjectSkillDto();
        skillDto.setSkillName("Java");
        projectPostRequest.setProjectSkills(Arrays.asList(skillDto));

        // ProjectUpdateRequest 설정
        projectUpdateRequest = new ProjectUpdateRequest();
        projectUpdateRequest.setName("수정된 프로젝트");
        projectUpdateRequest.setDescription("수정된 설명");
        projectUpdateRequest.setProjectCategory(ProjectCategory.EDUCATION);
        projectUpdateRequest.setPlatformCategory(PlatformCategory.WEB);
        projectUpdateRequest.setOfflineRequired(false);
        projectUpdateRequest.setStatus(ProjectStatus.PLANNING);
        projectUpdateRequest.setStartDate(LocalDate.now());
        projectUpdateRequest.setEndDate(LocalDate.now().plusMonths(3));
        projectUpdateRequest.setRecruitments(Arrays.asList(recruitDto));
        projectUpdateRequest.setSkills(Arrays.asList(skillDto));
    }


    // MultipartFile과 JSON 데이터를 함께 전송
    // @WithUserDetails로 인증된 사용자 시뮬레이션
    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("프로젝트 등록 성공 - 파일 포함")
    void projectPost_WithFile_Success() throws Exception {
        // Given
        ProjectPostResponse expectedResponse = new ProjectPostResponse(1L, "테스트 프로젝트", LocalDateTime.now());
        given(projectService.postProject(any(ProjectPostRequest.class), any(), eq("test@example.com")))
                .willReturn(expectedResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file", "project.jpg", "image/jpeg", "project image".getBytes()
        );

        String requestJson = objectMapper.writeValueAsString(projectPostRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "projectPostRequest", "", "application/json", requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/projects")
                        .file(requestPart)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));
    }

    // JSON 데이터만 전송하는 케이스
    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("프로젝트 등록 성공 - 파일 없음")
    void projectPost_WithoutFile_Success() throws Exception {
        // Given
        ProjectPostResponse expectedResponse = new ProjectPostResponse(1L, "테스트 프로젝트", LocalDateTime.now());
        given(projectService.postProject(any(ProjectPostRequest.class), isNull(), eq("test@example.com")))
                .willReturn(expectedResponse);

        String requestJson = objectMapper.writeValueAsString(projectPostRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "projectPostRequest", "", "application/json", requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/projects")
                        .file(requestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));
    }

    // Validation 실패 테스트, 필수 필드가 누락된 요청에 대해 400 Bad Request 반환
    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("프로젝트 등록 실패 - 필수 필드 누락")
    void projectPost_MissingRequiredFields_BadRequest() throws Exception {
        // Given - 빈 요청
        ProjectPostRequest invalidRequest = new ProjectPostRequest();
        invalidRequest.setDescription("설명만 있음");

        String requestJson = objectMapper.writeValueAsString(invalidRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "projectPostRequest", "", "application/json", requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/projects")
                        .file(requestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400으로 변경 (원래 500이었지만 validation 에러는 400이 맞음)
    }

    // 인증 없이 접근 가능한 공개 API
    @Test
    @DisplayName("프로젝트 목록 조회 성공")
    void getProjectList_Success() throws Exception {
        // Given
        ProjectListResponse response1 = ProjectListResponse.builder()
                .projectId(1L)
                .name("프로젝트1")
                .creatorName("생성자1")
                .skills(Arrays.asList("Java", "Spring"))
                .startDate(LocalDate.now())
                .build();

        List<ProjectListResponse> expectedList = Arrays.asList(response1);
        given(projectService.getProjectList()).willReturn(expectedList);

        // When & Then - 인증 불필요
        mockMvc.perform(get("/api/projects"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].projectId").value(1L))
                .andExpect(jsonPath("$.result[0].name").value("프로젝트1"));
    }

    // 인증 없이 접근 가능한 공개 API
    @Test
    @DisplayName("프로젝트 상세 조회 성공")
    void getProject_Success() throws Exception {
        // Given
        Long projectId = 1L;
        ProjectResponse expectedResponse = ProjectResponse.builder()
                .name("테스트 프로젝트")
                .description("프로젝트 설명")
                .projectCategory(ProjectCategory.EDUCATION)
                .platformCategory(PlatformCategory.WEB)
                .offlineRequired(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .projectMembers(Arrays.asList())
                .skills(Arrays.asList("Java", "Spring"))
                .recruitments(Arrays.asList())
                .build();

        given(projectService.getProject(projectId)).willReturn(expectedResponse);

        // When & Then - 인증 불필요
        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.name").value("테스트 프로젝트"));
    }

    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("프로젝트 수정 성공")
    void updateProject_Success() throws Exception {
        // Given
        Long projectId = 1L;
        ProjectUpdateResponse expectedResponse = ProjectUpdateResponse.responseDto(projectId);
        given(projectService.updateProject(eq(projectId), any(ProjectUpdateRequest.class), any(), eq("test@example.com")))
                .willReturn(expectedResponse);

        String requestJson = objectMapper.writeValueAsString(projectUpdateRequest);
        MockMultipartFile requestPart = new MockMultipartFile(
                "projectUpdateRequest", "", "application/json", requestJson.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/projects/{projectId}", projectId)
                        .file(requestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));
    }

    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("프로젝트 삭제 성공")
    void deleteProject_Success() throws Exception {
        // Given
        Long projectId = 1L;
        ProjectDeleteResponse expectedResponse = ProjectDeleteResponse.responseDto(projectId, "삭제된 프로젝트");
        given(projectService.deleteProject(projectId, "test@example.com"))
                .willReturn(expectedResponse);

        // When & Then
        mockMvc.perform(delete("/api/projects/{projectId}", projectId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.projectId").value(projectId));
    }

    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("프로젝트 레포 추가 성공")
    void addRepo_Success() throws Exception {
        // Given
        Long projectId = 1L;

        String requestJson = """
                {
                    "repoUrls": ["https://github.com/owner/repo"]
                }
                """;

        List<ProjectRepoResponse> expectedResponses = Arrays.asList(
                ProjectRepoResponse.builder()
                        .id(1L)
                        .repoFullName("owner/repo")
                        .build()
        );

        given(projectService.addRepo(eq(projectId), any(ProjectRepoRequest.class), eq("test@example.com")))
                .willReturn(expectedResponses);

        // When & Then
        mockMvc.perform(post("/api/projects/{projectId}/repos", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));
    }

    // CustomSecurityUserDetails 파라미터 정상 주입 확인
    @Test
    @WithUserDetails("test@example.com")
    @DisplayName("내 프로젝트 목록 조회 성공")
    void getMyProjects_Success() throws Exception {
        // Given
        List<MyProjectResponse> expectedList = Arrays.asList(
                MyProjectResponse.builder()
                        .projectId(1L)
                        .projectName("내 프로젝트 1")
                        .projectStatus(ProjectStatus.PLANNING)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusMonths(3))
                        .subCategoryName("웹서버")
                        .build()
        );

        given(projectService.getMyProject("test@example.com")).willReturn(expectedList);

        // When & Then
        mockMvc.perform(get("/api/projects/my"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].projectName").value("내 프로젝트 1"));
    }

    // 잘못된 파라미터 타입에 대한 400 Bad Request 반환 확인
    @Test
    @DisplayName("존재하지 않는 엔드포인트 테스트")
    void nonExistentEndpoint_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/projects/nonexistent"))
                .andDo(print())
                .andExpect(status().isBadRequest()); // GlobalExceptionHandler가 400으로 처리
    }

    @TestConfiguration
    static class TestConfig {
        /**
         *  테스트 환경 설정
         *
         * 핵심 해결사항:
         * 1. UserDetailsService: 테스트용 CustomSecurityUserDetails 생성
         * 2. WebMvcConfigurer: AuthenticationPrincipalArgumentResolver 등록
         * 3. Spring Security 비활성화로 인증 이슈 해결
         */

        // 테스트용 UserDetailsService, CustomSecurityUserDetails의 member 필드를 올바르게 초기화
        @Bean
        @Primary
        public UserDetailsService userDetailsService() {
            return username -> {
                System.out.println("UserDetailsService called with username: " + username);

                // Member 객체 생성
                Member member = Member.builder()
                        .email(username)
                        .password("test-password")
                        .role(UserRole.USER)
                        .build();

                // ReflectionTestUtils로 ID 설정 (JPA @GeneratedValue 시뮬레이션)
                ReflectionTestUtils.setField(member, "id", 1L);

                System.out.println("Created member: " + member);
                System.out.println("Member email: " + member.getEmail());

                // CustomSecurityUserDetails 생성
                CustomSecurityUserDetails userDetails = new CustomSecurityUserDetails(member);

                System.out.println("Created userDetails: " + userDetails);
                System.out.println("UserDetails username: " + userDetails.getUsername());

                return userDetails;
            };
        }

        // ArgumentResolver 명시적 설정 , CustomSecurityUserDetails 파라미터가 컨트롤러에 제대로 주입되도록 함
        @Bean
        public WebMvcConfigurer webMvcConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(new AuthenticationPrincipalArgumentResolver());
                }
            };
        }
    }
}
