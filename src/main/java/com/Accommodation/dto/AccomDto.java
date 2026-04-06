package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccomDto {

        private Long id;                  // 숙소 ID
        private String accomName;         // 숙소명
        private Integer pricePerNight;    // 1박 가격
        private String accomDetail;       // 숙소 상세 설명
        private AccomType accomType;      // 숙소 유형
        private AccomGrade grade;         // 숙소 등급
        private String location;          // 숙소 위치
        private Integer roomCount;        // 객실 수
        private Double avgRating;         // 평균 별점
        private Integer reviewCount;      // 리뷰 수
        private AccomStatus status;       // 운영 상태
}