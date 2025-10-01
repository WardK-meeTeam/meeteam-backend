package com.wardk.meeteam_backend.domain.member.entity;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class MemberSubCategory extends BaseEntity { // 중간테이블


    public MemberSubCategory() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;


    //소분류이름
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;


    public MemberSubCategory(Member member, SubCategory subCategory) {
        this.member = member;
        this.subCategory = subCategory;
    }
}
