package com.wardk.meeteam_backend.domain.skill.entity;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long id;


    @Column(name = "skill_name", nullable = false, length = 255)
    private String skillName;
}
