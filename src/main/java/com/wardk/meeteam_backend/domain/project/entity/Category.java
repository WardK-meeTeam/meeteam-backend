package com.wardk.meeteam_backend.domain.project.entity;

import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private ProjectCategory projectCategory;


    public Category(ProjectCategory projectCategory) {
        this.projectCategory = projectCategory;
    }


    public static Category createCategory (ProjectCategory projectCategory) {
        return new Category(projectCategory);
    }

}
