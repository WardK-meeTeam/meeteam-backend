package com.wardk.meeteam_backend.domain.chat.service;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRequestDto;
import com.wardk.meeteam_backend.domain.chat.entity.*;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 메시지 관리를 담당하는 서비스 클래스입니다. (TODO: 구현 예정)
 *
 * <p>이 서비스는 다음과 같은 기능을 제공할 예정입니다:</p>
 * <ul>
 *   <li>실시간 메시지 전송 및 WebSocket 알림</li>
 *   <li>메시지 수정 및 삭제 (작성자 권한 확인)</li>
 *   <li>커서 기반 페이징을 통한 메시지 조회</li>
 *   <li>읽음 상태 관리 및 업데이트</li>
 *   <li>타이핑 상태 실시간 전송</li>
 *   <li>멘션 기능 및 개별 알림</li>
 *   <li>읽지 않은 메시지 수 자동 관리</li>
 * </ul>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatMessageService {

    // TODO: 실제 레포지토리들 구현 필요
    // private final ChatMessageRepository chatMessageRepository;
    // private final ChatRoomRepository chatRoomRepository;
    // private final ChatRoomMemberRepository chatRoomMemberRepository;
    // private final MemberRepository memberRepository;
    // private final SimpMessagingTemplate messagingTemplate;

    /**
     * 새로운 메시지를 전송하고 실시간으로 알림을 보냅니다.
     *
     * TODO: 구현 필요
     * - 채팅방 권한 확인 로직
     * - 메시지 저장 및 WebSocket 브로드캐스트
     * - 멘션 기능 및 알림
     * - 읽지 않은 메시지 수 관리
     */
    @Transactional
    public ChatMessageDto sendMessage(ChatRequestDto.SendMessage request, Long senderId) {
        log.info("메시지 전송 시도 - 채팅방 ID: {}, 발신자 ID: {}", request.getChatRoomId(), senderId);

        // TODO: 실제 메시지 전송 로직 구현
        /*
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 발신자 정보 조회
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 채팅방 참여 권한 확인
        chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(request.getChatRoomId(), senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 메시지 생성 및 저장
        // WebSocket 브로드캐스트
        // 멘션 알림 처리
        */

        log.warn("ChatMessageService.sendMessage() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatMessageService.sendMessage() 구현 필요");
    }

    /**
     * 메시지 수정
     *
     * TODO: 구현 필요
     * - 작성자 권한 확인
     * - 메시지 수정 처리
     * - 실시간 수정 알림
     */
    @Transactional
    public ChatMessageDto editMessage(ChatRequestDto.EditMessage request, Long userId) {
        log.info("메시지 수정 시도 - 메시지 ID: {}, 사용자 ID: {}", request.getMessageId(), userId);

        log.warn("ChatMessageService.editMessage() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatMessageService.editMessage() 구현 필요");
    }

    /**
     * 메시지 삭제
     *
     * TODO: 구현 필요
     * - 작성자 권한 확인
     * - 소프트 삭제 처리
     * - 실시간 삭제 알림
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        log.info("메시지 삭제 시도 - 메시지 ID: {}, 사용자 ID: {}", messageId, userId);

        log.warn("ChatMessageService.deleteMessage() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatMessageService.deleteMessage() 구현 필요");
    }

    /**
     * 채팅방 메시지 조회 (페이징)
     *
     * TODO: 구현 필요
     * - 커서 기반 페이징
     * - 권한 확인
     * - DTO 변환
     */
    public List<ChatMessageDto> getChatMessages(Long chatRoomId, Long cursor, int size, Long userId) {
        log.info("채팅방 메시지 조회 - 채팅방 ID: {}, 사용자 ID: {}", chatRoomId, userId);

        log.warn("ChatMessageService.getChatMessages() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatMessageService.getChatMessages() 구현 필요");
    }

    /**
     * 읽음 상태 업데이트
     *
     * TODO: 구현 필요
     * - 마지막 읽은 시간 업데이트
     * - 읽지 않은 메시지 수 초기화
     */
    @Transactional
    public void updateReadStatus(Long chatRoomId, Long userId) {
        log.info("읽음 상태 업데이트 - 채팅방 ID: {}, 사용자 ID: {}", chatRoomId, userId);

        log.warn("ChatMessageService.updateReadStatus() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatMessageService.updateReadStatus() 구현 필요");
    }

    /**
     * 타이핑 상태 전송
     *
     * TODO: 구현 필요
     * - 실시간 타이핑 상태 브로드캐스트
     * - 권한 확인
     */
    public void sendTypingStatus(Long chatRoomId, Long userId, boolean isTyping) {
        log.info("타이핑 상태 전송 - 채팅방 ID: {}, 사용자 ID: {}, 타이핑: {}", chatRoomId, userId, isTyping);

        log.warn("ChatMessageService.sendTypingStatus() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatMessageService.sendTypingStatus() 구현 필요");
    }

    /**
     * 멘션 알림 전송 (TODO: 구현 필요)
     */
    private void sendMentionNotifications(List<Long> mentionedUserIds, ChatMessageDto messageDto) {
        // TODO: 멘션된 사용자들에게 개별 알림 전송 구현
    }

    /**
     * ChatMessage 엔티티를 ChatMessageDto로 변환합니다. (TODO: 구현 필요)
     */
    private ChatMessageDto convertToChatMessageDto(ChatMessage message, Member sender) {
        // TODO: 엔티티→DTO 변환 로직 구현
        return ChatMessageDto.builder().build();
    }
}
