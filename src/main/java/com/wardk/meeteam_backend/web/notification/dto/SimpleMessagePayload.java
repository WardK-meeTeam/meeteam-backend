package com.wardk.meeteam_backend.web.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class SimpleMessagePayload { // 단순 메시지형(승인, 거절)

    private Long receiverId; // 알림 받는 사람 id
    private Long projectId;
    private ApprovalResult approvalResult;
    private String message;
    private LocalDate date;
}
