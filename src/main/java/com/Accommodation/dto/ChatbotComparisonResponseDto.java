package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotComparisonResponseDto {

    private String requestedLeftName;
    private String requestedRightName;
    private ChatbotComparisonItemDto left;
    private ChatbotComparisonItemDto right;
    private List<String> comparisonPoints;
    private List<String> unavailablePoints;
}
