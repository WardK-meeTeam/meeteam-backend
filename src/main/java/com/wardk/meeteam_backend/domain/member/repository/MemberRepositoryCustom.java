package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Component
public interface MemberRepositoryCustom {

    Member getProfile(Long memberId);


    List<String> getSkill(Long memberId);
}
