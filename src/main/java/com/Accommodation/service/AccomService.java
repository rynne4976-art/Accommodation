package com.Accommodation.service;

import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.repository.AccomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccomService {

    private final AccomRepository accomRepository;

    @Transactional(readOnly = true)
    public Page<MainAccomDto> getMainItemPage(AccomSearchDto accomSearchDto, Pageable pageable){
        return accomRepository.getMainAccomPage(accomSearchDto, pageable);
    }



}
