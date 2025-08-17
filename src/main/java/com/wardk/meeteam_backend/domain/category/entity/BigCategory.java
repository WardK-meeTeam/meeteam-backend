package com.wardk.meeteam_backend.domain.category.entity;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class BigCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "big_category_id")
    Long id;

    @Column(name = "big_category")
    private String name;

    public BigCategory() {
    }
}
