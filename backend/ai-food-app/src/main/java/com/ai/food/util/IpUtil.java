package com.ai.food.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class IpUtil {

    private static Searcher searcher;

    static {
        try {
            ClassPathResource resource = new ClassPathResource("ip2region.xdb");
            byte[] dbBytes = resource.getInputStream().readAllBytes();
            searcher = Searcher.newWithBuffer(dbBytes);
            log.info("ip2region database loaded from classpath");
        } catch (Exception e) {
            log.warn("Failed to load ip2region database, geo lookup disabled: {}", e.getMessage());
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) return ip;
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) return ip;
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) return ip;
        ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    public static String getRegion(String ip) {
        if (searcher == null || ip == null || "127.0.0.1".equals(ip)) {
            return "内网IP";
        }
        try {
            String region = searcher.search(ip);
            if (region != null && !region.isEmpty()) {
                // 格式: 中国|广东|深圳|0|0 → 返回 "中国广东深圳"
                return region.replace("|0", "").replace("0|", "").replace("|", " ").trim();
            }
        } catch (Exception e) {
            log.warn("ip2region lookup failed for {}: {}", ip, e.getMessage());
        }
        return "未知";
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}
