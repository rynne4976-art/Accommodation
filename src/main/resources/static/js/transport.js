(function () {
    'use strict';

    const config = window.transportPageConfig || {};
    const addressInput = document.getElementById('transportAccomLocation');
    const statusNode = document.getElementById('transportStatus');
    const cardsRoot = document.getElementById('transportMetricCards');

    const AIRPORTS = [
        { label: '김포공항', lat: 37.5583, lng: 126.7906 },
        { label: '인천국제공항', lat: 37.4602, lng: 126.4407 },
        { label: '제주국제공항', lat: 33.5104, lng: 126.4928 },
        { label: '김해공항', lat: 35.1738, lng: 128.9487 }
    ];

    const STATIONS = [
        { label: '서울역', lat: 37.5547, lng: 126.9706 },
        { label: '대전역', lat: 36.3325, lng: 127.4341 },
        { label: '동대구역', lat: 35.8797, lng: 128.6285 },
        { label: '부산역', lat: 35.1151, lng: 129.0414 },
        { label: '광주송정역', lat: 35.1372, lng: 126.7916 }
    ];

    function normalizeAddress(value) {
        return (value || '').trim().replace(/\s+/g, ' ');
    }

    function buildQueries(rawAddress) {
        const queries = [];

        function push(value) {
            const normalized = normalizeAddress(value);
            if (normalized && !queries.includes(normalized)) {
                queries.push(normalized);
            }
        }

        const cleaned = normalizeAddress(rawAddress);
        push(cleaned);

        const withoutPostcode = cleaned.replace(/^\(\d{5}\)\s*/, '').trim();
        push(withoutPostcode);

        const parts = withoutPostcode.split(' ').filter(Boolean);
        for (let endIndex = parts.length - 1; endIndex >= Math.max(parts.length - 3, 1); endIndex -= 1) {
            push(parts.slice(0, endIndex).join(' '));
        }

        return queries;
    }

    function setStatus(message, tone) {
        if (!statusNode) {
            return;
        }

        statusNode.textContent = message;
        statusNode.dataset.tone = tone || 'neutral';
    }

    function toRadians(value) {
        return value * (Math.PI / 180);
    }

    function haversineKm(from, to) {
        const earthRadiusKm = 6371;
        const dLat = toRadians(to.lat - from.lat);
        const dLng = toRadians(to.lng - from.lng);
        const lat1 = toRadians(from.lat);
        const lat2 = toRadians(to.lat);

        const a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.sin(dLng / 2) * Math.sin(dLng / 2) * Math.cos(lat1) * Math.cos(lat2);

        return 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    function findNearest(origin, candidates) {
        return candidates
            .map(function (candidate) {
                return {
                    label: candidate.label,
                    distanceKm: haversineKm(origin, candidate)
                };
            })
            .sort(function (left, right) {
                return left.distanceKm - right.distanceKm;
            })[0];
    }

    function formatDistance(distanceKm) {
        return distanceKm < 1
            ? Math.round(distanceKm * 1000) + 'm'
            : distanceKm.toFixed(1) + 'km';
    }

    function estimateDriveMinutes(distanceKm) {
        return Math.max(8, Math.round((distanceKm / 42) * 60));
    }

    function estimateTransitMinutes(distanceKm) {
        return Math.max(10, Math.round((distanceKm / 26) * 60));
    }

    function recommendMobility(airport, station) {
        if (station.distanceKm <= 1.5) {
            return {
                title: '대중교통 추천',
                body: station.label + ' 기준 이동 부담이 적어 지하철이나 버스 연결이 가장 무난합니다.'
            };
        }

        if (airport.distanceKm >= 20 && station.distanceKm >= 8) {
            return {
                title: '렌터카 추천',
                body: '주요 거점과 거리가 있어 이동 동선이 많다면 렌터카가 더 편합니다.'
            };
        }

        return {
            title: '택시 또는 단기 렌터카 추천',
            body: '거점 접근은 가능하지만 마지막 이동 구간까지 고려하면 차량 이동이 더 깔끔합니다.'
        };
    }

    function metricCard(title, headline, detail) {
        return [
            '<article class="transport-metric-card">',
            '<span class="transport-metric-card__label">' + title + '</span>',
            '<strong class="transport-metric-card__headline">' + headline + '</strong>',
            '<p class="transport-metric-card__copy">' + detail + '</p>',
            '</article>'
        ].join('');
    }

    function renderMetrics(origin) {
        if (!cardsRoot) {
            return;
        }

        const nearestAirport = findNearest(origin, AIRPORTS);
        const nearestStation = findNearest(origin, STATIONS);
        const recommendation = recommendMobility(nearestAirport, nearestStation);

        cardsRoot.innerHTML = [
            metricCard(
                '가장 가까운 공항',
                nearestAirport.label + ' · ' + formatDistance(nearestAirport.distanceKm),
                '차량 이동 약 ' + estimateDriveMinutes(nearestAirport.distanceKm) + '분 예상'
            ),
            metricCard(
                '가장 가까운 역',
                nearestStation.label + ' · ' + formatDistance(nearestStation.distanceKm),
                '대중교통 기준 약 ' + estimateTransitMinutes(nearestStation.distanceKm) + '분 예상'
            ),
            metricCard(
                '이동 추천',
                recommendation.title,
                recommendation.body
            )
        ].join('');

        setStatus('숙소 주소를 기준으로 주요 거점과의 이동 정보를 계산했습니다. 거리 기준은 직선거리입니다.', 'success');
    }

    function geocodeQueries(queries, index) {
        if (!window.naver || !window.naver.maps || !naver.maps.Service) {
            setStatus('교통 계산용 지도 서비스를 불러오지 못했습니다.', 'error');
            return;
        }

        if (!queries.length || index >= queries.length) {
            setStatus('주소를 좌표로 변환하지 못했습니다. 숙소 상세 주소를 다시 확인해 주세요.', 'error');
            return;
        }

        naver.maps.Service.geocode({ query: queries[index] }, function (status, response) {
            if (status !== naver.maps.Service.Status.OK) {
                geocodeQueries(queries, index + 1);
                return;
            }

            const items = response && response.v2 ? response.v2.addresses : null;
            if (!items || !items.length) {
                geocodeQueries(queries, index + 1);
                return;
            }

            const first = items[0];
            const lat = parseFloat(first.y);
            const lng = parseFloat(first.x);

            if (Number.isNaN(lat) || Number.isNaN(lng)) {
                geocodeQueries(queries, index + 1);
                return;
            }

            renderMetrics({ lat: lat, lng: lng });
        });
    }

    function initTransportPage() {
        if (!addressInput || !normalizeAddress(addressInput.value)) {
            setStatus('숙소 상세에서 이동하면 주소 기준 교통 계산을 바로 볼 수 있습니다.', 'neutral');
            return;
        }

        setStatus('숙소 주소를 기준으로 교통 정보를 계산하고 있습니다.', 'loading');
        geocodeQueries(buildQueries(addressInput.value), 0);
    }

    window.initTransportPage = initTransportPage;

    document.addEventListener('DOMContentLoaded', function () {
        if (window.naver && window.naver.maps && window.naver.maps.Service) {
            initTransportPage();
        }
    });
})();
