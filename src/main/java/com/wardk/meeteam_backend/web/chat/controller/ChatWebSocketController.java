//package com.wardk.meeteam_backend.web.chat.controller;
//
//import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
//import com.wardk.meeteam_backend.domain.chat.service.ChatMessageService;
//import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
//import com.wardk.meeteam_backend.web.chat.dto.MessageSendRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.messaging.handler.annotation.DestinationVariable;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//
//import jakarta.validation.Valid;
//import java.security.Principal;
//
///**
// * WebSocket 실시간 채팅을 처리하는 컨트롤러
// * 기존 REST API와 함께 실시간 기능 제공
// *
// * @author MeeTeam Backend Team
// * @version 1.0
// * @since 1.0
// */
//@Controller
//@RequiredArgsConstructor
//@Slf4j
//public class ChatWebSocketController {
//
//    private final ChatMessageService chatMessageService;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    /**
//     * 실시간 채팅 메시지 전송
//     *
//     * Principal principal (WebSocketJwtAuthenticationInterceptor에서 설정)
//     *
//     * @param roomId 채팅방 ID
//     * @param messageRequest 메시지 전송 요청
//     * @param principal WebSocket 연결 시 설정된 인증 정보 (NOT @AuthenticationPrincipal!)
//     */
//    @MessageMapping("chat/{roomId}/send")
//    public void sendMessage(
//            @DestinationVariable Long roomId,
//            @Valid @Payload MessageSendRequest messageRequest,
//            Principal principal // WebSocketJwtAuthenticationInterceptor에서 설정
//    ) {
//        try {
//            if (principal == null) {
//                log.warn("❌ WebSocket Principal이 null - WebSocketJwtAuthenticationInterceptor 확인 필요");
//                return;
//            }
//
//            // Principal.getName()은 WebSocketJwtAuthenticationInterceptor에서
//            // userId.toString()으로 설정한 값
//            Long senderId = Long.parseLong(principal.getName());
//            log.info("✅ WebSocket 메시지 전송 - 사용자 ID: {}, 채팅방 ID: {}", senderId, roomId);
//
//            // 메시지 저장 (기존 ChatMessageService 활용)
//            chatMessageService.saveChatMessage(roomId, senderId.toString(), messageRequest.text());
//
//            // 실시간 브로드캐스트용 DTO 생성
//            ChatMessageDto response = ChatMessageDto.builder()
//                    .chatRoomId(roomId)
//                    .senderId(senderId)
//                    .content(messageRequest.text())
//                    .build();
//
//            // 채팅방 구독자들에게 실시간 브로드캐스트
//            messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
//
//            log.info("✅ 실시간 채팅 메시지 브로드캐스트 완료");
//
//        } catch (Exception e) {
//            log.error("❌ 실시간 채팅 메시지 전송 중 오류 발생", e);
//            if (principal != null) {
//                messagingTemplate.convertAndSendToUser(
//                        principal.getName(),
//                        "/queue/errors",
//                        "메시지 전송 중 오류가 발생했습니다: " + e.getMessage()
//                );
//            }
//        }
//    }
//
//    /**
//     * 채팅방 입장
//     */
//    @MessageMapping("chat/{roomId}/join")
//    public void joinChatRoom(
//            @DestinationVariable Long roomId,
//            SimpMessageHeaderAccessor headerAccessor,
//            Principal principal
//    ) {
//        try {
//            if (principal == null) {
//                return;
//            }
//
//            Long userId = Long.parseLong(principal.getName());
//            log.info("실시간 채팅방 입장 - 사용자 ID: {}, 채팅방 ID: {}", userId, roomId);
//
//            // WebSocket 세션에 사용자 정보 저장
//            if (headerAccessor.getSessionAttributes() != null) {
//                headerAccessor.getSessionAttributes().put("userId", userId);
//                headerAccessor.getSessionAttributes().put("roomId", roomId);
//            }
//
//            // 입장 시스템 메시지 (옵션)
//            ChatMessageDto systemMessage = ChatMessageDto.builder()
//                    .chatRoomId(roomId)
//                    .senderId(userId)
//                    .content("채팅방에 입장했습니다.")
//                    .build();
//
//            messagingTemplate.convertAndSend("/topic/chat/" + roomId, systemMessage);
//
//        } catch (Exception e) {
//            log.error("채팅방 입장 처리 중 오류 발생", e);
//        }
//    }
//
//    /**
//     * 채팅방 퇴장
//     */
//    @MessageMapping("chat/{roomId}/leave")
//    public void leaveChatRoom(
//            @DestinationVariable Long roomId,
//            Principal principal
//    ) {
//        try {
//            if (principal == null) {
//                return;
//            }
//
//            Long userId = Long.parseLong(principal.getName());
//            log.info("실시간 채팅방 퇴장 - 사용자 ID: {}, 채팅방 ID: {}", userId, roomId);
//
//            // 퇴장 시스템 메시지 (옵션)
//            ChatMessageDto systemMessage = ChatMessageDto.builder()
//                    .chatRoomId(roomId)
//                    .senderId(userId)
//                    .content("채팅방에서 퇴장했습니다.")
//                    .build();
//
//            messagingTemplate.convertAndSend("/topic/chat/" + roomId, systemMessage);
//
//        } catch (Exception e) {
//            log.error("채팅방 퇴장 처리 중 오류 발생", e);
//        }
//    }
//}
