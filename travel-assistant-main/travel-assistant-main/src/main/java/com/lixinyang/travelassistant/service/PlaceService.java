package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.entity.Place;
import com.lixinyang.travelassistant.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository repo;

    /** 用户端：只看上架的（可按城市） */
    public List<Place> userList(String type, String city) {
        if (city != null && !city.isBlank()) {
            return repo.findByTypeAndCityAndStatusOrderBySortDescIdDesc(type, city, 1);
        }
        return repo.findByTypeAndStatusOrderBySortDescIdDesc(type, 1);
    }

    /** 管理端：全部（可按城市/关键词/状态筛选） */
    public List<Place> adminList(String type, String city, String keyword, Integer status) {
        List<Place> all = (type == null || type.isBlank())
                ? repo.findAllByOrderBySortDescIdDesc()
                : repo.findByTypeOrderBySortDescIdDesc(type);
        return all.stream()
                .filter(p -> city == null || city.isBlank() || city.equals(p.getCity()))
                .filter(p -> status == null || status.equals(p.getStatus()))
                .filter(p -> keyword == null || keyword.isBlank()
                        || (p.getName() != null && p.getName().contains(keyword)))
                .toList();
    }

    public Place save(Place p) {
        p.setId(null);
        if (p.getStatus() == null) p.setStatus(1);
        if (p.getSort() == null) p.setSort(0);
        p.setCreateTime(LocalDateTime.now());
        return repo.save(p);
    }

    public Place update(Long id, Place patch) {
        Place p = repo.findById(id).orElse(null);
        if (p == null) return null;
        if (patch.getType() != null) p.setType(patch.getType());
        if (patch.getName() != null) p.setName(patch.getName());
        if (patch.getCity() != null) p.setCity(patch.getCity());
        if (patch.getAddress() != null) p.setAddress(patch.getAddress());
        if (patch.getPrice() != null) p.setPrice(patch.getPrice());
        if (patch.getRating() != null) p.setRating(patch.getRating());
        if (patch.getImage() != null) p.setImage(patch.getImage());
        if (patch.getTags() != null) p.setTags(patch.getTags());
        if (patch.getDescription() != null) p.setDescription(patch.getDescription());
        if (patch.getStatus() != null) p.setStatus(patch.getStatus());
        if (patch.getSort() != null) p.setSort(patch.getSort());
        return repo.save(p);
    }

    public boolean delete(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    /** 上架 / 下架 切换 */
    public Place toggle(Long id) {
        Place p = repo.findById(id).orElse(null);
        if (p == null) return null;
        p.setStatus(p.getStatus() != null && p.getStatus() == 1 ? 0 : 1);
        return repo.save(p);
    }
}
