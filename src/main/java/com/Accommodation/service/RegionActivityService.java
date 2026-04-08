package com.Accommodation.service;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class RegionActivityService {

    private final RestClient restClient;
    private final String serviceKey;
    private final String baseUrl;
    private final String mobileApp;

    private static final Pattern LINK_PATTERN =
            Pattern.compile("href\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_QUOTE_LINK_PATTERN =
            Pattern.compile("href\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://[^\\s<>\"']+)", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final List<RegionConfig> REGION_CONFIGS = List.of(
            new RegionConfig("서울", "/images/main/seoul.png", "서울 여행에 추천", "서울에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("부산", "/images/main/busan.png", "부산 여행에 추천", "부산에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("강릉", "/images/main/gangneung.png", "강릉 여행에 추천", "강릉에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("제주", "/images/main/jeju.png", "제주 여행에 추천", "제주에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다."),
            new RegionConfig("경주", "/images/main/gyeongju.png", "경주 여행에 추천", "경주에서 즐길 수 있는 행사, 축제, 대표 관광지를 모아봤습니다.")
    );

    private static final Map<String, RegionConfig> REGION_MAP = createRegionMap();

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

        List<RegionActivityItemDto> items = searchActivities(config.regionName(), 20);

        return new RegionActivityPageDto(
                config.regionName(),
                config.imagePath(),
                config.headline(),
                config.description(),
                getFeaturedSection(config.regionName()),
                items
        );
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

        List<RegionActivityItemDto> festivalItems = searchFestival(regionName, 12);
        for (RegionActivityItemDto item : festivalItems) {
            merged.putIfAbsent(buildMergeKey(item), item);
        }

        List<RegionActivityItemDto> areaItems = searchAreaBased(regionName, 20);
        for (RegionActivityItemDto item : areaItems) {
            merged.putIfAbsent(buildMergeKey(item), item);
        }

        List<String> regionKeywords = getRegionKeywords(regionName);
        for (String keyword : regionKeywords) {
            List<RegionActivityItemDto> items = searchByKeyword(keyword, 8);
            for (RegionActivityItemDto item : items) {
                merged.putIfAbsent(buildMergeKey(item), item);
            }
        }

        return merged.values().stream()
                .limit(size)
                .toList();
    }

    private List<String> getRegionKeywords(String regionName) {
        return switch (regionName) {
            case "서울" -> List.of(
                    "서울 축제",
                    "서울 전시",
                    "서울 공연",
                    "서울 한강",
                    "서울 야경",
                    "서울 체험"
            );
            case "부산" -> List.of(
                    "부산 축제",
                    "부산 해운대",
                    "부산 광안리",
                    "부산 야경",
                    "부산 체험",
                    "부산 공연"
            );
            case "강릉" -> List.of(
                    "강릉 축제",
                    "강릉 단오",
                    "강릉 바다",
                    "강릉 체험",
                    "강릉 커피",
                    "강릉 관광"
            );
            case "제주" -> List.of(
                    "제주 축제",
                    "제주 오름",
                    "제주 유채꽃",
                    "제주 체험",
                    "제주 관광",
                    "제주 전시"
            );
            case "경주" -> List.of(
                    "경주 축제",
                    "경주 벚꽃",
                    "경주 문화유산",
                    "경주 체험",
                    "경주 공연",
                    "경주 관광"
            );
            default -> List.of(
                    regionName + " 축제",
                    regionName + " 관광",
                    regionName + " 체험"
            );
        };
    }

    private String buildMergeKey(RegionActivityItemDto item) {
        String title = item.getTitle() == null ? "" : item.getTitle().trim();
        String externalUrl = item.getExternalUrl() == null ? "" : item.getExternalUrl().trim();
        String detailUrl = item.getDetailUrl() == null ? "" : item.getDetailUrl().trim();
        return title + "|" + externalUrl + "|" + detailUrl;
    }

    private List<RegionActivityItemDto> searchFestival(String regionName, int size) {
        try {
            String startDate = LocalDate.now().format(DATE_FORMATTER);
            String endDate = LocalDate.now().plusMonths(6).format(DATE_FORMATTER);

            String requestUrl = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/searchFestival2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("arrange", "Q")
                    .queryParam("numOfRows", size)
                    .queryParam("pageNo", 1)
                    .queryParam("eventStartDate", startDate)
                    .queryParam("eventEndDate", endDate)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> rawItems = extractItems(response);

            List<RegionActivityItemDto> result = new ArrayList<>();
            for (Map<String, Object> raw : rawItems) {
                String title = stringValue(raw.get("title"));
                String addr1 = stringValue(raw.get("addr1"));
                String addr2 = stringValue(raw.get("addr2"));
                String address = joinAddress(addr1, addr2);

                if (!containsRegion(regionName, title, address)) {
                    continue;
                }

                String contentId = stringValue(raw.get("contentid"));
                String imageUrl = firstNotBlank(
                        stringValue(raw.get("firstimage")),
                        stringValue(raw.get("firstimage2"))
                );

                DetailInfo detailInfo = fetchDetailInfo(contentId, "15");

                String period = formatPeriod(
                        stringValue(raw.get("eventstartdate")),
                        stringValue(raw.get("eventenddate"))
                );

                result.add(RegionActivityItemDto.builder()
                        .title(title)
                        .imageUrl(imageUrl)
                        .address(address)
                        .period(period)
                        .detailUrl(buildVisitKoreaDetailUrl(contentId))
                        .externalUrl(detailInfo.homepageUrl())
                        .category("행사/축제")
                        .tel(firstNotBlank(stringValue(raw.get("tel")), detailInfo.tel()))
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
        List<String> contentTypeIds = List.of("12", "14", "15", "25", "28");

        for (String contentTypeId : contentTypeIds) {
            try {
                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromHttpUrl(baseUrl + "/areaBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("arrange", "Q")
                        .queryParam("numOfRows", 10)
                        .queryParam("pageNo", 1)
                        .queryParam("contentTypeId", contentTypeId)
                        .queryParam("lDongRegnCd", areaCode.lDongRegnCd());

                if (StringUtils.hasText(areaCode.lDongSignguCd())) {
                    builder.queryParam("lDongSignguCd", areaCode.lDongSignguCd());
                }

                String requestUrl = builder
                        .encode()
                        .build()
                        .toUriString();

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

                    String contentId = stringValue(raw.get("contentid"));
                    String imageUrl = firstNotBlank(
                            stringValue(raw.get("firstimage")),
                            stringValue(raw.get("firstimage2"))
                    );
                    String address = joinAddress(
                            stringValue(raw.get("addr1")),
                            stringValue(raw.get("addr2"))
                    );

                    DetailInfo detailInfo = fetchDetailInfo(contentId, contentTypeId);

                    result.add(
                            RegionActivityItemDto.builder()
                                    .title(title)
                                    .imageUrl(imageUrl)
                                    .address(address)
                                    .period("")
                                    .detailUrl(buildVisitKoreaDetailUrl(contentId))
                                    .externalUrl(detailInfo.homepageUrl())
                                    .category(resolveCategory(contentTypeId))
                                    .tel(firstNotBlank(stringValue(raw.get("tel")), detailInfo.tel()))
                                    .regionName(regionName)
                                    .build()
                    );
                }
            } catch (Exception e) {
                log.warn("TourAPI areaBasedList2 호출 실패 - region={}, contentTypeId={}", regionName, contentTypeId, e);
            }
        }

        return result;
    }

    private AreaCode getAreaCode(String regionName) {
        return switch (regionName) {
            case "서울" -> new AreaCode("11", "");
            case "부산" -> new AreaCode("26", "");
            case "강릉" -> new AreaCode("51", "170");
            case "제주" -> new AreaCode("50", "");
            case "경주" -> new AreaCode("47", "130");
            default -> new AreaCode("", "");
        };
    }

    private List<RegionActivityItemDto> searchByKeyword(String keyword, int size) {
        try {
            String requestUrl = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/searchKeyword2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("listYN", "Y")
                    .queryParam("arrange", "Q")
                    .queryParam("numOfRows", size)
                    .queryParam("pageNo", 1)
                    .queryParam("keyword", keyword)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> rawItems = extractItems(response);
            return convertItems(rawItems, extractRegionFromKeyword(keyword));
        } catch (Exception e) {
            log.error("TourAPI searchKeyword2 호출 실패 - keyword={}", keyword, e);
            return List.of();
        }
    }

    private List<RegionActivityItemDto> convertItems(List<Map<String, Object>> rawItems, String regionName) {
        List<RegionActivityItemDto> result = new ArrayList<>();

        for (Map<String, Object> raw : rawItems) {
            String title = stringValue(raw.get("title"));
            if (!StringUtils.hasText(title)) {
                continue;
            }

            String contentId = stringValue(raw.get("contentid"));
            String contentTypeId = stringValue(raw.get("contenttypeid"));
            String imageUrl = firstNotBlank(
                    stringValue(raw.get("firstimage")),
                    stringValue(raw.get("firstimage2"))
            );
            String address = joinAddress(
                    stringValue(raw.get("addr1")),
                    stringValue(raw.get("addr2"))
            );

            DetailInfo detailInfo = fetchDetailInfo(contentId, contentTypeId);

            result.add(RegionActivityItemDto.builder()
                    .title(title)
                    .imageUrl(imageUrl)
                    .address(address)
                    .period("")
                    .detailUrl(buildVisitKoreaDetailUrl(contentId))
                    .externalUrl(detailInfo.homepageUrl())
                    .category(resolveCategory(contentTypeId))
                    .tel(firstNotBlank(stringValue(raw.get("tel")), detailInfo.tel()))
                    .regionName(regionName)
                    .build());
        }

        return result;
    }

    private DetailInfo fetchDetailInfo(String contentId, String contentTypeId) {
        if (!StringUtils.hasText(contentId)) {
            return new DetailInfo("", "", "");
        }

        try {
            String requestUrl = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/detailCommon2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("contentId", contentId)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = extractItems(response);

            if (items.isEmpty()) {
                return new DetailInfo("", "", "");
            }

            Map<String, Object> item = items.get(0);

            String overview = stringValue(item.get("overview"));
            String homepageRaw = stringValue(item.get("homepage"));
            String homepageUrl = extractHomepageUrl(homepageRaw);
            String tel = stringValue(item.get("tel"));

            if (!StringUtils.hasText(homepageUrl) && "15".equals(contentTypeId)) {
                homepageUrl = fetchFestivalHomepage(contentId, contentTypeId);
            }

            return new DetailInfo(overview, homepageUrl, tel);
        } catch (Exception e) {
            log.error("TourAPI detailCommon2 호출 실패 - contentId={}", contentId, e);
            return new DetailInfo("", "", "");
        }
    }

    private String fetchFestivalHomepage(String contentId, String contentTypeId) {
        try {
            String requestUrl = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/detailIntro2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("contentId", contentId)
                    .queryParam("contentTypeId", contentTypeId)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = extractItems(response);
            if (items.isEmpty()) {
                return "";
            }

            Map<String, Object> item = items.get(0);
            return extractHomepageUrl(stringValue(item.get("eventhomepage")));
        } catch (Exception e) {
            log.warn("TourAPI detailIntro2 호출 실패 - contentId={}, contentTypeId={}", contentId, contentTypeId, e);
            return "";
        }
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

    private String extractHomepageUrl(String homepageRaw) {
        if (!StringUtils.hasText(homepageRaw)) {
            return "";
        }

        Matcher matcher = LINK_PATTERN.matcher(homepageRaw);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = SINGLE_QUOTE_LINK_PATTERN.matcher(homepageRaw);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String cleaned = homepageRaw
                .replaceAll("<[^>]*>", " ")
                .replace("&amp;", "&")
                .trim();

        matcher = URL_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private String joinAddress(String addr1, String addr2) {
        if (StringUtils.hasText(addr1) && StringUtils.hasText(addr2)) {
            return addr1 + " " + addr2;
        }
        if (StringUtils.hasText(addr1)) {
            return addr1;
        }
        return addr2;
    }

    private String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limit(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean containsRegion(String regionName, String title, String address) {
        String source = (stringValue(title) + " " + stringValue(address)).replace(" ", "");
        String target = stringValue(regionName).replace(" ", "");
        return source.contains(target);
    }

    private String extractRegionFromKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.split(" ")[0];
    }

    private String resolveCategory(String contentTypeId) {
        return switch (contentTypeId) {
            case "12" -> "관광지";
            case "14" -> "문화시설";
            case "15" -> "행사/축제";
            case "25" -> "여행코스";
            case "28" -> "레포츠";
            case "32" -> "숙박";
            case "38" -> "쇼핑";
            case "39" -> "음식점";
            default -> "즐길거리";
        };
    }

    private String buildVisitKoreaDetailUrl(String contentId) {
        if (!StringUtils.hasText(contentId)) {
            return "";
        }
        return "https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=" + contentId;
    }

    private String formatPeriod(String start, String end) {
        if (!StringUtils.hasText(start) && !StringUtils.hasText(end)) {
            return "";
        }

        String startText = formatDate(start);
        String endText = formatDate(end);

        if (StringUtils.hasText(startText) && StringUtils.hasText(endText)) {
            return startText + " ~ " + endText;
        }
        return firstNotBlank(startText, endText);
    }

    private String formatDate(String value) {
        if (!StringUtils.hasText(value) || value.length() < 8) {
            return "";
        }
        return value.substring(0, 4) + "." + value.substring(4, 6) + "." + value.substring(6, 8);
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

    private record DetailInfo(
            String overview,
            String homepageUrl,
            String tel
    ) {
    }

    private record AreaCode(
            String lDongRegnCd,
            String lDongSignguCd
    ) {
    }
}
