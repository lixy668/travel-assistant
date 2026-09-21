package com.lixinyang.travelassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * 启动上下文测试：需要一个真实可连的 MySQL、Redis，以及 JWT_SECRET / MYSQL_PASSWORD 等环境变量。
 * 本机（IDEA / 命令行）配了环境变量就会跑；没配的环境（比如 CI、容器里只跑单测）自动跳过，
 * 这样 mvn test 在哪儿都能绿。要做纯单测请看 JwtUtilTest / OrderServiceTest / RateLimiterServiceTest。
 */
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
@SpringBootTest
class TravelassistantApplicationTests {

    @Test
    void contextLoads() {
    }

}
