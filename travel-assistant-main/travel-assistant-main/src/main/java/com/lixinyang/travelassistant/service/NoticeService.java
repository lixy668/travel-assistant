package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.Notice;
import com.lixinyang.travelassistant.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 站内消息：订单、支付、审批、退款都会给用户推一条 */
@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository repo;

    /** 发消息：发失败只打日志，绝不连累主业务 */
    public void push(String username, String type, String title, String content) {
        try {
            if (username == null || username.isBlank()) return;
            Notice n = new Notice();
            n.setUsername(username);
            n.setType(type);
            n.setTitle(title);
            String c = content == null ? "" : content;
            n.setContent(c.length() > 580 ? c.substring(0, 580) + "…" : c);
            repo.save(n);
        } catch (Exception e) {
            System.out.println("[站内信] 发送失败: " + e.getMessage());
        }
    }

    public List<Notice> my(String username) {
        return repo.findTop50ByUsernameOrderByCreateTimeDesc(username);
    }

    public long unread(String username) {
        return repo.countByUsernameAndReadFlag(username, false);
    }

    @Transactional
    public void read(Long id, String username) {
        Notice n = repo.findById(id).orElseThrow(() -> new BizException(404, "消息不存在"));
        if (!username.equals(n.getUsername())) throw new BizException(403, "无权操作他人的消息");
        n.setReadFlag(true);
        repo.save(n);
    }

    @Transactional
    public void readAll(String username) {
        repo.markAllRead(username);
    }
}
