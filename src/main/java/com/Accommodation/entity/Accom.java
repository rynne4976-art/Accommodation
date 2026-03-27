package com.Accommodation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "accom")
@Getter
@Setter
@ToString
public class Accom {

    @Id
    @GeneratedValue
    private Long id;                // 상품 번호

    private String AccomNm;          // 상품명

    private Integer Stars;          //등급

    private Integer price;          // 가격

    private String AccomDetail;      // 상세 설명

    private String reserveStatCd;      // 예약 코드

    private LocalDateTime regTime;  // 등록 시간

    private LocalDateTime updateTime; // 수정 시간

    private Date check_In; //입실 시간

    private Date check_Out; //퇴실 시간

    private Date reserveDay; //예약날짜

    private Integer StarRating;  //별점




}
