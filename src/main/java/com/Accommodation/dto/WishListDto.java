package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomType;
import lombok.Getter;

@Getter
public class WishListDto {

    private final Long accomId;
    private final String accomName;
    private final AccomType accomType;
    private final AccomGrade grade;
    private final String accomDetail;
    private final String location;
    private final Integer pricePerNight;
    private final String imgUrl;
    private final Double avgRating;
    private final Integer reviewCount;

    public WishListDto(Long accomId, String accomName, AccomType accomType, AccomGrade grade,
                       String accomDetail, String location, Integer pricePerNight,
                       String imgUrl, Double avgRating, Integer reviewCount) {
        this.accomId = accomId;
        this.accomName = accomName;
        this.accomType = accomType;
        this.grade = grade;
        this.accomDetail = accomDetail;
        this.location = location;
        this.pricePerNight = pricePerNight;
        this.imgUrl = imgUrl;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    public WishListDto(Long accomId, String accomName, AccomType accomType, AccomGrade grade,
                       String accomDetail, String location, Integer pricePerNight,
                       String imgUrl, Double avgRating, Long reviewCount) {
        this(accomId, accomName, accomType, grade, accomDetail, location, pricePerNight,
                imgUrl, avgRating, reviewCount != null ? reviewCount.intValue() : 0);
    }
}
