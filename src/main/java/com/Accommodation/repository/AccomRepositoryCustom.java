package com.Accommodation.repository;

import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Accom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccomRepositoryCustom {
    Page<Accom> getAdminAccomPage(AccomSearchDto accomSearchDto, Pageable pageable);
    Page<MainAccomDto> getMainAccomPage(AccomSearchDto accomSearchDto, Pageable pageable);
}
