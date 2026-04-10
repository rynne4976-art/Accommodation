package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatbotSelectableAccomDto {

    private Long accomId;
    private String accomName;
    private String accomType;
    private Integer grade;
    private String location;
    private Integer pricePerNight;
    private Double avgRating;
    private Integer reviewCount;
    private String imgUrl;
    private String source; // "wish" | "recent"
}
