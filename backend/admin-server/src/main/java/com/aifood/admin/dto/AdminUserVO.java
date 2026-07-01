package com.aifood.admin.dto;

import com.ai.food.common.model.SysUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户视图对象，仅暴露前端需要的字段（不含 password）。
 *
 * <p>类级别 {@code @JsonIgnoreProperties({"password"})} 是兜底防线：
 * 即便 SysUser 被错误地直接序列化到该 VO，Jackson 也会忽略 password 字段，
 * 防止 bcrypt hash 经 API 泄露。{@code from()} 方法本身也不读取 password。</p>
 */
@Data
@JsonIgnoreProperties({"password"})
public class AdminUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    public static AdminUserVO from(SysUser u) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setRole(u.getRole());
        vo.setEmail(u.getEmail());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setUpdatedAt(u.getUpdatedAt());
        vo.setIsDeleted(u.getIsDeleted());
        return vo;
    }
}
