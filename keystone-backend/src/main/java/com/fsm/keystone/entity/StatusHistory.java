package com.fsm.keystone.entity;

import com.fsm.keystone.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private WorkOrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private WorkOrderStatus newStatus;

    private String comment;

    private LocalDateTime changedAt;

    @ManyToOne(optional = false)  // many status history can be linked with one work order
    @JoinColumn(name = "work_order_id")// or we can say one work order can have many status history
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "changed_by_user_id")
    private AppUser changedBy; // many status history can be changed by one app user.

    @PrePersist
    public void onCreate(){
        changedAt=LocalDateTime.now();
    }
}
