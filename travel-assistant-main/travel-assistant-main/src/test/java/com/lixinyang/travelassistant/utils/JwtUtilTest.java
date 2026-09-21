package com.lixinyang.travelassistant.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/** JWT 工具类单元测试：签发 / 解析 / 校验 / 剩余有效期 */
class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "unit-test-secret-key-0123456789-abcdefghij");
        ReflectionTestUtils.setField(jwtUtil, "expire", 3_600_000L);
    }

    @Test
    @DisplayName("生成的 token 能解析出用户名和角色")
    void generateAndParse() {
        String token = jwtUtil.generate(100L, "lixinyang", "USER");
        assertNotNull(token);
        assertEquals("lixinyang", jwtUtil.parseUsername(token));
        assertEquals("USER", jwtUtil.parseRole(token));
        assertTrue(jwtUtil.validate(token));
    }

    @Test
    @DisplayName("篡改过的 token 校验失败")
    void tamperedTokenIsInvalid() {
        String token = jwtUtil.generate(1L, "user");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtUtil.validate(tampered));
        assertFalse(jwtUtil.validate("not-a-token"));
        assertFalse(jwtUtil.validate(null));
    }

    @Test
    @DisplayName("剩余有效期大于 0（登出黑名单靠它设置过期时间）")
    void remainingMillisPositive() {
        String token = jwtUtil.generate(1L, "user");
        long left = jwtUtil.remainingMillis(token);
        assertTrue(left > 0 && left <= 3_600_000L, "剩余有效期应在 0 ~ 1 小时之间，实际=" + left);
    }

    @Test
    @DisplayName("非法 token 的剩余有效期是 0")
    void remainingMillisZeroForBadToken() {
        assertEquals(0, jwtUtil.remainingMillis("bad.token.value"));
    }
}
