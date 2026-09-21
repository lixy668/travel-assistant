package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUsernameOrderByCreateTimeDesc(String username);

    Optional<Favorite> findByUsernameAndTypeAndName(String username, String type, String name);

    long countByUsername(String username);
}
