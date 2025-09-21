package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // PR 리뷰 채팅용 (기존 기능 유지)
  // 첫 페이지 (cursor 없을때): 오름차순
  @Query("""
           SELECT m FROM ChatMessage m
           WHERE m.threadId = :threadId
           ORDER BY m.id ASC
           """)
  List<ChatMessage> firstPage(@Param("threadId") Long threadId, Pageable pageable);

  // 다음 페이지 (cursor 있을때): id > :cursor
  @Query("""
           SELECT m FROM ChatMessage m
           WHERE m.threadId = :threadId AND m.id > :cursor
           ORDER BY m.id ASC
           """)
  List<ChatMessage> pageAfter(@Param("threadId") Long threadId,
                              @Param("cursor") Long cursor,
                              Pageable pageable);

  // 새로운 채팅방 시스템용
  // 채팅방의 메시지 조회 (삭제되지 않은 메시지만)
  @Query("SELECT m FROM ChatMessage m " +
         "WHERE m.chatRoom.id = :chatRoomId " +
         "AND m.isDeleted = false " +
         "ORDER BY m.createdAt DESC")
  List<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

  // 채팅방의 메시지 조회 (커서 기반 페이징)
  @Query("SELECT m FROM ChatMessage m " +
         "WHERE m.chatRoom.id = :chatRoomId " +
         "AND m.isDeleted = false " +
         "AND m.id < :cursor " +
         "ORDER BY m.createdAt DESC")
  List<ChatMessage> findByChatRoomIdBeforeCursor(@Param("chatRoomId") Long chatRoomId,
                                                @Param("cursor") Long cursor,
                                                Pageable pageable);

  // 특정 시간 이후의 메시지 조회 (읽지 않은 메시지 확인용)
  @Query("SELECT COUNT(m) FROM ChatMessage m " +
         "WHERE m.chatRoom.id = :chatRoomId " +
         "AND m.isDeleted = false " +
         "AND m.createdAt > :lastReadTime " +
         "AND m.memberId != :memberId")
  Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                          @Param("lastReadTime") java.time.LocalDateTime lastReadTime,
                          @Param("memberId") Long memberId);

  // 채팅방의 마지막 메시지 조회
  @Query("SELECT m FROM ChatMessage m " +
         "WHERE m.chatRoom.id = :chatRoomId " +
         "AND m.isDeleted = false " +
         "ORDER BY m.createdAt DESC " +
         "LIMIT 1")
  ChatMessage findLastMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}