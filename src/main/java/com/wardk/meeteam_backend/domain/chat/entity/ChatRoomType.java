package com.wardk.meeteam_backend.domain.chat.entity;

/**
 * 채팅방의 타입을 정의하는 열거형입니다.
 *
 * <p>MeeTeam 플랫폼에서 사용되는 다양한 종류의 채팅방을 구분합니다:</p>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
public enum ChatRoomType {

    /**
     * 프로젝트 기본 채팅방입니다.
     *
     * <p>프로젝트가 생성될 때 자동으로 생성되는 팀 전체 채팅방입니다.
     * 프로젝트 멤버들 간의 기본적인 소통 공간으로 사용됩니다.</p>
     */
    PROJECT,

    /**
     * 주제별 채팅방입니다.
     *
     * <p>프로젝트 내에서 특정 주제나 기능에 대해 논의하기 위해
     * 생성되는 별도의 채팅방입니다. 예: 'UI/UX 논의', '백엔드 개발' 등</p>
     */
    TOPIC,

    /**
     * 개인 채팅방입니다.
     *
     * <p>두 사용자 간의 1:1 개인 메시지를 위한 채팅방입니다.
     * 주로 프로젝트 지원 전 질문이나 개인적인 소통에 사용됩니다.</p>
     */
    PRIVATE,

    /**
     * Pull Request 리뷰 채팅방입니다.
     *
     * <p>기존 PR 리뷰 시스템과 연동된 채팅방으로,
     * 코드 리뷰 과정에서의 논의를 위해 사용됩니다.</p>
     */
    PR_REVIEW
}
