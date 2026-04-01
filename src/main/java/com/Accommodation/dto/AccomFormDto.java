package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import com.Accommodation.entity.Accom;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccomFormDto {

    private Long id;                  // 숙소 ID
    private String accomName;         // 숙소명
    private Integer pricePerNight;    // 1박 가격
    private String accomDetail;       // 숙소 상세 설명
    private AccomType accomType;      // 숙소 유형
    private AccomGrade grade;         // 숙소 등급
    private String location;          // 숙소 위치
    private AccomStatus status;       // 운영 상태

    public static AccomFormDto of(Accom accom) {
        AccomFormDto dto = new AccomFormDto();
        dto.setId(accom.getId());
        dto.setAccomName(accom.getAccomName());
        dto.setPricePerNight(accom.getPricePerNight());
        dto.setAccomDetail(accom.getAccomDetail());
        dto.setAccomType(accom.getAccomType());
        dto.setGrade(accom.getGrade());
        dto.setLocation(accom.getLocation());
        dto.setStatus(accom.getStatus());
        return dto;
    }
}