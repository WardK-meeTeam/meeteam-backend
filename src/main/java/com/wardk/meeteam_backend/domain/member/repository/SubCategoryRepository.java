package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    Optional<SubCategory> findByName(String subCategory);
}
