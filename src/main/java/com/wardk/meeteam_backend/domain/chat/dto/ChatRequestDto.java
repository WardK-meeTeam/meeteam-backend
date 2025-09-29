package com.wardk.meeteam_backend.domain.chat.dto;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 채팅 관련 요청을 위한 데이터 전송 객체들을 모아놓은 클래스입니다.
 *
 * <p>클라이언트에서 서버로 전송하는 다양한 채팅 요청들을 정의합니다:</p>
 * <ul>
 *   <li>채팅방 생성</li>
 *   <li>메시지 전송</li>
 *   <li>메시지 수정</li>
 *   <li>멤버 초대</li>
 *   <li>읽음 상태 업데이트</li>
 * </ul>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
public class ChatRequestDto {

    /**
     * 채팅방 생성 요청을 위한 데이터 전송 객체입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChatRoom {

        /**
         * 채팅방의 이름입니다.
         *
         * <p>필수 값이며, 빈 문자열이나 공백만으로 구성될 수 없습니다.</p>
         */
        @NotBlank(message = "채팅방 이름은 필수입니다")
        private String name;

        /**
         * 채팅방에 대한 설명입니다.
         *
         * <p>선택사항입니다.</p>
         */
        private String description;

        /**
         * 채팅방의 타입입니다.
         *
         * <p>필수 값이며, PROJECT, TOPIC, PRIVATE 중 하나여야 합니다.</p>
         *
         * @see ChatRoomType
         */
        @NotNull(message = "채팅방 타입은 필수입니다")
        private ChatRoomType type;

        /**
         * 연관될 프로젝트의 ID입니다.
         *
         * <p>PROJECT나 TOPIC 타입의 채팅방을 생성할 때 필수입니다.
         * PRIVATE 타입의 경우 null이어야 합니다.</p>
         */
        private Long projectId;

        /**
         * 개인 채팅방의 상대방 멤버 ID입니다.
         *
         * <p>PRIVATE 타입의 채팅방을 생성할 때 필수입니다.
         * 다른 타입의 경우 null이어야 합니다.</p>
         */
        private Long targetMemberId;

        /**
         * 초기 멤버 목록입니다.
         *
         * <p>선택사항이며, 채팅방 생성 시 함께 초대할 멤버들의 ID 목록입니다.</p>
         */
        private List<Long> memberIds;
    }

    /**
     * 메시지 전송 요청을 위한 데이터 전송 객체입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessage {

        /**
         * 메시지를 보낼 채팅방의 ID입니다.
         *
         * <p>필수 값입니다.</p>
         */
        @NotNull(message = "채팅방 ID는 필수입니다")
        private Long chatRoomId;

        /**
         * 전송할 메시지 내용입니다.
         *
         * <p>필수 값이며, 빈 문자열이나 공백만으로 구성될 수 없습니다.</p>
         */
        @NotBlank(message = "메시지 내용은 필수입니다")
        private String content;

        /**
         * 멘션할 사용자들의 ID 목록입니다.
         *
         * <p>선택사항이며, @username 형태로 멘션할 사용자들을 지정합니다.</p>
         */
        private List<Long> mentionedUserIds;
    }

    /**
     * 메시지 수정 요청을 위한 데이터 전송 객체입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditMessage {

        /**
         * 수정할 메시지의 ID입니다.
         *
         * <p>필수 값입니다.</p>
         */
        @NotNull(message = "메시지 ID는 필수입니다")
        private Long messageId;

        /**
         * 수정할 메시지의 새로운 내용입니다.
         *
         * <p>필수 값이며, 빈 문자열이나 공백만으로 구성될 수 없습니다.</p>
         */
        @NotBlank(message = "수정할 내용은 필수입니다")
        private String content;
    }

    /**
     * 채팅방 멤버 초대 요청을 위한 데이터 전송 객체입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InviteMembers {

        /**
         * 멤버를 초대할 채팅방의 ID입니다.
         *
         * <p>필수 값입니다.</p>
         */
        @NotNull(message = "채팅방 ID는 필수입니다")
        private Long chatRoomId;

        /**
         * 초대할 멤버들의 ID 목록입니다.
         *
         * <p>필수 값이며, 최소 한 명 이상의 멤버를 포함해야 합니다.</p>
         */
        @NotNull(message = "초대할 멤버 목록은 필수입니다")
        private List<Long> memberIds;
    }

    /**
     * 읽음 상태 업데이트 요청을 위한 데이터 전송 객체입니다.
     *
     * @author MeeTeam Backend Team
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateReadStatus {

        /**
         * 읽음 상태를 업데이트할 채팅방의 ID입니다.
         *
         * <p>필수 값입니다.</p>
         */
        @NotNull(message = "채팅방 ID는 필수입니다")
        private Long chatRoomId;
    }
}
