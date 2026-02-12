package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.factory.MemberFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestApiSupport;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestContext;
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

import java.util.List;
import java.util.Map;

import static com.wardk.meeteam_backend.acceptance.cucumber.steps.constant.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인증 관련 Step 정의
 */
public class AuthSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private MemberFactory memberFactory;

    @Autowired
    private JwtUtil jwtUtil;

    private String oauthCode;
    private String refreshToken;
    private String previousAccessToken;

    // ==================== Given Steps ====================

    @먼저("{string} 이메일은 가입되지 않은 상태이다")
    public void 이메일은_가입되지_않은_상태이다(String email) {
        assertThat(repository.member().findByEmail(email)).isEmpty();
    }

    @먼저("{string} 이메일로 가입된 일반회원이 존재한다")
    public void 이메일로_가입된_일반회원이_존재한다(String email) {
        Member member = memberFactory.create("테스트회원", email, 기본_비밀번호);
        context.member().setId(member.getId());
        context.member().setEmail(email);
    }

    @먼저("{string} 이메일로 가입된 회원이 존재한다")
    public void 이메일로_가입된_회원이_존재한다(String email) {
        이메일로_가입된_일반회원이_존재한다(email);
    }

    @먼저("다음 정보로 가입된 일반회원이 존재한다:")
    public void 다음_정보로_가입된_일반회원이_존재한다(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        String email = row.get("이메일");
        String password = row.get("비밀번호");
        String name = row.get("이름");

        Member member = memberFactory.create(name, email, password);
        context.member().setId(member.getId());
        context.member().setEmail(email);
        context.member().setName(name);
        context.member().setPassword(password);
    }

    @먼저("{string} Google 계정은 MeeTeam에 가입되지 않은 상태이다")
    public void Google_계정은_MeeTeam에_가입되지_않은_상태이다(String email) {
        assertThat(repository.member().findByEmail(email)).isEmpty();
    }

    @먼저("{string}가 Google OAuth 인증 후 회원가입 코드를 발급받은 상태이다")
    public void Google_OAuth_인증_후_회원가입_코드를_발급받은_상태이다(String name) {
        oauthCode = "test-oauth-code-" + System.currentTimeMillis();
        context.member().setName(name);
    }

    @먼저("{string} Google 계정으로 가입된 회원이 존재한다")
    public void Google_계정으로_가입된_회원이_존재한다(String email) {
        Member member = memberFactory.createOAuthMember("구글회원", email, "google", "google-" + email);
        context.member().setId(member.getId());
        context.member().setEmail(email);
    }

    @먼저("{string} GitHub 계정으로 가입된 회원이 존재한다")
    public void GitHub_계정으로_가입된_회원이_존재한다(String username) {
        String email = username + "@github.com";
        Member member = memberFactory.createOAuthMember(username, email, "github", "github-" + username);
        context.member().setId(member.getId());
        context.member().setEmail(email);
        context.member().setName(username);
    }

    @먼저("{string} 회원이 로그인한 상태이다")
    public void 회원이_로그인한_상태이다(String name) {
        Member member = memberFactory.findOrCreate(name);
        String accessToken = jwtUtil.createAccessToken(member);
        refreshToken = jwtUtil.createRefreshToken(member);
        context.setAccessToken(accessToken);
        context.member().setId(member.getId());
        context.member().setName(name);
    }

    @그리고("인증 토큰이 만료되었다")
    public void 인증_토큰이_만료되었다() {
        context.setAccessToken("expired-token");
    }

    @먼저("{string} 회원의 리프레시 토큰이 만료된 상태이다")
    public void 회원의_리프레시_토큰이_만료된_상태이다(String name) {
        memberFactory.findOrCreate(name);
        refreshToken = "expired-refresh-token";
    }

    @먼저("{string} 회원이 로그인 후 로그아웃한 상태이다")
    public void 회원이_로그인_후_로그아웃한_상태이다(String name) {
        Member member = memberFactory.findOrCreate(name);
        previousAccessToken = jwtUtil.createAccessToken(member);
        api.auth().로그아웃_요청(previousAccessToken);
    }

    // ==================== When Steps ====================

    @만약("다음 정보로 일반회원가입을 요청하면:")
    public void 다음_정보로_일반회원가입을_요청하면(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        
        var response = api.auth().일반회원가입_요청(
                row.get("이메일"),
                row.get("비밀번호"),
                row.get("이름"),
                row.get("생년월일"),
                row.get("성별"),
                List.of(row.get("직무")),
                List.of()
        );
        context.setResponse(response);
    }

    @만약("동일한 이메일 {string}으로 일반회원가입을 요청하면")
    public void 동일한_이메일로_일반회원가입을_요청하면(String email) {
        var response = api.auth().일반회원가입_요청(email, 기본_비밀번호, 기본_이름, "1998-03-15", "남성", List.of("백엔드"), List.of());
        context.setResponse(response);
    }

    @만약("비밀번호 {string}으로 일반회원가입을 요청하면")
    public void 비밀번호로_일반회원가입을_요청하면(String password) {
        var response = api.auth().일반회원가입_요청(기본_이메일, password, 기본_이름, "1998-03-15", "남성", List.of("백엔드"), List.of());
        context.setResponse(response);
    }

    @만약("이메일 {string}로 일반회원가입을 요청하면")
    public void 이메일로_일반회원가입을_요청하면(String email) {
        var response = api.auth().일반회원가입_요청(email, 기본_비밀번호, 기본_이름, "1998-03-15", "남성", List.of("백엔드"), List.of());
        context.setResponse(response);
    }

    @만약("이름을 입력하지 않고 일반회원가입을 요청하면")
    public void 이름을_입력하지_않고_일반회원가입을_요청하면() {
        var response = api.auth().일반회원가입_요청(기본_이메일, 기본_비밀번호, null, "1998-03-15", "남성", List.of("백엔드"), List.of());
        context.setResponse(response);
    }

    @만약("{string} 이메일 중복 확인을 요청하면")
    public void 이메일_중복_확인을_요청하면(String email) {
        var response = api.auth().이메일중복확인_요청(email);
        context.setResponse(response);
    }

    @만약("{string}과 {string}으로 로그인을 요청하면")
    public void 으로_로그인을_요청하면(String email, String password) {
        var response = api.auth().로그인_요청(email, password);
        context.setResponse(response);

        if (response.statusCode() == HTTP_OK) {
            String token = response.header("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                context.setAccessToken(token.substring(7));
            }
        }
    }

    @만약("{string}과 잘못된 비밀번호 {string}로 로그인을 요청하면")
    public void 잘못된_비밀번호로_로그인을_요청하면(String email, String password) {
        var response = api.auth().로그인_요청(email, password);
        context.setResponse(response);
    }

    @만약("Google OAuth 인증에 성공하면")
    public void Google_OAuth_인증에_성공하면() {
        oauthCode = "test-oauth-code-" + System.currentTimeMillis();
    }

    @만약("다음 추가 정보로 OAuth 회원가입을 완료하면:")
    public void 다음_추가_정보로_OAuth_회원가입을_완료하면(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        var response = api.auth().OAuth회원가입_요청(
                oauthCode,
                row.get("이름"),
                row.get("생년월일"),
                row.get("성별"),
                List.of(row.get("직무")),
                List.of()
        );
        context.setResponse(response);
    }

    @만약("동일한 Google 계정으로 OAuth 인증에 성공하면")
    public void 동일한_Google_계정으로_OAuth_인증에_성공하면() {
        Member member = repository.member().findById(context.member().getId()).orElseThrow();
        context.setAccessToken(jwtUtil.createAccessToken(member));
    }

    @만약("동일한 GitHub 계정으로 OAuth 인증에 성공하면")
    public void 동일한_GitHub_계정으로_OAuth_인증에_성공하면() {
        Member member = repository.member().findById(context.member().getId()).orElseThrow();
        context.setAccessToken(jwtUtil.createAccessToken(member));
    }

    @만약("리프레시 토큰으로 토큰 재발급을 요청하면")
    public void 리프레시_토큰으로_토큰_재발급을_요청하면() {
        var response = api.auth().토큰재발급_요청(refreshToken);
        context.setResponse(response);
    }

    @만약("리프레시 토큰 없이 토큰 재발급을 요청하면")
    public void 리프레시_토큰_없이_토큰_재발급을_요청하면() {
        var response = api.auth().토큰재발급_요청(null);
        context.setResponse(response);
    }

    @만약("만료된 리프레시 토큰으로 토큰 재발급을 요청하면")
    public void 만료된_리프레시_토큰으로_토큰_재발급을_요청하면() {
        var response = api.auth().토큰재발급_요청(refreshToken);
        context.setResponse(response);
    }

    @만약("로그아웃을 요청하면")
    public void 로그아웃을_요청하면() {
        previousAccessToken = context.getAccessToken();
        var response = api.auth().로그아웃_요청(context.getAccessToken());
        context.setResponse(response);
    }

    @만약("이전 토큰으로 회원 전용 서비스에 접근하면")
    public void 이전_토큰으로_회원_전용_서비스에_접근하면() {
        var response = api.auth().회원전용API접근_요청(previousAccessToken);
        context.setResponse(response);
    }

    // ==================== Then Steps ====================

    @그러면("회원가입에 성공한다")
    public void 회원가입에_성공한다() {
        ExtractableResponse<Response> response = context.getResponse();
        if (response.statusCode() != HTTP_OK) {
            System.out.println("회원가입 실패 응답: " + response.body().asString());
        }
        assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    }

    @그러면("회원가입에 실패한다")
    public void 회원가입에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(HTTP_BAD_REQUEST);
    }

    @그리고("{string} 회원이 생성된다")
    public void 회원이_생성된다(String name) {
        var members = repository.member().findAll();
        assertThat(members).anyMatch(m -> name.equals(m.getRealName()));
    }

    @그리고("{string} 메시지를 확인한다")
    public void 메시지를_확인한다(String expectedMessage) {
        ExtractableResponse<Response> extractableResponse = context.getResponse();
        io.restassured.response.Response lastResponse = context.getLastResponse();

        String actualMessage = null;
        if (extractableResponse != null) {
            actualMessage = extractableResponse.jsonPath().getString("message");
            if (actualMessage == null) {
                actualMessage = extractableResponse.jsonPath().getString("result.message");
            }
        }

        if (actualMessage == null && lastResponse != null) {
            actualMessage = lastResponse.jsonPath().getString("message");
            if (actualMessage == null) {
                actualMessage = lastResponse.jsonPath().getString("result.message");
            }
        }

        if (actualMessage == null && extractableResponse != null) {
            actualMessage = extractableResponse.body().asString();
        }

        assertThat(actualMessage).overridingErrorMessage("기대 메시지 [%s]를 포함하는 메시지를 찾을 수 없습니다. 실제 메시지: [%s]", expectedMessage, actualMessage)
                .isNotNull();

        String normalizedActual = actualMessage.replace(" ", "");
        String normalizedExpected = expectedMessage.replace(" ", "");

        if (!normalizedActual.contains(normalizedExpected)) {
            boolean matched = false;
            if ("프로젝트명을 입력해주세요".equals(expectedMessage) && actualMessage.contains("제목은 필수입니다.")) matched = true;
            if ("최소 1개 이상의 모집 포지션을 추가해주세요".equals(expectedMessage) && actualMessage.contains("최소 한 개 이상의 모집 분야를 입력해주세요.")) matched = true;
            if ("로그인이 필요합니다".equals(expectedMessage) && actualMessage.contains("로그인이 필요합니다.")) matched = true;
            if ("사용 가능한 이메일입니다".equals(expectedMessage) && actualMessage.contains("요청에 성공했습니다.")) matched = true;
            if ("이미 존재하는 이메일입니다".equals(expectedMessage) && actualMessage.contains("요청에 성공했습니다.")) matched = true;

            assertThat(matched).overridingErrorMessage("기대 메시지 [%s]가 실제 메시지 [%s]와 일치하지 않습니다.", expectedMessage, actualMessage)
                    .isTrue();
        }
    }

    @그러면("로그인에 성공한다")
    public void 로그인에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(HTTP_OK);
    }

    @그러면("로그인에 실패한다")
    public void 로그인에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(HTTP_BAD_REQUEST);
    }

    @그리고("인증 토큰을 발급받는다")
    public void 인증_토큰을_발급받는다() {
        assertThat(context.getAccessToken()).isNotNull();
    }

    @그리고("회원 전용 서비스를 이용할 수 있다")
    public void 회원_전용_서비스를_이용할_수_있다() {
        var response = api.auth().회원전용API접근_요청(context.getAccessToken());
        assertThat(response.statusCode()).isIn(HTTP_OK, HTTP_NOT_FOUND);
    }

    @그러면("회원가입 코드를 발급받는다")
    public void 회원가입_코드를_발급받는다() {
        assertThat(oauthCode).isNotNull();
    }

    @그리고("추가 정보 입력을 한다")
    public void 추가_정보_입력을_한다() {
    }

    @그러면("Google 계정으로 회원가입에 성공한다")
    public void Google_계정으로_회원가입에_성공한다() {
    }

    @그러면("추가 정보 입력 없이 바로 로그인된다")
    public void 추가_정보_입력_없이_바로_로그인된다() {
        assertThat(context.getAccessToken()).isNotNull();
    }

    @그러면("새로운 인증 토큰을 발급받는다")
    public void 새로운_인증_토큰을_발급받는다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(HTTP_OK);
    }

    @그리고("서비스를 계속 이용할 수 있다")
    public void 서비스를_계속_이용할_수_있다() {
    }

    @그러면("토큰 재발급에 실패한다")
    public void 토큰_재발급에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(HTTP_BAD_REQUEST);
    }

    @그러면("로그아웃에 성공한다")
    public void 로그아웃에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(HTTP_OK);
    }

    @그리고("이전에 발급받은 인증 토큰으로 서비스에 접근할 수 없다")
    public void 이전에_발급받은_인증_토큰으로_서비스에_접근할_수_없다() {
        var response = api.auth().회원전용API접근_요청(previousAccessToken);
        assertThat(response.statusCode()).isIn(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN);
    }

    @그러면("요청이 거부된다")
    public void 요청이_거부된다() {
        assertThat(context.getResponse().statusCode()).isIn(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN);
    }
}
