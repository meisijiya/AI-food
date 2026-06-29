package com.ai.food.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchUserDetailDTO {
    private Long userId;
    private String nickname;
    private String avatar;
    private String foodName;
    private Double similarity;
    private Boolean isFollowing;
    private Map<String, String> collectedParams;
}
