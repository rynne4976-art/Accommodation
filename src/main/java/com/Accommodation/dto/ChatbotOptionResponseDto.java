package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotOptionResponseDto {

    private List<String> locations;
    private List<String> accomTypes;
}
