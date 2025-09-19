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
public class ApplyNotiPayload {

    /**
     * 프로젝트 지원자가 지원을 완료하면 받는 본문내용
     */

    private Long receiverId;

    private String projectName;
    
    private String message;
    
    private LocalDate localDate;

}
