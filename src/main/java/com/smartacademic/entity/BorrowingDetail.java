package com.smartacademic.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "borrowing_details")
@Data
@NoArgsConstructor
public class BorrowingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private BorrowingRecord borrowingRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "return_quantity")
    private Integer returnQuantity = 0;

    public BorrowingDetail(BorrowingRecord borrowingRecord, Equipment equipment, Integer quantity) {
        this.borrowingRecord = borrowingRecord;
        this.equipment = equipment;
        this.quantity = quantity;
        this.returnQuantity = 0;
    }
}