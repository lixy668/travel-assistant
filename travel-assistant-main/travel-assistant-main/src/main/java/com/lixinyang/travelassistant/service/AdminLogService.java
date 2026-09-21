package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.entity.AdminLog;
import com.lixinyang.travelassistant.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 管理员操作审计：登录、审批、改单、删单、开票都留痕 */
@Service
@RequiredArgsConstructor
public class AdminLogService {
    private final AdminLogRepository repo;

    public void log(String admin, String action, String target, String detail) {
        try {
            AdminLog l = new AdminLog();
            l.setAdminUsername(admin == null ? "unknown" : admin);
            l.setAction(action);
            l.setTarget(target == null ? "" : target);
            l.setDetail(detail == null ? "" : (detail.length() > 280 ? detail.substring(0, 280) + "…" : detail));
            repo.save(l);
        } catch (Exception e) {
            System.out.println("[审计日志] 写入失败: " + e.getMessage());
        }
    }

    public List<AdminLog> recent() {
        return repo.findTop200ByOrderByCreateTimeDesc();
    }
}
