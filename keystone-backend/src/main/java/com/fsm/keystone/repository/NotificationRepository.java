package com.fsm.keystone.repository;

import com.fsm.keystone.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            select n from Notification n
            left join fetch n.workOrder
            where n.user.id = :userId
            order by n.createdAt desc
            """)
    List<Notification> findByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
