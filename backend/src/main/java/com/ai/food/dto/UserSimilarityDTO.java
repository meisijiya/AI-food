package com.ai.food.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSimilarityDTO {
    private Long userId;
    private String nickname;
    private Double similarity;
}