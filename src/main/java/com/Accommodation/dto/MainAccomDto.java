package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class MainAccomDto {

    private Long id;
    private String accomName;
    private AccomType accomType;
    private AccomGrade grade;
    private String accomDetail;
    private String imgUrl;
    private Integer pricePerNight;
    private String location;
    private Integer roomCount;
    private Double avgRating;
    private Integer reviewCount;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    @QueryProjection
    public MainAccomDto(Long id, String accomName, AccomType accomType, AccomGrade grade, String accomDetail,
                        String imgUrl, Integer pricePerNight, String location,
                        Integer roomCount, Double avgRating, Integer reviewCount,
                        LocalTime checkInTime, LocalTime checkOutTime) {
        this.id = id;
        this.accomName = accomName;
        this.accomType = accomType;
        this.grade = grade;
        this.accomDetail = accomDetail;
        this.imgUrl = imgUrl;
        this.pricePerNight = pricePerNight;
        this.location = location;
        this.roomCount = roomCount;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }
}
