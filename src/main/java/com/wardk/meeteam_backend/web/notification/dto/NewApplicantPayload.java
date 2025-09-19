package com.wardk.meeteam_backend.web.notification.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class NewApplicantPayload { // 내 프로젝트에 누가 지원함(프로젝트 소유자에게 가는 카드)

    /**
     * applicationId, projectId 를 통해
     * api/projects-application/{projectId}/{applicationId} 필요
     */
    Long applicationId;// 지원한 사람의 지원서Id
    Long projectId; // 프로젝트Id


    Long receiverId; // 팀장 ID
    Long applicantId; // 참여자
    String applicantName;
    String projectName;
    String message;
    LocalDate date;
}
