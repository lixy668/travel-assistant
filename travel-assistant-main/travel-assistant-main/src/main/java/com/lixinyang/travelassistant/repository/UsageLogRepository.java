package com.lixinyang.travelassistant.repository;
import com.lixinyang.travelassistant.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {
    long countByAction(String action);
    long countByActionAndCreateTimeAfter(String action, LocalDateTime time);
    long countByActionAndCreateTimeBetween(String action, LocalDateTime start, LocalDateTime end);
    List<UsageLog> findByCreateTimeAfter(LocalDateTime time);
    List<UsageLog> findTop50ByOrderByCreateTimeDesc();
}
