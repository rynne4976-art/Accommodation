package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocalActivityCardDto {

    private String regionName;
    private String searchKeyword;
    private String imagePath;
    private String title;
    private String description;
}