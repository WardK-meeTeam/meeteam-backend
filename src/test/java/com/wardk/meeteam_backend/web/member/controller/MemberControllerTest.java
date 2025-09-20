/*
package com.wardk.meeteam_backend.web.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.service.MemberProfileService;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.web.member.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
        })
class MemberControllerTest {

    // 가짜 웹서버 역할 -> 톰캣 서버 없이도 http 요청 응답 테스트 가능
    @Autowired
    private MockMvc mockMvc; // 가짜 웹 서버 역할

    @MockitoBean
    private MemberProfileService memberProfileService;

    // JSON <-> 객체 변환용
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("멤버 프로필 조회 성공")
    void getMemberprofile_Success() throws Exception {

        //given
        Long memberId = 1L;
        MemberProfileResponse mockResponse = createMockMemberProfileResponse(); // 가짜 응답 데이터 - 실제 DB에서 가져오는 것처럼
        given(memberProfileService.profile(anyLong())).willReturn(mockResponse); // service.profile(아무 Long값)이 호출되면 mockResponse를 리턴해줘

        // when, then
        ResultActions resultActions = mockMvc.perform(get("/api/members/{memberId}", memberId)
                        .contentType(MediaType.APPLICATION_JSON) // 응답이 JSON인지 확인
                )
                .andDo(print()) // 요청/응답 콘솔에 출력 - 디버깅용
                // 검증 시작
                .andExpect(status().isOk())  // HTTP 상태코드가 200 OK인지 확인
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))  // 응답이 JSON인지 확인

                // JSON 응답의 특정 값들이 예상값과 같은지 하나씩 확인
                .andExpect(jsonPath("$.result.name").value("홍길동"))
                .andExpect(jsonPath("$.result.age").value(25))
                .andExpect(jsonPath("$.result.gender").value("MALE"))
                .andExpect(jsonPath("$.result.email").value("test@example.com"))
                .andExpect(jsonPath("$.result.isParticipating").value(true))
                .andExpect(jsonPath("$.result.projectCount").value(2))
                .andExpect(jsonPath("$.result.introduce").value("안녕하세요 백엔드 개발자입니다"))

                // 배열(리스트) 데이터 검증
                .andExpect(jsonPath("$.result.categories").isArray())
                .andExpect(jsonPath("$.result.categories[0].bigCategory").value("개발"))
                .andExpect(jsonPath("$.result.categories[0].smallCategory").value("백엔드"))
                .andExpect(jsonPath("$.result.skills").isArray())
                .andExpect(jsonPath("$.result.skills[0].skill").value("Java"))
                .andExpect(jsonPath("$.result.projectList").isArray())  // 프로젝트 리스트가 배열인지 확인
                .andExpect(jsonPath("$.result.projectList[0].title").value("MeeTeam 프로젝트"))
                .andExpect(jsonPath("$.result.projectList[0].status").value("COMPLETED"));


    }

    @Test
    @DisplayName("존재하지 않는 멤버 ID로 조회 시 예외 발생")
    void getMemberProfile_NotFound() throws Exception {
        // given
        Long invalidMemberId = 999L;
        given(memberProfileService.profile(invalidMemberId))
                .willThrow(new IllegalArgumentException("Member not found"));

        // when & then
        mockMvc.perform(get("/api/members/{memberId}", invalidMemberId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 예외 처리 방식에 따라 달라질 수 있음
    }

    @Test
    @DisplayName("잘못된 경로 파라미터로 요청 시 400 에러")
    void getMemberProfile_InvalidPathVariable() throws Exception {
        // when & then
        mockMvc.perform(get("/api/members/{memberId}", "invalid-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 타입 변환 실패로 400 에러
    }

    @Test
    @DisplayName("빈 프로필 데이터 응답 테스트")
    void getMemberProfile_EmptyData() throws Exception {
        // given
        Long memberId = 2L;
        MemberProfileResponse emptyResponse = createEmptyMemberProfileResponse();
        given(memberProfileService.profile(memberId)).willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/api/members/{memberId}", memberId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categories").isEmpty())
                .andExpect(jsonPath("$.result.skills").isEmpty())
                .andExpect(jsonPath("$.result.projectList").isEmpty());
    }

    // 빈 데이터 Mock 생성 메서드
    private MemberProfileResponse createEmptyMemberProfileResponse() {
        MemberProfileResponse response = new MemberProfileResponse();

        // 기본 정보만 설정
        response.setName("신규유저");
        response.setAge(20);
        response.setGender(Gender.FEMALE);
        response.setEmail("newuser@example.com");
        response.setIsParticipating(false);
        response.setProjectCount(0);
        response.setIntroduce("");

        // 모든 리스트를 빈 리스트로 설정
        response.setCategories(List.of());
        response.setSkills(List.of());
        response.setProjectList(List.of());

        return response;
    }


    // Mock 데이터 생성 메서드
    private MemberProfileResponse createMockMemberProfileResponse(){

        // 빈 응답 객체 생성
        MemberProfileResponse response = new MemberProfileResponse();

        // 기본 정보 설정
        response.setName("홍길동");
        response.setAge(25);
        response.setGender(Gender.MALE);
        response.setEmail("test@example.com");
        response.setIsParticipating(true);
        response.setProjectCount(2);
        response.setIntroduce("안녕하세요 백엔드 개발자입니다");

        // 카테고리 목록 설정 (List.of()로 불변 리스트 생성)
        response.setCategories(List.of(
                new CategoryResponse("개발", "백엔드"),
                new CategoryResponse("개발", "데이터베이스")
        ));

        // 스킬 목록 설정
        response.setSkills(List.of(
                new SkillResponse("Java"),
                new SkillResponse("Spring Boot"),
                new SkillResponse("MySQL")
        ));


        // 프로젝트 목록 설정
        response.setProjectList(List.of(
                new ProjectResponse(LocalDate.of(2024, 12, 31), "MeeTeam 프로젝트", ProjectStatus.COMPLETED),
                new ProjectResponse(LocalDate.of(2024, 11, 30), "포트폴리오 웹사이트", ProjectStatus.COMPLETED)
        ));

        return response;  // 완성된 Mock 데이터 리턴
    }
}*/
