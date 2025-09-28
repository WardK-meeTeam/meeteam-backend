package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    /**
     * 채팅방 (다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 발신자 (다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    /**
     * 메시지 내용
     */
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    /**
     * 메시지 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    /**
     * 메시지 전송 시간
     */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /**
     * 읽음 상태
     */
    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    /**
     * 파일 URL (이미지, 파일 메시지의 경우)
     */
    @Column(name = "file_url")
    private String fileUrl;

    /**
     * 파일 이름 (파일 메시지의 경우)
     */
    @Column(name = "file_name")
    private String fileName;

    /**
     * 파일 크기 (파일 메시지의 경우)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 메시지를 읽음 처리합니다.
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 파일 정보를 설정합니다.
     *
     * @param fileUrl 파일 URL
     * @param fileName 파일 이름
     * @param fileSize 파일 크기
     */
    public void setFileInfo(String fileUrl, String fileName, Long fileSize) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
