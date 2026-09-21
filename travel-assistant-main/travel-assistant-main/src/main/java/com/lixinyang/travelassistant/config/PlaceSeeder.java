package com.lixinyang.travelassistant.config;

import com.lixinyang.travelassistant.entity.Place;
import com.lixinyang.travelassistant.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceSeeder implements CommandLineRunner {
    private final PlaceRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        // 酒店
        seed("HOTEL", "上海外滩豫园美居酒店", "上海", "黄浦区人民路", 1471.0, "4.8", "近外滩,商务", "位于外滩商圈，步行可达豫园与外滩。", 10);
        seed("HOTEL", "上海静安漫心酒店", "上海", "静安区南京西路", 965.0, "4.8", "近地铁,购物", "静安寺商圈，交通便利，适合购物出行。", 9);
        seed("HOTEL", "上海张江CitiGO欢阁酒店", "上海", "浦东新区张江", 660.0, "4.9", "科技园,性价比", "张江科技园区，适合商务差旅。", 8);
        seed("HOTEL", "北京王府井希尔顿酒店", "北京", "东城区王府井", 1180.0, "4.7", "近故宫,商务", "紧邻王府井步行街，去故宫很方便。", 10);
        seed("HOTEL", "成都春熙路亚朵酒店", "成都", "锦江区春熙路", 520.0, "4.8", "近地铁,美食", "春熙路商圈，周边美食多。", 9);
        seed("HOTEL", "三亚亚龙湾海景酒店", "三亚", "亚龙湾国家旅游度假区", 880.0, "4.9", "海景,亲子", "一线海景，适合亲子度假。", 10);

        // 景点
        seed("SPOT", "上海外滩", "上海", "黄浦区中山东一路", 0.0, "4.8", "免费,夜景", "万国建筑博览群，夜景必去。", 10);
        seed("SPOT", "上海迪士尼度假区", "上海", "浦东新区川沙新镇", 475.0, "4.7", "亲子,主题乐园", "国内热门主题乐园，建议玩一整天。", 10);
        seed("SPOT", "北京故宫博物院", "北京", "东城区景山前街4号", 60.0, "4.9", "历史,必打卡", "世界文化遗产，建议提前预约门票。", 10);
        seed("SPOT", "成都大熊猫繁育研究基地", "成都", "成华区熊猫大道", 55.0, "4.8", "亲子,动物", "建议早上前往，熊猫更活跃。", 9);
        seed("SPOT", "杭州西湖", "杭州", "西湖区龙井路", 0.0, "4.9", "免费,自然风光", "断桥、苏堤、雷峰塔，环湖骑行很惬意。", 9);
        seed("SPOT", "西安秦始皇兵马俑", "西安", "临潼区秦陵北路", 120.0, "4.8", "历史,世界遗产", "世界第八大奇迹，建议请讲解。", 9);

        System.out.println(">>> 已初始化酒店/景点推荐数据（可在管理后台增删改）");
    }

    private void seed(String type, String name, String city, String address, Double price,
                      String rating, String tags, String desc, int sort) {
        Place p = new Place();
        p.setType(type);
        p.setName(name);
        p.setCity(city);
        p.setAddress(address);
        p.setPrice(price);
        p.setRating(rating);
        p.setTags(tags);
        p.setDescription(desc);
        p.setStatus(1);
        p.setSort(sort);
        repo.save(p);
    }
}
