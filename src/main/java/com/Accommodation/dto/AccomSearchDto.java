package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AccomSearchDto {

    private String searchDataType;
    private AccomStatus accomStatus;
    private AccomType accomType;
    private AccomGrade grade;
    private String searchBy;
    private String searchQuery;
    private Integer minPrice;
    private Double minRating;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;
}
