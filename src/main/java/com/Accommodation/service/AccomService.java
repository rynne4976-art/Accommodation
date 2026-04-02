package com.Accommodation.service;

import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.repository.AccomImgRepository;
import com.Accommodation.repository.AccomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccomService {

    private final AccomRepository accomRepository;
    private final AccomImgService accomImgService;
    private final AccomImgRepository accomImgRepository;

    public Long saveAccom(AccomFormDto accomFormDto, List<MultipartFile> accomImgFileList) throws Exception {

        Accom accom = new Accom();
        accom.updateAccom(
                accomFormDto.getAccomName(),
                accomFormDto.getPricePerNight(),
                accomFormDto.getAccomDetail(),
                accomFormDto.getAccomType(),
                accomFormDto.getGrade(),
                accomFormDto.getLocation(),
                accomFormDto.getRoomCount(),
                accomFormDto.getStatus()
        );

        accomRepository.save(accom);

        if (accomImgFileList != null) {
            for (int i = 0; i < accomImgFileList.size(); i++) {
                MultipartFile multipartFile = accomImgFileList.get(i);

                if (multipartFile == null || multipartFile.isEmpty()) {
                    continue;
                }

                AccomImg accomImg = new AccomImg();
                accomImg.setAccom(accom);
                accomImg.setRepImgYn(i == 0 ? "Y" : "N");
                accomImgService.saveAccomImg(accomImg, multipartFile);
            }
        }

        return accom.getId();
    }

    @Transactional(readOnly = true)
    public Accom getAccomDtl(Long accomId) {
        Accom accom = accomRepository.findWithAccomImgListById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        if (Boolean.TRUE.equals(accom.getDeleted())) {
            throw new EntityNotFoundException("삭제된 숙소입니다.");
        }

        return accom;
    }

    @Transactional(readOnly = true)
    public AccomFormDto getAccomFormDto(Long accomId) {
        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        return AccomFormDto.of(accom);
    }

    @Transactional(readOnly = true)
    public Page<Accom> getAdminAccomPage(AccomSearchDto accomSearchDto, Pageable pageable) {
        return accomRepository.getAdminAccomPage(accomSearchDto, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MainAccomDto> getMainAccomPage(AccomSearchDto accomSearchDto, Pageable pageable) {
        return accomRepository.getMainAccomPage(accomSearchDto, pageable);
    }

    public Long updateAccom(Long accomId,
                            AccomFormDto accomFormDto,
                            List<MultipartFile> accomImgFileList) throws Exception {

        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        accom.updateAccom(
                accomFormDto.getAccomName(),
                accomFormDto.getPricePerNight(),
                accomFormDto.getAccomDetail(),
                accomFormDto.getAccomType(),
                accomFormDto.getGrade(),
                accomFormDto.getLocation(),
                accomFormDto.getRoomCount(),
                accomFormDto.getStatus()
        );

        boolean hasNewImage = false;
        if (accomImgFileList != null) {
            for (MultipartFile multipartFile : accomImgFileList) {
                if (multipartFile != null && !multipartFile.isEmpty()) {
                    hasNewImage = true;
                    break;
                }
            }
        }

        if (hasNewImage) {
            List<AccomImg> oldImgList = accomImgRepository.findByAccomIdOrderByIdAsc(accomId);
            accomImgRepository.deleteAll(oldImgList);

            int repIndex = 0;
            for (MultipartFile multipartFile : accomImgFileList) {
                if (multipartFile == null || multipartFile.isEmpty()) {
                    continue;
                }

                AccomImg accomImg = new AccomImg();
                accomImg.setAccom(accom);
                accomImg.setRepImgYn(repIndex == 0 ? "Y" : "N");
                accomImgService.saveAccomImg(accomImg, multipartFile);
                repIndex++;
            }
        }

        return accom.getId();
    }

    public void deleteAccom(Long accomId) {
        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        accom.softDelete();
    }
}