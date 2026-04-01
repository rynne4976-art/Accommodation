package com.Accommodation.dto;


import com.Accommodation.constant.AccomReserveStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccomSearchDto {

    private String searchDataType;
    private AccomReserveStatus accomReserveStatus;
    private String searchBy;
    private String searchQuery;

}
