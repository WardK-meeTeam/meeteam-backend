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
 * 채팅 메시지 관리를 담당하는 서비스 클래스입니다.
 *
 * <p>이 서비스는 다음과 같은 기능을 제공합니다:</p>
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
 * <p>모든 메시지 관련 작업은 WebSocket을 통해 실시간으로 다른 참여자들에게 전달되며,
 * 데이터 무결성을 위해 트랜잭션으로 관리됩니다.</p>
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

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 새로운 메시지를 전송하고 실시간으로 알림을 보냅니다.
     *
     * <p>메시지 전송 과정:</p>
     * <ol>
     *   <li>채팅방 존재 여부 및 참여 권한 확인</li>
     *   <li>메시지 데이터베이스 저장</li>
     *   <li>채팅방 마지막 메시지 시간 업데이트</li>
     *   <li>다른 멤버들의 읽지 않은 메시지 수 증가</li>
     *   <li>WebSocket을 통한 실시간 전송</li>
     *   <li>멘션된 사용자들에게 개별 알림 전송</li>
     * </ol>
     *
     * @param request 메시지 전송 요청 (채팅방 ID, 내용, 멘션 사용자 등)
     * @param senderId 메시지를 보내는 사용자 ID
     * @return 전송된 메시지 정보
     * @throws CustomException 채팅방을 찾을 수 없거나 접근 권한이 없는 경우
     */
    @Transactional
    public ChatMessageDto sendMessage(ChatRequestDto.SendMessage request, Long senderId) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 발신자 정보 조회
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 채팅방 참여 권한 확인
        chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(request.getChatRoomId(), senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .memberId(senderId)
                .senderRole(SenderRole.USER)
                .content(request.getContent())
                .messageType(MessageType.TEXT)
                .mentionedUserIds(request.getMentionedUserIds() != null ?
                    String.join(",", request.getMentionedUserIds().stream()
                            .map(String::valueOf).collect(Collectors.toList())) : null)
                .build();

        message = chatMessageRepository.save(message);

        // 채팅방 마지막 메시지 시간 업데이트
        chatRoom.updateLastMessageTime(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // 다른 멤버들의 읽지 않은 메시지 수 증가
        chatRoomMemberRepository.incrementUnreadCountForOtherMembers(request.getChatRoomId(), senderId);

        // DTO 변환
        ChatMessageDto messageDto = convertToChatMessageDto(message, sender);

        // WebSocket으로 실시간 전송
        messagingTemplate.convertAndSend("/topic/chat/" + request.getChatRoomId(), messageDto);

        // 멘션된 사용자에게 개별 알림 전송
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            sendMentionNotifications(request.getMentionedUserIds(), messageDto);
        }

        log.info("메시지 전송 완료 - 채팅방: {}, 발신자: {}", request.getChatRoomId(), senderId);
        return messageDto;
    }

    /**
     * 메시지 수정
     */
    @Transactional
    public ChatMessageDto editMessage(ChatRequestDto.EditMessage request, Long userId) {
        ChatMessage message = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 작성자 본인인지 확인
        if (!message.getMemberId().equals(userId)) {
            throw new CustomException(ErrorCode.MESSAGE_EDIT_NOT_ALLOWED);
        }

        // 삭제된 메시지는 수정 불가
        if (message.getIsDeleted()) {
            throw new CustomException(ErrorCode.MESSAGE_ALREADY_DELETED);
        }

        // 메시지 수정
        message.editMessage(request.getContent());
        message = chatMessageRepository.save(message);

        // 발신자 정보 조회
        Member sender = memberRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessageDto messageDto = convertToChatMessageDto(message, sender);
        messageDto.setType(ChatMessageDto.ChatMessageType.EDIT);

        // WebSocket으로 수정 알림 전송
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoom().getId(), messageDto);

        return messageDto;
    }

    /**
     * 메시지 삭제
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 작성자 본인인지 확인
        if (!message.getMemberId().equals(userId)) {
            throw new CustomException(ErrorCode.MESSAGE_DELETE_NOT_ALLOWED);
        }

        // 메시지 삭제 처리
        message.deleteMessage();
        chatMessageRepository.save(message);

        // WebSocket으로 삭제 알림 전송
        ChatMessageDto deleteDto = ChatMessageDto.builder()
                .id(messageId)
                .chatRoomId(message.getChatRoom().getId())
                .type(ChatMessageDto.ChatMessageType.DELETE)
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoom().getId(), deleteDto);
    }

    /**
     * 채팅방 메시지 조회 (페이징)
     */
    public List<ChatMessageDto> getChatMessages(Long chatRoomId, Long cursor, int size, Long userId) {
        // 채팅방 접근 권한 확인
        chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        Pageable pageable = PageRequest.of(0, size);
        List<ChatMessage> messages;

        if (cursor == null) {
            // 첫 페이지
            messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, pageable);
        } else {
            // 커서 기반 페이징
            messages = chatMessageRepository.findByChatRoomIdBeforeCursor(chatRoomId, cursor, pageable);
        }

        return messages.stream()
                .map(message -> {
                    Member sender = null;
                    if (message.getMemberId() != null) {
                        sender = memberRepository.findById(message.getMemberId()).orElse(null);
                    }
                    return convertToChatMessageDto(message, sender);
                })
                .collect(Collectors.toList());
    }

    /**
     * 읽음 상태 업데이트
     */
    @Transactional
    public void updateReadStatus(Long chatRoomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        member.updateLastReadTime(LocalDateTime.now());
        chatRoomMemberRepository.save(member);
    }

    /**
     * 타이핑 상태 전송
     */
    public void sendTypingStatus(Long chatRoomId, Long userId, boolean isTyping) {
        // 채팅방 접근 권한 확인
        chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessageDto typingDto = ChatMessageDto.builder()
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .senderName(member.getNickname())
                .type(isTyping ? ChatMessageDto.ChatMessageType.TYPING_START : ChatMessageDto.ChatMessageType.TYPING_END)
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, typingDto);
    }

    /**
     * 멘션 알림 전송
     */
    private void sendMentionNotifications(List<Long> mentionedUserIds, ChatMessageDto messageDto) {
        mentionedUserIds.forEach(userId -> {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/mention",
                    messageDto
            );
        });
    }

    /**
     * ChatMessage 엔티티를 ChatMessageDto로 변환합니다.
     *
     * <p>엔티티의 모든 정보를 클라이언트가 사용할 수 있는 DTO 형태로 변환합니다.
     * 발신자 정보가 없는 경우(시스템 메시지) "시스템"으로 표시됩니다.</p>
     *
     * @param message 변환할 ChatMessage 엔티티
     * @param sender 발신자 정보 (시스템 메시지의 경우 null 가능)
     * @return 변환된 ChatMessageDto
     */
    private ChatMessageDto convertToChatMessageDto(ChatMessage message, Member sender) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getMemberId())
                .senderName(sender != null ? sender.getNickname() : "시스템")
                .senderProfileImage(sender != null ? sender.getProfileImgSrc() : null)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .mentionedUserIds(message.getMentionedUserIds() != null ?
                    List.of(message.getMentionedUserIds().split(","))
                            .stream().map(Long::valueOf).collect(Collectors.toList()) : null)
                .type(ChatMessageDto.ChatMessageType.CHAT)
                .build();
    }
}
