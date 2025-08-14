package com.wardk.meeteam_backend.domain.member.entity;

import jakarta.persistence.*;

@Entity
public class MemberSubCategory { // 중간테이블


    public MemberSubCategory() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //소분류이름
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subCategory_id")
    private SignupSubCategory subCategory;


    public MemberSubCategory(Member member, SignupSubCategory subCategory) {
        this.member = member;
        this.subCategory = subCategory;
    }
}
