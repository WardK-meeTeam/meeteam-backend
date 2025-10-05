package com.wardk.meeteam_backend.web.codereview.controller;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.codereview.service.CodeReviewChatBroadcastService;
import com.wardk.meeteam_backend.domain.codereview.service.CodeReviewChatService;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.chat.dto.MessageSendRequest;
import com.wardk.meeteam_backend.web.codereview.dto.CodeReviewChatRoomDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 코드리뷰 전용 채팅 컨트롤러
 * PR 리뷰 과정에서 발생하는 실시간 채팅과 REST API를 모두 처리
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/codereviews/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CodeReview Chat", description = "코드리뷰 채팅 관련 API")
public class CodeReviewChatController {

    private final CodeReviewChatService codeReviewChatService;
    private final CodeReviewChatBroadcastService broadcastService;
    private final SimpMessagingTemplate messagingTemplate;


    /**
     * 채팅방 메시지 히스토리 조회 (최적화된 버전)
     * 필요한 컬럼만 조회하여 성능이 향상된 버전
     */
    @GetMapping("/chatroom/{chatRoomId}/messages/optimized")
    @Operation(summary = "채팅방 메시지 히스토리", description = "필요한 컬럼만 조회하여 메시지 히스토리 조회")
    public SuccessResponse<Page<ChatMessageDto>> getChatHistoryOptimized(
            @Parameter(description = "채팅방 ID") @PathVariable Long chatRoomId,
            @PageableDefault(size = 50) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        log.info("최적화된 채팅방 메시지 히스토리 조회 - 채팅방 ID: {}, 사용자 ID: {}", chatRoomId, userDetails.getMemberId());

        Page<ChatMessageDto> messages = codeReviewChatService.getChatHistoryByChatRoomId(chatRoomId, pageable);

        return SuccessResponse.onSuccess(messages);
    }

    /**
     * 채팅방 기본 메시지 조회 (가장 빠른 버전)
     * 간단한 메시지 목록만 필요할 때 사용
     */
    @GetMapping("/chatroom/{chatRoomId}/messages/basic")
    @Operation(summary = "채팅방 기본 메시지 조회", description = "기본 정보만 포함된 빠른 메시지 조회")
    public SuccessResponse<Page<ChatMessageDto>> getBasicChatHistory(
            @Parameter(description = "채팅방 ID") @PathVariable Long chatRoomId,
            @PageableDefault(size = 50) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        log.info("기본 채팅방 메시지 조회 - 채팅방 ID: {}, 사용자 ID: {}", chatRoomId, userDetails.getMemberId());

        Page<ChatMessageDto> messages = codeReviewChatService.getBasicChatHistory(chatRoomId, pageable);

        return SuccessResponse.onSuccess(messages);
    }

