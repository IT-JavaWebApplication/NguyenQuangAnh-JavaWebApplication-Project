package com.smartacademic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lab_room_types")
@Data
public class LabRoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private Integer capacity;

    private String description;
}