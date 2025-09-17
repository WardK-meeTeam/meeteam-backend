package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.github.GithubAppAuthService;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.util.FileObject;
import com.wardk.meeteam_backend.global.util.FileUtil;
import com.wardk.meeteam_backend.web.project.dto.*;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectUpdateResponse;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private FileUtil fileUtil;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SubCategoryRepository subCategoryRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private ProjectMemberService projectMemberService;
//    @Mock private ProjectRepoRepository projectRepoRepository;
//    @Mock private ProjectMemberRepository projectMemberRepository;
//    @Mock private GithubAppAuthService githubAppAuthService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Member creator;
    private SubCategory subCategory;
    private Skill skill;

    @BeforeEach
    void setUp() {
        creator = Member.builder()
                .id(1L)
                .email("test@naver.com")
                .build();

        subCategory = Mockito.mock(SubCategory.class);
        skill = Mockito.mock(Skill.class);
    }

    @Test
    @DisplayName("프로젝트 생성 성공")
    void postProject_success() {
        /**
         * 생성자로 등록할 멤버 찾기
         * 파일 저장 후 저장된 파일 이름 받기
         * endDate 검증
         * 프로젝트 엔티티 생성
         * 모집 정보 등록
         * 프로젝트 스킬 목록 등록
         * 엔티티 저장
         * 프로젝트 생성자 서브 카테고리 찾기
         * 생성자 프로젝트 멤버로 등록
         */
        //given
        ProjectPostRequest req = new ProjectPostRequest();
        req.setProjectName("test");
        req.setDescription("test description");
        req.setProjectCategory(ProjectCategory.ENVIRONMENT);
        req.setPlatformCategory(PlatformCategory.IOS);
        req.setOfflineRequired(true);
        req.setSubCategory("웹프론트엔드"); // 프로젝트 생성자 소분류
        req.setEndDate(LocalDate.now().plusDays(30));

        ProjectRecruitDto recruitDto = new ProjectRecruitDto();
        recruitDto.setSubCategory("웹프론트엔드");
        recruitDto.setRecruitmentCount(2);
        req.getRecruitments().add(recruitDto);

        ProjectSkillDto skillDto = new ProjectSkillDto();
        skillDto.setSkillName("Java");
        req.getProjectSkills().add(skillDto);

        MultipartFile mockFile = mock(MultipartFile.class);

        when(memberRepository.findOptionByEmail("test@naver.com"))
                .thenReturn(Optional.of(creator));
        when(subCategoryRepository.findByName("웹프론트엔드"))
                .thenReturn(Optional.of(subCategory));
        when(skillRepository.findBySkillName("Java"))
                .thenReturn(Optional.of(skill));
        when(fileUtil.storeFile(any()))
                .thenReturn(new FileObject("original.png", "stored.png"));

        //when
        ProjectPostResponse resp = projectService.postProject(req, mockFile, "test@naver.com");

        //then
        assertThat(resp.getTitle()).isEqualTo("test");
        verify(projectRepository, times(1)).save(any());
        verify(projectMemberService, times(1)).addCreator(any(), eq(creator.getId()), eq(subCategory));
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - endDate 검증 실패")
    void postProject_fail_endDate() {
        // given
        ProjectPostRequest req = new ProjectPostRequest();
        req.setProjectName("test");
        req.setDescription("test description");
        req.setProjectCategory(ProjectCategory.ENVIRONMENT);
        req.setPlatformCategory(PlatformCategory.IOS);
        req.setOfflineRequired(true);
        req.setSubCategory("웹프론트엔드"); // 프로젝트 생성자 소분류
        req.setEndDate(LocalDate.now().minusDays(1)); // 전날로 입력 시 오류 발생

        ProjectRecruitDto recruitDto = new ProjectRecruitDto();
        recruitDto.setSubCategory("웹프론트엔드");
        recruitDto.setRecruitmentCount(2);
        req.getRecruitments().add(recruitDto);

        ProjectSkillDto skillDto = new ProjectSkillDto();
        skillDto.setSkillName("Java");
        req.getProjectSkills().add(skillDto);

        when(memberRepository.findOptionByEmail("test@naver.com"))
                .thenReturn(Optional.of(creator));

        // when, then
        assertThrows(CustomException.class, () -> projectService.postProject(req, null, "test@naver.com"));
    }

    @Test
    @DisplayName("프로젝트 단건 조회 성공")
    void getProject_success() {
        //given
        Long projectId = 1L;

        Project project = Project.createProject(
                creator,
                "test",
                "test description",
                ProjectCategory.ENVIRONMENT,
                PlatformCategory.IOS,
                "stored.png",
                true,
                LocalDate.now().plusDays(30)
        );

        // ReflectionTestUtils를 사용해서 id 강제 주입
        ReflectionTestUtils.setField(project, "id", projectId);

        SubCategory subCategory = Mockito.mock(SubCategory.class);
        when(subCategory.getName()).thenReturn("웹프론트엔드");
        project.addRecruitment(ProjectCategoryApplication.createProjectCategoryApplication(subCategory, 3));

        Skill skill = Mockito.mock(Skill.class);
        when(skill.getSkillName()).thenReturn("Java");
        project.addProjectSkill(ProjectSkill.createProjectSkill(skill));

        ProjectMember creatorMember = mock(ProjectMember.class);
        when(creatorMember.getMember()).thenReturn(creator);
        project.joinMember(creatorMember);

        Member member = Member.builder()
                .id(2L)
                .email("test@naver.com")
                .realName("tester")
                .storeFileName("memberImage.png")
                .build();

        ProjectMember projectMember = Mockito.mock(ProjectMember.class);
        when(projectMember.getMember()).thenReturn(member);
        project.joinMember(projectMember);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // when
        ProjectResponse response = projectService.getProjectV1(projectId);

        assertThat(response.getName()).isEqualTo("test");
        assertThat(response.getDescription()).isEqualTo("test description");
        assertThat(response.getPlatformCategory()).isEqualTo(PlatformCategory.IOS);
        assertThat(response.getProjectCategory()).isEqualTo(ProjectCategory.ENVIRONMENT);
        assertThat(response.getImageUrl()).isEqualTo("stored.png");
        assertThat(response.isOfflineRequired()).isTrue();
        assertThat(response.getEndDate()).isAfter(LocalDate.now());

        assertThat(response.getSkills()).containsExactly("Java");

        assertThat(response.getRecruitments()).hasSize(1);
        assertThat(response.getRecruitments().get(0).getSubCategory()).isEqualTo("웹프론트엔드");
        assertThat(response.getRecruitments().get(0).getRecruitmentCount()).isEqualTo(3);

        assertThat(response.getProjectMembers()).hasSize(2);

        assertThat(response.getProjectMembers())
                .anySatisfy(pm -> {
                    if (pm.isCreator()) {
                        assertThat(pm.getName()).isEqualTo(creator.getRealName());
                    }
                });

        assertThat(response.getProjectMembers())
                .anySatisfy(pm -> {
                    if (!pm.isCreator()) {
                        assertThat(pm.getName()).isEqualTo(member.getRealName());
                    }
                });
    }

    @Test
    @DisplayName("프로젝트 수정 성공")
    void project_update_success() {
        /**
         * 프로젝트 조회
         * 생성자 확인
         * startDate, endDate 검증
         * 파일 저장 후 저장된 파일 이름 받기
         * project.updateProject
         * 모집 정보 수정
         * 프로젝트 스킬 목록 수정
         */
        //given
        Long projectId = 1L;

        Project project = Project.createProject(
                creator,
                "oldName",
                "old description",
                ProjectCategory.ENVIRONMENT,
                PlatformCategory.IOS,
                "old.png",
                true,
                LocalDate.now().plusDays(30)
        );
        ReflectionTestUtils.setField(project, "id", projectId);

        ProjectUpdateRequest req = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(req, "name", "newName");
        ReflectionTestUtils.setField(req, "description", "newDescription");
        ReflectionTestUtils.setField(req, "projectCategory", ProjectCategory.PET);
        ReflectionTestUtils.setField(req, "platformCategory", PlatformCategory.ANDROID);
        ReflectionTestUtils.setField(req, "offlineRequired", false);
        ReflectionTestUtils.setField(req, "status", ProjectStatus.ONGOING);
        ReflectionTestUtils.setField(req, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "endDate", LocalDate.now().plusDays(5));

        ProjectRecruitDto recruitDto = new ProjectRecruitDto();
        recruitDto.setSubCategory("웹프론트엔드");
        recruitDto.setRecruitmentCount(3);
        ReflectionTestUtils.setField(req, "recruitments", List.of(recruitDto));

        ProjectSkillDto skillDto = new ProjectSkillDto();
        skillDto.setSkillName("Spring");
        ReflectionTestUtils.setField(req, "skills", List.of(skillDto));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(subCategoryRepository.findByName("웹프론트엔드")).thenReturn(Optional.of(subCategory));
        when(subCategory.getName()).thenReturn("웹프론트엔드");
        when(skillRepository.findBySkillName("Spring")).thenReturn(Optional.of(skill));
        when(skill.getSkillName()).thenReturn("Spring");

        ProjectUpdateResponse resp = projectService.updateProject(projectId, req, null, creator.getEmail());

        assertThat(resp.getProjectId()).isEqualTo(projectId);
        assertThat(project.getName()).isEqualTo("newName");
        assertThat(project.getDescription()).isEqualTo("newDescription");
        assertThat(project.getProjectCategory()).isEqualTo(ProjectCategory.PET);
        assertThat(project.getPlatformCategory()).isEqualTo(PlatformCategory.ANDROID);
        assertThat(project.isOfflineRequired()).isFalse();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ONGOING);
        assertThat(project.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(project.getEndDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(project.getRecruitments()).hasSize(1);
        assertThat(project.getRecruitments()).extracting(r -> r.getSubCategory().getName(), ProjectCategoryApplication::getRecruitmentCount)
                .containsExactly(new Tuple("웹프론트엔드", 3));
        assertThat(project.getProjectSkills()).hasSize(1);
        assertThat(project.getProjectSkills()).extracting(s -> s.getSkill().getSkillName())
                .containsExactly("Spring");
    }

    @Test
    @DisplayName("프로젝트 수정 실패 - 프로젝트 없음")
    void project_update_fail_not_exists() {
        // given
        Long projectId = 10L;

        ProjectUpdateRequest req = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(req, "name", "newName");
        ReflectionTestUtils.setField(req, "description", "newDescription");
        ReflectionTestUtils.setField(req, "projectCategory", ProjectCategory.PET);
        ReflectionTestUtils.setField(req, "platformCategory", PlatformCategory.ANDROID);
        ReflectionTestUtils.setField(req, "offlineRequired", false);
        ReflectionTestUtils.setField(req, "status", ProjectStatus.ONGOING);
        ReflectionTestUtils.setField(req, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "endDate", LocalDate.now().plusDays(5));

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // when, then
        CustomException ex = assertThrows(CustomException.class, () -> projectService.updateProject(projectId, req, null, creator.getEmail()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("프로젝트 수정 실패 - 생성자 아님")
    void project_update_fail_not_creator() {
        //given
        Long projectId = 1L;

        Member notCreator = Member.builder()
                .id(2L)
                .email("notCreator@naver.com")
                .build();

        Project project = Project.createProject(
                creator,
                "oldName",
                "old description",
                ProjectCategory.ENVIRONMENT,
                PlatformCategory.IOS,
                "old.png",
                true,
                LocalDate.now().plusDays(30)
        );

        ProjectUpdateRequest req = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(req, "name", "newName");
        ReflectionTestUtils.setField(req, "description", "newDescription");
        ReflectionTestUtils.setField(req, "projectCategory", ProjectCategory.PET);
        ReflectionTestUtils.setField(req, "platformCategory", PlatformCategory.ANDROID);
        ReflectionTestUtils.setField(req, "offlineRequired", false);
        ReflectionTestUtils.setField(req, "status", ProjectStatus.ONGOING);
        ReflectionTestUtils.setField(req, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "endDate", LocalDate.now().plusDays(5));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // when, then
        CustomException ex = assertThrows(CustomException.class, () -> projectService.updateProject(projectId, req, null, notCreator.getEmail()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
    }

    @Test
    @DisplayName("프로젝트 수정 싪패 - endDate 검증 실패")
    void project_update_fail_endDate() {
        //given
        Long projectId = 1L;

        Project project = Project.createProject(
                creator,
                "oldName",
                "old description",
                ProjectCategory.ENVIRONMENT,
                PlatformCategory.IOS,
                "old.png",
                true,
                LocalDate.now().plusDays(30)
        );
        ReflectionTestUtils.setField(project, "id", projectId);

        ProjectUpdateRequest req = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(req, "name", "newName");
        ReflectionTestUtils.setField(req, "description", "newDescription");
        ReflectionTestUtils.setField(req, "projectCategory", ProjectCategory.PET);
        ReflectionTestUtils.setField(req, "platformCategory", PlatformCategory.ANDROID);
        ReflectionTestUtils.setField(req, "offlineRequired", false);
        ReflectionTestUtils.setField(req, "status", ProjectStatus.ONGOING);
        ReflectionTestUtils.setField(req, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "endDate", LocalDate.now().minusDays(5));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // when, then
        CustomException ex = assertThrows(CustomException.class, () -> projectService.updateProject(projectId, req, null, creator.getEmail()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROJECT_DATE);
    }

    @Test
    @DisplayName("프로젝트 삭제 성공")
    void delete_project_success() {
        /**
         * 프로젝트 조회
         * 생성자 확인
         * 삭제
         */
        //given
        Long projectId = 1L;

        Project project = Project.createProject(
                creator,
                "test",
                "description",
                ProjectCategory.ENVIRONMENT,
                PlatformCategory.IOS,
                "test.png",
                true,
                LocalDate.now().plusDays(30)
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        //when
        ProjectDeleteResponse resp = projectService.deleteProject(projectId, creator.getEmail());

        //then
        assertThat(resp.getProjectId()).isEqualTo(projectId);
        assertThat(resp.getProjectName()).isEqualTo(project.getName());
    }
}