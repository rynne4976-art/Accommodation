package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainAccomDto {

    private Long id;                  // 숙소 ID
    private String accomName;         // 숙소명
    private AccomGrade grade;         // 숙소 등급
    private String accomDetail;       // 숙소 설명
    private String imgUrl;            // 대표 이미지 경로
    private Integer pricePerNight;    // 1박 가격
    private String location;          // 숙소 위치
    private Integer roomCount;        // 객실 수
    private Double avgRating;         // 평균 별점
    private Integer reviewCount;      // 리뷰 수

    @QueryProjection
    public MainAccomDto(Long id, String accomName, AccomGrade grade, String accomDetail,
                        String imgUrl, Integer pricePerNight, String location,
                        Integer roomCount, Double avgRating, Integer reviewCount) {
        this.id = id;
        this.accomName = accomName;
        this.grade = grade;
        this.accomDetail = accomDetail;
        this.imgUrl = imgUrl;
        this.pricePerNight = pricePerNight;
        this.location = location;
        this.roomCount = roomCount;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }
}