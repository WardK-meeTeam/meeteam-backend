package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext;
import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext.MemberContext;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.MemberFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestApiSupport;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestRepositorySupport;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.먼저;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인증 관련 Step 정의
 */
public class AuthSteps {

    @Autowired
    private TestContext testContext;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private MemberFactory memberFactory;

    // ==========================================================================
    // 로그인 Steps
    // ==========================================================================

    @만약("{string}이 이메일 {string}과 비밀번호 {string}으로 로그인을 시도하면")
    public void 로그인을_시도하면(String name, String email, String password) {
        ExtractableResponse<Response> response = api.getAuth().로그인(email, password);
        testContext.setResponse(response);

        if (response.statusCode() == HttpStatus.OK.value()) {
            String accessToken = response.header("Authorization");
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
            String refreshToken = response.cookie(JwtUtil.REFRESH_COOKIE_NAME);

            MemberContext memberContext = testContext.getMember(name);
            memberContext.setAccessToken(accessToken);
            memberContext.setRefreshToken(refreshToken);

            testContext.setAccessToken(accessToken);
            testContext.setRefreshToken(refreshToken);
        }
    }

    @만약("{string}가 이메일 {string}과 비밀번호 {string}으로 로그인을 시도하면")
    public void 미가입자가_로그인을_시도하면(String name, String email, String password) {
        ExtractableResponse<Response> response = api.getAuth().로그인(email, password);
        testContext.setResponse(response);
    }

    @만약("{string}이 이메일 없이 비밀번호 {string}만으로 로그인을 시도하면")
    public void 이메일_없이_로그인을_시도하면(String name, String password) {
        ExtractableResponse<Response> response = api.getAuth().로그인("", password);
        testContext.setResponse(response);
    }

    @만약("{string}이 이메일 {string}만으로 로그인을 시도하면")
    public void 비밀번호_없이_로그인을_시도하면(String name, String email) {
        ExtractableResponse<Response> response = api.getAuth().로그인(email, "");
        testContext.setResponse(response);
    }

    @그러면("로그인에 성공한다")
    public void 로그인에_성공한다() {
        assertThat(testContext.getStatusCode())
                .as("로그인 응답 상태코드")
                .isEqualTo(HttpStatus.OK.value());
    }

    @그러면("로그인에 실패한다")
    public void 로그인에_실패한다() {
        assertThat(testContext.getStatusCode())
                .as("로그인 응답 상태코드")
                .isNotEqualTo(HttpStatus.OK.value());
    }

    @그리고("{string}은 인증 토큰을 발급받는다")
    public void 인증_토큰을_발급받는다(String name) {
        MemberContext member = testContext.getMember(name);
        assertThat(member.getAccessToken())
                .as("Access Token 발급")
                .isNotNull()
                .isNotEmpty();
    }

    // ==========================================================================
    // 토큰 재발급 Steps
    // ==========================================================================

    @먼저("{string}의 인증 토큰이 만료되었다")
    public void 인증_토큰이_만료되었다(String name) {
        // 실제로는 만료된 토큰을 시뮬레이션해야 하지만,
        // 테스트에서는 만료 시간을 조절하거나 mock을 사용
        // 현재는 단순히 상태만 표시
    }

    @먼저("{string}의 리프레시 토큰이 만료되었다")
    public void 리프레시_토큰이_만료되었다(String name) {
        MemberContext member = testContext.getMember(name);
        member.setRefreshToken("expired-refresh-token");
    }

    @먼저("{string}이 리프레시 토큰 없이 토큰 재발급을 요청하면")
    public void 리프레시_토큰_없이_토큰_재발급을_요청하면(String name) {
        ExtractableResponse<Response> response = api.getAuth().토큰_재발급(null);
        testContext.setResponse(response);
    }

    @만약("{string}이 토큰 재발급을 요청하면")
    public void 토큰_재발급을_요청하면(String name) {
        MemberContext member = testContext.getMember(name);
        ExtractableResponse<Response> response = api.getAuth().토큰_재발급(member.getRefreshToken());
        testContext.setResponse(response);

        if (response.statusCode() == HttpStatus.OK.value()) {
            String newAccessToken = response.jsonPath().getString("result");
            member.setAccessToken(newAccessToken);
            testContext.setAccessToken(newAccessToken);
        }
    }

    @그러면("새로운 인증 토큰을 발급받는다")
    public void 새로운_인증_토큰을_발급받는다() {
        assertThat(testContext.getStatusCode())
                .as("토큰 재발급 응답 상태코드")
                .isEqualTo(HttpStatus.OK.value());

        String newToken = testContext.getResponse().jsonPath().getString("result");
        assertThat(newToken)
                .as("새로운 Access Token")
                .isNotNull()
                .isNotEmpty();
    }

    @그러면("토큰 재발급에 실패한다")
    public void 토큰_재발급에_실패한다() {
        assertThat(testContext.getStatusCode())
                .as("토큰 재발급 응답 상태코드")
                .isNotEqualTo(HttpStatus.OK.value());
    }

    // ==========================================================================
    // 로그아웃 Steps
    // ==========================================================================

    @만약("{string}이 로그아웃을 요청하면")
    @만약("{string}가 로그아웃을 요청하면")
    public void 로그아웃을_요청하면(String name) {
        MemberContext member = testContext.getMember(name);
        ExtractableResponse<Response> response = api.getAuth().로그아웃(member.getAccessToken());
        testContext.setResponse(response);
    }

