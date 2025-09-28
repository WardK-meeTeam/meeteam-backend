package com.wardk.meeteam_backend.domain.chat.entity;

/**
 * 채팅방 멤버 역할 열거형
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
public enum ChatRoomRole {
    /**
     * 채팅방 소유자 (프로젝트 리더)
     */
    OWNER,

    /**
     * 채팅방 관리자
     */
    ADMIN,

    /**
     * 일반 멤버
     */
    MEMBER
}
