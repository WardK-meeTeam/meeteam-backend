// src/main/java/com/wardk/meeteam_backend/ai/tool/PrTools.java
package com.wardk.meeteam_backend.domain.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestFileRepository;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmToolCall {

    private final PullRequestRepository pullRequestRepository;
    private final PullRequestFileRepository pullRequestFileRepository;

    /**
     * PR의 전체 diff를 가져옵니다.
     */
    @Tool(description = "PR의 전체 diff를 가져옵니다. 모든 변경된 파일의 diff를 포함합니다.")
    public PullRequest getPrDiff(Long PullRequestId) {
        PullRequest pr = pullRequestRepository.findById(PullRequestId)
            .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));

        return pr;
    }

    // TODO: 필요한 Tool들 추가 구현    

    /**
     * 특정 파일의 변경사항(patch)을 가져옵니다.
     */
    

    /**
     * PR의 모든 변경된 파일 목록과 각 파일의 patch를 가져옵니다.
     */
    

    /**
     * 파일의 변경 전/후 내용을 비교합니다.
     */
}