package com.wardk.meeteam_backend.domain.chat.service;

import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import com.wardk.meeteam_backend.domain.chat.entity.SenderRole;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatThreadRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 채팅 메시지와 관련된 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 채팅 메시지 저장 및 조회 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatThreadRepository chatThreadRepository;
  private final MemberRepository memberRepository;

  // 채팅 메시지 저장
  @Transactional
  public void saveChatMessage(Long threadId, String email, String text) {
    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    // thread 없으면 예외 처리
    checkThreadExists(threadId);

    ChatMessage chatMessage = ChatMessage.builder()
        .threadId(threadId)
        .senderRole(SenderRole.USER)
        .memberId(member.getId())
        .content(text)
        .build();

    chatMessageRepository.save(chatMessage);
  }

  // cursor 기반 페이지네이션으로 메시지 조회
  @Transactional(readOnly = true)
  public ChatMessageResponse getMessages(Long threadId, Long cursor, int pageSize) {
    // thread 없으면 예외 처리
    checkThreadExists(threadId);

    // cursor 기준으로 페이지 조회
    List<ChatMessage> messages;
    Pageable pageable = PageRequest.of(0, pageSize);
    if (cursor == null)
      messages = chatMessageRepository.firstPage(threadId, pageable);
    else
      messages = chatMessageRepository.pageAfter(threadId, cursor, pageable);

    // 메시지가 없으면 cursor null 반환
    if(messages.isEmpty())
      return new ChatMessageResponse(messages, null);
    else // 마지막 메시지 ID 반환
      return new ChatMessageResponse(messages, messages.get(messages.size() - 1).getId());
  }


  // --------------------------- private 메서드 ---------------------------

  private void checkThreadExists(Long threadId) {
    if (!chatThreadRepository.existsById(threadId)) {
      throw new CustomException(ErrorCode.CHAT_THREAD_NOT_FOUND);
    }
  }
}