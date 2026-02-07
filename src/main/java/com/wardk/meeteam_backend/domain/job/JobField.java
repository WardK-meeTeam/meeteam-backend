package com.wardk.meeteam_backend.domain.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobField {
    PLANNING("기획"),
    DESIGN("디자인"),
    FRONTEND("프론트엔드"),
    BACKEND("백엔드"),
    ETC("기타");

    private final String description;
}