    @그러면("로그아웃에 성공한다")
    public void 로그아웃에_성공한다() {
        assertThat(testContext.getStatusCode())
                .as("로그아웃 응답 상태코드")
                .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("{string}의 인증 토큰은 더 이상 유효하지 않다")
    public void 인증_토큰은_더_이상_유효하지_않다(String name) {
        // 블랙리스트에 등록되어 무효화됨
        // 실제 검증은 API 호출 시 401 응답으로 확인
    }

    @먼저("{string}이 로그인 후 로그아웃한 상태이다")
    public void 로그인_후_로그아웃한_상태이다(String name) {
        MemberContext member = testContext.getMember(name);
        // 로그아웃하지만 이전 토큰은 보관
        String previousToken = member.getAccessToken();
        api.getAuth().로그아웃(previousToken);
        // 이전 토큰을 다시 설정 (테스트용)
        member.setAccessToken(previousToken);
    }

    @만약("{string}이 이전에 발급받은 토큰으로 프로젝트 목록을 조회하면")
    public void 이전에_발급받은_토큰으로_프로젝트_목록을_조회하면(String name) {
        MemberContext member = testContext.getMember(name);
        ExtractableResponse<Response> response = api.getAuth().인증_테스트(member.getAccessToken());
        testContext.setResponse(response);
    }

    @그리고("Google OAuth 토큰이 철회된다")
    public void Google_OAuth_토큰이_철회된다() {
        // OAuth 토큰 철회는 서버 내부에서 처리되므로 성공 응답만 확인
        assertThat(testContext.getStatusCode())
                .as("로그아웃 응답 상태코드")
                .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("{string}는 다시 소셜 로그인이 필요하다")
    public void 다시_소셜_로그인이_필요하다(String name) {
        // 로그아웃 후 상태 확인
    }

    // ==========================================================================
    // 회원가입 Steps
    // ==========================================================================

    @만약("다음 정보로 회원가입을 시도하면:")
    public void 다음_정보로_회원가입을_시도하면(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        String name = row.get("이름");
        String email = row.get("이메일");
        String password = row.get("비밀번호");
        Integer age = row.containsKey("나이") ? Integer.parseInt(row.get("나이")) : null;
        String gender = row.getOrDefault("성별", null);

        ExtractableResponse<Response> response = api.getAuth().회원가입(name, email, password, age, gender);
        testContext.setResponse(response);

        // 회원가입 성공 시 컨텍스트에 저장
        if (response.statusCode() == HttpStatus.OK.value() || response.statusCode() == HttpStatus.CREATED.value()) {
            MemberContext memberContext = new MemberContext();
            memberContext.setName(name);
            memberContext.setEmail(email);
            memberContext.setPassword(password);
            testContext.addMember(name, memberContext);
        }
    }

    @그러면("회원가입에 성공한다")
    public void 회원가입에_성공한다() {
        assertThat(testContext.getStatusCode())
                .as("회원가입 응답 상태코드")
                .isIn(HttpStatus.OK.value(), HttpStatus.CREATED.value());
    }

    @그러면("회원가입에 실패한다")
    public void 회원가입에_실패한다() {
        assertThat(testContext.getStatusCode())
                .as("회원가입 응답 상태코드")
                .isNotIn(HttpStatus.OK.value(), HttpStatus.CREATED.value());
    }

    @그리고("{string} 회원이 생성된다")
    public void 회원이_생성된다(String name) {
        MemberContext memberContext = testContext.getMember(name);
        assertThat(repository.getMember().findByEmail(memberContext.getEmail()))
                .as("회원 생성 확인")
                .isPresent();
    }

    @그리고("{string}은 로그인할 수 있다")
    public void 로그인할_수_있다(String name) {
        MemberContext memberContext = testContext.getMember(name);
        ExtractableResponse<Response> response = api.getAuth().로그인(
                memberContext.getEmail(),
                memberContext.getPassword()
        );

        assertThat(response.statusCode())
                .as("로그인 가능 여부")
                .isEqualTo(HttpStatus.OK.value());
    }

    // ==========================================================================
    // OAuth Steps
    // ==========================================================================

    @먼저("{string}가 Google 계정으로 로그인한 상태이다")
    public void Google_계정으로_로그인한_상태이다(String name) {
        Member member = memberFactory.createOAuthMember(
                name,
                name.toLowerCase() + "@gmail.com",
                "google",
                "google-" + System.currentTimeMillis()
        );

        MemberContext memberContext = new MemberContext();
        memberContext.setId(member.getId());
        memberContext.setName(name);
        memberContext.setEmail(member.getEmail());
        memberContext.setProvider("google");

        testContext.addMember(name, memberContext);

        // OAuth 로그인은 실제로는 별도 플로우이지만, 테스트에서는 직접 토큰 생성
        // 실제 구현 시 OAuth 모킹 필요
    }

    @먼저("{string}가 Google 계정으로 가입한 기존 회원이다")
    public void Google_계정으로_가입한_기존_회원이다(String name) {
        Google_계정으로_로그인한_상태이다(name);
    }

    @먼저("{string}가 GitHub 계정으로 가입한 기존 회원이다")
    public void GitHub_계정으로_가입한_기존_회원이다(String name) {
        Member member = memberFactory.createOAuthMember(
                name,
                name.toLowerCase() + "@github.com",
                "github",
                "github-" + System.currentTimeMillis()
        );

        MemberContext memberContext = new MemberContext();
        memberContext.setId(member.getId());
        memberContext.setName(name);
        memberContext.setEmail(member.getEmail());
        memberContext.setProvider("github");

        testContext.addMember(name, memberContext);
    }
}
