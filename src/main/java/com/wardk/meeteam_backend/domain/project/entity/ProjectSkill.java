package com.wardk.meeteam_backend.domain.project.entity;

import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import jakarta.persistence.*;

@Entity
public class ProjectSkill {//프로젝트_기술_스택_테이블(중간테이블)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_skill_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;




}
