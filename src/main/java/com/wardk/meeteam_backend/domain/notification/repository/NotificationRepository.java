package com.wardk.meeteam_backend.domain.notification.repository;

import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
                select n from Notification n
                left join fetch n.receiver m
                left join fetch n.project p
                where m.id = :memberId
                order by n.createdAt desc
            """)
    Slice<Notification> findByMemberId(Long memberId, Pageable pageable);

}
