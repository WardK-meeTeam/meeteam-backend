package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoom;
import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 프로젝트의 기본 채팅방 조회
    Optional<ChatRoom> findByProjectIdAndType(Long projectId, ChatRoomType type);

    // 프로젝트의 모든 채팅방 조회
    List<ChatRoom> findByProjectIdAndIsActiveTrue(Long projectId);

    // 개인 채팅방 조회 (두 사용자 간)
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN cr.members m1 " +
           "JOIN cr.members m2 " +
           "WHERE cr.type = :type " +
           "AND cr.isActive = true " +
           "AND m1.member.id = :userId1 " +
           "AND m2.member.id = :userId2 " +
           "AND m1.isActive = true " +
           "AND m2.isActive = true")
    Optional<ChatRoom> findPrivateChatRoom(@Param("type") ChatRoomType type,
                                          @Param("userId1") Long userId1,
                                          @Param("userId2") Long userId2);

    // 사용자가 참여한 모든 채팅방 조회
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN cr.members m " +
           "WHERE m.member.id = :memberId " +
           "AND cr.isActive = true " +
           "AND m.isActive = true " +
           "ORDER BY cr.lastMessageTime DESC")
    List<ChatRoom> findByMemberIdOrderByLastMessageTimeDesc(@Param("memberId") Long memberId);
}
