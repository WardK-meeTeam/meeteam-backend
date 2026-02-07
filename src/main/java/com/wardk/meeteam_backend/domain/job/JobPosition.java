package com.wardk.meeteam_backend.domain.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobPosition {
    // 기획
    PRODUCT_MANAGER(JobField.PLANNING, "프로덕트 매니저/오너"),

    // 디자인
    GRAPHIC_DESIGN(JobField.DESIGN, "그래픽디자인"),
    UI_UX_DESIGN(JobField.DESIGN, "UI/UX디자인"),
    MOTION_DESIGN(JobField.DESIGN, "모션 디자인"),

    // 프론트엔드
    WEB_FRONTEND(JobField.FRONTEND, "웹프론트엔드"),
    IOS(JobField.FRONTEND, "iOS"),
    ANDROID(JobField.FRONTEND, "안드로이드"),
    CROSS_PLATFORM(JobField.FRONTEND, "크로스플랫폼"),

    // 백엔드
    WEB_SERVER(JobField.BACKEND, "웹서버"),
    AI(JobField.BACKEND, "AI"),

    // 기타
    ETC(JobField.ETC, "기타");

    private final JobField jobField;
    private final String description;
}