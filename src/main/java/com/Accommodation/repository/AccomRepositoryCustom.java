package com.Accommodation.repository;

import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccomRepositoryCustom {

    Page<MainAccomDto> getMainAccomPage(AccomSearchDto accomSearchDto, Pageable pageable);
}
