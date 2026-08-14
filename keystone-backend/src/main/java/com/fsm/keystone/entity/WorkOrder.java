package com.fsm.keystone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.SlaStatus;
import com.fsm.keystone.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String workOrderNumber;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    private LocalDateTime actualStart;

    private LocalDateTime actualEnd;

    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status")
    private SlaStatus slaStatus;

    private BigDecimal totalPartsCost;

    private Integer totalMinutes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id") // many work order are linked to one customer
    private Customer customer;

    @ManyToOne(optional = false) // many workorder are linked to one site
    @JoinColumn(name = "site_id") // or we can say one site can have many customers
    private Site site;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id") // many work order are linked to app users
    private AppUser createdBy;  // or we can say one app user(manager/dispatcher like)
                                // can create many work order

    @ManyToOne
    @JoinColumn(name = "assigned_technician_id") // many work order can be created by one technician
    private AppUser assignedTechnician;

    @JsonIgnore
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL)// one work order can use many parts
    private List<PartUsage> partUsages = new ArrayList<>();// or one work order can use list of parts

    @JsonIgnore
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL) // one work order
    private List<TimeLog> timeLogs = new ArrayList<>();//  can have many timelogs or list of times

    @JsonIgnore
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL)// one work order can have
    private List<StatusHistory> statusHistory = new ArrayList<>();// many status history

    @PrePersist
    public void onCreate(){
        createdAt=LocalDateTime.now();
        updatedAt=createdAt;
        if(status==null)
            status=WorkOrderStatus.NEW;
        if(slaStatus==null)
            slaStatus=SlaStatus.ON_TRACK;
        if(totalPartsCost==null)
            totalPartsCost=BigDecimal.ZERO;
        if(totalMinutes==null)
            totalMinutes=0;
        if(priority==null)
            priority=Priority.MEDIUM;
        if(workOrderNumber==null)
            workOrderNumber="WO-"+System.currentTimeMillis();
    }
    @PreUpdate
    public void onUpdate(){
        updatedAt=LocalDateTime.now();
    }
}
