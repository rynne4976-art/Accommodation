package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotRecommendationResponseDto {

    private String query;
    private List<String> interpretedNeeds;
    private List<ChatbotRecommendationItemDto> recommendations;
}
