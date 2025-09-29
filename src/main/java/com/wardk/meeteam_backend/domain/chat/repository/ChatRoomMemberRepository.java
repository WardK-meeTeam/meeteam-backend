package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomMember;
import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 멤버 Repository
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    /**
     * 채팅방과 멤버로 채팅방 멤버 정보를 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param memberId 멤버 ID
     * @return 채팅방 멤버 정보
     */
    Optional<ChatRoomMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    /**
     * 채팅방의 모든 멤버를 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    /**
     * 사용자가 참여한 모든 채팅방을 조회합니다.
     *
     * @param memberId 멤버 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByMemberId(Long memberId);

    /**
     * 채팅방의 특정 역할을 가진 멤버들을 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param role 역할
     * @return 해당 역할의 멤버 목록
     */
    List<ChatRoomMember> findByChatRoomIdAndRole(Long chatRoomId, ChatRoomRole role);

    /**
     * 사용자가 채팅방에 참여하고 있는지 확인합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param memberId 멤버 ID
     * @return 참여 여부
     */
    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    /**
     * 채팅방의 멤버 수를 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 멤버 수
     */
    long countByChatRoomId(Long chatRoomId);
}
