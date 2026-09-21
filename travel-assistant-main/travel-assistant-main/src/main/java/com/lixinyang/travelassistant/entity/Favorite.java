package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 收藏的酒店 / 景点 */
@Data
@Entity
@Table(name = "t_favorite")
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String username;

    @Column(length = 20)
    private String type;

    @Column(length = 120)
    private String name;

    @Column(length = 60)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(length = 600)
    private String image;

    private Double price;

    private LocalDateTime createTime = LocalDateTime.now();
}
