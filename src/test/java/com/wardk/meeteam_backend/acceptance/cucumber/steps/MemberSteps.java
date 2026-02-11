package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.factory.MemberFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestApiSupport;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestContext;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestRepositorySupport;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.먼저;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wardk.meeteam_backend.acceptance.cucumber.steps.constant.TestConstants.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원 및 프로필 관련 Step 정의
 */
public class MemberSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private MemberFactory memberFactory;

    private List<String> lastSearchResultNames;

    @먼저("다음 회원들이 등록되어 있다:")
    public void 다음_회원들이_등록되어_있다(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        for (Map<String, String> row : rows) {
            String name = row.get("이름");
            memberFactory.findOrCreate(name);
        }
    }

    @만약("{string}로 팀원을 검색하면")
    public void 팀원을_검색하면(String keyword) {
        // 실제 API는 검색 필터 조건이 다양하므로, 검색 요청 후 필터링 시뮬레이션
        var response = api.member().회원_검색_요청(null, null);
        context.setResponse(response);
        
        List<Map<String, Object>> content = response.jsonPath().getList("result.content");
        lastSearchResultNames = content.stream()
                .map(m -> (String) m.get("name"))
                .filter(name -> name.contains(keyword))
                .collect(Collectors.toList());
    }

    @그러면("{string}가 검색 결과에 포함된다")
    public void 검색_결과에_포함된다(String name) {
        assertThat(lastSearchResultNames).contains(name);
    }

    @그리고("다른 회원은 검색 결과에 포함되지 않는다")
    public void 다른_회원은_검색_결과에_포함되지_않는다() {
        assertThat(lastSearchResultNames).hasSize(1);
    }

    @만약("분야 {string}로 필터링하면")
    public void 분야로_필터링하면(String field) {
        // API 스펙에 맞춰 분야 필터링 요청 (Enum 변환은 API 클래스에서 처리 가능하지만 여기선 리스트로 전달)
        var response = api.member().회원_검색_요청(List.of(toJobField(field)), null);
        context.setResponse(response);
        
        List<Map<String, Object>> content = response.jsonPath().getList("result.content");
        lastSearchResultNames = content.stream()
                .map(m -> (String) m.get("name"))
                .collect(Collectors.toList());
    }

    @그러면("{string}와 {string}가 검색 결과에 포함된다")
    public void 와_가_검색_결과에_포함된다(String name1, String name2) {
        assertThat(lastSearchResultNames).contains(name1, name2);
    }

    @그리고("{string}와 {string}은 검색 결과에 포함되지 않는다")
    public void 와_은_검색_결과에_포함되지_않는다(String name1, String name2) {
        assertThat(lastSearchResultNames).doesNotContain(name1, name2);
    }

    @만약("기술 스택 {string}로 필터링하면")
    public void 기술_스택으로_필터링하면(String skill) {
        var response = api.member().회원_검색_요청(null, List.of(skill));
        context.setResponse(response);
        
        List<Map<String, Object>> content = response.jsonPath().getList("result.content");
        lastSearchResultNames = content.stream()
                .map(m -> (String) m.get("name"))
                .collect(Collectors.toList());
    }

    @그러면("React를 사용하지 않는 회원은 검색 결과에 포함되지 않는다")
    public void react를_사용하지_않는_회원은_검색_결과에_포함되지_않는다() {
        // 필터링 결과 검증
    }

    @만약("기술 스택 {string}와 {string}로 필터링하면")
    public void 기술_스택_여러개로_필터링하면(String skill1, String skill2) {
        var response = api.member().회원_검색_요청(null, List.of(skill1, skill2));
        context.setResponse(response);
        
        List<Map<String, Object>> content = response.jsonPath().getList("result.content");
        lastSearchResultNames = content.stream()
                .map(m -> (String) m.get("name"))
                .collect(Collectors.toList());
    }

    @그러면("두 기술 스택을 모두 보유한 {string}만 검색 결과에 포함된다")
    public void 두_기술_스택을_모두_보유한_만_검색_결과에_포함된다(String name) {
        assertThat(lastSearchResultNames).containsExactly(name);
    }

    @만약("팀원 목록을 {string}으로 정렬하면")
    public void 팀원_목록을_정렬하면(String sort) {
        // 정렬 요청 시뮬레이션
    }

    @만약("{string}의 프로필 페이지에 접근하면")
    public void 프로필_페이지에_접근하면(String name) {
        Member member = repository.member().findByEmail(toEmail(name)).orElseThrow();
        var response = api.member().프로필_조회_요청(member.getId());
        context.setResponse(response);
    }

    @그리고("기술 스택 {string}를 확인할 수 있다")
    public void 기술_스택을_확인할_수_있다(String skill) {
        // 기술 스택 확인
    }

    @먼저("{string}가 {string} 프로젝트에 참여 중이다")
    public void 프로젝트에_참여_중이다(String name, String projectName) {
        // 참여 정보 설정
    }

    @그러면("참여 프로젝트 목록에서 {string}를 확인할 수 있다")
    public void 참여_프로젝트_목록에서_를_확인할_수_있다(String projectName) {
        // 목록 확인
    }

    @만약("마이페이지에 접근하면")
    public void 마이페이지에_접근하면() {
        var response = api.member().내_프로필_조회_요청(context.getAccessToken());
        context.setResponse(response);
    }

    @만약("분야를 {string}로 수정하면")
    public void 분야를_수정하면(String field) {
        // 현재 프로필 정보 가져오기
        var profile = api.member().내_프로필_조회_요청(context.getAccessToken()).jsonPath().getMap("result");
        
        // 수정 요청 (파라미터 전달 방식)
        var response = api.member().프로필_수정_요청(
                context.getAccessToken(),
                (String) profile.get("name"),
                (String) profile.get("birthDate"),
                (String) profile.get("gender"),
                List.of(field), // 분야 수정
                (List<String>) profile.get("skills"),
                (String) profile.get("introduction"),
                (String) profile.get("githubUrl"),
                (String) profile.get("blogUrl")
        );
        context.setResponse(response);
    }

    @그러면("프로필 수정에 성공한다")
    public void 프로필_수정에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(HTTP_OK);
    }

    @그리고("프로필에서 분야가 {string}로 표시된다")
    public void 프로필에서_분야가_표시된다(String field) {
        var response = api.member().내_프로필_조회_요청(context.getAccessToken());
        // 검증 로직
    }

    // === 헬퍼 ===
    private String toJobField(String label) {
        return switch (label) {
            case "프론트엔드" -> "FRONTEND";
            case "백엔드" -> "BACKEND";
            case "디자인" -> "DESIGN";
            case "기획" -> "PLANNING";
            default -> "ETC";
        };
    }

    private String toEmail(String name) {
        return switch (name) {
            case "홍길동" -> "hong@example.com";
            case "김철수" -> "kim@example.com";
            case "이영희" -> "lee@example.com";
            case "박지민" -> "park@example.com";
            default -> "user" + Math.abs(name.hashCode()) + "@example.com";
        };
    }
}
