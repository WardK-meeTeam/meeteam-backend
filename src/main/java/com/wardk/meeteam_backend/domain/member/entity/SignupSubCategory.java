package com.wardk.meeteam_backend.domain.member.entity;


import jakarta.persistence.*;

@Entity
public class SignupSubCategory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_category_id")
    Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_category_id")
    private SignupBigCategory bigCategory;


    @Column(name = "sub_category")
    private String subCategory;


    public SignupSubCategory() {
    }
}
