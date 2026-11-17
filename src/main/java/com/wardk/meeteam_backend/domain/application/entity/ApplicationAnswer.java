package com.wardk.meeteam_backend.domain.application.entity;

import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원서 답변(EAV) 엔티티 - 타입별 컬럼 분리 방식.
 *
 * <p>모든 값을 하나의 VARCHAR 에 담지 않고 <b>데이터 타입별 컬럼</b>으로 나눠 저장합니다.
 * 질문 타입에 따라 {@code valText} 또는 {@code valInt} 중 하나만 채워지고 나머지는 NULL 입니다.
 * 이렇게 하면 숫자 비교/정렬에서 CAST 없이 올바르게 동작하고 인덱스도 탈 수 있습니다.
 *
 * <ul>
 *   <li>E = {@code application} (어느 지원서)</li>
 *   <li>A = {@code question} (어느 질문)</li>
 *   <li>V = {@code valText}/{@code valInt} (타입별 컬럼)</li>
 * </ul>
 */
@Entity
@Getter
@Table(
        name = "application_answer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_answer_pair", columnNames = {"application_id", "question_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private ProjectApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ApplicationQuestion question;

    /** 문자형 값 (TEXT/TEXTAREA/SELECT) */
    @Column(name = "val_text", length = 500)
    private String valText;

    /** 숫자형 값 (NUMBER) - 진짜 INT 컬럼이라 CAST 없이 비교/정렬/인덱스 가능 */
    @Column(name = "val_int")
    private Integer valInt;

    private ApplicationAnswer(ProjectApplication application, ApplicationQuestion question,
                             String valText, Integer valInt) {
        this.application = application;
        this.question = question;
        this.valText = valText;
        this.valInt = valInt;
    }

    /**
     * 질문 타입에 맞는 컬럼에 값을 라우팅하여 답변을 생성합니다.
     * NUMBER 는 숫자 컬럼({@code valInt})으로, 그 외는 문자 컬럼({@code valText})으로 저장됩니다.
     */
    public static ApplicationAnswer of(ProjectApplication application, ApplicationQuestion question, String rawValue) {
        String valText = null;
        Integer valInt = null;

        switch (question.getInputType()) {
            case NUMBER -> valInt = Integer.parseInt(rawValue);
            case TEXT, TEXTAREA, SELECT -> valText = rawValue;
        }

        return new ApplicationAnswer(application, question, valText, valInt);
    }

    /**
     * 타입에 상관없이 표시용 문자열 값을 반환합니다. (조회/응답용)
     */
    public String getValue() {
        if (valInt != null) {
            return String.valueOf(valInt);
        }
        return valText;
    }
}