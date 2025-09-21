package com.wardk.meeteam_backend.web.chat;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRequestDto;
import com.wardk.meeteam_backend.domain.chat.service.ChatMessageService;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/**
 * WebSocket을 통한 실시간 채팅 메시지 처리를 담당하는 컨트롤러입니다.
 *
 * <p>단순화된 WebSocket 컨트롤러:</p>
 * <ul>
 *   <li>기존 Spring Security JWT 인증 활용</li>
 *   <li>복잡한 세션 관리 제거</li>
 *   <li>@AuthenticationPrincipal로 사용자 정보 직접 활용</li>
 *   <li>단일 서버 환경에 최적화</li>
 * </ul>
 *
 * <p>간소화된 메시지 처리 흐름:</p>
 * <ol>
 *   <li>클라이언트가 이미 JWT로 HTTP 인증 완료</li>
 *   <li>WebSocket 연결 시 추가 인증 불필요</li>
 *   <li>@AuthenticationPrincipal로 사용자 정보 자동 주입</li>
 *   <li>비즈니스 로직 처리 후 실시간 브로드캐스트</li>
 * </ol>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지를 전송합니다.
     *
     * @param message 전송할 메시지 정보
     * @param userDetails Spring Security에서 인증된 사용자 정보
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatRequestDto.SendMessage message,
                           @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        try {
            Long userId = userDetails.getMemberId();

            log.info("WebSocket 메시지 전송 - 사용자: {}, 채팅방: {}",
                    userId, message.getChatRoomId());

            ChatMessageDto sentMessage = chatMessageService.sendMessage(message, userId);

            log.info("WebSocket 메시지 전송 완료 - 메시지 ID: {}", sentMessage.getId());

        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패 - 채팅방: {}, 오류: {}",
                    message.getChatRoomId(), e.getMessage());

            sendErrorToUser(userDetails.getMemberId(),
                    "메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 메시지를 수정합니다.
     */
    @MessageMapping("/chat.editMessage")
    public void editMessage(@Payload ChatRequestDto.EditMessage request,
                           @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        try {
            Long userId = userDetails.getMemberId();

            log.info("WebSocket 메시지 수정 - 사용자: {}, 메시지: {}",
                    userId, request.getMessageId());

            chatMessageService.editMessage(request, userId);

        } catch (Exception e) {
            log.error("WebSocket 메시지 수정 실패 - 메시지: {}, 오류: {}",
                    request.getMessageId(), e.getMessage());

            sendErrorToUser(userDetails.getMemberId(),
                    "메시지 수정에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 메시지를 삭제합니다.
     */
    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload Long messageId,
                             @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        try {
            Long userId = userDetails.getMemberId();

            log.info("WebSocket 메시지 삭제 - 사용자: {}, 메시지: {}", userId, messageId);

            chatMessageService.deleteMessage(messageId, userId);

        } catch (Exception e) {
            log.error("WebSocket 메시지 삭제 실패 - 메시지: {}, 오류: {}",
                    messageId, e.getMessage());

            sendErrorToUser(userDetails.getMemberId(),
                    "메시지 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 타이핑 상태를 전송합니다.
     */
    @MessageMapping("/chat.typing/{chatRoomId}")
    public void handleTyping(@DestinationVariable Long chatRoomId,
                            @Payload Boolean isTyping,
                            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        try {
            Long userId = userDetails.getMemberId();
            chatMessageService.sendTypingStatus(chatRoomId, userId, isTyping);

        } catch (Exception e) {
            log.error("타이핑 상태 전송 실패 - 채팅방: {}, 오류: {}", chatRoomId, e.getMessage());
        }
    }

    /**
     * 읽음 상태를 업데이트합니다.
     */
    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload ChatRequestDto.UpdateReadStatus request,
                          @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        try {
            Long userId = userDetails.getMemberId();
            chatMessageService.updateReadStatus(request.getChatRoomId(), userId);

        } catch (Exception e) {
            log.error("읽음 상태 업데이트 실패 - 채팅방: {}, 오류: {}",
                    request.getChatRoomId(), e.getMessage());
        }
    }

    /**
     * 특정 사용자에게 에러 메시지를 전송합니다.
     */
    private void sendErrorToUser(Long userId, String errorMessage) {
        try {
            ChatMessageDto errorDto = ChatMessageDto.builder()
                    .type(ChatMessageDto.ChatMessageType.ERROR)
                    .content(errorMessage)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/errors",
                    errorDto
            );

            log.warn("사용자 {}에게 에러 메시지 전송: {}", userId, errorMessage);
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
        }
    }
}
