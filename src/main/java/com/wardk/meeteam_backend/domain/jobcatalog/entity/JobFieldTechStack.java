package com.wardk.meeteam_backend.domain.jobcatalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "job_field_tech_stack_catalog",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_field_tech_stack_catalog_pair",
                        columnNames = {"job_field_catalog_id", "tech_stack_catalog_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobFieldTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_field_tech_stack_catalog_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_field_catalog_id", nullable = false)
    private JobField jobField;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tech_stack_catalog_id", nullable = false)
    private TechStack techStack;

    private JobFieldTechStack(JobField jobField, TechStack techStack) {
        this.jobField = jobField;
        this.techStack = techStack;
    }

    public static JobFieldTechStack of(JobField jobField, TechStack techStack) {
        return new JobFieldTechStack(jobField, techStack);
    }
}
