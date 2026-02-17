package com.wardk.meeteam_backend.domain.job.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 직무(JobPosition) 코드 Enum.
 * 각 직무가 어느 직군(JobField)에 속하는지 함께 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum JobPositionCode {

    // 기획 (PLANNING)
    PRODUCT_MANAGER(JobFieldCode.PLANNING, "PM 프로덕트 매니저"),
    PRODUCT_OWNER(JobFieldCode.PLANNING, "PO 프로덕트 오너"),
    SERVICE_PLANNER(JobFieldCode.PLANNING, "서비스 기획"),

    // 디자인 (DESIGN)
    UI_UX_DESIGNER(JobFieldCode.DESIGN, "UI/UX 디자이너"),
    MOTION_DESIGNER(JobFieldCode.DESIGN, "모션 디자이너"),
    BX_BRAND_DESIGNER(JobFieldCode.DESIGN, "BX 브랜드 디자이너"),

    // 프론트 (FRONTEND)
    WEB_FRONTEND(JobFieldCode.FRONTEND, "웹 프론트엔드"),
    IOS(JobFieldCode.FRONTEND, "iOS"),
    ANDROID(JobFieldCode.FRONTEND, "Android"),
    CROSS_PLATFORM(JobFieldCode.FRONTEND, "크로스 플랫폼"),

    // 백엔드 (BACKEND)
    JAVA_SPRING(JobFieldCode.BACKEND, "Java/Spring"),
    KOTLIN_SPRING(JobFieldCode.BACKEND, "Kotlin/Spring"),
    NODE_NESTJS(JobFieldCode.BACKEND, "Node.js/NestJS"),
    PYTHON_BACKEND(JobFieldCode.BACKEND, "Python Backend"),

    // AI
    MACHINE_LEARNING(JobFieldCode.AI, "머신 러닝"),
    DEEP_LEARNING(JobFieldCode.AI, "딥러닝"),
    LLM(JobFieldCode.AI, "LLM"),
    MLOPS(JobFieldCode.AI, "MLOps"),

    // 인프라/운영 (INFRA_OPERATION)
    DEVOPS_ARCHITECT(JobFieldCode.INFRA_OPERATION, "DevOps 엔지니어/아키텍처"),
    QA(JobFieldCode.INFRA_OPERATION, "QA"),
    CLOUD_ENGINEER(JobFieldCode.INFRA_OPERATION, "Cloud 엔지니어");

    private final JobFieldCode jobFieldCode;
    private final String displayName;
}
