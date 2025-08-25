package com.wardk.meeteam_backend.web.notification.dto;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ApplyNotiPayload { // 지원시 응답 DTO


    private Long receiverId;

    private String projectName;
    
    private String message;
    
    private LocalDate localDate;

}
