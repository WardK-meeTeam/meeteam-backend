package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.member.entity.MemberJobPosition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberJobPositionRepository extends JpaRepository<MemberJobPosition, Long> {
}