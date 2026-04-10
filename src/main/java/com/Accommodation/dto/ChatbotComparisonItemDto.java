package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatbotComparisonItemDto {

    private Long accomId;
    private String accomName;
    private String accomType;
    private Integer grade;
    private String location;
    private Integer pricePerNight;
    private Double avgRating;
    private Integer reviewCount;
    private Integer roomCount;
    private Integer guestCount;
    private String checkInTime;
    private String checkOutTime;
    private String summary;
}
