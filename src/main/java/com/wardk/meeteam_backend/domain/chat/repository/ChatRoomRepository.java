package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 Repository
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * @param memberId 멤버 ID
     * @return 채팅방 목록
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN cr.members crm " +
           "WHERE crm.member.id = :memberId " +
           "AND cr.isActive = true " +
           "ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 활성화된 채팅방 목록을 조회합니다.
     *
     * @return 활성화된 채팅방 목록
     */
    List<ChatRoom> findByIsActiveTrue();

    /**
     * 채팅방 이름으로 검색합니다.
     *
     * @param roomName 채팅방 이름
     * @return 채팅방 목록
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.name LIKE %:roomName% " +
           "AND cr.isActive = true")
    List<ChatRoom> findByRoomNameContaining(@Param("roomName") String roomName);

    /**
     * 사용자가 속한 코드리뷰 채팅방 ID만 조회 (가장 빠른 쿼리)
     * 채팅방 목록만 필요하고 상세 정보는 나중에 로딩할 때 사용
     */
    @Query("""
        SELECT cr.id
        FROM ChatRoom cr
        WHERE cr.prReviewJob.id IN (
            SELECT prj.id FROM PrReviewJob prj
            JOIN prj.pullRequest pr
            JOIN pr.projectRepo repo
            JOIN repo.project p
            JOIN p.members pm
            WHERE pm.member.id = :memberId
        )
        AND cr.isActive = true
        ORDER BY cr.lastMessageAt DESC NULLS LAST
        """)
    List<Long> findChatRoomIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 기존 방식 (엔티티 전체 조회) - 호환성을 위해 유지
     * 프로젝트 멤버를 통해 채팅방을 조회합니다.
     */
    @Query("""
        SELECT DISTINCT cr FROM ChatRoom cr
        JOIN FETCH cr.prReviewJob prj
        JOIN FETCH prj.pullRequest pr
        WHERE cr.prReviewJob.id IN (
            SELECT prj2.id FROM PrReviewJob prj2
            JOIN prj2.pullRequest pr2
            JOIN pr2.projectRepo repo
            JOIN repo.project p
            JOIN p.members pm
            WHERE pm.member.id = :memberId
        )
        AND cr.isActive = true
        ORDER BY cr.lastMessageAt DESC NULLS LAST
        """)
    List<ChatRoom> findChatRoomsByMemberId(@Param("memberId") Long memberId);

    /**
     * PR Review Job ID로 채팅방 조회
     */
    Optional<ChatRoom> findByPrReviewJobId(Long prReviewJobId);
}
