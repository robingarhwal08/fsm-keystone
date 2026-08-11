package com.fsm.keystone.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false)
    private String action;

    private String previousValue;

    private String newValue;

    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
