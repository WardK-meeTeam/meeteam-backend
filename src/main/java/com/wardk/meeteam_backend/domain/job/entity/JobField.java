package com.wardk.meeteam_backend.domain.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "job_field",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_field_code", columnNames = "code")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_field_id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    private JobField(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static JobField of(String code, String name) {
        return new JobField(code, name);
    }
}
