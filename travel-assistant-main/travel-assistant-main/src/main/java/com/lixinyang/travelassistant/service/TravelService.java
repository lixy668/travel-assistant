package com.lixinyang.travelassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixinyang.travelassistant.utils.LlmUtils;
import com.lixinyang.travelassistant.vo.StreamChunk;
import com.lixinyang.travelassistant.vo.StreamDone;
import com.lixinyang.travelassistant.vo.Streamerror;
import com.lixinyang.travelassistant.vo.PlaceItem;
import com.lixinyang.travelassistant.vo.TravelRecommend;
import com.lixinyang.travelassistant.vo.TravelSearch;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Service
public class TravelService {
    @Value("${llm.api-key}") private String apikey;
    @Value("${llm.base-url}") private String baseurl;
    @Value("${llm.model}") private String model;

    @Autowired @Qualifier("chatExecutor") private Executor chatExecutor;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private AmapService amapService;

    private LlmUtils llmUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== 熔断器状态 =====
    private final Object breakerLock = new Object();
    private boolean circuitOpen = false;
    private long circuitOpenTime = 0;
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 3;     // 连续失败3次就熔断
    private static final long COOLDOWN_MS = 60_000;      // 熔断60秒后再试

    @PostConstruct
    public void init() {
        this.llmUtils = new LlmUtils(apikey, model, baseurl);
    }

    // ============ 行程推荐：缓存 + 重试 + 熔断 + 降级 ============
    public TravelRecommend recommend(String city, Integer days, double budget) {
        String cacheKey = "recommend:" + city + ":" + days + ":" + budget;
        TravelRecommend cached = getCache(cacheKey);
        if (cached != null) {
            System.out.println("命中缓存: " + cacheKey);
            return cached;
        }
        if (shouldFallback()) {
            return buildFallbackRecommend(city, days, budget);
        }
        try {
            String prompt = buildTravelPrompt(city, budget, days);
            String response = null;
            Exception last = null;
            for (int i = 0; i < 2; i++) { // 重试1次
                try {
                    response = llmUtils.chat(null, prompt);
                    break;
                } catch (Exception e) {
                    last = e;
                    System.out.println("LLM调用失败(第" + (i + 1) + "次): " + e.getMessage());
                    if (i == 0) { try { Thread.sleep(1000); } catch (InterruptedException ignored) {} }
                }
            }
            if (response == null) {
                throw (last != null) ? last : new RuntimeException("LLM调用失败");
            }
            TravelRecommend result = parseTravelResponse(response);
            if (result.getSuccess() == Boolean.FALSE) {
                throw new RuntimeException(result.getError() != null ? result.getError() : "解析失败");
            }
            onSuccess();
            if (result.getCity() == null) result.setCity(city);
            if (result.getDays() == null) result.setDays(String.valueOf(days));
            if (result.getTotalBudget() == 0.0) result.setTotalBudget(budget);
            putCache(cacheKey, result);
            return result;
        } catch (Exception e) {
            onFailure();
            System.out.println("推荐触发降级: " + e.getMessage());
            return buildFallbackRecommend(city, days, budget);
        }
    }

    // ============ 搜索：地点 / 景区 / 附近酒店 / 附近景区（大模型 + 缓存 + 降级）============
    public TravelSearch search(String keyword) {
        String cacheKey = "search:" + keyword;
        TravelSearch cached = getSearchCache(cacheKey);
        if (cached != null) {
            System.out.println("搜索命中缓存: " + cacheKey);
            return cached;
        }
        if (shouldFallback()) {
            return buildFallbackSearch(keyword);
        }
        try {
            String response = llmUtils.chat(null, buildSearchPrompt(keyword));
            TravelSearch result = parseSearchResponse(response, keyword);
            if (result.getSuccess() == Boolean.FALSE) {
                throw new RuntimeException(result.getError() != null ? result.getError() : "解析失败");
            }
            onSuccess();
            putSearchCache(cacheKey, result);
            return result;
        } catch (Exception e) {
            onFailure();
            System.out.println("搜索触发降级: " + e.getMessage());
            return buildFallbackSearch(keyword);
        }
    }

    private TravelSearch parseSearchResponse(String response, String keyword) {
        TravelSearch result = new TravelSearch();
        try {
            String json = extractJson(response);
            if (json != null) {
                result = objectMapper.readValue(json, TravelSearch.class);
                if (result.getKeyword() == null) result.setKeyword(keyword);
                if (result.getSuccess() == null) result.setSuccess(true);
            } else {
                result.setKeyword(keyword);
                result.setSuccess(false);
                result.setError("未能从响应中提取JSON");
            }
        } catch (Exception e) {
            result.setKeyword(keyword);
            result.setSuccess(false);
            result.setError("JSON解析失败: " + e.getMessage());
        }
        return result;
    }

