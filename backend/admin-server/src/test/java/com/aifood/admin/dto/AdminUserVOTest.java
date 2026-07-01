package com.aifood.admin.dto;

import com.ai.food.common.model.SysUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AdminUserVO 防回归测试。
 *
 * <p>核心目标：确保 SysUser 中的 password(bcrypt hash) 永远不会被序列化到 API 响应里。
 * 类级别 {@code @JsonIgnoreProperties({"password"})} + {@code from()} 不读 password
 * 是双重保险，这里以 JSON 字符串扫描和反射双重断言钉死。</p>
 */
class AdminUserVOTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ObjectMapper mapperWithSysUser = new ObjectMapper().registerModule(new JavaTimeModule());

    /** 构造一个带所有字段的 SysUser,模拟 MyBatis-Plus 查出的完整实体。 */
    private SysUser buildFullSysUser() {
        SysUser u = new SysUser();
        u.setId(123L);
        u.setUsername("alice");
        u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        u.setEmail("alice@example.com");
        u.setNickname("Alice");
        u.setAvatar("https://cdn/a.png");
        u.setRole("USER");
        u.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        u.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 0, 0, 0));
        u.setIsDeleted(0);
        u.setVersion(1);
        return u;
    }

    @Test
    void from_copiesAllExpectedFields() throws Exception {
        SysUser u = buildFullSysUser();
        AdminUserVO vo = AdminUserVO.from(u);

        assertEquals(123L, vo.getId());
        assertEquals("alice", vo.getUsername());
        assertEquals("alice@example.com", vo.getEmail());
        assertEquals("Alice", vo.getNickname());
        assertEquals("https://cdn/a.png", vo.getAvatar());
        assertEquals("USER", vo.getRole());
        assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4, 5), vo.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0, 0), vo.getUpdatedAt());
        assertEquals(0, vo.getIsDeleted());
    }

    @Test
    void from_doesNotCopyPassword() throws Exception {
        SysUser u = buildFullSysUser();
        AdminUserVO vo = AdminUserVO.from(u);

        // VO 类本身不应该有 password 字段（编译期保证）；运行时再反射钉死，防止有人后续加字段
        Field passwordField = null;
        for (Field f : AdminUserVO.class.getDeclaredFields()) {
            if ("password".equals(f.getName())) {
                passwordField = f;
                break;
            }
        }
        // 即使有 password 字段，也不应被 from() 写入
        if (passwordField != null) {
            passwordField.setAccessible(true);
            assertNull(passwordField.get(vo), "AdminUserVO.from() 不应复制 SysUser.password");
        }
        // 同时序列化校验：扫描 JSON 字符串
        String json = mapper.writeValueAsString(vo);
        assertFalse(json.toLowerCase().contains("password"),
                "AdminUserVO 序列化结果中不应出现 password 字段,实际: " + json);
    }

    @Test
    void serializedJson_doesNotContainPasswordField() throws Exception {
        // 直接序列化一个完整的 SysUser 含 password,模拟若误把 SysUser 暴露给 API 的灾难场景
        SysUser u = buildFullSysUser();
        String sysUserJson = mapperWithSysUser.writeValueAsString(u);
        // 防御:先确认 SysUser 序列化时确实有 password（基线,否则下面断言没意义）
        org.junit.jupiter.api.Assertions.assertTrue(sysUserJson.contains("password"),
                "基线检查:SysUser 序列化应该包含 password 字段,实际: " + sysUserJson);

        // VO 序列化后扫描
        AdminUserVO vo = AdminUserVO.from(u);
        String voJson = mapper.writeValueAsString(vo);
        assertFalse(voJson.contains("password"),
                "AdminUserVO 序列化结果中不应出现 password 字段,实际: " + voJson);
        assertFalse(voJson.contains("$2a$10$"),
                "AdminUserVO 序列化结果中不应出现 bcrypt hash 片段,实际: " + voJson);
    }

    @Test
    void serializedJson_containsAllExpectedFields() throws Exception {
        SysUser u = buildFullSysUser();
        AdminUserVO vo = AdminUserVO.from(u);
        String json = mapper.writeValueAsString(vo);

        // 9 个业务字段都应出现;password/version 不应出现
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"id\":123"));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"username\":\"alice\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"email\":\"alice@example.com\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"nickname\":\"Alice\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"avatar\":\"https://cdn/a.png\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"role\":\"USER\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"createdAt\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"updatedAt\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"isDeleted\":0"));

        // 兜底:version 也不应被泄露(SysUser 内部乐观锁字段)
        assertFalse(json.contains("\"version\""),
                "AdminUserVO 不应暴露 version 字段,实际: " + json);
    }
}
