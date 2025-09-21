package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 정보를 관리하는 엔티티입니다.
 *
 * <p>이 엔티티는 다음과 같은 기능을 지원합니다:</p>
 * <ul>
 *   <li>일반 채팅방 메시지 (ChatRoom과 연관)</li>
 *   <li>PR 리뷰 채팅 메시지 (기존 ChatThread와 연관)</li>
 *   <li>메시지 수정 및 삭제 상태 관리</li>
 *   <li>다양한 메시지 타입 지원 (텍스트, 이미지, 파일 등)</li>
 *   <li>사용자 멘션 기능</li>
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
@Table(indexes = {
        @Index(name = "ix_msg_thread_id_id", columnList = "thread_id, id"),
        @Index(name = "ix_msg_chat_room_id_id", columnList = "chat_room_id, id")
})
public class ChatMessage extends BaseEntity {

    /**
     * 메시지의 고유 식별자입니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * PR 리뷰 채팅용 스레드 ID입니다.
     *
     * <p>기존 PR 리뷰 기능과의 호환성을 위해 유지됩니다.
     * 일반 채팅방 메시지에서는 null입니다.</p>
     */
    @Column(name = "thread_id")
    private Long threadId;

    /**
     * 새로운 채팅방 시스템용 채팅방 정보입니다.
     *
     * <p>일반 채팅방 메시지에서 사용됩니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 메시지를 작성한 사용자의 ID입니다.
     *
     * <p>시스템 메시지의 경우 null일 수 있습니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    @Column(name = "member_id")
    private Member member;

    /**
     * 메시지 발신자의 역할을 나타냅니다.
     *
     * @see SenderRole
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderRole senderRole;

    /**
     * 메시지의 타입을 나타냅니다.
     *
     * <p>기본값은 TEXT이며, 향후 이미지, 파일 등의 타입을 지원할 예정입니다.</p>
     *
     * @see MessageType
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /**
     * 메시지의 내용입니다.
     *
     * <p>텍스트 형태로 저장되며, 긴 메시지도 지원합니다.</p>
     */
    @Column(columnDefinition = "text")
    private String content;

    /**
     * 멘션된 사용자들의 ID 목록입니다.
     *
     * <p>콤마로 구분된 사용자 ID 문자열로 저장됩니다.
     * 예: "1,2,3"</p>
     */
    @Column(columnDefinition = "text")
    private String mentionedMemberIds;

    /**
     * 메시지 삭제 여부를 나타냅니다.
     *
     * <p>기본값은 false이며, 삭제된 메시지는 true로 설정됩니다.
     * 삭제된 메시지는 목록에서 제외되지만 데이터는 보존됩니다.</p>
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 메시지 수정 여부를 나타냅니다.
     *
     * <p>기본값은 false이며, 수정된 메시지는 true로 설정됩니다.</p>
     */
    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    /**
     * 메시지가 마지막으로 수정된 시간입니다.
     *
     * <p>메시지가 수정되지 않았다면 null입니다.</p>
     */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /**
     * 메시지 내용을 수정합니다.
     *
     * @param newContent 새로운 메시지 내용
     */
    public void editMessage(String newContent) {
        this.content = newContent;
        this.isEdited = true;
        this.editedAt = LocalDateTime.now();
    }

    /**
     * 메시지를 삭제 처리합니다.
     *
     * <p>실제로 데이터를 삭제하지 않고 삭제 플래그만 설정합니다.</p>
     */
    public void deleteMessage() {
        this.isDeleted = true;
    }
}
