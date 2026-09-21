package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.TripPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
    List<TripPlan> findByUsernameOrderByCreateTimeDesc(String username);

    long countByUsername(String username);
}
