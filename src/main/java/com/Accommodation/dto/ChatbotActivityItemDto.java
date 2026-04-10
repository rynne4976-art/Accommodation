package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatbotActivityItemDto {

    private String title;
    private String imageUrl;
    private String address;
    private String period;      // 비어있으면 축제 아님
    private String category;    // 행사/축제 | 관광지 | 문화시설 | 레포츠
    private String linkUrl;
    private boolean ongoing;    // 현재 진행 중 여부
    private int score;          // 키워드 매칭 점수 (내부용)
}
