package com.wardk.meeteam_backend.domain.notification.repository;

import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
