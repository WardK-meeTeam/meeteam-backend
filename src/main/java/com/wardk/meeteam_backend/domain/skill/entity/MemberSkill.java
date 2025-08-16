package com.wardk.meeteam_backend.domain.skill.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import jakarta.persistence.*;

@Entity // member_기술스택
public class MemberSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_skill_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;


    public MemberSkill(Member member, Skill skill) {
        this.member = member;
        this.skill = skill;
    }
}
