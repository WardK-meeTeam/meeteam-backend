package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class MemberProjectResponse {

    private Long projectId;

    private LocalDate localDate;

    private String title;

    private String imageUrl;

    private Recruitment status;
}
