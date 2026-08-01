package com.fsm.keystone.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String partName;

    @Column(nullable = false, unique = true)
    private String partNumber;

    private String description;

    private BigDecimal unitPrice;

    private Integer stockQuantity;

    private Boolean active;

    @PrePersist
    public void init(){
        if(active==null) active=true;
        if(stockQuantity==null)
            stockQuantity=0;
    }
}
