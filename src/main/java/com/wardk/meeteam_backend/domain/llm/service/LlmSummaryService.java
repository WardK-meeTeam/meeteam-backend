package com.wardk.meeteam_backend.domain.llm.service;

import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import com.wardk.meeteam_backend.domain.chat.service.ChatService;
import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewFinding;
import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.codereview.repository.PrReviewFindingRepository;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTaskResult;
import com.wardk.meeteam_backend.domain.llm.repository.LlmTaskResultRepository;
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

/**
 * PR 리뷰 결과들을 종합하여 요약 생성을 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmSummaryService {

    private final ChatClient chatClient;
    private final LlmTaskResultRepository taskResultRepository;
    private final PrReviewFindingRepository findingRepository;
    private final ChatService chatService;

    /**
     * PR에 대한 요약을 생성합니다.
     *
     * @param summaryTask 요약 태스크
     * @return 요약 태스크 결과
     */
    @Transactional
    public LlmTaskResult createPrSummary(LlmTask summaryTask) {
        PrReviewJob reviewJob = summaryTask.getPrReviewJob();

        try {
            log.info("PR 요약 생성 시작: PR #{}", reviewJob.getPrNumber());

            // 파일별 리뷰 결과 조회
            List<PrReviewFinding> findings = findingRepository.findByPrReviewJob(reviewJob);

            if (findings.isEmpty()) {
                log.warn("PR #{} 리뷰 결과가 없습니다.", reviewJob.getPrNumber());
                return createEmptySummaryResult(summaryTask);
            }

            // 리뷰 결과를 포맷팅하여 요약 프롬프트 생성
            List<Message> messages = createSummaryPrompt(reviewJob, findings);
            Prompt prompt = new Prompt(messages);

            // LLM 호출
            log.info("PR #{} 요약 생성을 위한 LLM 호출", reviewJob.getPrNumber());
            ChatResponse response = chatClient.prompt(prompt)
                    .call()
                    .chatResponse();

            // 요약 내용 추출
            String summaryContent = response.getResults() != null && !response.getResults().isEmpty()
                ? response.getResults().get(0).getOutput().getText()
                : "요약 생성 중 오류가 발생했습니다.";
            log.info("PR #{} 요약 생성 완료", reviewJob.getPrNumber());

            // 채팅 메시지로 저장
            ChatMessage chatMessage = null;
            if (reviewJob.getChatRoom() != null) {
                try {
//                    chatMessage = chatService.createSystemMessage(
//                        reviewJob.getChatThread().getId(),
//                        summaryContent,
//                        "gpt-4", // 모델명 - 실제 사용한 모델명으로 수정 필요
//                        response.getMetadata().getUsage().getTotalTokens()
//                    );
                    log.info("PR #{} 요약이 채팅방에 저장되었습니다. 메시지 ID: {}",
                            reviewJob.getPrNumber(), chatMessage.getId());
                } catch (Exception e) {
                    log.error("PR #{} 요약을 채팅방에 저장하는 중 오류 발생", reviewJob.getPrNumber(), e);
                }
            }

            // 요약 결과 저장
            LlmTaskResult result = LlmTaskResult.builder()
                    .resultType("PR_SUMMARY")
                    .content(summaryContent)
                    .tokenUsage(response.getMetadata().getUsage().getTotalTokens())
                    .chatMessageId(chatMessage != null ? chatMessage.getId() : null)
                    .build();

            return taskResultRepository.save(result);

        } catch (Exception e) {
            log.error("PR #{} 요약 생성 중 오류 발생", reviewJob.getPrNumber(), e);

            // 오류 시 간단한 결과라도 반환
            LlmTaskResult errorResult = LlmTaskResult.builder()
                    .resultType("PR_SUMMARY_ERROR")
                    .content("요약 생성 중 오류가 발생했습니다: " + e.getMessage())
                    .build();

            return taskResultRepository.save(errorResult);
        }
    }

    /**
     * 리뷰 결과가 없을 때 빈 요약 결과를 생성합니다.
     */
    public LlmTaskResult createEmptySummaryResult(LlmTask summaryTask) {
        String emptyContent = "이 PR에 대한 리뷰 결과가 없습니다.";

        // 채팅 메시지로 저장
        ChatMessage chatMessage = null;
        if (summaryTask.getPrReviewJob().getChatRoom() != null) {
            try {
//                chatMessage = chatService.saveSystemMessage(
//                    summaryTask.getPrReviewJob().getChatThread().getId(),
//                    emptyContent,
//                    null, // 모델명 없음
//                    null  // 토큰 사용량 없음
//                );
                log.info("PR #{} 빈 요약이 채팅방에 저장되었습니다. 메시지 ID: {}",
                        summaryTask.getPrReviewJob().getPrNumber(), chatMessage.getId());
            } catch (Exception e) {
                log.error("PR #{} 빈 요약을 채팅방에 저장하는 중 오류 발생",
                         summaryTask.getPrReviewJob().getPrNumber(), e);
            }
        }

        // 요약 결과 저장
        LlmTaskResult result = LlmTaskResult.builder()
                .resultType("PR_SUMMARY_EMPTY")
                .content(emptyContent)
                .chatMessageId(chatMessage != null ? chatMessage.getId() : null)
                .build();

        return taskResultRepository.save(result);
    }

    /**
     * 요약 프롬프트를 생성합니다.
     */
    public List<Message> createSummaryPrompt(PrReviewJob reviewJob, List<PrReviewFinding> findings) {
        List<Message> messages = new ArrayList<>();

        // 시스템 메시지
        String systemPrompt = """
            당신은 코드 리뷰 분석가로, PR에 대한 여러 파일의 리뷰 결과를 종합하여 간결하고 유용한 요약을 제공합니다.
            다음 지침을 따라 요약을 작성하세요:

            필요한 경우 마크다운 형식을 사용하여 구조화된 응답을 제공하세요.
            """;
        messages.add(new SystemMessage(systemPrompt));

        // 사용자 메시지
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(String.format("# PR #%d 리뷰 요약\n\n", reviewJob.getPrNumber()));
        userPrompt.append("다음은 PR의 각 파일에 대한 리뷰 결과입니다:\n\n");

        // 파일별 리뷰 결과 포맷팅
        for (PrReviewFinding finding : findings) {
            userPrompt.append(String.format("## 파일: %s\n", finding.getFilePath()));

            // 내용 요약 (너무 길면 잘라내기)
            String content = finding.getChatResponse();
            if (content != null) {
                if (content.length() > 250) {
                    content = content.substring(0, 500) + "... (생략됨)";
                }
                userPrompt.append("\n```\n").append(content).append("\n```\n\n");
            }

            userPrompt.append("---\n\n");
        }

        userPrompt.append("위 리뷰 결과를 종합하여 PR에 대한 전체적인 요약과 개선 제안을 제공해주세요.");

        messages.add(new UserMessage(userPrompt.toString()));

        return messages;
    }
}
