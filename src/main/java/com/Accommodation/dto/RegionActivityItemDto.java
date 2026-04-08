package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionActivityItemDto {

    private String title;           // 행사/축제/즐길거리 제목
    private String imageUrl;        // 대표 이미지
    private String address;         // 주소
    private String period;          // 행사 기간
    private String detailUrl;       // 상세 페이지 링크
    private String externalUrl;     // 공식 홈페이지 / 외부 링크
    private String category;        // 행사 / 축제 / 관광지 등
    private String tel;             // 전화번호
    private String regionName;      // 지역명
}
