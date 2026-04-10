package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotRecommendationItemDto {

    private Long accomId;
    private String accomName;
    private String accomType;
    private Integer grade;
    private String location;
    private Integer pricePerNight;
    private Double avgRating;
    private Integer reviewCount;
    private String imgUrl;
    private List<String> reasons;
}
