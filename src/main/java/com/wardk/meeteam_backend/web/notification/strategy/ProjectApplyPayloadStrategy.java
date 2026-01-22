package com.wardk.meeteam_backend.web.notification.strategy;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.notification.context.NotificationContext;
import com.wardk.meeteam_backend.web.notification.payload.Payload;
import com.wardk.meeteam_backend.web.notification.payload.ProjectApplicationReceivedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ProjectApplyPayloadStrategy implements NotificationPayloadStrategy{

    private final MemberRepository memberRepository;

    @Override
    public NotificationType getType() {
        return NotificationType.PROJECT_APPLY;
    }

    @Override
    public Payload create(NotificationContext context) {

        Member applicant = memberRepository.findById(context.getActorId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTOR_NOT_FOUND));

        return ProjectApplicationReceivedPayload.create(context, applicant);
    }
}
