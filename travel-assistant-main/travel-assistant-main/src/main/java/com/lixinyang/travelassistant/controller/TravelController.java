package com.lixinyang.travelassistant.controller;

import com.lixinyang.travelassistant.dto.ChatRequest;
import com.lixinyang.travelassistant.dto.SearchRequest;
import com.lixinyang.travelassistant.dto.TravelRequest;
import com.lixinyang.travelassistant.service.TravelService;
import com.lixinyang.travelassistant.service.UsageLogService;
import com.lixinyang.travelassistant.service.StockService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import com.lixinyang.travelassistant.repository.PlaceRepository;
import com.lixinyang.travelassistant.vo.PlaceItem;
import com.lixinyang.travelassistant.vo.TravelRecommend;
import com.lixinyang.travelassistant.vo.TravelSearch;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;

@RestController
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelController {
    private final TravelService travelService;
    private final UsageLogService usageLogService;
    private final JwtUtil jwtUtil;
    private final StockService stockService;
    private final PlaceRepository placeRepository;

    /** 旅游页的“推荐城市”：常见旅游城市 + 推荐库里出现过的城市，去重后返回 */
    private static final List<String> DEFAULT_CITIES = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "西安", "重庆", "南京", "苏州",
            "厦门", "三亚", "青岛", "大连", "昆明", "丽江", "大理", "桂林", "张家界", "黄山",
            "武汉", "长沙", "天津", "哈尔滨", "沈阳", "郑州", "洛阳", "拉萨", "西宁", "兰州",
            "海口", "南宁", "福州", "贵阳", "乌鲁木齐", "呼和浩特", "银川", "南昌", "太原", "石家庄");

    @GetMapping("/cities")
    public Result<List<String>> cities() {
        LinkedHashSet<String> set = new LinkedHashSet<>(DEFAULT_CITIES);
        try {
            set.addAll(placeRepository.findDistinctCities());
        } catch (Exception e) {
            System.out.println("[城市列表] 读推荐库失败，只返回默认城市: " + e.getMessage());
        }
        return Result.ok(new ArrayList<>(set));
    }

    /** 某天各房型/票种的剩余库存（前端显示“仅剩 N 间”、售完置灰） */
    @GetMapping("/stock")
    public Result<List<Map<String, Object>>> stock(@RequestParam Long placeId,
                                                   @RequestParam String date) {
        return Result.ok(stockService.remaining(placeId, date));
    }

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok("hello world");
    }

    @PostMapping("/recommend")
    public Result<TravelRecommend> recommend(@Valid @RequestBody TravelRequest r,
                                             @RequestHeader(value = "Authorization", required = false) String auth) {
        TravelRecommend tr = travelService.recommend(r.getCity(), r.getDays(), r.getBudget());
        usageLogService.record(extractUsername(auth), "RECOMMEND");
        return Result.ok(tr);
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public SseEmitter chat(@Valid @RequestBody ChatRequest c,
                           @RequestHeader(value = "Authorization", required = false) String auth) {
        usageLogService.record(extractUsername(auth), "CHAT");
        return travelService.chat(c.getMessage());
    }

    // 搜索：具体地点 / 景区 / 附近酒店 / 附近景区
    @PostMapping("/search")
    public Result<TravelSearch> search(@Valid @RequestBody SearchRequest req,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        TravelSearch result = travelService.search(req.getKeyword());
        usageLogService.record(extractUsername(auth), "SEARCH");
        return Result.ok(result);
    }

    // 酒店：真实数据（高德），支持按城市/关键词搜索；machineId 用于按设备推荐城市
    @GetMapping("/hotels")
    public Result<List<PlaceItem>> hotels(@RequestParam(required = false) String city,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String machineId,
                                          @RequestHeader(value = "Authorization", required = false) String auth) {
        List<PlaceItem> list = travelService.hotels(city, keyword, machineId);
        usageLogService.record(extractUsername(auth), "HOTEL");
        return Result.ok(list);
    }

    // 景点/景区：真实数据（高德）
    @GetMapping("/spots")
    public Result<List<PlaceItem>> spots(@RequestParam(required = false) String city,
                                         @RequestParam(required = false) String keyword,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        List<PlaceItem> list = travelService.spots(city, keyword);
        usageLogService.record(extractUsername(auth), "SPOT");
        return Result.ok(list);
    }

    // 按关键词取一张图片（行程景点 / 城市卡片配图用）
    @GetMapping("/image")
    public Result<String> image(@RequestParam String keyword,
                                @RequestParam(required = false) String city) {
        return Result.ok(travelService.imageOf(keyword, city));
    }

    // AI 生成简介（景点 / 酒店）
    @GetMapping("/intro")
    public Result<String> intro(@RequestParam String name,
                                @RequestParam(required = false) String city) {
        return Result.ok(travelService.intro(name, city));
    }

    // 多张图片（酒店/景点相册）
    @GetMapping("/photos")
    public Result<List<String>> photos(@RequestParam String name,
                                       @RequestParam(required = false) String city) {
        return Result.ok(travelService.photosOf(name, city));
    }

    private String extractUsername(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return "anonymous";
        try { return jwtUtil.parseUsername(auth.substring(7)); } catch (Exception e) { return "anonymous"; }
    }
}
