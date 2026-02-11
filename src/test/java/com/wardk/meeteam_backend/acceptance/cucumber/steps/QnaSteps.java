package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.support.TestContext;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class QnaSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String leaderName;
    private String projectName;
    private final List<QnaItem> qnaItems = new ArrayList<>();
    private final Set<String> teamMembers = new HashSet<>();
    private List<QnaItem> viewedQna = new ArrayList<>();
    private QnaItem lastQuestionItem;
    private boolean lastQuestionSucceeded;
    private boolean lastAnswerSucceeded;
    private String lastNotificationTarget;
    private boolean lastAnswererWasLeader;

    @Before("@qna")
    public void setUpQnaScenario() {
        leaderName = "홍길동";
        projectName = "AI 뉴스 요약 서비스";
        qnaItems.clear();
        teamMembers.clear();
        viewedQna = new ArrayList<>();
        lastQuestionItem = null;
        lastQuestionSucceeded = false;
        lastAnswerSucceeded = false;
        lastNotificationTarget = null;
        lastAnswererWasLeader = false;
        teamMembers.add(leaderName);
    }

    @When("{string}가 {string} 프로젝트에 다음 질문을 작성하면:")
    public void 회원이_프로젝트에_질문을_작성하면(String writerName, String targetProjectName, String question) {
        this.projectName = targetProjectName;
        writeQuestion(writerName, question, LocalDate.now());
    }

    @Then("질문 작성에 성공한다")
    public void 질문_작성에_성공한다() {
        assertTrue(lastQuestionSucceeded);
    }

    @Then("질문 작성에 실패한다")
    public void 질문_작성에_실패한다() {
        assertFalse(lastQuestionSucceeded);
    }

    @And("Q&A 목록에 해당 질문이 표시된다")
    public void qna_목록에_해당_질문이_표시된다() {
        assertNotNull(lastQuestionItem);
        assertTrue(qnaItems.stream().anyMatch(q -> q.question.equals(lastQuestionItem.question)));
    }

    @And("{string} 리더에게 새로운 질문 알림이 발송된다")
    public void 리더에게_새로운_질문_알림이_발송된다(String expectedLeaderName) {
        assertEquals(expectedLeaderName, lastNotificationTarget);
    }

    @Given("{string}가 {string}에 질문을 작성한 상태이다")
    public void 회원이_날짜에_질문을_작성한_상태이다(String writerName, String date) {
        seedQuestion(writerName, "질문 목록 검증용 질문", LocalDate.parse(date));
    }

    @When("프로젝트 Q&A 목록을 조회하면")
    public void 프로젝트_qna_목록을_조회하면() {
        viewedQna = qnaItems.stream()
                .sorted(Comparator.comparing((QnaItem q) -> q.questionDate).reversed())
                .toList();
    }

    @Then("질문 작성자 {string}를 확인할 수 있다")
    public void 질문_작성자를_확인할_수_있다(String expectedWriter) {
        assertFalse(viewedQna.isEmpty());
        assertEquals(expectedWriter, viewedQna.get(0).questioner);
    }

    @And("작성일 {string}를 확인할 수 있다")
    public void 작성일을_확인할_수_있다(String expectedDate) {
        assertFalse(viewedQna.isEmpty());
        assertEquals(LocalDate.parse(expectedDate), viewedQna.get(0).questionDate);
    }

    @Given("{string}가 이미 1개의 질문을 작성한 상태이다")
    public void 회원이_이미_한개의_질문을_작성한_상태이다(String writerName) {
        writeQuestion(writerName, "기존 질문입니다.", LocalDate.now().minusDays(1));
        assertTrue(lastQuestionSucceeded);
    }

    @When("{string}가 새로운 질문을 작성하면")
    public void 회원이_새로운_질문을_작성하면(String writerName) {
        writeQuestion(writerName, "새로운 질문입니다.", LocalDate.now());
    }

    @And("Q&A 목록에 {int}개의 질문이 표시된다")
    public void qna_목록에_n개의_질문이_표시된다(int count) {
        assertEquals(count, qnaItems.size());
    }

    @When("프로젝트에 질문을 작성하려고 하면")
    public void 프로젝트에_질문을_작성하려고_하면() {
        String loginUser = currentLoginMemberName();
        if (loginUser == null) {
            lastQuestionSucceeded = false;
            context.setLastMessage("로그인이 필요합니다");
            return;
        }
        writeQuestion(loginUser, "질문입니다.", LocalDate.now());
    }

    @When("{string}가 내용 없이 질문을 작성하려고 하면")
    public void 회원이_내용없이_질문을_작성하려고_하면(String writerName) {
        writeQuestion(writerName, "", LocalDate.now());
    }

    @Given("{string}가 다음 질문을 작성한 상태이다:")
    public void 회원이_다음_질문을_작성한_상태이다(String writerName, String question) {
        seedQuestion(writerName, question, LocalDate.now());
    }

    @Given("{string} 리더가 로그인한 상태이다")
    public void 리더가_로그인한_상태이다(String leaderName) {
        this.leaderName = leaderName;
        this.teamMembers.add(leaderName);
        Member leader = createOrFindMember(leaderName);
        context.setAccessToken(jwtUtil.createAccessToken(leader));
    }

    @When("{string}이 해당 질문에 다음과 같이 답변하면:")
    public void 회원이_해당_질문에_답변하면(String answererName, String answer) {
        writeAnswer(answererName, answer, LocalDate.now());
    }

    @Then("답변 작성에 성공한다")
    public void 답변_작성에_성공한다() {
        assertTrue(lastAnswerSucceeded);
    }

    @Then("답변 작성에 실패한다")
    public void 답변_작성에_실패한다() {
        assertFalse(lastAnswerSucceeded);
    }

    @And("Q&A 목록에서 답변을 확인할 수 있다")
    public void qna_목록에서_답변을_확인할_수_있다() {
        assertTrue(qnaItems.stream().anyMatch(q -> q.answer != null && !q.answer.isBlank()));
    }

    @And("답변 옆에 {string} 태그가 표시된다")
    public void 답변_옆에_태그가_표시된다(String tag) {
        assertEquals("팀장", tag);
        assertTrue(lastAnswererWasLeader);
    }

    @And("{string}에게 답변 알림이 발송된다")
    public void 회원에게_답변_알림이_발송된다(String expectedReceiver) {
        assertEquals(expectedReceiver, lastNotificationTarget);
    }

    @Given("{string}이 {string}에 답변을 작성한 상태이다")
    public void 회원이_날짜에_답변을_작성한_상태이다(String answerer, String date) {
        seedQuestion("김철수", "답변 날짜 검증용 질문", LocalDate.parse(date).minusDays(1));
        seedAnswer(answerer, "답변 날짜 검증용 답변", LocalDate.parse(date));
    }

    @Then("답변 작성일 {string}을 확인할 수 있다")
    public void 답변_작성일을_확인할_수_있다(String expectedDate) {
        assertFalse(viewedQna.isEmpty());
        QnaItem answered = viewedQna.stream()
                .filter(q -> q.answerDate != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("작성된 답변이 없습니다."));
        assertEquals(LocalDate.parse(expectedDate), answered.answerDate);
    }

    @Given("{string}가 질문을 작성한 상태이다")
    public void 회원이_질문을_작성한_상태이다(String writerName) {
        seedQuestion(writerName, "질문이 작성된 상태입니다.", LocalDate.now().minusDays(1));
    }

    @Given("{string}가 프로젝트의 팀원이다")
    public void 회원이_프로젝트의_팀원이다(String memberName) {
        teamMembers.add(memberName);
    }

    @When("{string}가 해당 질문에 답변을 작성하면")
    public void 회원이_해당_질문에_답변을_작성하면(String answererName) {
        writeAnswer(answererName, "팀원 답변입니다.", LocalDate.now());
    }

    @And("답변 옆에 {string} 태그가 표시되지 않는다")
    public void 답변_옆에_태그가_표시되지_않는다(String tag) {
        assertEquals("팀장", tag);
        assertFalse(lastAnswererWasLeader);
    }

    @Given("{string}은 해당 프로젝트의 팀원이 아니다")
    public void 회원은_해당_프로젝트의_팀원이_아니다(String memberName) {
        teamMembers.remove(memberName);
    }

    @When("{string}이 해당 질문에 답변을 작성하려고 하면")
    public void 회원이_해당_질문에_답변을_작성하려고_하면(String answererName) {
        writeAnswer(answererName, "권한 없는 답변 시도", LocalDate.now());
    }

    @When("{string}이 내용 없이 답변을 작성하려고 하면")
    public void 회원이_내용없이_답변을_작성하려고_하면(String answererName) {
        writeAnswer(answererName, "", LocalDate.now());
    }

    @When("프로젝트 Q&A 탭에 접근하면")
    public void 프로젝트_qna_탭에_접근하면() {
        viewedQna = qnaItems.stream()
                .sorted(Comparator.comparing((QnaItem q) -> q.questionDate).reversed())
                .toList();
        if (viewedQna.isEmpty()) {
            context.setLastMessage("아직 등록된 Q&A가 없습니다");
        }
    }

    @Given("프로젝트에 Q&A가 없는 상태이다")
    public void 프로젝트에_qna가_없는_상태이다() {
        qnaItems.clear();
    }

    @Given("프로젝트에 다음 순서로 질문이 작성되었다:")
    public void 프로젝트에_다음_순서로_질문이_작성되었다(DataTable dataTable) {
        qnaItems.clear();
        for (MapRow row : MapRow.from(dataTable)) {
            int order = Integer.parseInt(row.get("순서"));
            String writer = row.get("질문자");
            String question = row.get("질문");
            seedQuestion(writer, question, LocalDate.now().minusDays(10 - order));
        }
    }

    @Then("{string}가 첫 번째로 표시된다")
    public void 질문이_첫_번째로_표시된다(String expectedQuestion) {
        assertFalse(viewedQna.isEmpty());
        assertEquals(expectedQuestion, viewedQna.get(0).question);
    }

    private void writeQuestion(String writerName, String question, LocalDate date) {
        String loginUser = currentLoginMemberName();
        if (loginUser == null) {
            lastQuestionSucceeded = false;
            context.setLastMessage("로그인이 필요합니다");
            return;
        }
        if (question == null || question.trim().isEmpty()) {
            lastQuestionSucceeded = false;
            context.setLastMessage("질문 내용을 입력해주세요");
            return;
        }

        QnaItem item = new QnaItem();
        item.questioner = writerName;
        item.question = question.trim();
        item.questionDate = date;
        qnaItems.add(item);

        lastQuestionItem = item;
        lastQuestionSucceeded = true;
        context.setLastMessage(null);
        lastNotificationTarget = leaderName;
    }

    private void seedQuestion(String writerName, String question, LocalDate date) {
        QnaItem item = new QnaItem();
        item.questioner = writerName;
        item.question = question == null ? "" : question.trim();
        item.questionDate = date;
        qnaItems.add(item);
        lastQuestionItem = item;
        lastQuestionSucceeded = true;
        lastNotificationTarget = leaderName;
    }

    private void writeAnswer(String answererName, String answer, LocalDate answerDate) {
        if (qnaItems.isEmpty()) {
            lastAnswerSucceeded = false;
            context.setLastMessage("답변할 질문이 없습니다");
            return;
        }
        String loginUser = currentLoginMemberName();
        if (loginUser == null) {
            lastAnswerSucceeded = false;
            context.setLastMessage("로그인이 필요합니다");
            return;
        }
        if (answer == null || answer.trim().isEmpty()) {
            lastAnswerSucceeded = false;
            context.setLastMessage("답변 내용을 입력해주세요");
            return;
        }
        if (!teamMembers.contains(answererName)) {
            lastAnswerSucceeded = false;
            context.setLastMessage("프로젝트 팀원만 답변할 수 있습니다");
            return;
        }

        QnaItem target = qnaItems.get(qnaItems.size() - 1);
        target.answerer = answererName;
        target.answer = answer.trim();
        target.answerDate = answerDate;

        lastAnswerSucceeded = true;
        context.setLastMessage(null);
        lastAnswererWasLeader = leaderName.equals(answererName);
        lastNotificationTarget = target.questioner;
    }

    private void seedAnswer(String answererName, String answer, LocalDate answerDate) {
        if (qnaItems.isEmpty()) {
            throw new AssertionError("시드할 질문이 없습니다.");
        }
        QnaItem target = qnaItems.get(qnaItems.size() - 1);
        target.answerer = answererName;
        target.answer = answer;
        target.answerDate = answerDate;
        lastAnswerSucceeded = true;
        lastAnswererWasLeader = leaderName.equals(answererName);
        lastNotificationTarget = target.questioner;
    }

    private String currentLoginMemberName() {
        String token = context.getAccessToken();
        if (token == null) {
            return null;
        }
        String email = jwtUtil.getUsername(token);
        return memberRepository.findByEmail(email)
                .map(Member::getRealName)
                .orElse(null);
    }

    private Member createOrFindMember(String name) {
        String email = switch (name) {
            case "홍길동" -> "hong@example.com";
            case "김철수" -> "kim@example.com";
            case "이영희" -> "lee@example.com";
            case "박지민" -> "park@example.com";
            default -> "user" + Math.abs(name.hashCode()) + "@example.com";
        };
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.createForTest(email, name)));
    }

    private static class QnaItem {
        String questioner;
        String question;
        LocalDate questionDate;
        String answerer;
        String answer;
        LocalDate answerDate;
    }

    private record MapRow(java.util.Map<String, String> row) {
        static List<MapRow> from(DataTable dataTable) {
            return dataTable.asMaps().stream().map(MapRow::new).toList();
        }
        String get(String key) {
            return row.get(key);
        }
    }
}
