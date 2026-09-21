package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.Stock;
import com.lixinyang.travelassistant.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存服务：占库存 / 回补库存 / 查剩余。
 * 规则：某个「地点 + 日期 + 房型」没配过库存记录 → 视为不限量（不影响老数据）；
 * 一旦配了库存，就必须按 total - sold 严格卡住，防止超卖。
 */
@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepo;

    /** 占库存：返回 false 表示库存不足 */
    @Transactional
    public boolean lock(Long placeId, String bizDate, String roomType, int n) {
        if (placeId == null || bizDate == null || roomType == null || roomType.isBlank()) return true;
        if (n <= 0) return true;
        if (!stockRepo.existsByPlaceIdAndBizDateAndRoomType(placeId, bizDate, roomType)) return true; // 没配库存 → 不限量
        int updated = stockRepo.tryLock(placeId, bizDate, roomType, n);
        if (updated > 0) return true;
        Stock s = stockRepo.findByPlaceIdAndBizDateAndRoomType(placeId, bizDate, roomType).orElse(null);
        int left = s == null ? 0 : Math.max(0, s.getTotal() - s.getSold());
        throw new BizException("该日期「" + roomType + "」只剩 " + left + " 间/张，库存不足");
    }

    /** 回补库存：出错只打日志，不影响主流程 */
    @Transactional
    public void release(Long placeId, String bizDate, String roomType, int n) {
        if (placeId == null || bizDate == null || roomType == null || roomType.isBlank() || n <= 0) return;
        try {
            stockRepo.release(placeId, bizDate, roomType, n);
        } catch (Exception e) {
            System.out.println("[库存] 回补失败: " + e.getMessage());
        }
    }

    /** 查某天各房型剩余量（前端显示“仅剩 N 间”） */
    public List<Map<String, Object>> remaining(Long placeId, String bizDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (placeId == null || bizDate == null) return list;
        for (Stock s : stockRepo.findByPlaceIdAndBizDate(placeId, bizDate)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roomType", s.getRoomType());
            m.put("total", s.getTotal());
            m.put("sold", s.getSold());
            m.put("left", Math.max(0, s.getTotal() - s.getSold()));
            m.put("price", s.getPrice());
            list.add(m);
        }
        return list;
    }

    /** 管理端维护库存：同一天同一房型有就改，没有就新增 */
    @Transactional
    public Stock save(Long placeId, String bizDate, String roomType, Integer total, Double price) {
        if (placeId == null || bizDate == null || bizDate.isBlank() || roomType == null || roomType.isBlank()) {
            throw new BizException("地点、日期、房型都不能为空");
        }
        Stock s = stockRepo.findByPlaceIdAndBizDateAndRoomType(placeId, bizDate, roomType).orElseGet(Stock::new);
        if (total != null) {
            if (total < 0) throw new BizException("库存不能为负数");
            if (s.getId() != null && s.getSold() != null && total < s.getSold()) {
                throw new BizException("总库存不能小于已售数量（已售 " + s.getSold() + "）");
            }
            s.setTotal(total);
        }
        if (price != null) s.setPrice(price);
        if (s.getId() == null) {
            s.setPlaceId(placeId);
            s.setBizDate(bizDate);
            s.setRoomType(roomType);
            if (s.getSold() == null) s.setSold(0);
        }
        return stockRepo.save(s);
    }

    public List<Stock> listByPlace(Long placeId) {
        return stockRepo.findByPlaceIdOrderByBizDateAscRoomTypeAsc(placeId);
    }
}
