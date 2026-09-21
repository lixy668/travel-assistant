package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.Stock;
import com.lixinyang.travelassistant.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存服务单元测试：库存不足要拦住、没配库存要不限量、回补要调到原子 SQL、总库存不能小于已售。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockServiceTest {

    @Mock
    private StockRepository stockRepo;
    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("没配库存的日期：视为不限量，直接放行")
    void unlimitedWhenNoStockConfigured() {
        when(stockRepo.existsByPlaceIdAndBizDateAndRoomType(1L, "2026-10-01", "标准大床房")).thenReturn(false);
        assertTrue(stockService.lock(1L, "2026-10-01", "标准大床房", 2));
        verify(stockRepo, never()).tryLock(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("库存充足：原子扣减成功")
    void lockSuccess() {
        when(stockRepo.existsByPlaceIdAndBizDateAndRoomType(anyLong(), anyString(), anyString())).thenReturn(true);
        when(stockRepo.tryLock(1L, "2026-10-01", "标准大床房", 1)).thenReturn(1);
        assertTrue(stockService.lock(1L, "2026-10-01", "标准大床房", 1));
    }

    @Test
    @DisplayName("库存不足：抛业务异常，提示还剩几间（防超卖的核心）")
    void lockFailWhenSoldOut() {
        when(stockRepo.existsByPlaceIdAndBizDateAndRoomType(anyLong(), anyString(), anyString())).thenReturn(true);
        when(stockRepo.tryLock(anyLong(), anyString(), anyString(), anyInt())).thenReturn(0);
        Stock s = new Stock();
        s.setTotal(5);
        s.setSold(5);
        when(stockRepo.findByPlaceIdAndBizDateAndRoomType(anyLong(), anyString(), anyString()))
                .thenReturn(Optional.of(s));

        BizException e = assertThrows(BizException.class,
                () -> stockService.lock(1L, "2026-10-01", "标准大床房", 1));
        assertTrue(e.getMessage().contains("库存不足"));
    }

    @Test
    @DisplayName("回补库存：调用原子回补 SQL，且不会减成负数（SQL 里带 sold >= n 条件）")
    void releaseStock() {
        stockService.release(1L, "2026-10-01", "标准大床房", 1);
        verify(stockRepo).release(1L, "2026-10-01", "标准大床房", 1);
    }

    @Test
    @DisplayName("维护库存：总库存不能小于已售数量")
    void cannotSetTotalBelowSold() {
        Stock exist = new Stock();
        exist.setId(9L);
        exist.setTotal(10);
        exist.setSold(8);
        when(stockRepo.findByPlaceIdAndBizDateAndRoomType(anyLong(), anyString(), anyString()))
                .thenReturn(Optional.of(exist));

        assertThrows(BizException.class, () -> stockService.save(1L, "2026-10-01", "标准大床房", 3, null));
        assertEquals(10, exist.getTotal(), "校验失败时不应该改掉原库存");
    }

    @Test
    @DisplayName("维护库存：日期/房型必填")
    void saveValidatesParams() {
        assertThrows(BizException.class, () -> stockService.save(1L, "", "标准大床房", 10, null));
        assertThrows(BizException.class, () -> stockService.save(1L, "2026-10-01", "", 10, null));
    }
}
