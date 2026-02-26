package com.wardk.meeteam_backend.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectCategory {
    ENVIRONMENT("친환경"),
    PET("반려동물"),
    HEALTHCARE("헬스케어"),
    EDUCATION("교육/학습"),
    AI_TECH("AI/테크"),
    FASHION_BEAUTY("패션/뷰티"),
    FINANCE_PRODUCTIVITY("금융/핀테크"),
    ETC("기타");

    private final String displayName;
}