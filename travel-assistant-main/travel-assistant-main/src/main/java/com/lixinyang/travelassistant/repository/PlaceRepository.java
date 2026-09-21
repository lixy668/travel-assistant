package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByTypeAndStatusOrderBySortDescIdDesc(String type, Integer status);
    List<Place> findByTypeAndCityAndStatusOrderBySortDescIdDesc(String type, String city, Integer status);
    List<Place> findByTypeOrderBySortDescIdDesc(String type);
    List<Place> findAllByOrderBySortDescIdDesc();

    /** 推荐库里出现过的所有城市（去重，用于旅游页的“推荐城市”） */
    @Query("select distinct p.city from Place p where p.city is not null and p.city <> '' order by p.city")
    List<String> findDistinctCities();
}
