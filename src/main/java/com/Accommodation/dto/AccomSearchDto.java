package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import lombok.Getter;
import lombok.Setter;

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
}
