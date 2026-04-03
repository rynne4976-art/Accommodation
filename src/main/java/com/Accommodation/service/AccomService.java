package com.Accommodation.service;

import com.Accommodation.constant.AccomType;
import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.entity.AccomOperationDay;
import com.Accommodation.entity.AccomOperationPolicy;
import com.Accommodation.repository.AccomImgRepository;
import com.Accommodation.repository.AccomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AccomService {

    private final AccomRepository accomRepository;
    private final AccomImgService accomImgService;
    private final AccomImgRepository accomImgRepository;
    private final S3FileService s3FileService;

    public Long saveAccom(AccomFormDto accomFormDto, List<MultipartFile> accomImgFileList) throws Exception {
        validateGuestCount(accomFormDto.getAccomType(), accomFormDto.getGuestCount());
        validateOperationInfo(accomFormDto);

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
        validateGuestCount(accomFormDto.getAccomType(), accomFormDto.getGuestCount());
        validateOperationInfo(accomFormDto);

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

        accom.clearOperationDays();

        Set<String> uniqueDateSet = new LinkedHashSet<>();
        if (accomFormDto.getOperationDateList() != null) {
            for (String operationDate : accomFormDto.getOperationDateList()) {
                if (operationDate != null && !operationDate.isBlank()) {
                    uniqueDateSet.add(operationDate);
                }
            }
        }

        for (String operationDateStr : uniqueDateSet) {
            LocalDate operationDate = LocalDate.parse(operationDateStr);

            AccomOperationDay operationDay = new AccomOperationDay();
            operationDay.setOperationDate(operationDate);
            accom.addOperationDay(operationDay);
        }
    }

    private void validateGuestCount(AccomType accomType, Integer guestCount) {
        if (accomType == null || guestCount == null) {
            throw new IllegalArgumentException("숙소 유형과 투숙 가능 인원을 입력해 주세요.");
        }

        boolean largeCapacity = accomType == AccomType.HOTEL
                || accomType == AccomType.RESORT
                || accomType == AccomType.PENSION;

        boolean smallCapacity = accomType == AccomType.GUESTHOUSE
                || accomType == AccomType.MOTEL;

        if (largeCapacity && (guestCount < 2 || guestCount > 10)) {
            throw new IllegalArgumentException("호텔, 리조트, 펜션은 투숙 가능 인원을 2명에서 10명 사이로 입력해 주세요.");
        }

        if (smallCapacity && (guestCount < 1 || guestCount > 6)) {
            throw new IllegalArgumentException("게스트하우스와 모텔은 투숙 가능 인원을 1명에서 6명 사이로 입력해 주세요.");
        }
    }

    private void validateOperationInfo(AccomFormDto accomFormDto) {
        if (accomFormDto.getOperationStartDate() == null || accomFormDto.getOperationEndDate() == null) {
            throw new IllegalArgumentException("운영 기간을 입력해 주세요.");
        }

        if (accomFormDto.getOperationStartDate().isAfter(accomFormDto.getOperationEndDate())) {
            throw new IllegalArgumentException("운영 시작일은 운영 종료일보다 늦을 수 없습니다.");
        }

        if (accomFormDto.getCheckInTime() == null || accomFormDto.getCheckOutTime() == null) {
            throw new IllegalArgumentException("체크인 및 체크아웃 시간을 입력해 주세요.");
        }

        if (accomFormDto.getCheckInTime().equals(accomFormDto.getCheckOutTime())) {
            throw new IllegalArgumentException("체크인 시간과 체크아웃 시간은 같을 수 없습니다.");
        }

        if (accomFormDto.getOperationDateList() == null || accomFormDto.getOperationDateList().isEmpty()) {
            throw new IllegalArgumentException("운영일을 한 개 이상 선택해 주세요.");
        }

        for (String operationDateStr : accomFormDto.getOperationDateList()) {
            try {
                LocalDate operationDate = LocalDate.parse(operationDateStr);

                if (operationDate.isBefore(accomFormDto.getOperationStartDate())
                        || operationDate.isAfter(accomFormDto.getOperationEndDate())) {
                    throw new IllegalArgumentException("운영일은 운영 기간 안에서만 선택할 수 있습니다.");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("운영일 형식이 올바르지 않습니다.");
            }
        }
    }
}
