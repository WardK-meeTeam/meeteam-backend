package com.wardk.meeteam_backend.domain.application.entity;

/**
 * 지원자 답변 필터 연산자.
 * <ul>
 *   <li>{@code GTE}/{@code LTE} : NUMBER 질문 - 숫자 이상/이하 (value 를 CAST 하여 비교)</li>
 *   <li>{@code EQ} : SELECT 질문 - 선택지 정확히 일치</li>
 *   <li>{@code CONTAINS} : TEXT/TEXTAREA 질문 - 특정 단어 포함(LIKE)</li>
 * </ul>
 */
public enum FilterOperator {
    GTE,
    LTE,
    EQ,
    CONTAINS
}
