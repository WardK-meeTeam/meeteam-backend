package com.wardk.meeteam_backend.domain.notification.entity;

public enum NotificationType {

    PROJECT_APPLY,     // 프로젝트 생성자에게: 누군가 지원했음
    PROJECT_MY_APPLY,  // 지원자에게: 내가 지원을 완료했음
    PROJECT_APPROVE,   // 지원자에게: 내가 지원한 프로젝트가 승인됨
    PROJECT_REJECT     // 지원자에게: 내가 지원한 프로젝트가 거절됨
}
