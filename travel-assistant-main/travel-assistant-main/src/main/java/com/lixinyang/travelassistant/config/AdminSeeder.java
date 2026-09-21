package com.lixinyang.travelassistant.config;
import com.lixinyang.travelassistant.entity.Admin;
import com.lixinyang.travelassistant.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private final AdminRepository adminRepo;
    @Override public void run(String... args) {
        if (!adminRepo.existsByUsername("admin")) {
            Admin a = new Admin();
            a.setUsername("admin");
            a.setPassword(new BCryptPasswordEncoder().encode("admin123"));
            a.setRole("ADMIN");
            adminRepo.save(a);
            System.out.println(">>> 已初始化默认管理员：admin / admin123");
        }
    }
}