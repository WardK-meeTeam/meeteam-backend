package com.wardk.meeteam_backend.domain.chat.dto;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomType;
import com.wardk.meeteam_backend.domain.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket을 통한 채팅 메시지 전송을 위한 데이터 전송 객체입니다.
 *
 * <p>이 DTO는 다음과 같은 용도로 사용됩니다:</p>
 * <ul>
 *   <li>클라이언트와 서버 간 실시간 메시지 전송</li>
 *   <li>메시지 수정/삭제 알림</li>
 *   <li>타이핑 상태 전송</li>
 *   <li>채팅방 입장/퇴장 알림</li>
 * </ul>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    /**
     * 메시지의 고유 식별자입니다.
     */
    private Long id;

    /**
     * 메시지가 속한 채팅방의 ID입니다.
     */
    private Long chatRoomId;

    /**
     * 메시지를 보낸 사용자의 ID입니다.
     */
    private Long senderId;

    /**
     * 메시지를 보낸 사용자의 닉네임입니다.
     */
    private String senderName;

    /**
     * 메시지를 보낸 사용자의 프로필 이미지 URL입니다.
     */
    private String senderProfileImage;

    /**
     * 메시지의 내용입니다.
     */
    private String content;

    /**
     * 메시지의 타입입니다.
     *
     * @see MessageType
     */
    private MessageType messageType;

    /**
     * 메시지가 생성된 시간입니다.
     */
    private LocalDateTime createdAt;

    /**
     * 메시지가 마지막으로 수정된 시간입니다.
     *
     * <p>수정되지 않은 메시지의 경우 null입니다.</p>
     */
    private LocalDateTime editedAt;

    /**
     * 메시지가 수정되었는지 여부입니다.
     */
    private Boolean isEdited;

    /**
     * 메시지가 삭제되었는지 여부입니다.
     */
    private Boolean isDeleted;

    /**
     * 메시지에서 멘션된 사용자들의 ID 목록입니다.
     */
    private List<Long> mentionedUserIds;

    /**
     * WebSocket 메시지의 타입입니다.
     *
     * <p>클라이언트에서 메시지를 적절히 처리하기 위해 사용됩니다.</p>
     */
    private ChatMessageType type;

    /**
     * WebSocket을 통해 전송되는 메시지의 타입을 정의하는 열거형입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    public enum ChatMessageType {
        /**
         * 일반 채팅 메시지입니다.
         */
        CHAT,

        /**
         * 채팅방 입장 알림입니다.
         */
        JOIN,

        /**
         * 채팅방 퇴장 알림입니다.
         */
        LEAVE,

        /**
         * 메시지 수정 알림입니다.
         */
        EDIT,

        /**
         * 메시지 삭제 알림입니다.
         */
        DELETE,

        /**
         * 타이핑 시작 알림입니다.
         */
        TYPING_START,

        /**
         * 타이핑 종료 알림입니다.
         */
        TYPING_END,

        /**
         * 에러 메시지입니다.
         *
         * <p>WebSocket 처리 중 발생한 오류를 개별 사용자에게 전송할 때 사용됩니다.</p>
         */
        ERROR
    }
}
