package com.wardk.meeteam_backend.domain.llm.service;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewFinding;
import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.codereview.repository.PrReviewFindingRepository;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTaskResult;
import com.wardk.meeteam_backend.domain.llm.repository.LlmTaskResultRepository;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LlmReviewService {

    private final ChatClient chatClient;
    private final PrReviewFindingRepository prReviewFindingRepository;
    private final LlmTaskResultRepository llmTaskResultRepository;

    /**
     * LLM을 사용하여 PR 파일에 대한 코드 리뷰를 수행합니다.
     *
     * @param task 리뷰할 LLM 태스크
     * @return LLM의 응답
     */
    public LlmTaskResult reviewFile(LlmTask task) {
        PullRequestFile file = task.getPullRequestFile();

        log.info("파일 리뷰 시작: {}", file.getFileName());

        // 프롬프트 생성
        List<Message> messages = createReviewPrompt(file);
        Prompt prompt = new Prompt(messages);

        // LLM 호출
        
        ChatResponse response = chatClient.prompt(prompt)
                .call()
                .chatResponse();

        LlmTaskResult result = savePrReviewFindingAndLlmTaskResult(file, response, task);

        log.info("pr번호: {}, fileName: {}, reviewFinding에 저장 완료", file.getPullRequest().getPrNumber(),
                file.getFileName());

        return result;
    }

    /*
     * PR 리뷰 발견 항목을 저장합니다.
     * 
     * @param file 리뷰할 Pull Request 파일
     * 
     * @param job 리뷰 작업
     * 
     * @param chatResponse LLM의 응답
     */
    private LlmTaskResult savePrReviewFindingAndLlmTaskResult(PullRequestFile file, ChatResponse chatResponse,
            LlmTask task) {

        PrReviewFinding finding = PrReviewFinding.builder()
                .prReviewJob(task.getPrReviewJob())
                .pullRequest(task.getPrReviewJob().getPullRequest())
                .filePath(file.getFileName())
                .severity(PrReviewFinding.Severity.NOTICE)
                .title("리뷰: " + file.getFileName())
                .chatResponse(chatResponse.getResults().toString())
                .status(PrReviewFinding.Status.OPEN)
                .build();
        prReviewFindingRepository.save(finding);

        LlmTaskResult llmTaskResult = LlmTaskResult.builder()
                .resultType("FILE_REVIEW")
                .content(chatResponse.getResults().toString())
                .tokenUsage(chatResponse.getMetadata().getUsage().getTotalTokens())
                .prReviewFinding(finding)
                .build();

        return llmTaskResultRepository.save(llmTaskResult);
    }

    /**
     * 코드 리뷰를 위한 프롬프트를 생성합니다.
     * 
     * @param file 리뷰할 Pull Request 파일
     * @return 프롬프트 메시지 목록
     */
    private List<Message> createReviewPrompt(PullRequestFile file) {
        List<Message> messages = new ArrayList<>();

        // 시스템 메시지: LLM의 역할 정의 (대폭 간소화)
        String systemPrompt = """
                코드 리뷰어로서 다음 파일을 간략히 분석하세요:
                1. 주요 문제점 1개 (있는 경우만)
                2. 개선 제안 1개 (있는 경우만)
                3. 긍정적 측면 1개

                ## 리뷰 지침
                아래 코드를 분석하여 다음을 제공하세요:
                1. 코드 품질 평가 (가독성, 네이밍, 복잡도)
                """;
        messages.add(new SystemMessage(systemPrompt));

        // 사용자 메시지 (대폭 간소화)
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("## 파일명: ").append(file.getFileName()).append("\n\n");
        userPrompt.append("## 변경 유형: ").append(file.getStatus()).append("\n\n");

        if (file.getPatch() != null) {
            userPrompt.append("## 변경 내용 (patch):\n```diff\n")
                    .append(file.getPatch())
                    .append("\n```\n");
        }

        messages.add(new UserMessage(userPrompt.toString()));

        return messages;
    }
}