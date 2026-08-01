package com.fsm.keystone.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String siteName;

    private String address;

    private String city;

    private String state;

    private String pincode;

    private Double latitude;

    private Double longitude;

    private String contactPerson;

    private String contactPhone;

    @ManyToOne(optional = false)    //  many sites are link to one customer
    @JoinColumn(name = "customer_id")  // ( customer is parent) and site is child table
    private Customer customer;
}
