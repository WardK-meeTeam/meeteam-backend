package com.wardk.meeteam_backend.domain.application.entity;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 지원서 질문(양식 정의) 엔티티.
 *
 * 리더가 프로젝트마다 직접 정의하며, 지원자는 이 질문에 답합니다.
 * 질문은 지원서가 아니라 <b>프로젝트</b>에 속합니다. (지원 전에 존재)
 */
@Entity
@Getter
@Table(name = "application_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private QuestionInputType inputType;

    @Column(nullable = false)
    private boolean required;

    /** SELECT 타입 선택지. 쉼표로 연결하여 저장합니다. */
    @Column(name = "options_csv", length = 1000)
    private String optionsCsv;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private ApplicationQuestion(Project project, String label, QuestionInputType inputType,
                                boolean required, List<String> options, int displayOrder) {
        this.project = project;
        this.label = label;
        this.inputType = inputType;
        this.required = required;
        this.optionsCsv = (options == null || options.isEmpty()) ? null : String.join(",", options);
        this.displayOrder = displayOrder;
    }

    public static ApplicationQuestion of(Project project, String label, QuestionInputType inputType,
                                         boolean required, List<String> options, int displayOrder) {
        return new ApplicationQuestion(project, label, inputType, required, options, displayOrder);
    }

    /** 저장된 선택지를 리스트로 반환합니다. */
    public List<String> getOptions() {
        if (optionsCsv == null || optionsCsv.isBlank()) {
            return List.of();
        }
        return List.of(optionsCsv.split(","));
    }
}