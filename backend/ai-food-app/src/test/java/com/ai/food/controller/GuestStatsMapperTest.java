package com.ai.food.controller;

import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.SysUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * GuestController.getGuestStats 真实化查询的烟雾测试。
 *
 * <p>ponytail: 故意走纯单元测试而非 SpringBootTest,避免 30s+ 上下文启动。
 * 只验证 Wrapper 构建不出异常、不为 null。Mapper / DB 的真实查询交给集成测试。
 */
@DisplayName("GuestStats query wrapper 烟雾测试")
class GuestStatsMapperTest {

    @Test
    @DisplayName("FeedPost selectCount wrapper 构建 OK")
    void feedPost_queryBuilder_doesNotThrow() {
        var q = Wrappers.<FeedPost>lambdaQuery()
                .eq(FeedPost::getVisibility, "public")
                .eq(FeedPost::getIsDeleted, 0);
        assertNotNull(q);
    }

    @Test
    @DisplayName("SysUser selectCount wrapper 构建 OK")
    void sysUser_queryBuilder_doesNotThrow() {
        var q = Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getIsDeleted, 0);
        assertNotNull(q);
    }
}
