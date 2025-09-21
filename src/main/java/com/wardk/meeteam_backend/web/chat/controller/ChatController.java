package com.wardk.meeteam_backend.web.chat.controller;


import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.chat.service.ChatService;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.chat.dto.ChatMessageResponse;
import com.wardk.meeteam_backend.web.chat.dto.MessageSendRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


/**
 * 채팅 메시지와 관련된 API 요청을 처리하는 컨트롤러입니다.
 * 특정 채팅 스레드의 메시지 조회 및 메시지 전송 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 모든 채팅 스레드를 조회합니다.
     *
     * @param request                   채팅 스레드 조회에 필요한 요청 객체
     * @param customSecurityUserDetails 인증된 사용자 정보
     * @return 조회된 채팅 스레드 목록을 포함한 성공 응답
     */
    @GetMapping
    public SuccessResponse<Page<ChatThread>> getAllMessages(@RequestParam int pageNumber,
                                                            @RequestParam int pageSize,
                                                            @AuthenticationPrincipal CustomSecurityUserDetails customSecurityUserDetails) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        return SuccessResponse.onSuccess(chatService.getAllThreads(customSecurityUserDetails.getMemberId(), pageable));
    }

    /**
     * 특정 채팅 스레드의 메시지를 조회합니다.
     *
     * @param threadId 조회할 채팅 스레드의 ID
     * @param cursor   메시지 조회 시작 위치 (기본값: 0)
     * @param pageSize 한 번에 조회할 메시지 수 (기본값: 20)
     * @return 조회된 메시지 목록을 포함한 성공 응답
     */
    @GetMapping("/{threadId}/messages")
    public SuccessResponse<ChatMessageResponse> getMessages(@PathVariable Long threadId,
                                                            @RequestParam Long cursor,
                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                            @AuthenticationPrincipal CustomSecurityUserDetails customSecurityUserDetails) {
        return SuccessResponse.onSuccess(chatService.getMessages(threadId, customSecurityUserDetails.getUsername(), cursor, pageSize));
    }

    /**
     * 특정 채팅 스레드에 메시지를 전송합니다.
     *
     * @param threadId                  메시지를 전송할 채팅 스레드의 ID
     * @param request                   전송할 메시지 내용을 포함한 요청 객체
     * @param customSecurityUserDetails 인증된 사용자 정보
     * @return 메시지 전송이 성공했음을 나타내는 응답
     */
    @PostMapping("/{threadId}/messages")
    public ResponseEntity<Void> send(@PathVariable Long threadId,
                                     @Valid @RequestBody MessageSendRequest request,
                                     @AuthenticationPrincipal CustomSecurityUserDetails customSecurityUserDetails) {

        chatService.saveChatMessage(threadId, customSecurityUserDetails.getUsername(), request.text());
        return ResponseEntity.accepted().build();
    }
}

