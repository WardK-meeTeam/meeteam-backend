package com.wardk.meeteam_backend.domain.review;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    //리뷰 쓴사람
    private Integer reviewerId;

    //리뷰 당한사람
    private Integer revieweeId;

    @Column(length = 1000)
    private String comment;


    //validation 을 통해서 Dto 에서 검증 필요
    @Check(constraints = "score BETWEEN 0 AND 5")
    private Integer score; // 0점에서 5점까지 가능



}
