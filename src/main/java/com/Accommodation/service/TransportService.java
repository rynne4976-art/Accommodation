package com.Accommodation.service;

import com.Accommodation.dto.TransportGeocodeDto;
import com.Accommodation.dto.TransportPathPointDto;
import com.Accommodation.dto.TransportRouteDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TransportService {

    private static final String KAKAO_LOCAL_ADDRESS_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

    private static final String KAKAO_LOCAL_CATEGORY_URL =
            "https://dapi.kakao.com/v2/local/search/category.json";

    private static final String KAKAO_LOCAL_KEYWORD_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";

    private static final String KAKAO_DIRECTIONS_URL =
            "https://apis-navi.kakaomobility.com/v1/directions";

    private final RestClient restClient;
    private final String kakaoRestApiKey;

    public TransportService(@Value("${kakao.rest.api.key:}") String kakaoRestApiKey) {
        this.restClient = RestClient.builder().build();
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    public boolean isDirectionsEnabled() {
        return StringUtils.hasText(kakaoRestApiKey);
    }

    /**
     * 주소 → 좌표 변환
     * 실패하면 기본 좌표 fallback
     */
    public TransportGeocodeDto geocodeAddress(String address) {

        if (!isDirectionsEnabled()) {
            throw new IllegalStateException("Kakao API key not configured.");
        }

        try {

            String url = UriComponentsBuilder
                    .fromHttpUrl(KAKAO_LOCAL_ADDRESS_URL)
                    .queryParam("query", address)
                    .build()
                    .encode()
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> documents = castList(response.get("documents"));

            if (!documents.isEmpty()) {

                Map<String, Object> first = documents.get(0);

                double lng = Double.parseDouble(String.valueOf(first.get("x")));
                double lat = Double.parseDouble(String.valueOf(first.get("y")));

                return new TransportGeocodeDto(lat, lng, address);
            }

        } catch (Exception ignored) {
        }

        /**
         * fallback 좌표 (지도에서 보이는 위치)
         * 에버랜드 기준
         */
        double fallbackLat = 37.2945;
        double fallbackLng = 127.2020;

        return new TransportGeocodeDto(
                fallbackLat,
                fallbackLng,
                address
        );
    }

    /**
     * 자동차 이동 경로 계산
     * - 출발지/도착지 좌표를 받아 실제 도로 경로를 계산합니다.
     * - 총 거리/예상 시간과 함께 지도에 그릴 path 좌표 목록을 반환합니다.
     */
    public TransportRouteDto getDrivingRoute(double startLat, double startLng,
                                             double goalLat, double goalLng,
                                             String source) {

        if (!isDirectionsEnabled()) {
            throw new IllegalStateException("Kakao API key not configured.");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(KAKAO_DIRECTIONS_URL)
                .queryParam("origin", startLng + "," + startLat)
                .queryParam("destination", goalLng + "," + goalLat)
                .queryParam("summary", "true")
                .build()
                .encode()
                .toUriString();

        Map<String, Object> response = restClient.get()
                .uri(url)
                .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        List<Map<String, Object>> routes = castList(response.get("routes"));

        if (routes.isEmpty()) {
            throw new IllegalStateException("No route returned from Kakao API.");
        }

        Map<String, Object> firstRoute = routes.get(0);
        Map<String, Object> summary = castMap(firstRoute.get("summary"));

        Number distanceMeters = (Number) summary.get("distance");
        Number durationSeconds = (Number) summary.get("duration");

        if (distanceMeters == null || durationSeconds == null) {
            throw new IllegalStateException("Route summary missing values.");
        }

        List<TransportPathPointDto> path = extractDrivingPath(firstRoute);

        return new TransportRouteDto(
                distanceMeters.doubleValue() / 1000.0,
                (int) Math.round(durationSeconds.doubleValue() / 60.0),
                source,
                path
        );
    }

    private List<TransportPathPointDto> extractDrivingPath(Map<String, Object> route) {
        List<Map<String, Object>> sections = castList(route.get("sections"));
        if (sections.isEmpty()) {
            return List.of();
        }

        List<TransportPathPointDto> points = new ArrayList<>();

        // Kakao 응답의 vertexes는 [경도,위도,경도,위도...] 순서라
        // 2개씩 끊어 (lat,lng) 형태로 변환해 프론트에 전달합니다.
        List<Map<String, Object>> roads = castList(sections.get(0).get("roads"));
        for (Map<String, Object> road : roads) {
            Object vertexesObj = road.get("vertexes");
            if (!(vertexesObj instanceof List<?> vertexes)) {
                continue;
            }

            for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                double x = Double.parseDouble(String.valueOf(vertexes.get(i)));
                double y = Double.parseDouble(String.valueOf(vertexes.get(i + 1)));
                points.add(new TransportPathPointDto(y, x));
            }
        }

        return points;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }


    public Map<String,Object> findNearbySubway(double lat,double lng){
        // 현재 위치를 중심으로 가장 가까운 지하철역 1개를 찾습니다.

        if (!isDirectionsEnabled()) {
            throw new IllegalStateException("Kakao API key not configured.");
        }

        Map<String, Object> response = restClient.get()
                .uri(buildKakaoCategoryUrl("SW8", lat, lng, 2000))
                .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return nearestPlaceFromKakao(response, "지하철역");

    }


    public Map<String,Object> findNearbyBus(double lat,double lng){
        // 현재 위치를 중심으로 가장 가까운 버스정류장 1개를 찾습니다.

        if (!isDirectionsEnabled()) {
            throw new IllegalStateException("Kakao API key not configured.");
        }

        Map<String, Object> response = restClient.get()
                .uri(buildKakaoKeywordUrl("버스정류장", lat, lng, 2000))
                .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return nearestPlaceFromKakao(response, "버스정류장");

    }

    private String buildKakaoCategoryUrl(String categoryGroupCode, double lat, double lng, int radiusMeters) {
        return UriComponentsBuilder
                .fromHttpUrl(KAKAO_LOCAL_CATEGORY_URL)
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", radiusMeters)
                .queryParam("sort", "distance")
                .build()
                .encode()
                .toUriString();
    }

    private String buildKakaoKeywordUrl(String keyword, double lat, double lng, int radiusMeters) {
        return UriComponentsBuilder
                .fromHttpUrl(KAKAO_LOCAL_KEYWORD_URL)
                .queryParam("query", keyword)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", radiusMeters)
                .queryParam("sort", "distance")
                .build()
                .encode()
                .toUriString();
    }

    private Map<String, Object> nearestPlaceFromKakao(Map<String, Object> kakaoResponse, String fallbackName) {
        // 검색 결과가 없으면 화면에서 처리하기 쉬운 기본값을 반환합니다.
        List<Map<String, Object>> documents = castList(kakaoResponse == null ? null : kakaoResponse.get("documents"));
        if (documents.isEmpty()) {
            return Map.of(
                    "name", fallbackName + " 없음",
                    "distance", -1,
                    "lat", 0,
                    "lng", 0
            );
        }

        Map<String, Object> first = documents.get(0);

        String name = stringValue(first.get("place_name"));
        if (!StringUtils.hasText(name)) {
            name = stringValue(first.get("address_name"));
        }

        int distance = 0;
        try {
            distance = Integer.parseInt(stringValue(first.get("distance")));
        } catch (Exception ignored) {
        }

        double placeLng = Double.parseDouble(stringValue(first.get("x")));
        double placeLat = Double.parseDouble(stringValue(first.get("y")));

        return Map.of(
                "name", name,
                "distance", distance,
                "lat", placeLat,
                "lng", placeLng
        );
    }

}