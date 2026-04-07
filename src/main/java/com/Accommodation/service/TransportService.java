package com.Accommodation.service;

import com.Accommodation.dto.TransportGeocodeDto;
import com.Accommodation.dto.TransportRouteDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class TransportService {

    private static final String KAKAO_LOCAL_ADDRESS_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

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

        Map<String, Object> summary = castMap(routes.get(0).get("summary"));

        Number distanceMeters = (Number) summary.get("distance");
        Number durationSeconds = (Number) summary.get("duration");

        if (distanceMeters == null || durationSeconds == null) {
            throw new IllegalStateException("Route summary missing values.");
        }

        return new TransportRouteDto(
                distanceMeters.doubleValue() / 1000.0,
                (int) Math.round(durationSeconds.doubleValue() / 60.0),
                source
        );
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
}