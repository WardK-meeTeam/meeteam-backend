package com.wardk.meeteam_backend.domain.skill.repository;

import com.wardk.meeteam_backend.domain.skill.entity.MemberSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberSkillRepository extends JpaRepository<MemberSkill, Long> {
}