    /**
     * 특정 시간 이후 최신 메시지 조회 (실시간 채팅용)
     */
    @GetMapping("/chatroom/{chatRoomId}/messages/recent")
    @Operation(summary = "최신 메시지 조회", description = "특정 시간 이후의 새로운 메시지를 조회합니다")
    public SuccessResponse<List<ChatMessageDto>> getRecentMessages(
            @Parameter(description = "채팅방 ID") @PathVariable Long chatRoomId,
            @Parameter(description = "기준 시간") @RequestParam String afterTime,
            @Parameter(description = "최대 조회 개수") @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        log.info("최신 메시지 조회 - 채팅방 ID: {}, 기준 시간: {}, 사용자 ID: {}",
                chatRoomId, afterTime, userDetails.getMemberId());

        try {
            LocalDateTime afterDateTime = LocalDateTime.parse(afterTime);
            List<ChatMessageDto> messages = codeReviewChatService.getRecentMessages(chatRoomId, afterDateTime, limit);

            return SuccessResponse.onSuccess(messages);
        } catch (Exception e) {
            log.error("시간 파싱 오류: {}", afterTime, e);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 채팅방 ID로 직접 메시지 전송 (권장)
     * 프론트엔드에서 chatRoomId를 알고 있을 때 사용
     */
    @PostMapping("/chatroom/{chatRoomId}/messages")
    @Operation(summary = "채팅방 메시지 전송", description = "채팅방 ID로 직접 메시지를 전송합니다")
    public SuccessResponse<ChatMessageDto> sendMessageToChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long chatRoomId,
            @Valid @RequestBody MessageSendRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        log.info("채팅방 메시지 전송 - 채팅방 ID: {}, 사용자 ID: {}", chatRoomId, userDetails.getMemberId());

        ChatMessageDto message = codeReviewChatService.sendRealtimeMessageByChatRoomId(
                chatRoomId, userDetails.getMemberId(), request);

        return SuccessResponse.onSuccess(message);
    }

    /**
     * 현재 로그인한 사용자의 채팅방 목록 조회 (편의 메서드)
     */
    @GetMapping("/my/chatrooms")
    @Operation(summary = "내 채팅방 목록 조회", description = "현재 로그인한 사용자의 코드리뷰 채팅방 목록을 조회합니다")
    public SuccessResponse<List<CodeReviewChatRoomDto>> getMyChatRooms(
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        log.info("내 채팅방 목록 조회 - 사용자 ID: {}", userDetails.getMemberId());

        List<CodeReviewChatRoomDto> chatRooms = codeReviewChatService.getChatRoomsByMemberId(userDetails.getMemberId());

        return SuccessResponse.onSuccess(chatRooms);
    }

    // ============================================
    // WebSocket 실시간 채팅 (STOMP)
    // ============================================

    /**
     * 코드리뷰 채팅방 입장
     * 구독 시작: /topic/codereview.pr.{prId}
     */
    @MessageMapping("/code-reviews/pr/{prId}/join")
    public void joinCodeReviewChat(
            @DestinationVariable Long prId,
            Principal principal
    ) {
        try {
            if (principal == null) {
                log.warn("인증되지 않은 사용자의 코드리뷰 채팅방 입장 시도 - PR ID: {}", prId);
                return;
            }

            Long userId = Long.parseLong(principal.getName());
            log.info("코드리뷰 채팅방 입장 - PR ID: {}, 사용자 ID: {}", prId, userId);

            // 사용자 정보 조회 (캐시 또는 DB에서)
            String username = getUsernameById(userId);

            // RabbitMQ 호환 destination으로 브로드캐스트
            String destination = "/topic/codereview.pr." + prId;
            Map<String, Object> joinNotification = Map.of(
                "type", "USER_JOIN",
                "userId", userId,
                "username", username,
                "message", username + "님이 코드리뷰 채팅방에 입장했습니다.",
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, joinNotification);

            // 입장한 사용자에게 환영 메시지 전송
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/welcome",
                Map.of("message", "코드리뷰 채팅방에 입장했습니다. PR #" + prId));

            log.info("코드리뷰 채팅방 입장 처리 완료 - PR ID: {}, 사용자: {}", prId, username);

        } catch (Exception e) {
            log.error("코드리뷰 채팅방 입장 처리 중 오류 발생 - PR ID: {}", prId, e);

            if (principal != null) {
                Long userId = Long.parseLong(principal.getName());
                messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/error",
                    Map.of("message", "채팅방 입장 중 오류가 발생했습니다."));
            }
        }
    }

    /**
     * 코드리뷰 채팅방 퇴장
     */
    @MessageMapping("/code-reviews/pr/{prId}/leave")
    public void leaveCodeReviewChat(
            @DestinationVariable Long prId,
            Principal principal
    ) {
        try {
            if (principal == null) return;

            Long userId = Long.parseLong(principal.getName());
            String username = getUsernameById(userId);

            log.info("코드리뷰 채팅방 퇴장 - PR ID: {}, 사용자: {}", prId, username);

            // RabbitMQ 호환 destination으로 브로드캐스트
            String destination = "/topic/codereview.pr." + prId;
            Map<String, Object> leaveNotification = Map.of(
                "type", "USER_LEAVE",
                "userId", userId,
                "username", username,
                "message", username + "님이 코드리뷰 채팅방에서 나갔습니다.",
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, leaveNotification);

        } catch (Exception e) {
            log.error("코드리뷰 채팅방 퇴장 처리 중 오류 발생 - PR ID: {}", prId, e);
        }
    }

    /**
     * 코드리뷰 채팅방 구독
     * 클라이언트가 /topic/codereview.pr.{prId} 구독 시 호출
     */
    @MessageMapping("/code-reviews/pr/{prId}/subscribe")
    public void subscribeToCodeReview(
            @DestinationVariable Long prId,
            Principal principal
    ) {
        try {
            if (principal == null) return;

            Long userId = Long.parseLong(principal.getName());
            log.info("코드리뷰 채팅방 구독 - PR ID: {}, 사용자 ID: {}", prId, userId);

            // 구독 확인 메시지를 개인에게 전송
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/subscription-confirmed",
                Map.of("message", "PR #" + prId + " 코드리뷰 채팅 구독을 시작했습니다."));

        } catch (Exception e) {
            log.error("코드리뷰 채팅방 구독 처리 중 오류 발생", e);
        }
    }

    /**
     * 코드리뷰 채팅방 실시간 메시지 전송 (채팅방 ID 기반)
     * 구독 경로: /topic/chatroom.{chatRoomId}
     */
    @MessageMapping("/code-reviews/chatroom/{chatRoomId}/send")
    public void sendCodeReviewMessageByChatRoom(
            @DestinationVariable Long chatRoomId,
            @Valid @Payload MessageSendRequest messageRequest,
            Principal principal
    ) {
        try {
            if (principal == null) {
                log.warn("인증되지 않은 사용자의 채팅방 메시지 전송 시도 - 채팅방 ID: {}", chatRoomId);
                return;
            }

            Long senderId = Long.parseLong(principal.getName());
            log.info("채팅방 실시간 메시지 전송 - 채팅방 ID: {}, 사용자 ID: {}, 내용: {}",
                    chatRoomId, senderId, messageRequest.text());

            // 메시지 저장 및 DTO 생성
            ChatMessageDto response = codeReviewChatService.sendRealtimeMessageByChatRoomId(
                    chatRoomId, senderId, messageRequest);

            // RabbitMQ 호환 destination으로 브로드캐스트
            String destination = "/topic/chatroom." + chatRoomId;
            messagingTemplate.convertAndSend(destination, response);

            log.info("채팅방 실시간 메시지 브로드캐스트 완료 - 메시지 ID: {}", response.getId());

        } catch (Exception e) {
            log.error("채팅방 실시간 메시지 전송 중 오류 발생 - 채팅방 ID: {}", chatRoomId, e);

            if (principal != null) {
                Long userId = Long.parseLong(principal.getName());
                messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/error",
                    Map.of("message", "채팅방 메시지 전송 중 오류가 발생했습니다: " + e.getMessage()));
            }
        }
    }

    /**
     * 채팅방 입장 (채팅방 ID 기반)
     */
    @MessageMapping("/code-reviews/chatroom/{chatRoomId}/join")
    public void joinChatRoomById(
            @DestinationVariable Long chatRoomId,
            Principal principal
    ) {
        try {
            if (principal == null) {
                log.warn("인증되지 않은 사용자의 채팅방 입장 시도 - 채팅방 ID: {}", chatRoomId);
                return;
            }

            Long userId = Long.parseLong(principal.getName());
            String username = getUsernameById(userId);

            log.info("채팅방 입장 - 채팅방 ID: {}, 사용자: {}", chatRoomId, username);

            // RabbitMQ 호환 destination으로 브로드캐스트
            String joinMessage = String.format("👋 %s님이 채팅방에 입장했습니다.", username);
            String presenceDestination = "/topic/chatroom." + chatRoomId + ".presence";

            Map<String, Object> presenceNotification = Map.of(
                "type", "JOIN",
                "userId", userId,
                "username", username,
                "message", joinMessage,
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(presenceDestination, presenceNotification);

            // 입장한 사용자에게 환영 메시지 전송
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/welcome",
                Map.of("message", "채팅방에 입장했습니다. 채팅방 ID: " + chatRoomId));

        } catch (Exception e) {
            log.error("채팅방 입장 처리 중 오류 발생 - 채팅방 ID: {}", chatRoomId, e);
        }
    }

    /**
     * 채팅방 퇴장 (채팅방 ID 기반)
     */
    @MessageMapping("/code-reviews/chatroom/{chatRoomId}/leave")
    public void leaveChatRoomById(
            @DestinationVariable Long chatRoomId,
            Principal principal
    ) {
        try {
            if (principal == null) return;

            Long userId = Long.parseLong(principal.getName());
            String username = getUsernameById(userId);

            log.info("채팅방 퇴장 - 채팅방 ID: {}, 사용자: {}", chatRoomId, username);

            // RabbitMQ 호환 destination으로 브로드캐스트
            String leaveMessage = String.format("👋 %s님이 채팅방에서 나갔습니다.", username);
            String presenceDestination = "/topic/chatroom." + chatRoomId + ".presence";

            Map<String, Object> presenceNotification = Map.of(
                "type", "LEAVE",
                "userId", userId,
                "username", username,
                "message", leaveMessage,
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(presenceDestination, presenceNotification);

        } catch (Exception e) {
            log.error("채팅방 퇴장 처리 중 오류 발생 - 채팅방 ID: {}", chatRoomId, e);
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * 사용자 ID로 사용자명 조회
     * 실제 구현에서는 캐시나 사용자 서비스를 통해 조회
     */
    private String getUsernameById(Long userId) {
        try {
            // TODO: 실제 구현에서는 MemberService나 캐시를 통해 조회
            return "User" + userId; // 임시 구현
        } catch (Exception e) {
            log.warn("사용자명 조회 실패 - 사용자 ID: {}", userId, e);
            return "Unknown User";
        }
    }
}