    private TravelSearch buildFallbackSearch(String keyword) {
        TravelSearch r = new TravelSearch();
        r.setKeyword(keyword);
        r.setSuccess(true);
        r.setAttractions(new ArrayList<>());
        r.setHotels(new ArrayList<>());
        r.setNearby(new ArrayList<>());
        r.setError("AI 服务暂时不可用，请稍后再试");
        return r;
    }

    private String buildSearchPrompt(String keyword) {
        return "你是一个旅游信息检索助手。用户搜索关键词：「" + keyword + "」。\n" +
                "请返回与该关键词相关的旅游信息，必须用 JSON 输出，结构如下：\n" +
                "{\n" +
                "  \"keyword\": \"" + keyword + "\",\n" +
                "  \"attractions\": [ {\"name\":\"景区或景点名称\",\"city\":\"所在城市\",\"ticket\":\"门票价格\",\"rating\":\"评分如4.8\",\"address\":\"大致位置\",\"tags\":\"标签\",\"description\":\"一句话介绍\"} ],\n" +
                "  \"hotels\": [ {\"name\":\"酒店名称\",\"city\":\"所在城市\",\"price\":\"参考价如￥500起\",\"rating\":\"评分\",\"address\":\"位置\",\"tags\":\"如近地铁\",\"description\":\"一句话介绍\"} ],\n" +
                "  \"nearby\": [ {\"name\":\"附近景区或去处\",\"city\":\"城市\",\"distance\":\"距离如3km\",\"ticket\":\"门票\",\"tags\":\"标签\",\"description\":\"一句话介绍\"} ]\n" +
                "}\n" +
                "要求：attractions 给 5 个与关键词最相关的知名景点；hotels 给 5 个附近或该城市的热门酒店；nearby 给 5 个附近值得去的景区或去处。" +
                "请确保 JSON 格式正确、可以被解析。";
    }

