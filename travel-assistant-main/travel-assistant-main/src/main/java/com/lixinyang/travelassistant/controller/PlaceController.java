package com.lixinyang.travelassistant.controller;

import com.lixinyang.travelassistant.entity.Place;
import com.lixinyang.travelassistant.service.PlaceService;
import com.lixinyang.travelassistant.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceService placeService;

    // ===== 用户端：推荐（读库） =====
    @GetMapping("/travel/places")
    public Result<List<Place>> userList(@RequestParam String type,
                                        @RequestParam(required = false) String city) {
        return Result.ok(placeService.userList(type, city));
    }

    // ===== 管理端：增删改查 =====
    @GetMapping("/admin/places")
    public Result<List<Place>> adminList(@RequestParam(required = false) String type,
                                         @RequestParam(required = false) String city,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status) {
        return Result.ok(placeService.adminList(type, city, keyword, status));
    }

    @PostMapping("/admin/places")
    public Result<Place> create(@RequestBody Place p) {
        return Result.ok(placeService.save(p));
    }

    @PutMapping("/admin/places/{id}")
    public Result<Place> update(@PathVariable Long id, @RequestBody Place p) {
        Place r = placeService.update(id, p);
        return r == null ? Result.fail(404, "记录不存在") : Result.ok(r);
    }

    @DeleteMapping("/admin/places/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return placeService.delete(id) ? Result.ok("已删除") : Result.fail(404, "记录不存在");
    }

    @PostMapping("/admin/places/{id}/toggle")
    public Result<Place> toggle(@PathVariable Long id) {
        Place r = placeService.toggle(id);
        return r == null ? Result.fail(404, "记录不存在") : Result.ok(r);
    }
}
