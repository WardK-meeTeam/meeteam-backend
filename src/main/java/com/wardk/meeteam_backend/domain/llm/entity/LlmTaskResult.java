package com.wardk.meeteam_backend.domain.llm.entity;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewFinding;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LlmTaskResult extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pr_review_finding_id")
    private PrReviewFinding prReviewFinding;
    
    @Column(name = "result_type", nullable = false)
    private String resultType;
    
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;
    
    @Column(name = "token_usage")
    private Integer tokenUsage;
    
    @Column(name = "chat_message_id")
    private Long chatMessageId;
}