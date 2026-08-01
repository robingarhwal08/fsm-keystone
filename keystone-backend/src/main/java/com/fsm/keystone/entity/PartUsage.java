package com.fsm.keystone.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "part_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantityUsed;

    private BigDecimal unitPriceAtUsage;

    private BigDecimal totalCost;

    private LocalDateTime usedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "work_order_id") // many parts uages can be happen by one work order
    private WorkOrder workOrder;

    @ManyToOne(optional = false) // many part usage are linked to one parts
    @JoinColumn(name = "part_id") // ie part table is parent and part usage is child
    private Part part;

    @ManyToOne
    @JoinColumn(name = "used_by_user_id") // many parts can be used by one user
    private AppUser usedBy;

    @PrePersist
    public void onCreate(){
        usedAt=LocalDateTime.now();
    }
}
