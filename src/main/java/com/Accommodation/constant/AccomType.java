package com.Accommodation.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccomType {

    HOTEL("호텔"),
    MOTEL("모텔"),
    PENSION("펜션"),
    RESORT("리조트"),
    GUESTHOUSE("게스트하우스");

    private final String label;

}
