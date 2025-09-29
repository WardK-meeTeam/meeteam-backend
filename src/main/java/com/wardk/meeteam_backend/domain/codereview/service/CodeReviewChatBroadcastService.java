package com.wardk.meeteam_backend.domain.codereview.service;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 코드 리뷰 채팅 브로드캐스트 서비스
 * RabbitMQ STOMP 호환 destination 패턴 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewChatBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * PR 리뷰 채팅방의 모든 구독자에게 메시지 브로드캐스트
     * RabbitMQ 호환 destination: /topic/codereview.pr.{prId}
     */
    public void broadcastToCodeReviewRoom(Long prId, ChatMessageDto message) {
        try {
            // RabbitMQ 호환 destination 패턴 사용 (점으로 구분)
            String destination = "/topic/codereview.pr." + prId;
            messagingTemplate.convertAndSend(destination, message);

            log.info("코드 리뷰 메시지 브로드캐스트 완료: PR={}, 메시지ID={}, 구독경로={}",
                    prId, message.getId(), destination);
        } catch (Exception e) {
            log.error("코드 리뷰 메시지 브로드캐스트 실패: PR={}, 메시지ID={}", prId, message.getId(), e);
        }
    }

    /**
     * 채팅방 ID 기반 메시지 브로드캐스트
     * RabbitMQ 호환 destination: /topic/chatroom.{chatRoomId}
     */
    public void broadcastToChatRoom(Long chatRoomId, ChatMessageDto message) {
        try {
            String destination = "/topic/chatroom." + chatRoomId;
            messagingTemplate.convertAndSend(destination, message);

            log.info("채팅방 메시지 브로드캐스트 완료: 채팅방={}, 메시지ID={}, 구독경로={}",
                    chatRoomId, message.getId(), destination);
        } catch (Exception e) {
            log.error("채팅방 메시지 브로드캐스트 실패: 채팅방={}, 메시지ID={}", chatRoomId, message.getId(), e);
        }
    }

    /**
     * 사용자 입장/퇴장 알림 브로드캐스트
     */
    public void broadcastUserPresence(Long prId, Long userId, String username, boolean isJoin) {
        try {
            String destination = "/topic/codereview.pr." + prId;
            String action = isJoin ? "입장" : "퇴장";

            Map<String, Object> presenceMessage = Map.of(
                "type", isJoin ? "USER_JOIN" : "USER_LEAVE",
                "userId", userId,
                "username", username,
                "message", String.format("%s님이 코드리뷰 채팅방에 %s했습니다.", username, action),
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, presenceMessage);

            log.info("사용자 {} 알림 브로드캐스트 완료: PR={}, 사용자={}", action, prId, username);
        } catch (Exception e) {
            log.error("사용자 presence 브로드캐스트 실패: PR={}, 사용자={}", prId, username, e);
        }
    }

    /**
     * 특정 사용자에게 개인 메시지 전송
     */
    public void sendToUser(Long userId, String queueDestination, Object message) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), queueDestination, message);
            log.debug("개인 메시지 전송 완료: 사용자={}, 경로={}", userId, queueDestination);
        } catch (Exception e) {
            log.error("개인 메시지 전송 실패: 사용자={}, 경로={}", userId, queueDestination, e);
        }
    }

    /**
     * 사용자에게 에러 메시지 전송
     */
    public void sendErrorToUser(Long userId, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                "type", "ERROR",
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );
            sendToUser(userId, "/queue/error", error);
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패: 사용자={}", userId, e);
        }
    }

    /**
     * PR 리뷰 시작 알림 브로드캐스트
     */
    public void broadcastReviewStart(Long prId, Integer prNumber, int totalFiles) {
        try {
            String destination = "/topic/codereview.pr." + prId;

            Map<String, Object> startMessage = Map.of(
                "type", "REVIEW_START",
                "prNumber", prNumber,
                "totalFiles", totalFiles,
                "message", String.format("🚀 PR #%d 리뷰가 시작되었습니다. (총 %d개 파일)", prNumber, totalFiles),
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, startMessage);
            log.info("PR 리뷰 시작 알림 브로드캐스트 완료: PR #{}", prNumber);
        } catch (Exception e) {
            log.error("PR 리뷰 시작 알림 브로드캐스트 실패: PR #{}", prNumber, e);
        }
    }

    /**
     * PR 리뷰 완료 알림 브로드캐스트
     */
    public void broadcastReviewComplete(Long prId, Integer prNumber, long successCount, long failedCount) {
        try {
            String destination = "/topic/codereview.pr." + prId;

            Map<String, Object> completeMessage = Map.of(
                "type", "REVIEW_COMPLETE",
                "prNumber", prNumber,
                "successCount", successCount,
                "failedCount", failedCount,
                "message", String.format("✅ PR #%d 리뷰가 완료되었습니다! (성공: %d개, 실패: %d개)",
                    prNumber, successCount, failedCount),
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, completeMessage);
            log.info("PR 리뷰 완료 알림 브로드캐스트 완료: PR #{}", prNumber);
        } catch (Exception e) {
            log.error("PR 리뷰 완료 알림 브로드캐스트 실패: PR #{}", prNumber, e);
        }
    }

    /**
     * 파일 리뷰 완료 알림 브로드캐스트
     */
    public void broadcastFileReviewComplete(Long prId, String fileName) {
        try {
            String destination = "/topic/codereview.pr." + prId;

            Map<String, Object> fileCompleteMessage = Map.of(
                "type", "FILE_REVIEW_COMPLETE",
                "fileName", fileName,
                "message", String.format("📄 %s 파일 리뷰가 완료되었습니다.", fileName),
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, fileCompleteMessage);
            log.debug("파일 리뷰 완료 알림 브로드캐스트 완료: 파일={}", fileName);
        } catch (Exception e) {
            log.error("파일 리뷰 완료 알림 브로드캐스트 실패: 파일={}", fileName, e);
        }
    }
}
