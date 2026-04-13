package com.Accommodation.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ChatbotAssistantResponseDto {

    private final String answer;
    private final List<ChatbotRecommendationItemDto> recommendations;
    private final String actionType;
    private final Long selectedAccomId;
    private final String region;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public ChatbotAssistantResponseDto(String answer, List<ChatbotRecommendationItemDto> recommendations) {
        this(answer, recommendations, null, null, null, null, null);
    }

    public ChatbotAssistantResponseDto(
            String answer,
            List<ChatbotRecommendationItemDto> recommendations,
            String actionType,
            Long selectedAccomId) {
        this(answer, recommendations, actionType, selectedAccomId, null, null, null);
    }

    public ChatbotAssistantResponseDto(
            String answer,
            List<ChatbotRecommendationItemDto> recommendations,
            String actionType,
            Long selectedAccomId,
            String region,
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        this.answer = answer;
        this.recommendations = recommendations;
        this.actionType = actionType;
        this.selectedAccomId = selectedAccomId;
        this.region = region;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }
}
