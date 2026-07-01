package com.ai.food.service.conversation;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * ConversationService facade 结构测试：仅验证 facade 暴露了原 ConversationService（618 行）的全部公开方法，
 * 不测行为（行为由 ConversationServiceTest + 各子 service 共同保证）。
 * <p>
 * 按 Oracle 修订建议：{@code processAnswer} 70 行状态机必须保留在 facade（不拆），本测试固定该约束。
 * </p>
 */
class ConversationServiceFacadeTest {

    /**
     * 原 ConversationService（拆分前）公开的对外契约方法。facade 必须暴露这些签名才能保证
     * ConversationController / ConversationWebSocketHandler 等调用方零修改。
     */
    private static final List<String> EXPECTED_PUBLIC_METHODS = List.of(
            "validateOwnership",
            "initializeConversation",
            "getFirstQuestion",
            "processAnswer",
            "handleInterrupt",
            "generateRecommendationMessage",
            "getRequiredParams",
            "getOptionalParams",
            "isAllRequiredParamsCollected",
            "getRemainingRequiredParams",
            "cancelSession"
    );

    @Test
    void conversationService_exposesAllExpectedPublicMethods() {
        Set<String> actual = new HashSet<>();
        for (Method m : ConversationService.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                actual.add(m.getName());
            }
        }
        for (String expected : EXPECTED_PUBLIC_METHODS) {
            assertTrue(actual.contains(expected),
                    "ConversationService facade 缺少公开方法: " + expected + "（实际: " + actual + "）");
        }
        assertEquals(EXPECTED_PUBLIC_METHODS.size(), actual.size(),
                "ConversationService facade 公开方法数应与拆分前一致（" + EXPECTED_PUBLIC_METHODS.size()
                        + "），实际: " + actual);
    }

    @Test
    void processAnswer_isDefinedInFacade() throws NoSuchMethodException {
        // Oracle 修订关键点：70 行 processAnswer 状态机必须留在 facade（不拆）。
        Method m = ConversationService.class.getMethod(
                "processAnswer", String.class, String.class,
                com.ai.food.dto.ConversationState.class);
        assertNotNull(m);
        assertTrue(Modifier.isPublic(m.getModifiers()), "processAnswer 必须是 public");
    }

    @Test
    void messageTagParser_isIndependentComponent() {
        // MessageTagParser 必须保持 @Component 独立，未合并到 facade 或子 service。
        assertTrue(MessageTagParser.class.isAnnotationPresent(Component.class),
                "MessageTagParser 必须是独立 @Component（Oracle 修订建议）");
    }

    @Test
    void conversationService_extendsServiceImpl() {
        // facade 仍 extends ServiceImpl（因为有 baseMapper.findBySessionId 等调用）
        assertTrue(
                com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.isAssignableFrom(ConversationService.class),
                "ConversationService facade 必须保留 ServiceImpl 继承（baseMapper 还在用）");
    }

    @Test
    void facade_constructor_injectsTwoSubServices() {
        // 验证 facade 通过 @RequiredArgsConstructor 注入了 2 个子 service
        // （ConversationParamService + ConversationAiService）。其它依赖（AiService / Mapper / Redis）是
        // facade 自身需要保留的（状态机内部用）。
        java.lang.reflect.Constructor<?>[] ctors = ConversationService.class.getDeclaredConstructors();
        assertEquals(1, ctors.length, "facade 应该只有一个构造器（Lombok @RequiredArgsConstructor 生成）");
        Class<?>[] params = ctors[0].getParameterTypes();

        Set<Class<?>> expectedSubServices = new HashSet<>(Arrays.asList(
                ConversationParamService.class,
                ConversationAiService.class
        ));
        for (Class<?> p : params) {
            // 不强制 count == 2（facade 还要 aiService / mapper / redis 等），只断言子 service 在参数列表中
            if (expectedSubServices.contains(p)) {
                expectedSubServices.remove(p);
            }
        }
        assertTrue(expectedSubServices.isEmpty(),
                "facade 构造器缺少子 service: " + expectedSubServices);
    }

    @Test
    void cancelSession_isTransactional() throws NoSuchMethodException {
        // ponytail: @Transactional 必须保留在 facade 上（cancelSession 跨多张表软删除，需要事务边界）。
        Method m = ConversationService.class.getDeclaredMethod("cancelSession", String.class);
        assertNotNull(m.getAnnotation(Transactional.class),
                "cancelSession 上的 @Transactional 必须保留（跨多表软删除事务边界）");
    }

    @Test
    void paramServiceAndAiService_areSpringServices() {
        assertTrue(ConversationParamService.class.isAnnotationPresent(Service.class),
                "ConversationParamService 必须是 @Service");
        assertTrue(ConversationAiService.class.isAnnotationPresent(Service.class),
                "ConversationAiService 必须是 @Service");
    }

    @Test
    void conversationUtil_holdsSharedConstants() {
        // 验证 ConversationUtil 暴露了共享 Redis key + ObjectMapper + 必选/可选参数列表
        assertNotNull(ConversationUtil.OBJECT_MAPPER);
        assertNotNull(ConversationUtil.PENDING_RECOMMEND_KEY);
        assertNotNull(ConversationUtil.REQUIRED_PARAMS);
        assertNotNull(ConversationUtil.OPTIONAL_PARAMS);
        assertEquals(7, ConversationUtil.REQUIRED_PARAMS_COUNT);
        assertEquals(7, ConversationUtil.REQUIRED_PARAMS.size());
        assertEquals(3, ConversationUtil.OPTIONAL_PARAMS.size());
        assertTrue(ConversationUtil.PENDING_RECOMMEND_KEY.startsWith("pending:recommend:"));
        assertTrue(ConversationUtil.REQUIRED_PARAMS.contains("time"));
        assertTrue(ConversationUtil.REQUIRED_PARAMS.contains("taste"));
        assertTrue(ConversationUtil.OPTIONAL_PARAMS.contains("restriction"));
    }
}
