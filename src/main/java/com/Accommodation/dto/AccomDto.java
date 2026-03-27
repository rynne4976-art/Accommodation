package com.Accommodation.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class AccomDto {

    @Getter
    @Setter                     // getter/setter 자동 생성
    public class ItemDto {
        private Long id;                // 상품 번호
        private String AccomNm;          // 상품명
        private Integer price;          // 가격
        private String AccomDetail;      // 상세 설명
        private String reserveStatCd;      // 판매 상태 코드
        private LocalDateTime regTime;  // 등록 시간
        private LocalDateTime updateTime; // 수정 시간
    }




}
