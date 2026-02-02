package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext;
import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext.MemberContext;
import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext.ProjectContext;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.FileFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.MemberFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.ProjectFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.support.FakeS3Server;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestApiSupport;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestRepositorySupport;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
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
 * 파일 업로드 관련 Step 정의
 * <p>
 * 실제 API:
 * - PUT /api/members (프로필 수정 + 프로필 사진 업로드, 인증 필요)
 * - POST /api/projects/{projectId} (프로젝트 수정 + 썸네일 업로드, 인증 필요)
 */
public class FileUploadSteps {

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

    @Autowired
    private FileFactory fileFactory;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private FakeS3Server fakeS3Server;

    // ==========================================================================
    // 파일 업로드 설정 Steps
    // ==========================================================================

    @먼저("{string}이 {string} 프로젝트의 팀장이다")
    @먼저("{string}가 {string} 프로젝트의 팀장이다")
    public void 프로젝트의_팀장이다(String name, String projectName) {
        MemberContext memberContext = testContext.getMember(name);
        Member member = repository.getMember().findById(memberContext.getId())
                .orElseThrow(() -> new IllegalStateException(name + " 회원을 찾을 수 없습니다."));

        Project project = projectFactory.createProject(projectName, member);

        ProjectContext projectContext = new ProjectContext();
        projectContext.setId(project.getId());
        projectContext.setName(projectName);
        projectContext.setLeaderId(member.getId());

        testContext.addProject(projectName, projectContext);
        testContext.getCurrentProject().setId(project.getId());
        testContext.getCurrentProject().setName(projectName);
        testContext.getCurrentProject().setLeaderId(member.getId());
    }

    @먼저("{string}이 {string} 프로젝트의 일반 멤버이다")
    @먼저("{string}가 {string} 프로젝트의 일반 멤버이다")
    public void 프로젝트의_일반_멤버이다(String name, String projectName) {
        MemberContext memberContext = testContext.getMember(name);
        Member member = repository.getMember().findById(memberContext.getId())
                .orElseThrow(() -> new IllegalStateException(name + " 회원을 찾을 수 없습니다."));

        // 다른 사람이 만든 프로젝트 생성
        Member creator = memberFactory.createMember(projectName + "_리더");
        Project project = projectFactory.createProject(projectName, creator);

        // 일반 멤버로 추가
        ProjectMember projectMember = ProjectMember.builder()
                .member(member)
                .build();
        projectMember.assignProject(project);
        projectMemberRepository.save(projectMember);

        ProjectContext projectContext = new ProjectContext();
        projectContext.setId(project.getId());
        projectContext.setName(projectName);
        projectContext.setLeaderId(creator.getId());

        testContext.addProject(projectName, projectContext);
        testContext.getCurrentProject().setId(project.getId());
        testContext.getCurrentProject().setName(projectName);
        testContext.getCurrentProject().setLeaderId(creator.getId());
    }

    @먼저("최대 허용 용량은 {int}MB이다")
    public void 최대_허용_용량은_MB이다(int maxSizeMB) {
        // 서버 설정에 의해 결정되므로 별도 설정 불필요
        // 시나리오 문맥 제공용 스텝
    }

    // ==========================================================================
    // 파일 업로드 액션 Steps
    // ==========================================================================

    @만약("{string}이 {string} 파일을 프로필 사진으로 업로드하면")
    @만약("{string}가 {string} 파일을 프로필 사진으로 업로드하면")
    public void 파일을_프로필_사진으로_업로드하면(String name, String fileName) {
        MemberContext member = testContext.getMember(name);
        byte[] content = fileFactory.createDefaultFileContent();
        String contentType = fileFactory.getContentType(fileName);
        Map<String, Object> memberInfo = fileFactory.createDefaultMemberInfo(name);

        ExtractableResponse<Response> response = api.getFile().프로필_사진_업로드(
                member.getAccessToken(), memberInfo, content, fileName, contentType);
        testContext.setResponse(response);
    }

