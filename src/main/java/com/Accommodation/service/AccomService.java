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
import com.Accommodation.repository.ReviewRepository;
import com.Accommodation.validation.AccomValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private final ReviewRepository reviewRepository;
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

        long reviewCount = reviewRepository.countByAccomId(accomId);
        Double avgRating = reviewRepository.findAverageRatingByAccomId(accomId);
        accom.setReviewCount((int) reviewCount);
        accom.setAvgRating(avgRating != null ? avgRating : 0.0);

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
        Page<MainAccomDto> accomPage = accomRepository.getMainAccomPage(accomSearchDto, pageable);
        accomPage.getContent().forEach(this::applyReviewSummary);
        return accomPage;
    }

    @Transactional(readOnly = true)
    public List<MainAccomDto> getRecentViewedAccomList(List<Long> accomIds) {
        List<MainAccomDto> recentViewedList = new ArrayList<>();

        if (accomIds == null || accomIds.isEmpty()) {
            return recentViewedList;
        }

        for (Long accomId : accomIds) {
            if (accomId == null) {
                continue;
            }

            accomRepository.findWithOperationInfoById(accomId)
                    .filter(accom -> !Boolean.TRUE.equals(accom.getDeleted()))
                    .ifPresent(accom -> recentViewedList.add(toMainAccomDto(accom)));
        }

        return recentViewedList;
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

    private void applyReviewSummary(MainAccomDto mainAccomDto) {
        if (mainAccomDto == null || mainAccomDto.getId() == null) {
            return;
        }

        long reviewCount = reviewRepository.countByAccomId(mainAccomDto.getId());
        Double avgRating = reviewRepository.findAverageRatingByAccomId(mainAccomDto.getId());

        mainAccomDto.setReviewCount((int) reviewCount);
        mainAccomDto.setAvgRating(avgRating != null ? avgRating : 0.0);
    }

    private MainAccomDto toMainAccomDto(Accom accom) {
        List<AccomImg> accomImgList = accomImgRepository.findByAccomIdOrderByIdAsc(accom.getId());
        String imgUrl = null;

        if (!accomImgList.isEmpty()) {
            AccomImg repImage = accomImgList.stream()
                    .filter(img -> "Y".equalsIgnoreCase(img.getRepImgYn()))
                    .findFirst()
                    .orElse(accomImgList.get(0));
            imgUrl = s3FileService.getProxyImageUrl(repImage.getImgName());
        }

        MainAccomDto dto = new MainAccomDto(
                accom.getId(),
                accom.getAccomName(),
                accom.getAccomType(),
                accom.getGrade(),
                accom.getAccomDetail(),
                imgUrl,
                accom.getPricePerNight(),
                accom.getLocation(),
                accom.getRoomCount(),
                accom.getAvgRating(),
                accom.getReviewCount(),
                accom.getOperationPolicy() != null ? accom.getOperationPolicy().getCheckInTime() : null,
                accom.getOperationPolicy() != null ? accom.getOperationPolicy().getCheckOutTime() : null
        );

        applyReviewSummary(dto);
        return dto;
    }
}
