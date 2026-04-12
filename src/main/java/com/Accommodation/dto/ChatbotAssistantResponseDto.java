package com.Accommodation.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatbotAssistantResponseDto {

    private final String answer;
    private final List<ChatbotRecommendationItemDto> recommendations;
    private final String actionType;
    private final Long selectedAccomId;

    public ChatbotAssistantResponseDto(String answer, List<ChatbotRecommendationItemDto> recommendations) {
        this(answer, recommendations, null, null);
    }

    public ChatbotAssistantResponseDto(
            String answer,
            List<ChatbotRecommendationItemDto> recommendations,
            String actionType,
            Long selectedAccomId) {
        this.answer = answer;
        this.recommendations = recommendations;
        this.actionType = actionType;
        this.selectedAccomId = selectedAccomId;
    }
}