    @만약("{string}이 {string} 파일을 프로젝트 썸네일로 업로드하면")
    @만약("{string}가 {string} 파일을 프로젝트 썸네일로 업로드하면")
    public void 파일을_프로젝트_썸네일로_업로드하면(String name, String fileName) {
        MemberContext member = testContext.getMember(name);
        ProjectContext project = testContext.getCurrentProject();

        byte[] content = fileFactory.createDefaultFileContent();
        String contentType = fileFactory.getContentType(fileName);
        Map<String, Object> projectInfo = fileFactory.createDefaultProjectInfo(project.getName());

        ExtractableResponse<Response> response = api.getFile().프로젝트_썸네일_업로드(
                member.getAccessToken(), project.getId(), projectInfo, content, fileName, contentType);
        testContext.setResponse(response);
    }

    @만약("{string}이 {int}MB 크기의 {string} 파일을 프로필 사진으로 업로드하면")
    @만약("{string}가 {int}MB 크기의 {string} 파일을 프로필 사진으로 업로드하면")
    public void MB_크기의_파일을_프로필_사진으로_업로드하면(String name, int sizeMB, String fileName) {
        MemberContext member = testContext.getMember(name);
        byte[] content = fileFactory.createFileContent(sizeMB * 1024L * 1024L);
        String contentType = fileFactory.getContentType(fileName);
        Map<String, Object> memberInfo = fileFactory.createDefaultMemberInfo(name);

        ExtractableResponse<Response> response = api.getFile().프로필_사진_업로드(
                member.getAccessToken(), memberInfo, content, fileName, contentType);
        testContext.setResponse(response);
    }

    @만약("{string}이 프로젝트 썸네일을 변경하려고 하면")
    @만약("{string}가 프로젝트 썸네일을 변경하려고 하면")
    public void 프로젝트_썸네일을_변경하려고_하면(String name) {
        MemberContext member = testContext.getMember(name);
        ProjectContext project = testContext.getCurrentProject();

        byte[] content = fileFactory.createDefaultFileContent();
        Map<String, Object> projectInfo = fileFactory.createDefaultProjectInfo(project.getName());

        ExtractableResponse<Response> response = api.getFile().프로젝트_썸네일_업로드(
                member.getAccessToken(), project.getId(), projectInfo, content, "thumbnail.jpg", "image/jpeg");
        testContext.setResponse(response);
    }

    // ==========================================================================
    // 파일 업로드 검증 Steps
    // ==========================================================================

    @그러면("업로드에 성공한다")
    public void 업로드에_성공한다() {
        assertThat(testContext.getStatusCode())
                .as("파일 업로드 응답 상태코드")
                .isIn(HttpStatus.OK.value(), HttpStatus.CREATED.value());

        assertThat(fakeS3Server.getUploadCount())
                .as("S3에 파일이 업로드되었는지 확인")
                .isGreaterThan(0);
    }

    @그러면("업로드가 거부된다")
    public void 업로드가_거부된다() {
        assertThat(testContext.getStatusCode())
                .as("파일 업로드 거부 응답 상태코드")
                .isIn(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.FORBIDDEN.value()
                );
    }

    @그리고("{string}의 프로필 사진이 변경된다")
    public void 프로필_사진이_변경된다(String name) {
        assertThat(testContext.getStatusCode())
                .as("프로필 사진 변경 확인")
                .isEqualTo(HttpStatus.OK.value());

        assertThat(fakeS3Server.getUploadCount())
                .as("S3에 프로필 사진이 업로드되었는지 확인")
                .isGreaterThan(0);
    }

    @그리고("프로젝트 썸네일이 설정된다")
    public void 프로젝트_썸네일이_설정된다() {
        assertThat(testContext.getStatusCode())
                .as("프로젝트 썸네일 설정 확인")
                .isEqualTo(HttpStatus.OK.value());

        assertThat(fakeS3Server.getUploadCount())
                .as("S3에 프로젝트 썸네일이 업로드되었는지 확인")
                .isGreaterThan(0);
    }
}