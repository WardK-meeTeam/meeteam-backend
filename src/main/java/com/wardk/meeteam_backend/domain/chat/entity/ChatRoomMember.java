package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 멤버 엔티티
 * 채팅방과 사용자 간의 다대다 관계를 관리합니다.
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "chat_room_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_member_id")
    private Long id;

    /**
     * 채팅방 (다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 멤버 (다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 채팅방 입장 시간
     */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * 마지막 읽은 메시지 시간
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * 채팅방 알림 설정
     */
    @Column(name = "notification_enabled", nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    /**
     * 채팅방 멤버 역할 (OWNER, ADMIN, MEMBER)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private ChatRoomRole role = ChatRoomRole.MEMBER;

    /**
     * 마지막 읽은 시간을 업데이트합니다.
     */
    public void updateLastReadAt() {
        this.lastReadAt = LocalDateTime.now();
    }

    /**
     * 알림 설정을 변경합니다.
     * @param enabled 알림 활성화 여부
     */

    public void setNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    /**
     * 역할을 변경합니다.
     *
     * @param role 새로운 역할
     */
    public void updateRole(ChatRoomRole role) {
        this.role = role;
    }
}
