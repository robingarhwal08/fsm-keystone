package com.fsm.keystone.repository;

import com.fsm.keystone.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    List<UserAuditLog> findByTargetUserId(Long targetUserId);

    List<UserAuditLog> findByActorId(Long actorId);
}
