package com.wardk.meeteam_backend.domain.category.entity;


import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubCategory  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_category_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_category_id")
    private BigCategory bigCategory;

    @Column(name = "sub_category")
    private String name;

    public SubCategory(String name) {
        this.name = name;
    }
}
