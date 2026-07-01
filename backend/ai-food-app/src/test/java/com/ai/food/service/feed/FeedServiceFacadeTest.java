package com.ai.food.service.feed;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FeedService facade 结构测试：仅验证 facade 暴露了原 FeedService 的全部公开方法，
 * 不测行为（行为由各子 service + 集成测试保证）。
 */
class FeedServiceFacadeTest {

    /**
     * 原 FeedService（拆分前）公开的对外契约方法。facade 必须暴露这些签名才能保证
     * Controller / RecordService 等调用方零修改。
     */
    private static final List<String> EXPECTED_PUBLIC_METHODS = List.of(
            // 查询
            "getPublicFeedList",
            "getFeedDetail",
            "getHotRank",
            "getFriendFeedList",
            // 发布 / 撤回
            "publishPost",
            "unpublish",
            "checkPublishedWithVisibility",
            "checkPublished",
            "cleanRedisForDeletedPost",
            // 评论 / 点赞
            "toggleLike",
            "addComment",
            "getComments",
            "deleteComment",
            // 定时
            "refreshHotRankCacheScheduled"
    );

    @Test
    void feedService_exposesAllExpectedPublicMethods() {
        Set<String> actual = new HashSet<>();
        for (Method m : FeedService.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                actual.add(m.getName());
            }
        }
        for (String expected : EXPECTED_PUBLIC_METHODS) {
            assertTrue(actual.contains(expected),
                    "FeedService facade 缺少公开方法: " + expected + "（实际: " + actual + "）");
        }
    }

    @Test
    void feedService_doesNotExtendServiceImpl() {
        // ponytail: facade 不再继承 ServiceImpl；baseMapper 由子 service 直接注入。
        assertTrue(!com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.isAssignableFrom(FeedService.class)
                        && !FeedService.class.getSuperclass().equals(Object.class)
                        || FeedService.class.getSuperclass().equals(Object.class),
                "FeedService facade 不应该继承 ServiceImpl（828 行胖类已拆分）");
    }

    @Test
    void facade_constructor_injectsAllFourSubServices() throws NoSuchMethodException {
        // 验证 facade 通过 @RequiredArgsConstructor 注入了 4 个子 service。
        // ponytail: 不验证注解本身，只验证构造器参数列表里有 4 个 FeedService 子类型字段。
        java.lang.reflect.Constructor<?>[] ctors = FeedService.class.getDeclaredConstructors();
        assertEquals(1, ctors.length, "facade 应该只有一个构造器（Lombok @RequiredArgsConstructor 生成）");
        Class<?>[] params = ctors[0].getParameterTypes();
        assertEquals(4, params.length, "facade 应该注入 4 个子 service，实际: " + Arrays.toString(params));

        Set<Class<?>> expectedSubServices = new HashSet<>(Arrays.asList(
                FeedQueryService.class,
                FeedPublishService.class,
                FeedCommentService.class,
                FeedHotRankService.class
        ));
        for (Class<?> p : params) {
            assertTrue(expectedSubServices.contains(p),
                    "facade 构造器参数 " + p.getName() + " 不是 4 个子 service 之一");
        }
    }

    @Test
    void scheduledMethod_isPublicAndAnnotated() throws NoSuchMethodException {
        Method m = FeedService.class.getDeclaredMethod("refreshHotRankCacheScheduled");
        assertTrue(Modifier.isPublic(m.getModifiers()), "refreshHotRankCacheScheduled 必须是 public");
        Scheduled scheduled = m.getAnnotation(Scheduled.class);
        assertNotNull(scheduled, "@Scheduled 注解必须保留在 facade 上（Spring AOP 代理边界）");
    }

    @Test
    void feedUtil_holdsSharedConstantsAndHelpers() {
        // 验证 FeedUtil 暴露了共享 Redis key 常量（避免各 service 重复硬编码）
        // ponytail: 静态字段访问本身即验证存在。
        assertNotNull(FeedUtil.HOT_RANK_KEY);
        assertNotNull(FeedUtil.HOT_DETAILS_KEY);
        assertNotNull(FeedUtil.FRIEND_FEED_KEY);
        assertNotNull(FeedUtil.LIKE_SET_KEY);
        assertNotNull(FeedUtil.LIKE_COUNT_KEY);
        assertNotNull(FeedUtil.OBJECT_MAPPER);
        assertTrue(FeedUtil.HOT_RANK_KEY.startsWith("feed:hot:"));
        assertTrue(FeedUtil.FRIEND_FEED_KEY.startsWith("feed:"));
    }
}
