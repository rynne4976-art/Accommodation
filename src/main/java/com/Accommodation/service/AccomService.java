package com.Accommodation.service;

import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.entity.AccomOperationDay;
import com.Accommodation.entity.AccomOperationPolicy;
import com.Accommodation.repository.AccomImgRepository;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.validation.AccomValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AccomService {

    private final AccomRepository accomRepository;
    private final AccomImgService accomImgService;
    private final AccomImgRepository accomImgRepository;
    private final S3FileService s3FileService;
    private final AccomValidator accomValidator;

    public Long saveAccom(AccomFormDto accomFormDto, List<MultipartFile> accomImgFileList) throws Exception {
        accomValidator.validateOrThrow(accomFormDto);

        Accom accom = new Accom();
        accom.updateAccom(
                accomFormDto.getAccomName(),
                accomFormDto.getPricePerNight(),
                accomFormDto.getAccomDetail(),
                accomFormDto.getAccomType(),
                accomFormDto.getGrade(),
                accomFormDto.getLocation(),
                accomFormDto.getRoomCount(),
                accomFormDto.getGuestCount(),
                accomFormDto.getStatus()
        );

        syncOperationInfo(accom, accomFormDto);
        accomRepository.save(accom);

        saveAccomImages(accom, accomImgFileList);
        return accom.getId();
    }

    @Transactional(readOnly = true)
    public Accom getAccomDtl(Long accomId) {
        Accom accom = accomRepository.findWithOperationInfoById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        if (Boolean.TRUE.equals(accom.getDeleted())) {
            throw new EntityNotFoundException("삭제된 숙소입니다.");
        }

        List<AccomImg> accomImgList =
                accomImgRepository.findByAccomIdOrderByIdAsc(accomId);

        accom.setAccomImgList(accomImgList);

        accom.getAccomImgList().forEach(accomImg ->
                accomImg.setImgUrl(
                        s3FileService.getProxyImageUrl(accomImg.getImgName())
                )
        );

        return accom;
    }

    @Transactional(readOnly = true)
    public AccomFormDto getAccomFormDto(Long accomId) {
        Accom accom = accomRepository.findWithOperationInfoById(accomId)
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
        accomValidator.validateOrThrow(accomFormDto);

        Accom accom = accomRepository.findWithOperationInfoById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        accom.updateAccom(
                accomFormDto.getAccomName(),
                accomFormDto.getPricePerNight(),
                accomFormDto.getAccomDetail(),
                accomFormDto.getAccomType(),
                accomFormDto.getGrade(),
                accomFormDto.getLocation(),
                accomFormDto.getRoomCount(),
                accomFormDto.getGuestCount(),
                accomFormDto.getStatus()
        );

        syncOperationInfo(accom, accomFormDto);

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

            for (AccomImg oldImg : oldImgList) {
                accomImgService.deleteAccomImg(oldImg);
            }

            accom.getAccomImgList().clear();
            accomImgRepository.deleteByAccomId(accomId);

            saveAccomImages(accom, accomImgFileList);
        }

        return accom.getId();
    }

    public void deleteAccom(Long accomId) {
        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        accom.softDelete();
    }

    private void saveAccomImages(Accom accom, List<MultipartFile> accomImgFileList) throws Exception {
        if (accomImgFileList == null) {
            return;
        }

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

    private void syncOperationInfo(Accom accom, AccomFormDto accomFormDto) {
        AccomOperationPolicy policy = accom.getOperationPolicy();

        if (policy == null) {
            policy = new AccomOperationPolicy();
            accom.setOperationPolicy(policy);
        }

        policy.updatePolicy(
                accomFormDto.getOperationStartDate(),
                accomFormDto.getOperationEndDate(),
                accomFormDto.getCheckInTime(),
                accomFormDto.getCheckOutTime()
        );

        Set<LocalDate> requestedDateSet = new LinkedHashSet<>();
        if (accomFormDto.getOperationDateList() != null) {
            for (String operationDate : accomFormDto.getOperationDateList()) {
                if (operationDate != null && !operationDate.isBlank()) {
                    requestedDateSet.add(LocalDate.parse(operationDate));
                }
            }
        }

        Map<LocalDate, AccomOperationDay> existingDayMap = new HashMap<>();
        for (AccomOperationDay operationDay : accom.getOperationDayList()) {
            existingDayMap.put(operationDay.getOperationDate(), operationDay);
        }

        Set<LocalDate> removableDateSet = new HashSet<>(existingDayMap.keySet());
        removableDateSet.removeAll(requestedDateSet);

        for (LocalDate removableDate : removableDateSet) {
            accom.removeOperationDay(existingDayMap.get(removableDate));
        }

        for (LocalDate requestedDate : requestedDateSet) {
            if (existingDayMap.containsKey(requestedDate)) {
                continue;
            }

            AccomOperationDay operationDay = new AccomOperationDay();
            operationDay.setOperationDate(requestedDate);
            accom.addOperationDay(operationDay);
        }
    }
}
