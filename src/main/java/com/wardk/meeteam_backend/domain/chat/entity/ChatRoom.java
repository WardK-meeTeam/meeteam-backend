package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 다양한 타입의 채팅방을 관리하는 엔티티입니다.
 *
 * <p>채팅방은 다음과 같은 타입들을 지원합니다:</p>
 * <ul>
 *   <li>PROJECT: 프로젝트 기본 채팅방 (프로젝트 생성 시 자동 생성)</li>
 *   <li>TOPIC: 주제별 채팅방 (프로젝트 내 특정 주제 논의용)</li>
 *   <li>PRIVATE: 개인 채팅방 (1:1 개인 메시지)</li>
 * </ul>
 *
 * <p>각 채팅방은 멤버 관리, 메시지 관리, 읽음 상태 추적 등의 기능을 제공합니다.</p>
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
@Table(name = "chat_room")
public class ChatRoom extends BaseEntity {

    /**
     * 채팅방의 고유 식별자입니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방의 이름입니다.
     *
     * <p>최대 50자까지 설정 가능하며, 필수 값입니다.</p>
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 채팅방에 대한 설명입니다.
     *
     * <p>최대 500자까지 설정 가능하며, 선택사항입니다.</p>
     */
    @Column(length = 500)
    private String description;

    /**
     * 채팅방의 타입을 나타냅니다.
     *
     * @see ChatRoomType
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    /**
     * 채팅방과 연관된 프로젝트입니다.
     *
     * <p>PROJECT나 TOPIC 타입의 채팅방에서만 설정됩니다.
     * PRIVATE 타입의 채팅방에서는 null입니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = true)
    private PullRequest pullRequest;

    /**
     * 채팅방을 생성한 사용자의 ID입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false, updatable = false)
    private Member creatorId;

    /**
     * 채팅방의 활성 상태를 나타냅니다.
     *
     * <p>기본값은 true이며, 채팅방이 비활성화되면 false로 설정됩니다.</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 마지막 메시지가 전송된 시간입니다.
     *
     * <p>채팅방 목록을 정렬할 때 사용됩니다.</p>
     */
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    /**
     * 채팅방에 참여한 멤버들의 목록입니다.
     *
     * <p>ChatRoomMember 엔티티와 1:N 관계를 가집니다.</p>
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> members = new ArrayList<>();

    /**
     * 채팅방의 메시지들입니다.
     *
     * <p>ChatMessage 엔티티와 1:N 관계를 가집니다.</p>
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 마지막 메시지 시간을 업데이트합니다.
     *
     * @param time 새로운 마지막 메시지 시간
     */
    public void updateLastMessageTime(LocalDateTime time) {
        this.lastMessageTime = time;
    }

    /**
     * 채팅방을 비활성화합니다.
     *
     * <p>비활성화된 채팅방은 사용자에게 표시되지 않습니다.</p>
     */
    public void deactivate() {
        this.isActive = false;
    }
}
