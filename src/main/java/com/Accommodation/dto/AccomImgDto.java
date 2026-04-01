package com.Accommodation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccomImgDto {

    private Long id;              // 이미지 ID
    private String imgName;       // 저장 파일명
    private String oriImgName;    // 원본 파일명
    private String imgUrl;        // 이미지 경로
    private String repImgYn;      // 대표 이미지 여부
}
