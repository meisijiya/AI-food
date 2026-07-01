package com.ai.food.service.chat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

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
 * ChatService facade 结构测试：仅验证 facade 暴露了原 ChatService（752 行）的全部公开方法，
 * 不测行为（行为由各子 service + 现有 ChatServiceTest 保证）。
 * <p>
 * 拆分参照 fix-7 FeedServiceFacadeTest：facade 是纯转调，0 业务逻辑；事务边界与 @Async 注解由
 * 子 service / facade 自己负责。
 * </p>
 */
class ChatServiceFacadeTest {

    /**
     * 原 ChatService（拆分前）公开的对外契约方法。facade 必须暴露这些签名才能保证
     * ChatController / ChatWebSocketHandler / CleanupSoftDeletedJob 等调用方零修改。
     */
    private static final List<String> EXPECTED_PUBLIC_METHODS = List.of(
            // 权限
            "checkSendPermission",
            "getRemainingMessages",
            // 消息 CRUD
            "sendMessage",
            "getOrCreateConversation",
            "getOrCreateConversationWith",
            "getConversationList",
            "getChatHistory",
            "markAsRead",
            "clearConversation",
            "hardDeleteClearedMessages",
            "deleteChatFile",
            "deleteChatPhoto",
            "asyncDeleteFileAndRecord",
            "asyncDeletePhotoAndRecord",
            "deleteMessage",
            // 未读 + 在线
            "getUnreadCounts",
            "getTotalUnreadCount",
            "getContacts",
            "setUserOnline",
            "setUserOffline",
            "isOnline"
    );

    @Test
    void chatService_exposesAllExpectedPublicMethods() {
        Set<String> actual = new HashSet<>();
        for (Method m : ChatService.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                actual.add(m.getName());
            }
        }
        for (String expected : EXPECTED_PUBLIC_METHODS) {
            assertTrue(actual.contains(expected),
                    "ChatService facade 缺少公开方法: " + expected + "（实际: " + actual + "）");
        }
        assertEquals(EXPECTED_PUBLIC_METHODS.size(), actual.size(),
                "ChatService facade 公开方法数应与拆分前一致（" + EXPECTED_PUBLIC_METHODS.size()
                        + "），实际: " + actual);
    }

    @Test
    void chatService_doesNotExtendServiceImpl() {
        // ponytail: facade 不再继承 ServiceImpl；baseMapper 已由 ChatMessageService 直接继承并注入。
        assertTrue(!com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.isAssignableFrom(ChatService.class),
                "ChatService facade 不应该继承 ServiceImpl（752 行胖类已拆分）");
        assertTrue(ChatService.class.getSuperclass().equals(Object.class),
                "ChatService facade 应该直接继承 Object，实际父类: " + ChatService.class.getSuperclass());
    }

    @Test
    void facade_constructor_injectsThreeSubServices() {
        // 验证 facade 通过 @RequiredArgsConstructor 注入了 3 个子 service。
        // ponytail: 不验证注解本身，只验证构造器参数列表里有 3 个 Chat 子类型字段。
        java.lang.reflect.Constructor<?>[] ctors = ChatService.class.getDeclaredConstructors();
        assertEquals(1, ctors.length, "facade 应该只有一个构造器（Lombok @RequiredArgsConstructor 生成）");
        Class<?>[] params = ctors[0].getParameterTypes();
        assertEquals(3, params.length, "facade 应该注入 3 个子 service，实际: " + Arrays.toString(params));

        Set<Class<?>> expectedSubServices = new HashSet<>(Arrays.asList(
                ChatMessageService.class,
                ChatUnreadService.class,
                ChatPermissionService.class
        ));
        for (Class<?> p : params) {
            assertTrue(expectedSubServices.contains(p),
                    "facade 构造器参数 " + p.getName() + " 不是 3 个子 service 之一");
        }
    }

    @Test
    void asyncMethods_arePublicAndAnnotated() throws NoSuchMethodException {
        // @Async 必须留在 facade 上，否则 self-invocation（ChatMessageService#deleteChatFile → facade.asyncXxx）
        // 不经过 Spring 异步代理，物理文件删除会阻塞请求线程。
        Method m1 = ChatService.class.getDeclaredMethod("asyncDeleteFileAndRecord", Long.class, String.class);
        assertTrue(Modifier.isPublic(m1.getModifiers()), "asyncDeleteFileAndRecord 必须是 public");
        assertNotNull(m1.getAnnotation(Async.class),
                "asyncDeleteFileAndRecord 上的 @Async 注解必须保留在 facade 上（Spring AOP 代理边界）");

        Method m2 = ChatService.class.getDeclaredMethod("asyncDeletePhotoAndRecord", Long.class, String.class, String.class);
        assertTrue(Modifier.isPublic(m2.getModifiers()), "asyncDeletePhotoAndRecord 必须是 public");
        assertNotNull(m2.getAnnotation(Async.class),
                "asyncDeletePhotoAndRecord 上的 @Async 注解必须保留在 facade 上（Spring AOP 代理边界）");
    }

    @Test
    void chatUtil_holdsSharedConstantsAndThreshold() {
        // 验证 ChatUtil 暴露了共享 Redis key 常量与阈值常量，避免各 service 重复硬编码。
        assertNotNull(ChatUtil.UNREAD_KEY);
        assertNotNull(ChatUtil.UNREAD_TOTAL_KEY);
        assertNotNull(ChatUtil.ONLINE_KEY);
        assertNotNull(ChatUtil.MSG_COUNT_KEY);
        assertEquals(5, ChatUtil.MAX_NON_MUTUAL_MESSAGES);
        assertTrue(ChatUtil.UNREAD_KEY.startsWith("chat:unread:"));
        assertTrue(ChatUtil.UNREAD_TOTAL_KEY.startsWith("chat:unread:total:"));
        assertTrue(ChatUtil.ONLINE_KEY.startsWith("chat:online:"));
        assertTrue(ChatUtil.MSG_COUNT_KEY.startsWith("chat:msgcount:"));
    }
}
