package com.wardk.meeteam_backend.web.codereview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 코드리뷰 채팅방 정보 DTO
 */
@Data
@Builder
@AllArgsConstructor
public class CodeReviewChatRoomDto {
    private Long id;
    private String name;
    private String description;
    private Long prId;
    private String prTitle;
    private Integer prNumber;
    private Long projectId;
    private String projectName;
    private Long creatorId;
    private String lastMessage;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageTime;
}
