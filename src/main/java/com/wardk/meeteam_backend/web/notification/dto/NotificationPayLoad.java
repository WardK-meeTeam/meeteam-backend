package com.wardk.meeteam_backend.web.notification.dto;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import lombok.Data;

@Data
public class NotificationPayLoad {

    private Long projectId;
    private String projectName;
    private Long receiverId;
    private String receiverName;

    public NotificationPayLoad(Project project, Member receiver) {

        this.projectId = project.getId();
        this.projectName = project.getName();
        this.receiverId = receiver.getId();
        this.receiverName = receiver.getRealName();
    }
}
