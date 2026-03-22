package com.ai.food.service.user;

import com.ai.food.model.SysUser;
import com.ai.food.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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

        Map<String, Object> result = new HashMap<>();
        result.put("monthTotal", monthTotal != null ? monthTotal : 0);
        result.put("todaySigned", Boolean.TRUE.equals(todaySigned));
        result.put("continuousDays", continuousDays);
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
