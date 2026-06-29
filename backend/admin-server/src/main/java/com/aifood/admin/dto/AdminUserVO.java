package com.aifood.admin.dto;

import com.ai.food.common.model.SysUser;
import lombok.Data;

/** 管理后台用户视图对象，仅暴露前端需要的字段（不含 password）。 */
@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;

    public static AdminUserVO from(SysUser u) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setRole(u.getRole());
        return vo;
    }
}
