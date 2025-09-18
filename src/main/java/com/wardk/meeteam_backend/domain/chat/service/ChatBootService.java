package com.wardk.meeteam_backend.domain.chat.service;


import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.chat.entity.SenderRole;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatThreadRepository;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ChatBootService {
  private final ChatThreadRepository chatThreadRepository;
  private final ChatMessageRepository chatMessageRepository;

  /**
   * PR마다 1개의 ChatThread를 갖도록 보장
   * - 최초 생성 시 SYSTEM/USER 초기 메시지 삽입
   */
  @Transactional
  public ChatThread ensureThreadForPr(PullRequest pullRequest, String summary, Long memberId) {
    // 이미 존재하면 재사용
    Optional<ChatThread> existing = chatThreadRepository.findByPullRequest(pullRequest);
    if (existing.isPresent()) return existing.get();

    ChatThread saved;
    try {
      ChatThread thread = ChatThread.builder()
          .memberId(memberId)
          .pullRequest(pullRequest)
          .title(summary)
          .build();
      saved = chatThreadRepository.save(thread);
    } catch (DataIntegrityViolationException dup) {
      // 동시성 문제로 이미 생성된 경우 재조회
      return chatThreadRepository.findByPullRequest(pullRequest).orElseThrow();
    }

    // 초기 메시지 삽입
    ChatMessage sys = ChatMessage.builder()
        .threadId(saved.getId())
        .senderRole(SenderRole.SYSTEM)
        .content("""
            잠시만 기다려주세요. AI가 PR 내용을 분석하고 있습니다. :)
            """)
        .build();
    chatMessageRepository.save(sys);

    return saved;
  }
}
