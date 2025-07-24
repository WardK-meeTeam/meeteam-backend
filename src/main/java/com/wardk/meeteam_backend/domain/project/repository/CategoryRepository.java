package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.project.entity.Category;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
