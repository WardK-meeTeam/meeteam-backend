package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 엔티티
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;


    /**
    *  채팅방에 속한 prReviewJob
    */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_review_job_id", unique = true)
    private PrReviewJob prReviewJob;

    /**
     * 채팅방 이름
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 채팅방 설명
     */
    @Column(name = "description")
    private String description;

    /**
     * 채팅방 종류
    */
    @Column(name = "type", nullable = false)
    private ChatRoomType type;

    /**
     * 채팅방 생성자
     */
    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;

    /**
     * 마지막 메시지 내용
     */
    @Column(name = "last_message_content", columnDefinition = "LONGTEXT")
    private String lastMessageContent;

    /**
     * 마지막 메시지 전송 시간
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 채팅방 활성 상태
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * 채팅 메시지 목록 (일대다 관계)
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 채팅방 참가자 목록 (일대다 관계)
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> members = new ArrayList<>();

    /**
     * 마지막 메시지 정보를 업데이트합니다.
     *
     * @param lastMessage 마지막 메시지
     */
    public void updateLastMessage(ChatMessage lastMessage) {
        this.lastMessageContent = lastMessage.getContent();
        this.lastMessageAt = lastMessage.getSentAt();
    }

    /**
     * 채팅방을 비활성화합니다.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 채팅방을 활성화합니다.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 채팅방 이름을 변경합니다.
     *
     * @param newRoomName 새로운 채팅방 이름
     */
    public void updateRoomName(String newRoomName) {
        this.name = newRoomName;
    }

    /**
     * 채팅방 설명을 변경합니다.
     *
     * @param newDescription 새로운 채팅방 설명
     */
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
}
