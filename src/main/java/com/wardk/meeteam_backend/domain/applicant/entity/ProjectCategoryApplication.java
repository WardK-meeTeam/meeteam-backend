package com.wardk.meeteam_backend.domain.applicant.entity;


import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;

@Entity
public class ProjectCategoryApplication {//project_대분류_모집

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_category_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "recruit_count")
    private Integer recruitmentCount;

}
