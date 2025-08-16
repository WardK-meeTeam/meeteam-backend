package com.wardk.meeteam_backend.domain.category.entity;


import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
public class SubCategory  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_category_id")
    Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_category_id")
    private BigCategory bigCategory;


    @Column(name = "sub_category")
    private String subCategory;


    public SubCategory() {
    }
}
