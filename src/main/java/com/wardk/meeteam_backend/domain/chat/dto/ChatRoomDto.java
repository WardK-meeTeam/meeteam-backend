package com.wardk.meeteam_backend.domain.chat.dto;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 정보를 전송하기 위한 데이터 전송 객체입니다.
 *
 * <p>클라이언트에게 채팅방의 상세 정보와 상태를 제공합니다:</p>
 * <ul>
 *   <li>채팅방 기본 정보 (이름, 설명, 타입)</li>
 *   <li>프로젝트 연관 정보</li>
 *   <li>멤버 목록 및 상태</li>
 *   <li>읽지 않은 메시지 수</li>
 *   <li>마지막 메시지 정보</li>
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
public class ChatRoomDto {

    /**
     * 채팅방의 고유 식별자입니다.
     */
    private Long id;

    /**
     * 채팅방의 이름입니다.
     */
    private String name;

    /**
     * 채팅방에 대한 설명입니다.
     */
    private String description;

    /**
     * 채팅방의 타입입니다.
     *
     * @see ChatRoomType
     */
    private ChatRoomType type;

    /**
     * 연관된 프로젝트의 ID입니다.
     *
     * <p>PRIVATE 타입 채팅방의 경우 null입니다.</p>
     */
    private Long projectId;

    /**
     * 연관된 프로젝트의 이름입니다.
     *
     * <p>PRIVATE 타입 채팅방의 경우 null입니다.</p>
     */
    private String projectName;

    /**
     * 채팅방을 생성한 사용자의 ID입니다.
     */
    private Long creatorId;

    /**
     * 채팅방 생성자의 이름입니다.
     */
    private String creatorName;

    /**
     * 채팅방의 활성 상태를 나타냅니다.
     */
    private Boolean isActive;

    /**
     * 마지막 메시지가 전송된 시간입니다.
     *
     * <p>채팅방 목록 정렬에 사용됩니다.</p>
     */
    private LocalDateTime lastMessageTime;

    /**
     * 채팅방에 참여한 활성 멤버 수입니다.
     */
    private Integer memberCount;

    /**
     * 현재 사용자의 읽지 않은 메시지 수입니다.
     */
    private Integer unreadCount;

    /**
     * 마지막 메시지의 내용입니다.
     *
     * <p>채팅방 목록에서 미리보기로 표시됩니다.</p>
     */
    private String lastMessage;

    /**
     * 채팅방이 생성된 시간입니다.
     */
    private LocalDateTime createdAt;

    /**
     * 채팅방에 참여한 멤버들의 목록입니다.
     */
    private List<ChatRoomMemberDto> members;

    /**
     * 채팅방 멤버 정보를 나타내는 내부 데이터 전송 객체입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRoomMemberDto {

        /**
         * 멤버의 사용자 ID입니다.
         */
        private Long memberId;

        /**
         * 멤버의 이름(닉네임)입니다.
         */
        private String memberName;

        /**
         * 멤버의 프로필 이미지 URL입니다.
         */
        private String profileImage;

        /**
         * 멤버가 마지막으로 메시지를 읽은 시간입니다.
         */
        private LocalDateTime lastReadTime;

        /**
         * 멤버의 채팅방 활성 상태입니다.
         */
        private Boolean isActive;
    }
}
