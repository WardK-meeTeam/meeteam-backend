package com.wardk.meeteam_backend.domain.chat.service;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import com.wardk.meeteam_backend.domain.chat.entity.ChatRoom;
import com.wardk.meeteam_backend.domain.chat.entity.MessageType;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.web.chat.dto.MessageSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * WebSocket 채팅 관련 비즈니스 로직을 처리하는 서비스
 * 기존 채팅 구조에 WebSocket 실시간 기능을 추가
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;

    /**
     * WebSocket을 통한 채팅 메시지를 저장하고 응답 DTO를 반환합니다.
     *
     * @param senderId 발신자 ID
     * @param roomId 채팅방 ID
     * @param messageRequest 메시지 전송 요청
     * @return 저장된 채팅 메시지 DTO
     */
    @Transactional
    public ChatMessageDto sendMessage(Long senderId, Long roomId, MessageSendRequest messageRequest) {
        // 발신자 조회
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + senderId));

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + roomId));

        // 채팅 메시지 엔티티 생성
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(messageRequest.text())
                .messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        // 메시지 저장
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // 채팅방 최근 메시지 업데이트
        chatRoom.updateLastMessage(savedMessage);

        log.info("WebSocket 채팅 메시지 저장 완료 - ID: {}, 발신자: {}, 채팅방: {}",
                savedMessage.getId(), sender.getRealName(), chatRoom.getId());

        // 응답 DTO 변환
        return convertToDto(savedMessage);
    }

    /**
     * 시스템 메시지를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @param content 시스템 메시지 내용
     * @return 시스템 메시지 DTO
     */
    @Transactional
    public ChatMessageDto createSystemMessage(Long userId, Long roomId, String content) {
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + roomId));

        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .sentAt(LocalDateTime.now())
                .isRead(true) // 시스템 메시지는 읽음 처리
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(systemMessage);
        return convertToDto(savedMessage);
    }

    /**
     * ChatMessage 엔티티를 ChatMessageDto로 변환합니다.
     *
     * @param chatMessage 채팅 메시지 엔티티
     * @return 채팅 메시지 DTO
     */
    private ChatMessageDto convertToDto(ChatMessage chatMessage) {
        Member sender = chatMessage.getSender();

        return ChatMessageDto.builder()
                .id(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoom().getId())
                .senderId(sender.getId())
                .senderName(sender.getRealName())
                .senderProfileImage(sender.getStoreFileName())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .createdAt(chatMessage.getSentAt())
                .isEdited(false)
                .isDeleted(false)
                .build();
    }
}
