package com.Accommodation.service;

import com.Accommodation.dto.LocalActivityCardDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class LocalActivityService {

    private final RestClient restClient;
    private final String serviceKey;
    private final String baseUrl;
    private final String mobileApp;

    private static final List<RegionConfig> REGION_CONFIGS = List.of(
            new RegionConfig(
                    "서울",
                    "서울",
                    "/images/main/seoul.png",
                    "서울 도심 인기 즐길거리",
                    "전시, 야경 명소, 실내 체험, 문화 공간까지 서울에서 가볍게 즐길 수 있는 코스를 모아봤어요."
            ),
            new RegionConfig(
                    "부산",
                    "부산",
                    "/images/main/busan.png",
                    "부산 바다 · 감성 여행 코스",
                    "해변 산책, 전망 명소, 로컬 체험과 함께 부산에서 즐길 수 있는 대표 코스를 확인해보세요."
            ),
            new RegionConfig(
                    "강릉",
                    "강릉",
                    "/images/main/gangneung.png",
                    "강릉 바다 · 카페 · 체험",
                    "바다 풍경과 함께 즐길 수 있는 카페, 산책, 체험형 여행 코스를 중심으로 구성했어요."
            ),
            new RegionConfig(
                    "제주",
                    "제주",
                    "/images/main/jeju.png",
                    "제주 자연 · 액티비티 추천",
                    "오름, 해변, 드라이브, 야외 체험처럼 제주에서만 즐길 수 있는 여행 포인트를 담았어요."
            ),
            new RegionConfig(
                    "경주",
                    "경주",
                    "/images/main/gyeongju.png",
                    "경주 문화유산 · 야경 코스",
                    "한옥 감성, 역사 명소, 야경 스팟까지 경주에서 즐길 수 있는 대표 여행 코스를 담았어요."
            )
    );

    public LocalActivityService(
            @Value("${tour.api.service-key:}") String serviceKey,
            @Value("${tour.api.base-url:https://apis.data.go.kr/B551011/KorService1}") String baseUrl,
            @Value("${tour.api.mobile-app:Accommodation}") String mobileApp
    ) {
        this.restClient = RestClient.builder().build();
        this.serviceKey = serviceKey;
        this.baseUrl = baseUrl;
        this.mobileApp = mobileApp;
    }

    public List<LocalActivityCardDto> getMainActivities() {
        return REGION_CONFIGS.stream()
                .map(this::buildCard)
                .toList();
    }

    private LocalActivityCardDto buildCard(RegionConfig region) {
        TourApiItem item = fetchActivity(region.searchKeyword());

        if (item == null) {
            return new LocalActivityCardDto(
                    region.regionName(),
                    region.searchKeyword(),
                    region.imagePath(),
                    region.fallbackTitle(),
                    region.fallbackDescription()
            );
        }

        String title = region.regionName() + " · " + limit(item.title(), 22);

        String description = StringUtils.hasText(item.overview())
                ? limit(cleanText(item.overview()), 88)
                : (StringUtils.hasText(item.address())
                   ? limit(cleanText(item.address()), 88)
                   : region.fallbackDescription());

        return new LocalActivityCardDto(
                region.regionName(),
                region.searchKeyword(),
                region.imagePath(),
                title,
                description
        );
    }

    private TourApiItem fetchActivity(String keyword) {
        if (!StringUtils.hasText(serviceKey)) {
            return null;
        }

        try {
            String searchUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/searchKeyword1")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("listYN", "Y")
                    .queryParam("arrange", "Q")
                    .queryParam("numOfRows", 5)
                    .queryParam("pageNo", 1)
                    .queryParam("contentTypeId", 12)
                    .queryParam("keyword", keyword)
                    .build(true)
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Map<String, Object> body = castMap(castMap(response.get("response")).get("body"));
            List<Map<String, Object>> items = castItemList(castMap(body.get("items")).get("item"));

            if (items.isEmpty()) {
                return null;
            }

            Map<String, Object> first = items.get(0);

            String contentId = stringValue(first.get("contentid"));
            String title = stringValue(first.get("title"));
            String address = stringValue(first.get("addr1"));
            String overview = fetchOverview(contentId);

            return new TourApiItem(title, overview, address);
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchOverview(String contentId) {
        if (!StringUtils.hasText(contentId)) {
            return "";
        }

        try {
            String detailUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/detailCommon1")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("contentId", contentId)
                    .queryParam("defaultYN", "Y")
                    .queryParam("addrinfoYN", "Y")
                    .queryParam("overviewYN", "Y")
                    .build(true)
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(detailUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Map<String, Object> body = castMap(castMap(response.get("response")).get("body"));
            List<Map<String, Object>> items = castItemList(castMap(body.get("items")).get("item"));

            if (items.isEmpty()) {
                return "";
            }

            return stringValue(items.get(0).get("overview"));
        } catch (Exception e) {
            return "";
        }
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

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castItemList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        if (value instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }

        return List.of();
    }

    private record RegionConfig(
            String regionName,
            String searchKeyword,
            String imagePath,
            String fallbackTitle,
            String fallbackDescription
    ) {
    }

    private record TourApiItem(
            String title,
            String overview,
            String address
    ) {
    }
}