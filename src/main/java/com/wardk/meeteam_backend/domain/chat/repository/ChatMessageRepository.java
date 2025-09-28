package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 메시지 Repository
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅방의 메시지를 최신순으로 페이징 조회합니다. (기존 방식 - 호환성 유지)
     *
     * @param roomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return 페이징된 채팅 메시지 목록
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :roomId " +
           "ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findByChatRoomIdOrderBySentAtDesc(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 채팅방의 메시지를 최신순으로 페이징 조회 (최적화된 버전)
     * 필요한 컬럼만 선택하여 성능을 개선합니다.
     *
     * @param roomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return Object[] 배열 형태의 메시지 데이터 (id, senderId, senderName, content, messageType, sentAt, isRead)
     */
    @Query("""
        SELECT cm.id, cm.sender.id, cm.sender.realName, cm.content, 
               cm.messageType, cm.sentAt, cm.isRead
        FROM ChatMessage cm
        LEFT JOIN cm.sender s
        WHERE cm.chatRoom.id = :roomId
        ORDER BY cm.sentAt DESC
        """)
    Page<Object[]> findChatMessageInfoByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 채팅방 메시지 조회 (가장 빠른 버전 - ID와 기본 정보만)
     * 단순 메시지 목록이나 카운트가 필요할 때 사용
     *
     * @param roomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return Object[] 배열 (id, content, sentAt)
     */
    @Query("""
        SELECT cm.id, cm.content, cm.sentAt
        FROM ChatMessage cm
        WHERE cm.chatRoom.id = :roomId
        ORDER BY cm.sentAt DESC
        """)
    Page<Object[]> findBasicMessagesByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 채팅방 메시지 ID 목록만 조회 (초고속)
     * 메시지 존재 여부 확인이나 간단한 처리용
     *
     * @param roomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return 메시지 ID 목록
     */
    @Query("SELECT cm.id FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId ORDER BY cm.sentAt DESC")
    Page<Long> findMessageIdsByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 특정 시간 이후 메시지 조회 (최적화된 버전)
     * 실시간 채팅에서 새 메시지 확인용
     *
     * @param roomId 채팅방 ID
     * @param afterTime 기준 시간
     * @param limit 최대 조회 개수
     * @return Object[] 배열 형태의 메시지 데이터
     */
    @Query("""
        SELECT cm.id, cm.sender.id, cm.sender.realName, cm.content, 
               cm.messageType, cm.sentAt, cm.isRead
        FROM ChatMessage cm
        LEFT JOIN cm.sender s
        WHERE cm.chatRoom.id = :roomId AND cm.sentAt > :afterTime
        ORDER BY cm.sentAt ASC
        LIMIT :limit
        """)
    List<Object[]> findRecentMessagesByRoomId(@Param("roomId") Long roomId,
                                             @Param("afterTime") LocalDateTime afterTime,
                                             @Param("limit") int limit);

    /**
     * 읽지 않은 메시지 개수 조회 (최적화)
     * 인덱스를 효율적으로 사용하도록 쿼리 개선
     */
    @Query("""
        SELECT COUNT(cm.id) 
        FROM ChatMessage cm 
        WHERE cm.chatRoom.id = :roomId 
        AND cm.sentAt > :lastReadAt 
        AND (cm.sender.id != :userId OR cm.sender.id IS NULL)
        """)
    long countUnreadMessages(@Param("roomId") Long roomId,
                            @Param("lastReadAt") LocalDateTime lastReadAt,
                            @Param("userId") Long userId);

    /**
     * 채팅방의 마지막 메시지를 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @return 마지막 메시지
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :roomId " +
           "ORDER BY cm.sentAt DESC LIMIT 1")
    ChatMessage findLastMessageByRoomId(@Param("roomId") Long roomId);

    /**
     * 특정 사용자가 보낸 메시지를 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 메시지 목록
     */
    Page<ChatMessage> findByChatRoomIdAndSenderIdOrderBySentAtDesc(Long roomId, Long senderId, Pageable pageable);
}
