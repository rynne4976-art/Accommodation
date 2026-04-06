package com.Accommodation.repository;

import com.Accommodation.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberEmailOrderByRegTimeDesc(String email, Pageable pageable);
}
