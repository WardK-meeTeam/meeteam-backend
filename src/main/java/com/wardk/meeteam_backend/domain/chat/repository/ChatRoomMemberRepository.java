package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 특정 채팅방의 활성 멤버 조회
    List<ChatRoomMember> findByChatRoomIdAndIsActiveTrue(Long chatRoomId);

    // 특정 사용자가 특정 채팅방에 참여하고 있는지 확인
    Optional<ChatRoomMember> findByChatRoomIdAndMemberIdAndIsActiveTrue(Long chatRoomId, Long memberId);

    // 특정 채팅방의 멤버 수 조회
    @Query("SELECT COUNT(crm) FROM ChatRoomMember crm " +
           "WHERE crm.chatRoom.id = :chatRoomId AND crm.isActive = true")
    Long countActiveMembersByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    // 읽지 않은 메시지가 있는 채팅방 조회
    @Query("SELECT crm FROM ChatRoomMember crm " +
           "WHERE crm.member.id = :memberId " +
           "AND crm.isActive = true " +
           "AND crm.unreadCount > 0")
    List<ChatRoomMember> findUnreadChatRoomsByMemberId(@Param("memberId") Long memberId);

    // 특정 채팅방의 모든 멤버의 읽지 않은 메시지 수 증가
    @Modifying
    @Query("UPDATE ChatRoomMember crm " +
           "SET crm.unreadCount = crm.unreadCount + 1 " +
           "WHERE crm.chatRoom.id = :chatRoomId " +
           "AND crm.member.id != :senderId " +
           "AND crm.isActive = true")
    void incrementUnreadCountForOtherMembers(@Param("chatRoomId") Long chatRoomId,
                                           @Param("senderId") Long senderId);
}
