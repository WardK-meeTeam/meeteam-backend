package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext;
import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext.MemberContext;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.MemberFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.ProjectFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestApiSupport;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestRepositorySupport;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공통 Step 정의
 * 여러 Feature에서 공유되는 Step을 정의합니다.
 */
public class CommonSteps {

    @Autowired
    private TestContext testContext;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private MemberFactory memberFactory;

    @Autowired
    private ProjectFactory projectFactory;

    // ==========================================================================
    // 회원 관련 공통 Steps
    // ==========================================================================

    @먼저("{string} 회원이 다음 정보로 가입되어 있다:")
    public void 회원이_다음_정보로_가입되어_있다(String name, DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        String email = row.get("이메일");
        String password = row.get("비밀번호");

        ExtractableResponse<Response> response = api.getAuth().회원가입(name, email, password, 25, "MALE");
        assertThat(response.statusCode()).isIn(HttpStatus.OK.value(), HttpStatus.CREATED.value());

        Member member = repository.getMember().findByEmail(email)
                .orElseThrow(() -> new RuntimeException("회원가입 후 회원을 찾을 수 없습니다: " + email));

        MemberContext memberContext = new MemberContext();
        memberContext.setId(member.getId());
        memberContext.setName(name);
        memberContext.setEmail(email);
        memberContext.setPassword(password);

        testContext.addMember(name, memberContext);
    }

    @먼저("다음 회원들이 존재한다:")
    public void 다음_회원들이_존재한다(DataTable dataTable) {
        dataTable.asMaps().forEach(row -> {
            String name = row.get("이름");
            Member member = memberFactory.createMember(name);

            MemberContext memberContext = new MemberContext();
            memberContext.setId(member.getId());
            memberContext.setName(name);
            memberContext.setEmail(member.getEmail());
            memberContext.setPassword(memberFactory.getDefaultPassword());

            testContext.addMember(name, memberContext);
        });
    }

    @먼저("{string}이 로그인한 상태이다")
    public void 로그인한_상태이다(String name) {
        MemberContext member = testContext.getMember(name);

        ExtractableResponse<Response> response = api.getAuth().로그인(
                member.getEmail(),
                member.getPassword()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        String accessToken = response.header("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        String refreshToken = response.cookie(JwtUtil.REFRESH_COOKIE_NAME);

        member.setAccessToken(accessToken);
        member.setRefreshToken(refreshToken);

        testContext.setAccessToken(accessToken);
        testContext.setRefreshToken(refreshToken);
    }

    @먼저("{string} 회원이 로그인한 상태이다")
    public void 회원이_로그인한_상태이다(String name) {
        로그인한_상태이다(name);
    }

    // ==========================================================================
    // 응답 검증 공통 Steps
    // ==========================================================================

    @그러면("요청이 성공한다")
    public void 요청이_성공한다() {
        assertThat(testContext.getStatusCode())
                .as("API 응답 상태코드")
                .isIn(HttpStatus.OK.value(), HttpStatus.CREATED.value());
    }

    @그러면("요청이 거부된다")
    public void 요청이_거부된다() {
        assertThat(testContext.getStatusCode())
                .as("API 응답 상태코드")
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value(), HttpStatus.BAD_REQUEST.value());
    }

    @그리고("{string} 메시지를 확인한다")
    public void 메시지를_확인한다(String expectedMessage) {
        String responseBody = testContext.getResponse().body().asString();
        assertThat(responseBody)
                .as("응답 메시지")
                .containsIgnoringCase(expectedMessage);
    }

    @그리고("{string}은 MeeTeam 서비스를 이용할 수 있다")
    public void 서비스를_이용할_수_있다(String name) {
        MemberContext member = testContext.getMember(name);
        ExtractableResponse<Response> response = api.getAuth().인증_테스트(member.getAccessToken());

        assertThat(response.statusCode())
                .as("인증된 API 호출")
                .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("{string}은 계속해서 서비스를 이용할 수 있다")
    public void 계속해서_서비스를_이용할_수_있다(String name) {
        서비스를_이용할_수_있다(name);
    }

    @그리고("{string}은 보호된 서비스에 접근할 수 없다")
    public void 보호된_서비스에_접근할_수_없다(String name) {
        MemberContext member = testContext.getMember(name);
        ExtractableResponse<Response> response = api.getAuth().인증_테스트(member.getAccessToken());

        assertThat(response.statusCode())
                .as("인증 실패")
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
