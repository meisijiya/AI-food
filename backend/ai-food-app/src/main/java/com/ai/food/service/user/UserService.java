package com.ai.food.service.user;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户业务（签到 / 资料修改 / 搜索等）。
 * <p>继承 {@link ServiceImpl}，{@code baseMapper} 由父类注入；分页参数由
 * {@code IPage} 承载，原 Spring Data {@code Pageable} 已被移除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, SysUser> {

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    // [P0-用户缓存] Caffeine 5min 缓存；update* 末尾 evict
    private final Cache<Long, SysUser> userInfoCache;

    public SysUser getUserInfo(Long userId) {
        SysUser cached = userInfoCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        SysUser user = Optional.ofNullable(baseMapper.selectById(userId))
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        userInfoCache.put(userId, user);
        return user;
    }

    public Map<String, Object> signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String yyyyMM = today.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOffset = today.getDayOfMonth() - 1;
        String redisKey = "sign:" + userId + ":" + yyyyMM;

        Boolean alreadySigned = redisTemplate.opsForValue().getBit(redisKey, dayOffset);
        if (Boolean.TRUE.equals(alreadySigned)) {
            throw new RuntimeException("今日已签到");
        }

        redisTemplate.opsForValue().setBit(redisKey, dayOffset, true);

        int continuousDays = calculateContinuousDays(userId, today);

        Map<String, Object> result = new HashMap<>();
        result.put("signed", true);
        result.put("continuousDays", continuousDays);
        return result;
    }

    public Map<String, Object> getSignStatus(Long userId) {
        LocalDate today = LocalDate.now();
        String yyyyMM = today.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOffset = today.getDayOfMonth() - 1;
        String redisKey = "sign:" + userId + ":" + yyyyMM;

        Long monthTotal = redisTemplate.execute(
                connection -> connection.stringCommands().bitCount(redisKey.getBytes()),
                true);
        Boolean todaySigned = redisTemplate.opsForValue().getBit(redisKey, dayOffset);
        int continuousDays = calculateContinuousDays(userId, today);

        // 查询本月已签到的天数列表
        int daysInMonth = YearMonth.from(today).lengthOfMonth();
        List<Integer> signedDays = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            Boolean signed = redisTemplate.opsForValue().getBit(redisKey, day - 1);
            if (Boolean.TRUE.equals(signed)) {
                signedDays.add(day);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("monthTotal", monthTotal != null ? monthTotal : 0);
        result.put("todaySigned", Boolean.TRUE.equals(todaySigned));
        result.put("continuousDays", continuousDays);
        result.put("signedDays", signedDays);
        return result;
    }

    public Map<String, Object> searchUsers(Long currentUserId, String keyword, int page, int size) {
        // ponytail: 控制器传入 0-based（Spring Data 习惯），MP Page 内部 1-based，外层 API 仍回显 0-based
        IPage<SysUser> userPage =
                baseMapper.searchUsers(new Page<>(page + 1, size), keyword, currentUserId);

        List<SysUser> users = new ArrayList<>(userPage.getRecords());

        // Batch compute continuous days
        Map<Long, Integer> continuousMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (SysUser user : users) {
            int days = calculateContinuousDays(user.getId(), today);
            continuousMap.put(user.getId(), days);
        }

        // Sort by continuous days DESC within the page
        users.sort((a, b) -> continuousMap.getOrDefault(b.getId(), 0) - continuousMap.getOrDefault(a.getId(), 0));

        List<Map<String, Object>> items = new ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            item.put("avatar", user.getAvatar());
            item.put("continuousDays", continuousMap.getOrDefault(user.getId(), 0));
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);  // 回显 0-based，与原 Spring Data Page.getNumber() 等价
        result.put("size", userPage.getSize());
        result.put("totalElements", userPage.getTotal());
        result.put("totalPages", userPage.getPages());
        return result;
    }

    public SysUser updateNickname(Long userId, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new RuntimeException("昵称不能为空");
        }
        if (nickname.length() > 50) {
            throw new RuntimeException("昵称不能超过50个字符");
        }
        SysUser user = getUserInfo(userId);
        user.setNickname(nickname.trim());
        // ponytail: 显式 updateById 语义清晰，避免 ServiceImpl.save 的 select-then-decide
        baseMapper.updateById(user);
        userInfoCache.invalidate(userId);  // [P0-用户缓存] evict
        return user;
    }

    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new RuntimeException("请输入旧密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码不能少于6个字符");
        }
        SysUser user = getUserInfo(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(user);
        // ponytail: 不 evict password 缓存——密码字段不影响 getUserInfo 行为，
        // 5min 自然过期后下次读取会自动更新；其他字段（nickname/avatar）已 evict
    }

    public SysUser updateAvatar(Long userId, String avatarUrl) {
        SysUser user = getUserInfo(userId);
        user.setAvatar(avatarUrl);
        baseMapper.updateById(user);
        userInfoCache.invalidate(userId);  // [P0-用户缓存] evict
        return user;
    }

    private int calculateContinuousDays(Long userId, LocalDate date) {
        int continuous = 0;
        LocalDate checkDate = date;

        while (true) {
            String yyyyMM = checkDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            int dayOffset = checkDate.getDayOfMonth() - 1;
            String redisKey = "sign:" + userId + ":" + yyyyMM;

            Boolean signed = redisTemplate.opsForValue().getBit(redisKey, dayOffset);
            if (!Boolean.TRUE.equals(signed)) {
                break;
            }
            continuous++;
            checkDate = checkDate.minusDays(1);
        }

        return continuous;
    }
}
