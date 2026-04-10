package com.Accommodation.service;

import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.ChatbotComparisonItemDto;
import com.Accommodation.dto.ChatbotComparisonResponseDto;
import com.Accommodation.dto.ChatbotRecommendationItemDto;
import com.Accommodation.dto.ChatbotRecommendationResponseDto;
import com.Accommodation.dto.ChatbotSelectableAccomDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.entity.AccomOperationDay;
import com.Accommodation.entity.AccomOperationPolicy;
import com.Accommodation.repository.AccomImgRepository;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.ReviewRepository;
import com.Accommodation.repository.WishRepository;
import com.Accommodation.validation.AccomValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AccomService {

    private final AccomRepository accomRepository;
    private final AccomImgService accomImgService;
    private final AccomImgRepository accomImgRepository;
    private final ReviewRepository reviewRepository;
    private final WishRepository wishRepository;
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

    @Transactional(readOnly = true)
    public List<MainAccomDto> getTransportSelectableAccomList() {
        return accomRepository.findAll().stream()
                .filter(accom -> accom != null && accom.getId() != null)
                .filter(accom -> !Boolean.TRUE.equals(accom.getDeleted()))
                .sorted(Comparator.comparing(Accom::getId).reversed())
                .map(accom -> accomRepository.findWithOperationInfoById(accom.getId()).orElse(accom))
                .map(this::toMainAccomDto)
                .toList();
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

    @Transactional(readOnly = true)
    public ChatbotRecommendationResponseDto getChatbotRecommendations(String query) {
        String safeQuery = query == null ? "" : query.trim();
        List<String> interpretedNeeds = interpretNeeds(safeQuery);
        List<MainAccomDto> candidates = getMainAccomPage(new AccomSearchDto(), PageRequest.of(0, 60)).getContent();

        List<ChatbotRecommendationItemDto> recommendations = candidates.stream()
                .map(candidate -> toRecommendationItem(candidate, safeQuery))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt((ChatbotRecommendationItemDto item) -> item.getReasons().size()).reversed()
                        .thenComparing(ChatbotRecommendationItemDto::getAvgRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ChatbotRecommendationItemDto::getReviewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        if (recommendations.isEmpty()) {
            recommendations = candidates.stream()
                    .sorted(Comparator
                            .comparing(MainAccomDto::getAvgRating, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(MainAccomDto::getReviewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(3)
                    .map(candidate -> new ChatbotRecommendationItemDto(
                            candidate.getId(),
                            candidate.getAccomName(),
                            candidate.getAccomType() != null ? candidate.getAccomType().getLabel() : "숙소",
                            candidate.getGrade() != null ? candidate.getGrade().getNum() : 0,
                            candidate.getLocation(),
                            candidate.getPricePerNight(),
                            candidate.getAvgRating(),
                            candidate.getReviewCount(),
                            candidate.getImgUrl(),
                            List.of("요청과 정확히 일치하는 조건은 적었지만, 평점과 후기 수가 좋은 숙소를 우선 골랐습니다.")
                    ))
                    .toList();
        }

        return new ChatbotRecommendationResponseDto(safeQuery, interpretedNeeds, recommendations);
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
                accom.getGuestCount(),
                accom.getAvgRating(),
                accom.getReviewCount(),
                accom.getOperationPolicy() != null ? accom.getOperationPolicy().getCheckInTime() : null,
                accom.getOperationPolicy() != null ? accom.getOperationPolicy().getCheckOutTime() : null
        );

        applyReviewSummary(dto);
        return dto;
    }

    private List<String> interpretNeeds(String query) {
        if (query == null || query.isBlank()) {
            return List.of("지역, 동행자, 분위기, 숙소 유형을 자유롭게 입력하면 맞춤 추천을 제공합니다.");
        }

        List<String> needs = new ArrayList<>();
        String normalized = normalize(query);

        extractFirstKeyword(normalized, List.of("강릉", "서울", "부산", "제주", "경주", "여수", "속초", "전주", "춘천"))
                .ifPresent(keyword -> needs.add("희망 지역: " + keyword));
        extractFirstKeyword(normalized, List.of("부모", "가족", "아이", "연인", "친구", "혼자"))
                .ifPresent(keyword -> needs.add("동행 유형: " + keyword + " 여행"));
        extractFirstKeyword(normalized, List.of("조용", "오션뷰", "한옥", "감성", "조식", "수영장", "가성비", "럭셔리"))
                .ifPresent(keyword -> needs.add("선호 포인트: " + keyword));
        extractFirstKeyword(normalized, List.of("호텔", "리조트", "펜션", "모텔", "게스트하우스"))
                .ifPresent(keyword -> needs.add("숙소 유형: " + keyword));

        return needs.isEmpty() ? List.of("입력 문장에서 핵심 조건을 넓게 해석해 추천합니다.") : needs;
    }

    private ChatbotRecommendationItemDto toRecommendationItem(MainAccomDto candidate, String query) {
        List<String> reasons = buildRecommendationReasons(candidate, query);
        if (query != null && !query.isBlank() && reasons.isEmpty()) {
            return null;
        }

        return new ChatbotRecommendationItemDto(
                candidate.getId(),
                candidate.getAccomName(),
                candidate.getAccomType() != null ? candidate.getAccomType().getLabel() : "숙소",
                candidate.getGrade() != null ? candidate.getGrade().getNum() : 0,
                candidate.getLocation(),
                candidate.getPricePerNight(),
                candidate.getAvgRating(),
                candidate.getReviewCount(),
                candidate.getImgUrl(),
                reasons.isEmpty() ? List.of("기본 추천 기준으로 평점과 후기 수가 좋은 숙소입니다.") : reasons
        );
    }

    private List<String> buildRecommendationReasons(MainAccomDto candidate, String query) {
        String normalizedQuery = normalize(query);
        String location = normalize(candidate.getLocation());
        String detail = normalize(candidate.getAccomDetail());
        String name = normalize(candidate.getAccomName());
        String type = candidate.getAccomType() != null ? normalize(candidate.getAccomType().getLabel()) : "";

        List<String> reasons = new ArrayList<>();

        if (!normalizedQuery.isBlank()) {
            List<String> locationKeywords = Arrays.asList("강릉", "서울", "부산", "제주", "경주", "여수", "속초", "전주", "춘천");
            boolean queryHasLocation = locationKeywords.stream()
                    .anyMatch(keyword -> normalizedQuery.contains(normalize(keyword)));
            boolean candidateMatchesLocation = locationKeywords.stream()
                    .anyMatch(keyword -> normalizedQuery.contains(normalize(keyword)) && location.contains(normalize(keyword)));

            if (queryHasLocation && !candidateMatchesLocation) {
                return List.of();
            }

            locationKeywords.forEach(keyword -> {
                if (normalizedQuery.contains(normalize(keyword)) && location.contains(normalize(keyword))) {
                    reasons.add(keyword + " 지역 조건과 맞습니다.");
                }
            });

            if (normalizedQuery.contains("한옥") && (detail.contains("한옥") || name.contains("한옥"))) {
                reasons.add("한옥 스테이 키워드와 일치합니다.");
            }
            if (normalizedQuery.contains("조용") && (detail.contains("조용") || detail.contains("프라이빗") || detail.contains("힐링"))) {
                reasons.add("조용하거나 프라이빗한 분위기 설명이 있습니다.");
            }
            if (normalizedQuery.contains("조식") && detail.contains("조식")) {
                reasons.add("조식 관련 설명이 포함되어 있습니다.");
            }
            if ((normalizedQuery.contains("부모") || normalizedQuery.contains("가족")) && candidate.getGuestCount() != null && candidate.getGuestCount() >= 3) {
                reasons.add("가족 단위 숙박에 맞는 수용 인원을 갖췄습니다.");
            }
            if (normalizedQuery.contains("가성비") && candidate.getPricePerNight() != null && candidate.getPricePerNight() <= 120000) {
                reasons.add("가격대가 비교적 부담이 적습니다.");
            }
            if (normalizedQuery.contains("럭셔리") && candidate.getGrade() != null && candidate.getGrade().getNum() >= 4) {
                reasons.add("상위 등급 숙소입니다.");
            }
            if (!type.isBlank() && normalizedQuery.contains(type)) {
                reasons.add("원하는 숙소 유형과 맞습니다.");
            }
        }

        if (candidate.getAvgRating() != null && candidate.getAvgRating() >= 4.5) {
            reasons.add("평점이 높은 편입니다.");
        }
        if (candidate.getReviewCount() != null && candidate.getReviewCount() >= 10) {
            reasons.add("후기 수가 충분해 검토 근거가 있습니다.");
        }

        return reasons.stream().distinct().toList();
    }


    private String formatTime(LocalTime time) {
        return time != null ? time.toString() : "정보 없음";
    }

    @Transactional(readOnly = true)
    public List<ChatbotSelectableAccomDto> getSelectableAccoms(String email, String recentViewedCookie) {
        Map<Long, ChatbotSelectableAccomDto> map = new LinkedHashMap<>();

        if (email != null) {
            wishRepository.findWishListDtosByMemberEmailOrderByRegTimeDesc(email).stream()
                    .limit(8)
                    .forEach(item -> map.put(item.getAccomId(), new ChatbotSelectableAccomDto(
                            item.getAccomId(),
                            item.getAccomName(),
                            item.getAccomType() != null ? item.getAccomType().getLabel() : "숙소",
                            item.getGrade() != null ? item.getGrade().getNum() : 0,
                            item.getLocation(),
                            item.getPricePerNight(),
                            item.getAvgRating(),
                            item.getReviewCount(),
                            item.getImgUrl(),
                            "wish"
                    )));
        }

        if (recentViewedCookie != null && !recentViewedCookie.isBlank()) {
            List<Long> ids = Arrays.stream(recentViewedCookie.split("[,-]"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                    .filter(Objects::nonNull)
                    .limit(8)
                    .toList();
            getRecentViewedAccomList(ids).stream()
                    .filter(item -> !map.containsKey(item.getId()))
                    .forEach(item -> map.put(item.getId(), new ChatbotSelectableAccomDto(
                            item.getId(),
                            item.getAccomName(),
                            item.getAccomType() != null ? item.getAccomType().getLabel() : "숙소",
                            item.getGrade() != null ? item.getGrade().getNum() : 0,
                            item.getLocation(),
                            item.getPricePerNight(),
                            item.getAvgRating(),
                            item.getReviewCount(),
                            item.getImgUrl(),
                            "recent"
                    )));
        }

        return new ArrayList<>(map.values()).stream().limit(12).toList();
    }

    @Transactional(readOnly = true)
    public ChatbotComparisonResponseDto compareChatbotAccoms(Long leftId, Long rightId) {
        MainAccomDto left = accomRepository.findWithOperationInfoById(leftId)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .map(this::toMainAccomDto)
                .orElse(null);
        MainAccomDto right = accomRepository.findWithOperationInfoById(rightId)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .map(this::toMainAccomDto)
                .orElse(null);

        ChatbotComparisonItemDto leftItem = left != null ? toComparisonItem(left) : null;
        ChatbotComparisonItemDto rightItem = right != null ? toComparisonItem(right) : null;

        List<String> comparisonPoints = new ArrayList<>();
        if (left != null && right != null) {
            comparisonPoints.add(comparePrice(left, right));
            comparisonPoints.add(compareRating(left, right));
            comparisonPoints.add(compareCapacity(left, right));
        }

        return new ChatbotComparisonResponseDto(
                left != null ? left.getAccomName() : "-",
                right != null ? right.getAccomName() : "-",
                leftItem,
                rightItem,
                comparisonPoints,
                List.of()
        );
    }

    private ChatbotComparisonItemDto toComparisonItem(MainAccomDto item) {
        return new ChatbotComparisonItemDto(
                item.getId(),
                item.getAccomName(),
                item.getAccomType() != null ? item.getAccomType().getLabel() : "숙소",
                item.getGrade() != null ? item.getGrade().getNum() : 0,
                item.getLocation(),
                item.getPricePerNight(),
                item.getAvgRating(),
                item.getReviewCount(),
                item.getRoomCount(),
                item.getGuestCount(),
                formatTime(item.getCheckInTime()),
                formatTime(item.getCheckOutTime()),
                null
        );
    }

    private String comparePrice(MainAccomDto left, MainAccomDto right) {
        int l = left.getPricePerNight() != null ? left.getPricePerNight() : 0;
        int r = right.getPricePerNight() != null ? right.getPricePerNight() : 0;
        if (l == r) return "가격이 동일합니다.";
        MainAccomDto cheaper = l < r ? left : right;
        return cheaper.getAccomName() + "이(가) 1박 기준 " + String.format("%,d", Math.abs(l - r)) + "원 더 저렴합니다.";
    }

    private String compareRating(MainAccomDto left, MainAccomDto right) {
        double l = left.getAvgRating() != null ? left.getAvgRating() : 0.0;
        double r = right.getAvgRating() != null ? right.getAvgRating() : 0.0;
        if (Double.compare(l, r) == 0) return "평점이 동일합니다.";
        return (l > r ? left : right).getAccomName() + "이(가) 평점이 더 높습니다.";
    }

    private String compareCapacity(MainAccomDto left, MainAccomDto right) {
        int l = left.getGuestCount() != null ? left.getGuestCount() : 0;
        int r = right.getGuestCount() != null ? right.getGuestCount() : 0;
        if (l == r) return "수용 인원이 동일합니다.";
        return (l > r ? left : right).getAccomName() + "이(가) 더 많은 인원을 수용합니다.";
    }

    private Optional<String> extractFirstKeyword(String text, List<String> keywords) {
        return keywords.stream()
                .filter(keyword -> text.contains(normalize(keyword)))
                .findFirst();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.KOREA).replaceAll("\\s+", "");
    }
}
