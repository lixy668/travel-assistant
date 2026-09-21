package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findTop50ByUsernameOrderByCreateTimeDesc(String username);

    long countByUsernameAndReadFlag(String username, Boolean readFlag);

    @Modifying
    @Query("update Notice n set n.readFlag = true where n.username = ?1 and n.readFlag = false")
    int markAllRead(String username);
}
