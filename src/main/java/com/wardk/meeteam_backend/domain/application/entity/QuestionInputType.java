package com.wardk.meeteam_backend.domain.application.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원서 질문 입력 위젯 타입.
 * NUMBER 는 EAV 에서 문자열 비교 문제를 드러내는 대표 타입입니다.
 */
@Getter
@RequiredArgsConstructor
public enum QuestionInputType {

    TEXT("한 줄 텍스트"),
    TEXTAREA("긴 글"),
    NUMBER("숫자"),
    SELECT("선택지");

    private final String displayName;
}