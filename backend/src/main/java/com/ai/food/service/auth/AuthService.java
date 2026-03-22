package com.ai.food.service.auth;

import com.ai.food.dto.LoginRequest;
import com.ai.food.dto.LoginResponse;
import com.ai.food.dto.RegisterRequest;
import com.ai.food.dto.SendCodeRequest;
import com.ai.food.model.SysUser;
import com.ai.food.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final Cache<String, Boolean> emailLimitByUsername;
    private final Cache<String, Boolean> emailLimitByIp;

    public void sendCode(SendCodeRequest req, String clientIp) {
        String username = req.getUsername();
        if (username != null && !username.isBlank()) {
            if (Boolean.TRUE.equals(emailLimitByUsername.getIfPresent(username))) {
                throw new RuntimeException("请求过于频繁，请60秒后再试");
            }
            emailLimitByUsername.put(username, true);
        }

        if (clientIp != null && !clientIp.isBlank()) {
            if (Boolean.TRUE.equals(emailLimitByIp.getIfPresent(clientIp))) {
                throw new RuntimeException("请求过于频繁，请60秒后再试");
            }
            emailLimitByIp.put(clientIp, true);
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String redisKey = "code:email:" + req.getEmail();
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(5));

        emailService.sendVerificationCode(req.getEmail(), code);
        log.info("验证码已生成 - email: {}, code: {}", req.getEmail(), code);
    }

    public LoginResponse register(RegisterRequest req) {
        String redisKey = "code:email:" + req.getEmail();
        String cachedCode = redisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(req.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("邮箱已注册");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        userRepository.save(user);

        redisTemplate.delete(redisKey);

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        log.info("用户注册成功: {}", user.getUsername());

        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getNickname(), user.getEmail(), user.getAvatar());
    }

    public LoginResponse login(LoginRequest req) {
        SysUser user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        log.info("用户登录成功: {}", user.getUsername());

        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getNickname(), user.getEmail(), user.getAvatar());
    }
}
