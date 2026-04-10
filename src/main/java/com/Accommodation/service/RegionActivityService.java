package com.Accommodation.service;

import com.Accommodation.dto.ChatbotActivityItemDto;
import com.Accommodation.dto.RegionActivityItemDto;
import com.Accommodation.dto.RegionActivityPageDto;
import com.Accommodation.dto.RegionFeaturedCardDto;
import com.Accommodation.dto.RegionFeaturedSectionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class RegionActivityService {

    private final RestClient restClient;
    private final String serviceKey;
    private final String baseUrl;
    private final String mobileApp;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long CACHE_TTL_MILLIS = Duration.ofMinutes(10).toMillis();

    private final Map<String, CachedRegionActivityPage> pageCache = new ConcurrentHashMap<>();

    private static final List<RegionConfig> REGION_CONFIGS = List.of(
            new RegionConfig("서울", "/images/main/seoul.png", "서울 여행에 추천", "서울에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("부산", "/images/main/busan.png", "부산 여행에 추천", "부산에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("강릉", "/images/main/gangneung.png", "강릉 여행에 추천", "강릉에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("제주", "/images/main/jeju.png", "제주 여행에 추천", "제주에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("경주", "/images/main/gyeongju.png", "경주 여행에 추천", "경주에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다.")
    );

    private static final Map<String, RegionConfig> REGION_MAP = createRegionMap();

    private static final Map<String, List<String>> PRIORITY_SPOTS = Map.of(
            "서울", List.of("경복궁", "남산타워", "남산 서울타워", "명동", "북촌한옥마을", "창덕궁", "광화문", "인사동", "롯데월드", "청계천"),
            "부산", List.of("해운대", "광안리", "감천문화마을", "태종대", "송도해수욕장", "송정", "자갈치시장", "국제시장", "용궁사", "부산타워"),
            "강릉", List.of("경포대", "경포해변", "안목해변", "안목", "주문진", "오죽헌", "정동진", "강문해변", "선교장", "경포호"),
            "제주", List.of("성산일출봉", "한라산", "우도", "협재해수욕장", "협재", "중문관광단지", "섭지코지", "용두암", "천지연폭포", "비자림"),
            "경주", List.of("불국사", "첨성대", "동궁과 월지", "동궁월지", "황리단길", "대릉원", "석굴암", "월정교", "경주월드", "보문호")
    );

    public RegionActivityService(
            @Value("${tour.api.service-key:}") String serviceKey,
            @Value("${tour.api.base-url:https://apis.data.go.kr/B551011/KorService2}") String baseUrl,
            @Value("${tour.api.mobile-app:Accommodation}") String mobileApp
    ) {
        this.restClient = RestClient.builder().build();
        this.serviceKey = serviceKey;
        this.baseUrl = baseUrl;
        this.mobileApp = mobileApp;
    }

    public RegionActivityPageDto getRegionActivityPage(String regionName) {
        RegionConfig config = REGION_MAP.get(regionName);
        if (config == null) {
            config = REGION_MAP.get("서울");
        }

        String cacheKey = config.regionName();
        long now = System.currentTimeMillis();

        CachedRegionActivityPage cached = pageCache.get(cacheKey);
        if (cached != null && (now - cached.cachedAt()) < CACHE_TTL_MILLIS) {
            return cached.pageDto();
        }

        List<RegionActivityItemDto> items = searchActivities(config.regionName(), 60);

        RegionActivityPageDto pageDto = new RegionActivityPageDto(
                config.regionName(),
                config.imagePath(),
                config.headline(),
                config.description(),
                getFeaturedSection(config.regionName()),
                items
        );

        pageCache.put(cacheKey, new CachedRegionActivityPage(pageDto, now));
        return pageDto;
    }

    private RegionFeaturedSectionDto getFeaturedSection(String regionName) {
        return switch (regionName) {
            case "서울" -> new RegionFeaturedSectionDto(
                    "서울 여행에 추천",
                    "서울 공식 관광 사이트에서 바로 이동할 수 있는 행사·여행지 링크입니다.",
                    List.of(
                            new RegionFeaturedCardDto("비짓서울 메인", "서울 공식 관광 포털에서 최신 관광 정보를 확인하세요.", "/images/main/seoul.png", "https://korean.visitseoul.net/"),
                            new RegionFeaturedCardDto("서울 축제·행사 검색", "비짓서울에서 서울 축제와 이벤트 결과를 바로 확인합니다.", "/images/main/sample1.png", "https://korean.visitseoul.net/search?lang=ko&searchTerm=%EC%84%9C%EC%9A%B8%20%EC%B6%95%EC%A0%9C&search_radio=T"),
                            new RegionFeaturedCardDto("서울 야경 여행", "서울 야간 명소와 여행지를 공식 검색 결과로 연결합니다.", "/images/main/sample2.png", "https://korean.visitseoul.net/search?lang=ko&searchTerm=%EC%84%9C%EC%9A%B8%20%EC%95%BC%EA%B2%BD&search_radio=T"),
                            new RegionFeaturedCardDto("한강 여행·체험", "한강 중심의 서울 여행지와 체험 콘텐츠를 찾습니다.", "/images/main/sample3.png", "https://korean.visitseoul.net/search?lang=ko&searchTerm=%ED%95%9C%EA%B0%95&search_radio=T")
                    )
            );
            case "부산" -> new RegionFeaturedSectionDto(
                    "부산 여행에 추천",
                    "부산 공식 관광 포털에서 바로 이동할 수 있는 행사·투어·즐길거리 링크입니다.",
                    List.of(
                            new RegionFeaturedCardDto("비짓부산 메인", "부산 공식 관광 포털에서 최신 행사와 여행 코스를 확인하세요.", "/images/main/busan.png", "https://www.visitbusan.net/kr/index.do"),
                            new RegionFeaturedCardDto("부산 축제 목록", "비짓부산의 공식 축제 카테고리 목록으로 이동합니다.", "/images/main/sample1.png", "https://www.visitbusan.net/index.do?menuCd=DOM_000000201005000000"),
                            new RegionFeaturedCardDto("부산 축제·행사 일정", "현재 진행 중인 부산 공식 행사 일정을 확인합니다.", "/images/main/sample2.png", "https://www.visitbusan.net/schedule/list.do?boardId=BBS_0000009&menuCd=DOM_000000204012000000&contentsSid=447"),
                            new RegionFeaturedCardDto("부산 일정여행 추천", "부산 공식 추천 일정여행 페이지로 이동합니다.", "/images/main/sample3.png", "https://www.visitbusan.net/index.do?menuCd=DOM_000000202012000000"),
                            new RegionFeaturedCardDto("부산 체험·해양·웰니스", "체험형 여행과 해양·웰니스 콘텐츠를 공식 페이지에서 확인합니다.", "/images/main/sample1.png", "https://www.visitbusan.net/index.do?menuCd=DOM_000000202008000000")
                    )
            );
            case "강릉" -> new RegionFeaturedSectionDto(
                    "강릉 여행에 추천",
                    "강릉 공식 관광 사이트에서 바로 이동할 수 있는 여행·행사 링크입니다.",
                    List.of(
                            new RegionFeaturedCardDto("비짓강릉 메인", "강릉 공식 관광 메인 페이지입니다.", "/images/main/gangneung.png", "https://visitgangneung.net/"),
                            new RegionFeaturedCardDto("강릉 테마여행", "강릉 공식 테마여행 페이지로 이동합니다.", "/images/main/sample1.png", "https://visitgangneung.net/en/tourtheme.do"),
                            new RegionFeaturedCardDto("강릉 축제·이벤트", "강릉 공식 축제/이벤트 페이지로 이동합니다.", "/images/main/sample2.png", "https://visitgangneung.net/en/gnevent.do"),
                            new RegionFeaturedCardDto("강릉 체험·레포츠", "강릉 공식 체험·레포츠 페이지입니다.", "/images/main/sample3.png", "https://visitgangneung.net/en/leisure_sports.do"),
                            new RegionFeaturedCardDto("강릉 여행안내 이벤트", "강릉 여행안내 공지/이벤트 페이지입니다.", "/images/main/sample1.png", "https://visitgangneung.net/en/eventnotice.do")
                    )
            );
            case "제주" -> new RegionFeaturedSectionDto(
                    "제주 여행에 추천",
                    "제주 공식 관광 포털에서 바로 이동할 수 있는 행사·여행 링크입니다.",
                    List.of(
                            new RegionFeaturedCardDto("비짓제주 메인", "제주 공식 관광 포털에서 여행 테마를 확인하세요.", "/images/main/jeju.png", "https://www.visitjeju.net/kr"),
                            new RegionFeaturedCardDto("제주 축제·행사", "비짓제주의 월별 축제·행사 목록으로 이동합니다.", "/images/main/sample1.png", "https://www.visitjeju.net/kr/festival/list?menuId=DOM_000001718007000000"),
                            new RegionFeaturedCardDto("제주 테마여행", "비짓제주의 공식 테마여행 목록으로 이동합니다.", "/images/main/sample2.png", "https://www.visitjeju.net/kr/themtour/list?cate1cd=cate0000001340&menuId=DOM_000002000000000221"),
                            new RegionFeaturedCardDto("제주 추천 여행기사", "비짓제주의 실제 추천 여행 상세 페이지입니다.", "/images/main/sample3.png", "https://www.visitjeju.net/kr/themtour/view?contentsid=CNTS_200000000015347")
                    )
            );
            case "경주" -> new RegionFeaturedSectionDto(
                    "경주 여행에 추천",
                    "경주 공식 관광 사이트에서 바로 이동할 수 있는 축제·코스·체험 링크입니다.",
                    List.of(
                            new RegionFeaturedCardDto("경주문화관광 메인", "경주 공식 관광 페이지에서 주요 여행 정보를 확인하세요.", "/images/main/gyeongju.png", "https://www.gyeongju.go.kr/tour/index.do"),
                            new RegionFeaturedCardDto("경주 핵심 여행코스", "경주 공식 핵심 여행코스 페이지입니다.", "/images/main/sample1.png", "https://www.gyeongju.go.kr/tour/page.do?mnu_uid=2297"),
                            new RegionFeaturedCardDto("경주 10Pick", "경주 공식 테마여행 10Pick 페이지입니다.", "/images/main/sample2.png", "https://www.gyeongju.go.kr/tour/page.do?mnu_uid=2552"),
                            new RegionFeaturedCardDto("경주 전통문화체험", "경주 공식 체험과 레저 페이지입니다.", "/images/main/sample3.png", "https://www.gyeongju.go.kr/tour/page.do?mnu_uid=2318"),
                            new RegionFeaturedCardDto("경주 축제·행사", "경주 공식 문화행사 페이지입니다.", "/images/main/sample1.png", "https://www.gyeongju.go.kr/tour/page.do?code_uid=300&mnu_uid=2393")
                    )
            );
            default -> new RegionFeaturedSectionDto(regionName + " 여행에 추천", "대표 즐길거리 링크를 모았습니다.", List.of());
        };
    }

    private List<RegionActivityItemDto> searchActivities(String regionName, int size) {
        if (!StringUtils.hasText(serviceKey)) {
            log.warn("Tour API serviceKey가 비어 있습니다.");
            return List.of();
        }

        Map<String, RegionActivityItemDto> merged = new LinkedHashMap<>();

        List<RegionActivityItemDto> festivalItems = searchFestival(regionName, 15);
        for (RegionActivityItemDto item : festivalItems) {
            merged.putIfAbsent(buildMergeKey(item), item);
        }

        List<RegionActivityItemDto> areaItems = searchAreaBased(regionName, 45);
        for (RegionActivityItemDto item : areaItems) {
            merged.putIfAbsent(buildMergeKey(item), item);
        }

        List<RegionActivityItemDto> sorted = sortByPriority(regionName, new ArrayList<>(merged.values()));

        if (sorted.size() > size) {
            return sorted.subList(0, size);
        }
        return sorted;
    }

    private List<RegionActivityItemDto> searchFestival(String regionName, int size) {
        AreaCode areaCode = getAreaCode(regionName);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(baseUrl + "/searchFestival2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("arrange", "Q")
                    .queryParam("numOfRows", size)
                    .queryParam("pageNo", 1)
                    .queryParam("eventStartDate", LocalDate.now().format(DATE_FORMATTER));

            if (StringUtils.hasText(areaCode.lDongRegnCd())) {
                builder.queryParam("lDongRegnCd", areaCode.lDongRegnCd());
            }
            if (StringUtils.hasText(areaCode.lDongSignguCd())) {
                builder.queryParam("lDongSignguCd", areaCode.lDongSignguCd());
            }

            String requestUrl = builder.encode().build().toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> rawItems = extractItems(response);
            List<RegionActivityItemDto> result = new ArrayList<>();

            for (Map<String, Object> raw : rawItems) {
                String title = stringValue(raw.get("title"));
                if (!StringUtils.hasText(title)) {
                    continue;
                }

                String address = joinAddress(
                        stringValue(raw.get("addr1")),
                        stringValue(raw.get("addr2"))
                );

                if (!containsRegion(regionName, title, address)) {
                    continue;
                }

                String imageUrl = firstNotBlank(
                        stringValue(raw.get("firstimage")),
                        stringValue(raw.get("firstimage2"))
                );

                result.add(RegionActivityItemDto.builder()
                        .title(title)
                        .imageUrl(imageUrl)
                        .address(address)
                        .period(formatPeriod(
                                stringValue(raw.get("eventstartdate")),
                                stringValue(raw.get("eventenddate"))
                        ))
                        .detailUrl(buildVisitKoreaSearchUrl(title, regionName))
                        .externalUrl(buildVisitKoreaSearchUrl(title, regionName))
                        .category("행사/축제")
                        .tel(stringValue(raw.get("tel")))
                        .regionName(regionName)
                        .build());
            }

            return result;
        } catch (Exception e) {
            log.error("TourAPI searchFestival2 호출 실패 - region={}", regionName, e);
            return List.of();
        }
    }

    private List<RegionActivityItemDto> searchAreaBased(String regionName, int size) {
        AreaCode areaCode = getAreaCode(regionName);

        if (!StringUtils.hasText(areaCode.lDongRegnCd())) {
            return List.of();
        }

        List<RegionActivityItemDto> result = new ArrayList<>();

        // 여행코스(25)는 제외
        List<String> contentTypeIds = List.of("12", "14", "28");
        int rowsPerType = Math.max(8, size / contentTypeIds.size());

        for (String contentTypeId : contentTypeIds) {
            try {
                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromUriString(baseUrl + "/areaBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("arrange", "Q")
                        .queryParam("numOfRows", rowsPerType)
                        .queryParam("pageNo", 1)
                        .queryParam("contentTypeId", contentTypeId)
                        .queryParam("lDongRegnCd", areaCode.lDongRegnCd());

                if (StringUtils.hasText(areaCode.lDongSignguCd())) {
                    builder.queryParam("lDongSignguCd", areaCode.lDongSignguCd());
                }

                String requestUrl = builder.encode().build().toUriString();

                Map<String, Object> response = restClient.get()
                        .uri(requestUrl)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                List<Map<String, Object>> rawItems = extractItems(response);

                for (Map<String, Object> raw : rawItems) {
                    String title = stringValue(raw.get("title"));
                    if (!StringUtils.hasText(title)) {
                        continue;
                    }

                    String address = joinAddress(
                            stringValue(raw.get("addr1")),
                            stringValue(raw.get("addr2"))
                    );

                    if (!containsRegion(regionName, title, address)) {
                        continue;
                    }

                    String imageUrl = firstNotBlank(
                            stringValue(raw.get("firstimage")),
                            stringValue(raw.get("firstimage2"))
                    );

                    result.add(RegionActivityItemDto.builder()
                            .title(title)
                            .imageUrl(imageUrl)
                            .address(address)
                            .period("")
                            .detailUrl(buildVisitKoreaSearchUrl(title, regionName))
                            .externalUrl(buildVisitKoreaSearchUrl(title, regionName))
                            .category(resolveCategory(contentTypeId))
                            .tel(stringValue(raw.get("tel")))
                            .regionName(regionName)
                            .build());
                }
            } catch (Exception e) {
                log.warn("TourAPI areaBasedList2 호출 실패 - region={}, contentTypeId={}", regionName, contentTypeId, e);
            }
        }

        return result;
    }

    private List<RegionActivityItemDto> sortByPriority(String regionName, List<RegionActivityItemDto> items) {
        List<String> prioritySpots = PRIORITY_SPOTS.getOrDefault(regionName, List.of());

        return items.stream()
                .sorted(
                        Comparator
                                .comparingInt((RegionActivityItemDto item) -> getPrioritySpotIndex(item, prioritySpots))
                                .thenComparingInt(this::getCategoryRank)
                                .thenComparing(item -> safe(item.getTitle()))
                )
                .toList();
    }

    private int getPrioritySpotIndex(RegionActivityItemDto item, List<String> prioritySpots) {
        String target = (safe(item.getTitle()) + " " + safe(item.getAddress())).replace(" ", "");

        for (int i = 0; i < prioritySpots.size(); i++) {
            String keyword = prioritySpots.get(i).replace(" ", "");
            if (target.contains(keyword)) {
                return i;
            }
        }

        return 999;
    }

    private int getCategoryRank(RegionActivityItemDto item) {
        String category = safe(item.getCategory());

        if ("행사/축제".equals(category)) {
            return 0;
        }
        if ("관광지".equals(category)) {
            return 1;
        }
        if ("문화시설".equals(category)) {
            return 2;
        }
        if ("레포츠".equals(category)) {
            return 3;
        }
        return 9;
    }

    private String buildMergeKey(RegionActivityItemDto item) {
        return (safe(item.getTitle()) + "|" + safe(item.getAddress())).toLowerCase();
    }

    private AreaCode getAreaCode(String regionName) {
        return switch (regionName) {
            case "서울" -> new AreaCode("11", "");
            case "부산" -> new AreaCode("26", "");
            // 강릉은 시군구 코드까지 고정하면 Tour API 결과가 비는 경우가 있어
            // 강원권으로 넓게 조회한 뒤 제목/주소의 "강릉" 포함 여부로 다시 필터링한다.
            case "강릉" -> new AreaCode("51", "");
            case "제주" -> new AreaCode("50", "");
            case "경주" -> new AreaCode("47", "130");
            default -> new AreaCode("", "");
        };
    }

    private String resolveCategory(String contentTypeId) {
        return switch (contentTypeId) {
            case "12" -> "관광지";
            case "14" -> "문화시설";
            case "15" -> "행사/축제";
            case "28" -> "레포츠";
            default -> "즐길거리";
        };
    }

    private String buildVisitKoreaSearchUrl(String title, String regionName) {
        String keyword = firstNotBlank(title, regionName);

        if (!StringUtils.hasText(keyword)) {
            return "https://korean.visitkorea.or.kr/";
        }

        return UriComponentsBuilder
                .fromUriString("https://korean.visitkorea.or.kr/search/search_list.do")
                .queryParam("keyword", keyword)
                .build()
                .encode()
                .toUriString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }

        Object responseObj = response.get("response");
        if (!(responseObj instanceof Map<?, ?> responseMap)) {
            return List.of();
        }

        Object bodyObj = responseMap.get("body");
        if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
            return List.of();
        }

        Object itemsObj = bodyMap.get("items");
        if (!(itemsObj instanceof Map<?, ?> itemsMap)) {
            return List.of();
        }

        Object itemObj = itemsMap.get("item");

        if (itemObj instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }

        if (itemObj instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }

        return List.of();
    }

    private boolean containsRegion(String regionName, String title, String address) {
        String target = (safe(title) + " " + safe(address)).replace(" ", "");
        String region = safe(regionName).replace(" ", "");

        if (!StringUtils.hasText(region)) {
            return false;
        }

        if (target.contains(region)) {
            return true;
        }

        // 강릉은 강원 전체 조회 후 다시 필터링하므로,
        // 대표 관광지 키워드도 함께 허용해야 결과가 안정적으로 노출된다.
        if ("강릉".equals(regionName)) {
            List<String> gangneungKeywords = List.of(
                    "강릉", "경포", "경포대", "경포해변", "안목", "안목해변",
                    "주문진", "정동진", "강문", "오죽헌", "선교장"
            );

            for (String keyword : gangneungKeywords) {
                String normalizedKeyword = keyword.replace(" ", "");
                if (target.contains(normalizedKeyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String formatPeriod(String startDate, String endDate) {
        if (!StringUtils.hasText(startDate) && !StringUtils.hasText(endDate)) {
            return "";
        }

        String formattedStart = formatDate(startDate);
        String formattedEnd = formatDate(endDate);

        if (StringUtils.hasText(formattedStart) && StringUtils.hasText(formattedEnd)) {
            return formattedStart + " ~ " + formattedEnd;
        }

        return firstNotBlank(formattedStart, formattedEnd);
    }

    private String formatDate(String value) {
        if (!StringUtils.hasText(value) || value.length() < 8) {
            return "";
        }
        return value.substring(0, 4) + "." + value.substring(4, 6) + "." + value.substring(6, 8);
    }

    private String joinAddress(String addr1, String addr2) {
        if (StringUtils.hasText(addr1) && StringUtils.hasText(addr2)) {
            return addr1 + " " + addr2;
        }
        if (StringUtils.hasText(addr1)) {
            return addr1;
        }
        return firstNotBlank(addr2);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, RegionConfig> createRegionMap() {
        Map<String, RegionConfig> map = new LinkedHashMap<>();
        for (RegionConfig config : REGION_CONFIGS) {
            map.put(config.regionName(), config);
        }
        return map;
    }

    private record RegionConfig(
            String regionName,
            String imagePath,
            String headline,
            String description
    ) {
    }

    private record AreaCode(
            String lDongRegnCd,
            String lDongSignguCd
    ) {
    }

    private record CachedRegionActivityPage(
            RegionActivityPageDto pageDto,
            long cachedAt
    ) {
    }

    // ── 챗봇용 즐길거리 추천 ───────────────────────────────────────────────

    private static final Map<String, List<String>> KEYWORD_TERMS = Map.of(
            "자연/힐링",    List.of("자연", "힐링", "공원", "산", "바다", "숲", "폭포", "해변", "해수욕", "호수", "계곡"),
            "문화/역사",    List.of("문화", "역사", "박물관", "미술관", "유적", "궁", "성", "사찰", "전시", "고궁"),
            "액티브/레포츠", List.of("레포츠", "스포츠", "체험", "서핑", "카약", "클라이밍", "스키", "수상", "낚시"),
            "축제/행사",    List.of("축제", "행사", "이벤트", "페스티벌", "공연", "콘서트", "마켓", "불꽃"),
            "야경/감성",    List.of("야경", "감성", "뷰", "전망", "타워", "다리", "포토", "일출", "일몰"),
            "음식/체험",    List.of("음식", "맛", "시장", "먹거리", "체험", "공방", "만들기", "전통", "음식점")
    );

    private static final Map<String, String> KEYWORD_CATEGORY = Map.of(
            "자연/힐링",    "관광지",
            "문화/역사",    "문화시설",
            "액티브/레포츠", "레포츠",
            "축제/행사",    "행사/축제",
            "야경/감성",    "관광지",
            "음식/체험",    "관광지"
    );

    public List<ChatbotActivityItemDto> getChatbotActivities(String keyword, String region) {
        String safeRegion = (region == null || region.isBlank()) ? "서울" : region;
        String safeKeyword = (keyword == null) ? "" : keyword;

        RegionActivityPageDto page = getRegionActivityPage(safeRegion);
        List<RegionActivityItemDto> items = page.getItems();

        List<String> terms = KEYWORD_TERMS.getOrDefault(safeKeyword, List.of());
        String preferredCategory = KEYWORD_CATEGORY.getOrDefault(safeKeyword, "");

        LocalDate today = LocalDate.now();

        List<ChatbotActivityItemDto> festivals = new ArrayList<>();
        List<ChatbotActivityItemDto> others = new ArrayList<>();

        for (RegionActivityItemDto item : items) {
            boolean isFestival = "행사/축제".equals(item.getCategory())
                    && item.getPeriod() != null && !item.getPeriod().isBlank();
            boolean ongoing = isFestival && isCurrentlyOngoing(item.getPeriod(), today);
            int score = scoreItem(item, terms, preferredCategory);

            ChatbotActivityItemDto dto = new ChatbotActivityItemDto(
                    item.getTitle(),
                    item.getImageUrl(),
                    item.getAddress(),
                    item.getPeriod(),
                    item.getCategory(),
                    item.getExternalUrl() != null ? item.getExternalUrl() : item.getDetailUrl(),
                    ongoing,
                    score
            );

            if (isFestival) {
                festivals.add(dto);
            } else {
                others.add(dto);
            }
        }

        festivals.sort(Comparator.comparingInt(ChatbotActivityItemDto::getScore).reversed());
        others.sort(Comparator.comparingInt(ChatbotActivityItemDto::getScore).reversed());

        return Stream.concat(
                festivals.stream().limit(3),
                others.stream().limit(6)
        ).limit(9).toList();
    }

    private int scoreItem(RegionActivityItemDto item, List<String> terms, String preferredCategory) {
        int score = 0;
        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase();
        String address = item.getAddress() == null ? "" : item.getAddress().toLowerCase();
        String category = item.getCategory() == null ? "" : item.getCategory();

        if (!preferredCategory.isBlank() && preferredCategory.equals(category)) {
            score += 10;
        }
        for (String term : terms) {
            String t = term.toLowerCase();
            if (title.contains(t)) score += 5;
            if (address.contains(t)) score += 1;
        }
        if (item.getPeriod() != null && !item.getPeriod().isBlank()) {
            score += 3;
        }
        return score;
    }

    private boolean isCurrentlyOngoing(String period, LocalDate today) {
        if (period == null || period.isBlank()) return false;
        try {
            // "2026.04.01 ~ 2026.04.30" 형식 파싱
            String[] parts = period.split("~");
            if (parts.length < 2) return false;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            LocalDate start = LocalDate.parse(parts[0].trim(), fmt);
            LocalDate end = LocalDate.parse(parts[1].trim(), fmt);
            return !today.isBefore(start) && !today.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }
}
