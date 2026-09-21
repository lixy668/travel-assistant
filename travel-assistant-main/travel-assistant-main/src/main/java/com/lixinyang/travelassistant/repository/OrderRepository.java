package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUsernameOrderByCreateTimeDesc(String username);
    List<Order> findByTypeOrderByCreateTimeDesc(String type);
    List<Order> findAllByOrderByCreateTimeDesc();
    long countByType(String type);
    List<Order> findByChangeStatusOrderByChangeApplyTimeDesc(String changeStatus);
    // 超时未支付（用于定时关单）
    List<Order> findByPayStatusAndCreateTimeBefore(String payStatus, LocalDateTime time);
}