    private TravelSearch getSearchCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) return objectMapper.readValue(json, TravelSearch.class);
        } catch (Exception e) {
            System.out.println("搜索缓存读取失败(忽略): " + e.getMessage());
        }
        return null;
    }

    private void putSearchCache(String key, TravelSearch result) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), Duration.ofHours(6));
        } catch (Exception e) {
            System.out.println("搜索缓存写入失败(忽略): " + e.getMessage());
        }
    }

    // ============ 酒店（高德真实数据；按 machineId 推荐城市）============
    private static final List<String> CITY_POOL = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "西安", "重庆",
            "南京", "武汉", "三亚", "大理", "厦门", "青岛", "长沙", "昆明");

    public List<PlaceItem> hotels(String city, String keyword, String machineId) {
        String c = (city == null || city.isBlank()) ? recommendCity(machineId) : city;
        String kw = (keyword == null || keyword.isBlank()) ? "酒店" : keyword;
        // 优先周边搜索（结果带距离）；失败再退回关键字搜索
        String center = amapService.geocode(c);
        List<PlaceItem> list = amapService.around(center, kw, 5000);
        if (list != null && !list.isEmpty()) return list;
        return amapService.searchPoi(kw, c);
    }

    public List<PlaceItem> spots(String city, String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? "景点" : keyword;
        if (city != null && !city.isBlank()) {
            String center = amapService.geocode(city);
            List<PlaceItem> list = amapService.around(center, kw, 10000);
            if (list != null && !list.isEmpty()) return list;
        }
        return amapService.searchPoi(kw, city);
    }

    /** 按设备 id 稳定地推荐一个城市（同一设备每次结果一致） */
    private String recommendCity(String machineId) {
        if (machineId == null || machineId.isBlank()) return "上海";
        int idx = Math.abs(machineId.hashCode()) % CITY_POOL.size();
        return CITY_POOL.get(idx);
    }

    /** 按关键词/城市取一张高德图片（用于行程景点、城市卡片配图），带 Redis 缓存 */
    public String imageOf(String keyword, String city) {
        if (keyword == null || keyword.isBlank()) return "";
        String key = "img:" + keyword + ":" + (city == null ? "" : city);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) return cached;
        } catch (Exception ignored) {}

        String image = "";
        try {
            List<PlaceItem> list = amapService.searchPoi(keyword, city);
            for (PlaceItem p : list) {
                if (p.getImage() != null && !p.getImage().isBlank()) {
                    image = p.getImage();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("取图片失败: " + e.getMessage());
        }
        try {
            redisTemplate.opsForValue().set(key, image, Duration.ofDays(7));
        } catch (Exception ignored) {}
        return image;
    }

    /** AI 生成一句简介（景点/酒店通用），带 Redis 缓存 */
    public String intro(String name, String city) {
        if (name == null || name.isBlank()) return "";
        String key = "intro:" + name + ":" + (city == null ? "" : city);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) return cached;
        } catch (Exception ignored) {}

        String text = "";
        try {
            String prompt = "请用不超过120字的中文，简要介绍「" + name + "」"
                    + ((city == null || city.isBlank()) ? "" : "（位于" + city + "）")
                    + " 的特点与亮点或游玩建议。必须严格以 JSON 输出：{\"intro\":\"介绍文字\"}";
            String resp = llmUtils.chat(null, prompt);
            String json = extractJson(resp);
            if (json != null) {
                text = objectMapper.readTree(json).path("intro").asText("");
            }
        } catch (Exception e) {
            System.out.println("生成简介失败: " + e.getMessage());
        }
        try {
            redisTemplate.opsForValue().set(key, text, Duration.ofDays(30));
        } catch (Exception ignored) {}
        return text;
    }

    /** 取某个酒店/景点的多张图片，带 Redis 缓存 */
    public List<String> photosOf(String name, String city) {
        if (name == null || name.isBlank()) return new ArrayList<>();
        String key = "photos:" + name + ":" + (city == null ? "" : city);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (Exception ignored) {}

        List<String> list = amapService.photos(name, city);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(list), Duration.ofDays(7));
        } catch (Exception ignored) {}
        return list;
    }

    // ============ AI 对话：熔断 + 降级 ============
    public SseEmitter chat(String message) {
        SseEmitter emitter = new SseEmitter(180000L);
        chatExecutor.execute(() -> {
            if (shouldFallback()) {
                // 直接给降级回答，不再调 DeepSeek
                sendFallbackChat(emitter, message, true);
                return;
            }
            try {
                String systemPrompt = "你是一个友好的旅游助手,请用中文回答用户关于旅游的问题";
                Consumer<String> callback = content -> {
                    try {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamChunk.of(content))));
                    } catch (Exception e) {
                        System.out.println("发送消息失败" + e);
                    }
                };
                try {
                    llmUtils.chatStream(systemPrompt, message, callback);
                    onSuccess();
                } catch (IOException e) {
                    onFailure();
                    // 主服务坏了，改用降级回答
                    sendFallbackChat(emitter, message, false);
                    return;
                }
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamDone.of())));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Streamerror.of(e.getMessage()))));
                } catch (Exception e1) {
                    System.out.println("发送消息失败:" + e1);
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private void sendFallbackChat(SseEmitter emitter, String message, boolean dueToCircuit) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamChunk.of(fallbackChatReply(message)))));
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamDone.of())));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    // ============ 熔断器 ============
    private boolean shouldFallback() {
        synchronized (breakerLock) {
            if (!circuitOpen) return false;
            if (System.currentTimeMillis() - circuitOpenTime >= COOLDOWN_MS) {
                circuitOpen = false; // 冷却结束，半开：允许试一次
                return false;
            }
            return true;
        }
    }
    private void onSuccess() {
        synchronized (breakerLock) {
            circuitOpen = false;
            consecutiveFailures = 0;
        }
    }
    private void onFailure() {
        synchronized (breakerLock) {
            consecutiveFailures++;
            if (consecutiveFailures >= FAILURE_THRESHOLD) {
                circuitOpen = true;
                circuitOpenTime = System.currentTimeMillis();
                System.out.println("熔断开启，60秒内直接走降级");
            }
        }
    }

    // ============ 降级：本地生成兜底行程 ============
    private TravelRecommend buildFallbackRecommend(String city, Integer days, double budget) {
        int n = (days == null || days <= 0) ? 1 : days;
        TravelRecommend r = new TravelRecommend();
        r.setSuccess(true);
        r.setDegraded(true);
        r.setCity(city);
        r.setDays(String.valueOf(n));
        r.setTotalBudget(budget);

        TravelRecommend.BudgetBreakdown bd = new TravelRecommend.BudgetBreakdown();
        bd.setAccommodation(budget * 0.3);
        bd.setFood(budget * 0.25);
        bd.setTransportation(budget * 0.2);
        bd.setTickets(budget * 0.15);
        bd.setOther(budget * 0.1);
        r.setBudgetBreakdown(bd);

        List<TravelRecommend.DailyItinerary> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            TravelRecommend.DailyItinerary day = new TravelRecommend.DailyItinerary();
            day.setDay(i);
            day.setDate("第" + i + "天");
            day.setMorning(slot(city + "核心景区/博物馆", "3小时", "免费-80元", "地铁/公交", "城市代表性景点或博物馆，适合上午游览。"));
            day.setAfternoon(slot("老城区/特色街区", "3小时", "免费", "步行/公交", "逛当地特色街区，体验城市风貌。"));
            day.setEvening(slot("本地美食街/夜市", "2小时", "人均50-100元", "步行/打车", "品尝当地特色美食，感受夜晚氛围。"));
            list.add(day);
        }
        r.setDailyItinerary(list);
        r.setTips(List.of("提前查看天气", "景区门票可提前线上购买", "注意本地交通高峰时段"));
        r.setWarnings(List.of("当前为离线降级内容，AI 恢复后可获取更详细规划"));
        r.setRawResponse("");
        return r;
    }

    private TravelRecommend.Timeslot slot(String spot, String duration, String ticket, String transportation, String description) {
        TravelRecommend.Timeslot t = new TravelRecommend.Timeslot();
        t.setSpot(spot); t.setDuration(duration); t.setTicket(ticket); t.setTransportation(transportation); t.setDescription(description);
        return t;
    }

    private String fallbackChatReply(String message) {
        String m = message == null ? "" : message;
        if (m.contains("景点") || m.contains("玩")) {
            return "抱歉，AI 服务暂时不可用（已自动切换到离线模式）。你可以先试试经典的景区、博物馆和特色街区，多数上午逛比较舒服。等 AI 恢复后我能为你生成更详细的行程。";
        }
        if (m.contains("吃") || m.contains("美食")) {
            return "抱歉，AI 服务暂时不可用。离线建议：先去市中心或老城区，通常能尝到本地特色小吃和正餐。";
        }
        return "抱歉，我暂时无法连接 AI 服务（已切换离线模式）。你可以稍后再试，或先去首页体验推荐行程。";
    }

    // ============ 解析 / Prompt ============
    private TravelRecommend parseTravelResponse(String response) {
        TravelRecommend result = new TravelRecommend();
        try {
            String jsonContent = extractJson(response);
            if (jsonContent != null) {
                result = objectMapper.readValue(jsonContent, TravelRecommend.class);
                if (result.getSuccess() == null) result.setSuccess(true);
            } else {
                result.setSuccess(false);
                result.setError("未能从响应中提取JSON");
                result.setRawResponse(response);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("JSON解析失败: " + e.getMessage());
            result.setRawResponse(response);
        }
        return result;
    }

    private String extractJson(String response) {
        if (response == null || response.isEmpty()) return null;
        String[] patterns = {"```json\\n([\\s\\S]*?)\\n```", "```\\n([\\s\\S]*?)\\n```"};
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(response);
            if (m.find()) return m.group(1);
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) return response.substring(start, end + 1);
        return null;
    }

    private String buildTravelPrompt(String city, double budget, Integer days) {
        return "你是一个专业的旅游规划师，擅长根据用户的需求生成详细的旅行行程。\n\n" +
                "请根据以下信息为用户生成一份详细的旅游规划：\n" +
                "- 目的地城市： " + city + "\n" + "- 预算： " + budget + "元\n" + "- 旅行天数： " + days + "天\n\n" +
                "要求：\n1. 每天的行程安排（上午、下午、晚上）\n2. 每个景点的详细介绍\n3. 交通建议\n4. 预算分配明细\n5. 注意事项\n\n" +
                "请以JSON格式输出，结构如下：\n" +
                "{\n  \"success\": true,\n  \"city\": \"城市名\",\n  \"days\": 天数,\n  \"totalBudget\": 总预算,\n" +
                "  \"dailyItinerary\": [\n    {\n      \"day\": 1,\n      \"date\": \"第1天\",\n" +
                "      \"morning\": {\"spot\": \"景点名称\",\"duration\": \"游览时长\",\"ticket\": \"门票价格\",\"transportation\": \"交通方式\",\"description\": \"景点介绍\"},\n" +
                "      \"afternoon\": {\"spot\": \"景点名称\",\"duration\": \"游览时长\",\"ticket\": \"门票价格\",\"transportation\": \"交通方式\",\"description\": \"景点介绍\"},\n" +
                "      \"evening\": {\"spot\": \"活动名称\",\"duration\": \"活动时长\",\"ticket\": \"费用\",\"transportation\": \"交通方式\",\"description\": \"活动介绍\"}\n    }\n  ],\n" +
                "  \"budgetBreakdown\": {\"accommodation\": 住宿,\"food\": 餐饮,\"transportation\": 交通,\"tickets\": 门票,\"other\": 其他},\n" +
                "  \"tips\": [\"提示1\", \"提示2\"],\n  \"warnings\": [\"注意事项1\"]\n}\n\n请确保JSON格式正确，可以被解析。";
    }

    // ============ Redis 缓存 ============
    private TravelRecommend getCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) return objectMapper.readValue(json, TravelRecommend.class);
        } catch (Exception e) {
            System.out.println("缓存读取失败(忽略): " + e.getMessage());
        }
        return null;
    }
    private void putCache(String key, TravelRecommend result) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), Duration.ofHours(12));
        } catch (Exception e) {
            System.out.println("缓存写入失败(忽略): " + e.getMessage());
        }
    }
}
