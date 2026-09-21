package com.lixinyang.travelassistant.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Entity @Table(name = "t_admin")
public class Admin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, length = 50) private String username;
    @Column(nullable = false) private String password;
    @Column(length = 20) private String role = "ADMIN";
    private LocalDateTime createTime = LocalDateTime.now();
}