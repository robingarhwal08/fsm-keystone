package com.fsm.keystone.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal hoursSpent;

    private Integer minutesSpent;

    @Column(length = 1000)
    private String workDescription;

    @ManyToOne(optional = false)
    @JoinColumn(name = "work_order_id") // many time logs can be lined to one work order
    private WorkOrder workOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "technician_id") // many time logs can by updated by one techician or user
    private AppUser technician;

    @OneToMany(mappedBy = "timeLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimeLogPhoto> photos = new ArrayList<>();
}
