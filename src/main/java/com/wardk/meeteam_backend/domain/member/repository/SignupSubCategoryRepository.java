package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.member.entity.SignupSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignupSubCategoryRepository extends JpaRepository<SignupSubCategory, Long> {

    Optional<SignupSubCategory> findBySubCategory(String subCategory);
}
