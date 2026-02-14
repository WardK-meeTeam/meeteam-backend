package com.wardk.meeteam_backend.domain.jobcatalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "job_position_catalog",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_position_catalog_code", columnNames = "code")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_position_catalog_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_field_catalog_id", nullable = false)
    private JobField jobField;

    @Column(name = "code", nullable = false, length = 70)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    private JobPosition(JobField jobField, String code, String name) {
        this.jobField = jobField;
        this.code = code;
        this.name = name;
    }

    public static JobPosition of(JobField jobField, String code, String name) {
        return new JobPosition(jobField, code, name);
    }
}
