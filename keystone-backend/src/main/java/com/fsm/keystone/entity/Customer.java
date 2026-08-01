package com.fsm.keystone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private String phone;

    private String billingAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore // one customer can have many site or a list of sites
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Site> sites = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "customer") // one customer can have many workorders or list of work orders
    private List<WorkOrder> workOrders = new ArrayList<>();

    @PrePersist // Runs before INSERT.
    public void onCreate(){
        createdAt=LocalDateTime.now();
        updatedAt=createdAt;
    }
    @PreUpdate // Runs before UPDATE.
    public void onUpdate(){
        updatedAt=LocalDateTime.now();
    }
}
