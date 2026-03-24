package com.ai.food.service.user;

import com.ai.food.model.SysUser;
import com.ai.food.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public SysUser getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
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
        Pageable pageable = PageRequest.of(page, size);
        Page<SysUser> userPage = userRepository.searchUsers(keyword, currentUserId, pageable);

        List<SysUser> users = userPage.getContent();
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());

        // Batch compute continuous days
        Map<Long, Integer> continuousMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (SysUser user : users) {
            int days = calculateContinuousDays(user.getId(), today);
            continuousMap.put(user.getId(), days);
        }

        // Sort by continuous days DESC within the page
        List<Map<String, Object>> items = new ArrayList<>();
        users.sort((a, b) -> continuousMap.getOrDefault(b.getId(), 0) - continuousMap.getOrDefault(a.getId(), 0));

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
        result.put("page", userPage.getNumber());
        result.put("size", userPage.getSize());
        result.put("totalElements", userPage.getTotalElements());
        result.put("totalPages", userPage.getTotalPages());
        return result;
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
