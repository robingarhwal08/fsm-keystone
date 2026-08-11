package com.fsm.keystone.service;

import com.fsm.keystone.entity.UserAuditLog;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAdminAuditService {

    private final UserAuditLogRepository userAuditLogRepository;

    public void recordRoleChange(Long actorId, Long targetUserId, Role oldRole, Role newRole) {
        userAuditLogRepository.save(UserAuditLog.builder()
                .actorId(actorId)
                .targetUserId(targetUserId)
                .action("ROLE_CHANGE")
                .previousValue(oldRole != null ? oldRole.name() : null)
                .newValue(newRole != null ? newRole.name() : null)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    public void recordActivationChange(Long actorId, Long targetUserId,
                                       Boolean oldActive, Boolean newActive) {
        userAuditLogRepository.save(UserAuditLog.builder()
                .actorId(actorId)
                .targetUserId(targetUserId)
                .action("ACTIVE_CHANGE")
                .previousValue(oldActive != null ? oldActive.toString() : null)
                .newValue(newActive != null ? newActive.toString() : null)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
