package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByPlaceIdAndBizDateAndRoomType(Long placeId, String bizDate, String roomType);

    List<Stock> findByPlaceIdAndBizDate(Long placeId, String bizDate);

    List<Stock> findByPlaceIdOrderByBizDateAscRoomTypeAsc(Long placeId);

    boolean existsByPlaceIdAndBizDateAndRoomType(Long placeId, String bizDate, String roomType);

    /**
     * 原子扣减库存：只有「剩余 ≥ 需要」时才会更新成功（受影响行数 = 1）。
     * 靠数据库行锁保证并发安全，不会出现两个请求同时扣到最后一间的情况。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update t_stock set sold = sold + :n, update_time = now() "
            + "where place_id = :placeId and biz_date = :bizDate and room_type = :roomType "
            + "and total - sold >= :n", nativeQuery = true)
    int tryLock(@Param("placeId") Long placeId,
                @Param("bizDate") String bizDate,
                @Param("roomType") String roomType,
                @Param("n") int n);

    /** 回补库存（超时关单、退订、改期、删除订单时调用），sold 不会减成负数 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update t_stock set sold = sold - :n, update_time = now() "
            + "where place_id = :placeId and biz_date = :bizDate and room_type = :roomType "
            + "and sold >= :n", nativeQuery = true)
    int release(@Param("placeId") Long placeId,
                @Param("bizDate") String bizDate,
                @Param("roomType") String roomType,
                @Param("n") int n);
}
