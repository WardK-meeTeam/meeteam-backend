package com.wardk.meeteam_backend.domain.member.entity;


import jakarta.persistence.*;

@Entity
public class SignupBigCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "big_category_id")
    Long id;


    @Column(name = "big_category")
    private String bigCategory;

    public SignupBigCategory() {
    }
}
