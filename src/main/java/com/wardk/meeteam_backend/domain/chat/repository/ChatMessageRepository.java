package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List; import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

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
}