package com.wardk.meeteam_backend.web.notification.controller;

import com.wardk.meeteam_backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림을 위한 Server-Sent Events(SSE) 구독을 처리하는 컨트롤러입니다.
 * 클라이언트가 SSE를 통해 알림 스트림에 구독할 수 있도록 합니다.
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {


    private final NotificationService notificationService;

    /**
     * 인증된 사용자를 Server-Sent Events(SSE)를 이용한 알림 스트림에 구독시킵니다.
     *
     * @param userDetails 인증된 사용자의 정보로, 구독할 사용자를 식별하는 데 사용됩니다.
     * @param lastEventId 클라이언트가 마지막으로 받은 이벤트의 ID로, 이벤트 재전송 또는 복구에 사용됩니다.
     * @return 클라이언트에게 알림 이벤트를 스트리밍하는 SseEmitter 객체를 반환합니다.
     */
    // produces = "text/event-stream"을 사용하여 SSE 응답 콘텐츠 타입을 지정합니다.
    @GetMapping(value = "/api/subscribe", produces = "text/event-stream")
    @ResponseStatus(HttpStatus.OK)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails,
                                // Last-Event-ID 헤더는 마지막으로 받은 이벤트부터 이벤트 스트리밍을 재개하는 데 사용됩니다.
                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "")
                                String lastEventId) {
        return notificationService.subscribe(userDetails.getUsername(), lastEventId);
    }
}
