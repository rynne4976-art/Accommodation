package com.Accommodation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "newaccom")
@Getter
@Setter
@ToString
public class NewAccom {

    @Id
    @GeneratedValue
    private Long Id; //등록 번호

    private String AccomNm; //숙박소명

    private Integer Stars; // 등급

    private Integer price; //가격

    private String reg_name; //등록자 이름

    private LocalDateTime reg_date; //등록일

    private String location;

    private String imgName;


}
