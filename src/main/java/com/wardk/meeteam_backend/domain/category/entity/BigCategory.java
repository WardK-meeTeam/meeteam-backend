package com.wardk.meeteam_backend.domain.category.entity;


import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
public class BigCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "big_category_id")
    Long id;


    @Column(name = "big_category")
    private String bigCategory;

    public BigCategory() {
    }
}
