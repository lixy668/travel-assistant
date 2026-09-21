package com.lixinyang.travelassistant.service;
import com.lixinyang.travelassistant.repository.AdminRepository;
import com.lixinyang.travelassistant.repository.UserRepository;
import com.lixinyang.travelassistant.repository.UsageLogRepository;
import com.lixinyang.travelassistant.entity.UsageLog;
import com.lixinyang.travelassistant.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Service @RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepo;
    private final UserRepository userRepo;
    private final UsageLogRepository logRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String login(String username, String password) {
        return adminRepo.findByUsername(username)
                .filter(a -> encoder.matches(password, a.getPassword()))
                .map(a -> jwtUtil.generate(a.getId(), a.getUsername(), "ADMIN"))
                .orElse(null);
    }

    public Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", userRepo.count());
        m.put("totalLogins", logRepo.countByAction("LOGIN"));
        m.put("totalRecommends", logRepo.countByAction("RECOMMEND"));
        m.put("totalChats", logRepo.countByAction("CHAT"));

        m.put("todayLogins", logRepo.countByActionAndCreateTimeAfter("LOGIN", todayStart));
        m.put("todayRecommends", logRepo.countByActionAndCreateTimeAfter("RECOMMEND", todayStart));
        m.put("todayChats", logRepo.countByActionAndCreateTimeAfter("CHAT", todayStart));

        m.put("yesterdayLogins", logRepo.countByActionAndCreateTimeBetween("LOGIN", yesterdayStart, todayStart));
        m.put("yesterdayRecommends", logRepo.countByActionAndCreateTimeBetween("RECOMMEND", yesterdayStart, todayStart));
        m.put("yesterdayChats", logRepo.countByActionAndCreateTimeBetween("CHAT", yesterdayStart, todayStart));

        // 近 7 天按天聚合
        List<UsageLog> weekLogs = logRepo.findByCreateTimeAfter(weekStart);
        List<Map<String, Object>> last7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long lg = weekLogs.stream().filter(l -> "LOGIN".equals(l.getAction())
                    && l.getCreateTime() != null && d.equals(l.getCreateTime().toLocalDate())).count();
            long rc = weekLogs.stream().filter(l -> "RECOMMEND".equals(l.getAction())
                    && l.getCreateTime() != null && d.equals(l.getCreateTime().toLocalDate())).count();
            long ch = weekLogs.stream().filter(l -> "CHAT".equals(l.getAction())
                    && l.getCreateTime() != null && d.equals(l.getCreateTime().toLocalDate())).count();
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("logins", lg);
            day.put("recommends", rc);
            day.put("chats", ch);
            last7Days.add(day);
        }
        m.put("last7Days", last7Days);

        m.put("recentLogs", logRepo.findTop50ByOrderByCreateTimeDesc().stream()
                .map(l -> Map.of(
                        "time", l.getCreateTime() == null ? "" : l.getCreateTime().toString(),
                        "user", l.getUsername() == null ? "anonymous" : l.getUsername(),
                        "action", l.getAction() == null ? "" : l.getAction()))
                .toList());
        return m;
    }
}
