package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 멤버 관리 및 읽음 상태를 추적하는 엔티티입니다.
 *
 * <p>이 엔티티는 다음과 같은 기능을 제공합니다:</p>
 * <ul>
 *   <li>채팅방 참여자 관리</li>
 *   <li>개별 사용자의 읽음 상태 추적</li>
 *   <li>읽지 않은 메시지 수 관리</li>
 *   <li>멤버의 채팅방 활성 상태 관리</li>
 * </ul>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "chat_room_member",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_chat_room_member",
        columnNames = {"chat_room_id", "member_id"}
    )
)
public class ChatRoomMember extends BaseEntity {

    /**
     * 채팅방 멤버의 고유 식별자입니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 멤버가 속한 채팅방입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 채팅방에 참여한 사용자 정보입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 사용자가 마지막으로 메시지를 읽은 시간입니다.
     *
     * <p>이 시간 이후의 메시지들은 읽지 않은 메시지로 간주됩니다.</p>
     */
    @Column(name = "last_read_time")
    private LocalDateTime lastReadTime;

    /**
     * 채팅방에서의 멤버 활성 상태를 나타냅니다.
     *
     * <p>기본값은 true이며, 멤버가 채팅방을 나가면 false로 설정됩니다.</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 읽지 않은 메시지의 개수입니다.
     *
     * <p>새 메시지가 도착하면 증가하고, 사용자가 메시지를 읽으면 0으로 초기화됩니다.</p>
     */
    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private Integer unreadCount = 0;

    /**
     * 마지막 읽은 시간을 업데이트하고 읽지 않은 메시지 수를 초기화합니다.
     *
     * @param time 새로운 마지막 읽은 시간
     */
    public void updateLastReadTime(LocalDateTime time) {
        this.lastReadTime = time;
        this.unreadCount = 0;
    }

    /**
     * 읽지 않은 메시지 수를 1 증가시킵니다.
     *
     * <p>새로운 메시지가 도착했을 때 호출됩니다.</p>
     */
    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    /**
     * 채팅방에서 나가기 처리를 합니다.
     *
     * <p>멤버의 활성 상태를 false로 설정합니다.</p>
     */
    public void leave() {
        this.isActive = false;
    }
}
