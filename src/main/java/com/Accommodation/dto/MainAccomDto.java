package com.Accommodation.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MainAccomDto {

    private Long id; //업소id
    private String accomNm; //업소명
    private Integer Price; //가격
    private Integer Stars; //등급
    private String accomDetail; //업소 상세설명
    private Integer StarRating; //별점
    private  String ImName; // 대표이미지명

    @QueryProjection
    public MainAccomDto(Long id, String accomNm, Integer price, Integer stars, String accomDetail, Integer starRating, String imName) {
        this.id = id;
        this.accomNm = accomNm;
        Price = price;
        Stars = stars;
        this.accomDetail = accomDetail;
        StarRating = starRating;
        ImName = imName;
    }
}
