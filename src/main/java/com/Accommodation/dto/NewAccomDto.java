package com.Accommodation.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NewAccomDto {

    private Long Id; //등록 번호
    private String AccomNm; //숙박소명
    private Integer price; //가격
    private String reg_name; //등록자 이름
    private LocalDateTime reg_date; //등록일
    private String location;

}
