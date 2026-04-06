package com.Accommodation.dto;

import com.Accommodation.constant.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSearchDto {
    private String searchBy;
    private String searchQuery;
    private Role role;
}
