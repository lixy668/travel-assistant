package com.lixinyang.travelassistant.service;
import com.lixinyang.travelassistant.entity.UsageLog;
import com.lixinyang.travelassistant.repository.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class UsageLogService {
    private final UsageLogRepository logRepo;
    public void record(String username, String action) {
        UsageLog log = new UsageLog();
        log.setUsername(username == null || username.isBlank() ? "anonymous" : username);
        log.setAction(action);
        logRepo.save(log);
    }
}