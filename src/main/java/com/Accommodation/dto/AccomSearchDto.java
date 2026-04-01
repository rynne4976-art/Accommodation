package com.Accommodation.dto;


import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccomSearchDto {

    private String searchDataType;     // 조회 기간 조건
    private AccomStatus accomStatus;   // 숙소 상태
    private AccomType accomType;       // 숙소 유형
    private AccomGrade grade;          // 숙소 등급
    private String searchBy;           // 검색 기준
    private String searchQuery;        // 검색어

}
