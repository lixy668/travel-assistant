package com.lixinyang.travelassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixinyang.travelassistant.vo.PlaceItem;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 高德地图 Web 服务封装：地理编码 / POI 关键字搜索 / 周边搜索
 * 需要在环境变量里配置 AMAP_KEY（application.properties: amap.key=${AMAP_KEY:}）
 */
@Service
public class AmapService {
    @Value("${amap.key:}") private String amapKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean hasKey() {
        return amapKey != null && !amapKey.isBlank();
    }

    /** 地理编码：地址/城市 -> "lng,lat" */
    public String geocode(String address) {
        if (!hasKey() || address == null || address.isBlank()) return null;
        try {
            HttpUrl url = HttpUrl.parse("https://restapi.amap.com/v3/geocode/geo").newBuilder()
                    .addQueryParameter("key", amapKey)
                    .addQueryParameter("address", address)
                    .build();
            JsonNode root = get(url);
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isArray() && geocodes.size() > 0) {
                return geocodes.get(0).path("location").asText(null);
            }
        } catch (Exception e) {
            System.out.println("高德地理编码失败: " + e.getMessage());
        }
        return null;
    }

    /** 关键字搜索 POI（全国或指定城市） */
    public List<PlaceItem> searchPoi(String keywords, String city) {
        if (!hasKey()) return new ArrayList<>();
        try {
            HttpUrl.Builder b = HttpUrl.parse("https://restapi.amap.com/v3/place/text").newBuilder()
                    .addQueryParameter("key", amapKey)
                    .addQueryParameter("keywords", blankTo(keywords, "酒店"))
                    .addQueryParameter("offset", "20")
                    .addQueryParameter("page", "1")
                    .addQueryParameter("extensions", "all");
            if (city != null && !city.isBlank()) b.addQueryParameter("city", city);
            return parsePois(get(b.build()));
        } catch (Exception e) {
            System.out.println("高德POI搜索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 周边搜索（结果带距离） */
    public List<PlaceItem> around(String location, String keywords, int radius) {
        if (!hasKey() || location == null || location.isBlank()) return new ArrayList<>();
        try {
            HttpUrl url = HttpUrl.parse("https://restapi.amap.com/v3/place/around").newBuilder()
                    .addQueryParameter("key", amapKey)
                    .addQueryParameter("location", location)
                    .addQueryParameter("keywords", blankTo(keywords, "酒店"))
                    .addQueryParameter("radius", String.valueOf(radius))
                    .addQueryParameter("offset", "20")
                    .addQueryParameter("page", "1")
                    .addQueryParameter("extensions", "all")
                    .build();
            return parsePois(get(url));
        } catch (Exception e) {
            System.out.println("高德周边搜索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 取某个 POI 的多张图片（最多 6 张） */
    public List<String> photos(String keywords, String city) {
        List<String> list = new ArrayList<>();
        if (!hasKey() || keywords == null || keywords.isBlank()) return list;
        try {
            HttpUrl.Builder b = HttpUrl.parse("https://restapi.amap.com/v3/place/text").newBuilder()
                    .addQueryParameter("key", amapKey)
                    .addQueryParameter("keywords", keywords)
                    .addQueryParameter("offset", "1")
                    .addQueryParameter("page", "1")
                    .addQueryParameter("extensions", "all");
            if (city != null && !city.isBlank()) b.addQueryParameter("city", city);
            JsonNode root = get(b.build());
            JsonNode pois = root.path("pois");
            if (pois.isArray() && pois.size() > 0) {
                list.addAll(pickPhotos(pois.get(0).path("photos"), keywords, 6));
                // 这个 POI 一张图都没有时，用「名字 + 风景」再搜一次，尽量别留空图
                if (list.isEmpty()) {
                    HttpUrl.Builder b2 = HttpUrl.parse("https://restapi.amap.com/v3/place/text").newBuilder()
                            .addQueryParameter("key", amapKey)
                            .addQueryParameter("keywords", keywords + " 风景")
                            .addQueryParameter("offset", "3")
                            .addQueryParameter("page", "1")
                            .addQueryParameter("extensions", "all");
                    if (city != null && !city.isBlank()) b2.addQueryParameter("city", city);
                    JsonNode pois2 = get(b2.build()).path("pois");
                    if (pois2.isArray()) {
                        for (JsonNode p2 : pois2) {
                            List<String> more = pickPhotos(p2.path("photos"), keywords, 6);
                            if (!more.isEmpty()) {
                                list.addAll(more);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("取多图失败: " + e.getMessage());
        }
        return list;
    }

    private String blankTo(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    // ================== 配图筛选：优先风景图，压掉“人占主体”的游客照 ==================

    /** 个人照关键字：标题命中就大幅降权 */
    private static final String[] PERSONAL_WORDS = {
            "合影", "留念", "游客", "自拍", "全家", "亲子", "宝贝", "宝宝", "儿童",
            "学生", "朋友", "生日", "我们", "打卡", "毕业", "参观" };

    /** 风景关键字：标题命中就加权 */
    private static final String[] SCENIC_WORDS = {
            "风景", "全景", "外景", "景区", "公园", "博物馆", "纪念馆", "广场", "大门", "入口",
            "湖", "山", "塔", "桥", "寺", "园", "夜景", "日出", "日落", "古镇", "街区", "花", "海", "江", "河" };

    /**
     * 从 POI 的图库里挑一张当封面/卡片图。
     * 打分规则：标题命中景点名 +4、命中风景词 +2、命中个人照词 -6、越靠前越优先。
     * 如果挑出来最好的一张明显是"人像游客照"（分数 ≤ -3），宁可返回空串，
     * 让前端显示带景点名的渐变占位图，也不放一张人占满屏的照片。
     */
    private String pickBestPhoto(JsonNode photos, String poiName) {
        if (photos == null || !photos.isArray() || photos.size() == 0) return "";
        String bestUrl = "";
        int bestScore = Integer.MIN_VALUE;
        int size = Math.min(photos.size(), 20);
        for (int i = 0; i < size; i++) {
            String url = photos.get(i).path("url").asText("");
            if (url.isBlank()) continue;
            String title = photos.get(i).path("title").asText("");
            int score = scoreOf(title, poiName, i);
            if (score > bestScore) {
                bestScore = score;
                bestUrl = url;
            }
        }
        return bestScore <= -3 ? "" : bestUrl;
    }

    /** 相册：按上面的分数排序后取前 limit 张，排序让风景图排在前面 */
    private List<String> pickPhotos(JsonNode photos, String poiName, int limit) {
        List<String> urls = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        if (photos == null || !photos.isArray() || photos.size() == 0) return urls;
        int size = Math.min(photos.size(), 20);
        for (int i = 0; i < size; i++) {
            String url = photos.get(i).path("url").asText("");
            if (url.isBlank()) continue;
            urls.add(url);
            scores.add(scoreOf(photos.get(i).path("title").asText(""), poiName, i));
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < urls.size() && result.size() < limit; i++) {
            int best = i;
            for (int j = i + 1; j < urls.size(); j++) {
                if (scores.get(j) > scores.get(best)) best = j;
            }
            String u = urls.get(best);
            urls.set(best, urls.get(i));
            urls.set(i, u);
            int s = scores.get(best);
            scores.set(best, scores.get(i));
            scores.set(i, s);
            result.add(urls.get(i));
        }
        return result;
    }

    private int scoreOf(String title, String poiName, int index) {
        int score = -index; // 同等条件下，高德给的顺序越靠前越优先
        if (poiName != null && !poiName.isBlank() && title.contains(poiName)) score += 4;
        for (String w : SCENIC_WORDS) {
            if (title.contains(w)) {
                score += 2;
                break;
            }
        }
        for (String w : PERSONAL_WORDS) {
            if (title.contains(w)) {
                score -= 6;
                break;
            }
        }
        return score;
    }

    private JsonNode get(HttpUrl url) throws Exception {
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = client.newCall(req).execute()) {
            if (resp.body() == null) throw new RuntimeException("empty body");
            return mapper.readTree(resp.body().string());
        }
    }

    private List<PlaceItem> parsePois(JsonNode root) {
        List<PlaceItem> list = new ArrayList<>();
        JsonNode pois = root.path("pois");
        if (pois.isArray()) {
            for (JsonNode p : pois) {
                PlaceItem it = new PlaceItem();
                it.setName(p.path("name").asText(""));
                it.setAddress(p.path("address").asText(""));
                it.setTel(p.path("tel").asText(""));
                it.setType(p.path("type").asText(""));
                it.setLocation(p.path("location").asText(""));
                it.setCity(p.path("cityname").asText(""));
                String dist = p.path("distance").asText("");
                if (!dist.isEmpty()) {
                    try {
                        int d = Integer.parseInt(dist);
                        it.setDistance(d >= 1000 ? String.format("%.1fkm", d / 1000.0) : d + "m");
                    } catch (NumberFormatException ignore) {
                        it.setDistance(dist);
                    }
                } else {
                    it.setDistance("");
                }
                it.setRating(p.path("biz_ext").path("rating").asText(""));
                it.setCost(p.path("biz_ext").path("cost").asText(""));
                it.setImage(pickBestPhoto(p.path("photos"), it.getName()));
                list.add(it);
            }
        }
        return list;
    }
}
