(function () {
    'use strict';

    const config = window.transportPageConfig || {};
    const addressInput = document.getElementById('transportAccomLocation');
    const statusNode = document.getElementById('transportStatus');
    const metricCardsRoot = document.getElementById('transportMetricCards');
    const currentRouteRoot = document.getElementById('transportCurrentRoute');
    const airportCardsRoot = document.getElementById('transportAirportCards');
    const stationCardsRoot = document.getElementById('transportStationCards');
    const rentalMessageNode = document.getElementById('transportRentalMessage');
    const sortSelect = document.getElementById('transportCarSort');
    const filterButtons = Array.from(document.querySelectorAll('.transport-filter'));
    const carGrid = document.getElementById('transportCarGrid');
    const carCards = carGrid ? Array.from(carGrid.querySelectorAll('.transport-car-card')) : [];

    const AIRPORTS = [
        { label: '김포공항', lat: 37.5583, lng: 126.7906 },
        { label: '인천국제공항', lat: 37.4602, lng: 126.4407 },
        { label: '제주국제공항', lat: 33.5104, lng: 126.4928 },
        { label: '김해공항', lat: 35.1738, lng: 128.9487 }
    ];

    const STATIONS = [
        { label: '서울역', lat: 37.5547, lng: 126.9706 },
        { label: '강남역', lat: 37.4979, lng: 127.0276 },
        { label: '홍대입구역', lat: 37.5572, lng: 126.9245 },
        { label: '부산역', lat: 35.1151, lng: 129.0414 },
        { label: '해운대역', lat: 35.1631, lng: 129.1586 },
        { label: '제주시청 인근역권', lat: 33.4996, lng: 126.5312 }
    ];

    let destinationCoords = null;
    let activeCarFilter = 'all';
    let carUiBound = false;

    function normalizeAddress(value) {
        return (value || '').trim().replace(/\s+/g, ' ');
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

    function formatDistance(distanceKm) {
        return distanceKm < 1
            ? Math.round(distanceKm * 1000) + 'm'
            : distanceKm.toFixed(1) + 'km';
    }

    function estimateMinutes(distanceKm, speedKmh) {
        return Math.max(6, Math.round((distanceKm / speedKmh) * 60));
    }

    function createMetricCard(icon, label, headline, copy) {
        return [
            '<article class="transport-mode-card">',
            '<span class="transport-mode-card__icon">', icon, '</span>',
            '<strong>', headline, '</strong>',
            '<p>', label, '</p>',
            '<p>', copy, '</p>',
            '</article>'
        ].join('');
    }

    function createRouteCard(route, title, note) {
        return [
            '<article class="transport-route-card">',
            '<span class="transport-route-card__eyebrow">', route.source, '</span>',
            '<strong>', title, '</strong>',
            '<div class="transport-route-card__stats">',
            '<span>예상 소요시간<strong>', route.durationMinutes, '분</strong></span>',
            '<span>이동 거리<strong>', formatDistance(route.distanceKm), '</strong></span>',
            '</div>',
            '<p>', route.live ? '카카오 길찾기 기준 결과입니다.' : '대체 계산 기준 결과입니다.', '</p>',
            '<p class="transport-route-card__note">', note, '</p>',
            '</article>'
        ].join('');
    }

    function classifyCategory(category) {
        const normalized = (category || '').toLowerCase();
        if (normalized.includes('경') || normalized.includes('소형') || normalized.includes('compact')) {
            return 'compact';
        }
        if (normalized.includes('대형') || normalized.includes('suv') || normalized.includes('승합') || normalized.includes('family')) {
            return 'family';
        }
        if (normalized.includes('수입') || normalized.includes('premium') || normalized.includes('럭셔리')) {
            return 'premium';
        }
        return 'all';
    }

    function applyCarFilters() {
        if (!carCards.length) {
            return;
        }

        const sortValue = sortSelect ? sortSelect.value : 'priceAsc';
        const cards = carCards.slice();

        cards.forEach(function (card) {
            const category = classifyCategory(card.dataset.category);
            const shouldShow = activeCarFilter === 'all' || category === activeCarFilter;
            card.classList.toggle('is-hidden', !shouldShow);
        });

        cards.sort(function (left, right) {
            if (sortValue === 'peopleDesc') {
                return Number(right.dataset.people) - Number(left.dataset.people);
            }
            if (sortValue === 'nameAsc') {
                return (left.dataset.name || '').localeCompare(right.dataset.name || '', 'ko');
            }
            return Number(left.dataset.price) - Number(right.dataset.price);
        });

        cards.forEach(function (card) {
            carGrid.appendChild(card);
        });
    }

    function bindCarUi() {
        if (carUiBound) {
            return;
        }

        carUiBound = true;

        filterButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                activeCarFilter = button.dataset.filter || 'all';
                filterButtons.forEach(function (item) {
                    item.classList.toggle('is-active', item === button);
                });
                applyCarFilters();
            });
        });

        if (sortSelect) {
            sortSelect.addEventListener('change', applyCarFilters);
        }

        applyCarFilters();
    }

    function fallbackRoute(origin, destination, source, speedKmh) {
        const distanceKm = haversineKm(origin, destination);
        return {
            source: source,
            distanceKm: Number(distanceKm.toFixed(1)),
            durationMinutes: estimateMinutes(distanceKm, speedKmh || 43),
            live: false
        };
    }

    async function fetchGeocode(address) {
        const params = new URLSearchParams({ address: address });
        const response = await fetch('/transport/geocode?' + params.toString(), {
            headers: { Accept: 'application/json' }
        });

        if (!response.ok) {
            let message = '주소 좌표 변환 요청에 실패했습니다.';
            try {
                const errorBody = await response.json();
                if (errorBody && errorBody.message) {
                    message = errorBody.message;
                }
            } catch (error) {
                // ignore response parsing failure and keep fallback message
            }
            throw new Error(message);
        }

        return response.json();
    }

    async function fetchDrivingRoute(origin, destination, source) {
        if (!config.directionsEnabled) {
            return fallbackRoute(origin, destination, source, 43);
        }

        const params = new URLSearchParams({
            startLat: origin.lat,
            startLng: origin.lng,
            goalLat: destination.lat,
            goalLng: destination.lng,
            source: source
        });

        try {
            const response = await fetch('/transport/directions/driving?' + params.toString(), {
                headers: { Accept: 'application/json' }
            });

            if (!response.ok) {
                let message = '길찾기 요청에 실패했습니다.';
                try {
                    const errorBody = await response.json();
                    if (errorBody && errorBody.message) {
                        message = errorBody.message;
                    }
                } catch (error) {
                    // ignore response parsing failure and use fallback route below
                }
                throw new Error(message);
            }

            const data = await response.json();
            return {
                source: data.source,
                distanceKm: data.distanceKm,
                durationMinutes: data.durationMinutes,
                live: true
            };
        } catch (error) {
            return fallbackRoute(origin, destination, source, 43);
        }
    }

    function findNearestByDistance(origin, candidates) {
        return candidates
            .map(function (candidate) {
                const distanceKm = haversineKm(origin, candidate);
                return {
                    source: candidate.label,
                    distanceKm: Number(distanceKm.toFixed(1)),
                    durationMinutes: estimateMinutes(distanceKm, 4.5),
                    live: false
                };
            })
            .sort(function (left, right) {
                return left.distanceKm - right.distanceKm;
            });
    }

    function renderMetrics(currentRoute, bestAirportRoute, nearestStationRoute) {
        if (!metricCardsRoot) {
            return;
        }

        metricCardsRoot.innerHTML = [
            createMetricCard(
                '택시',
                '현재 위치에서 숙소까지',
                '약 ' + currentRoute.durationMinutes + '분',
                '이동 거리 ' + formatDistance(currentRoute.distanceKm)
            ),
            createMetricCard(
                '공항',
                bestAirportRoute.source,
                '약 ' + bestAirportRoute.durationMinutes + '분',
                '숙소와의 거리 ' + formatDistance(bestAirportRoute.distanceKm)
            ),
            createMetricCard(
                '지하철',
                nearestStationRoute.source,
                '약 ' + nearestStationRoute.durationMinutes + '분',
                '숙소와의 거리 ' + formatDistance(nearestStationRoute.distanceKm)
            )
        ].join('');

        if (rentalMessageNode) {
            rentalMessageNode.textContent =
                currentRoute.distanceKm >= 20 || bestAirportRoute.durationMinutes >= 50
                    ? '현재 위치나 공항에서 숙소까지 이동 거리가 길어 렌트카를 우선 검토하는 것이 좋습니다.'
                    : '숙소 접근 거리가 무난해 대중교통과 렌트카를 함께 비교해도 좋습니다.';
        }
    }

    function renderCurrentRoute(currentRoute) {
        if (!currentRouteRoot) {
            return;
        }

        currentRouteRoot.innerHTML = createRouteCard(
            currentRoute,
            '현재 위치에서 숙소까지',
            currentRoute.live
                ? '브라우저 현재 위치 기준으로 숙소까지의 차량 이동 경로를 계산했습니다.'
                : '위치 권한이 없거나 API 응답이 없어 대체 계산 결과를 보여줍니다.'
        );
    }

    function renderAirportRoutes(airportRoutes) {
        if (!airportCardsRoot) {
            return;
        }

        airportCardsRoot.innerHTML = airportRoutes.slice(0, 3).map(function (route, index) {
            return createRouteCard(
                route,
                route.source + '에서 숙소까지',
                index === 0
                    ? '숙소 기준 가장 가까운 공항 후보입니다.'
                    : '함께 비교할 수 있는 다른 공항 후보입니다.'
            );
        }).join('');
    }

    function renderStationRoutes(stationRoutes) {
        if (!stationCardsRoot) {
            return;
        }

        stationCardsRoot.innerHTML = stationRoutes.slice(0, 3).map(function (route, index) {
            return createRouteCard(
                route,
                route.source + ' 기준 숙소 접근',
                index === 0
                    ? '숙소 기준 가장 가까운 역 후보입니다.'
                    : '비교용 추가 역 후보입니다.'
            );
        }).join('');
    }

    function getCurrentLocation() {
        return new Promise(function (resolve, reject) {
            if (!navigator.geolocation) {
                reject(new Error('geolocation unsupported'));
                return;
            }

            navigator.geolocation.getCurrentPosition(
                function (position) {
                    resolve({
                        lat: position.coords.latitude,
                        lng: position.coords.longitude
                    });
                },
                function () {
                    reject(new Error('geolocation denied'));
                },
                {
                    enableHighAccuracy: true,
                    timeout: 8000,
                    maximumAge: 300000
                }
            );
        });
    }

    async function loadRouteData() {
        if (!destinationCoords) {
            return;
        }

        setStatus('현재 위치와 주변 교통 거점을 계산하고 있습니다.', 'loading');

        const airportRoutes = await Promise.all(
            AIRPORTS.map(function (airport) {
                return fetchDrivingRoute(
                    { lat: airport.lat, lng: airport.lng },
                    destinationCoords,
                    airport.label
                );
            })
        );
        airportRoutes.sort(function (left, right) {
            return left.distanceKm - right.distanceKm;
        });

        const stationRoutes = findNearestByDistance(destinationCoords, STATIONS);

        let currentRoute;
        try {
            const currentLocation = await getCurrentLocation();
            currentRoute = await fetchDrivingRoute(currentLocation, destinationCoords, '현재 위치');
        } catch (error) {
            currentRoute = fallbackRoute(
                { lat: 37.5665, lng: 126.9780 },
                destinationCoords,
                '현재 위치',
                38
            );
        }

        renderMetrics(currentRoute, airportRoutes[0], stationRoutes[0]);
        renderCurrentRoute(currentRoute);
        renderAirportRoutes(airportRoutes);
        renderStationRoutes(stationRoutes);

        setStatus(
            config.directionsEnabled
                ? '카카오 API 기준으로 현재 위치와 주변 교통 거점을 계산했습니다.'
                : '카카오 API 키가 없어 일부 결과는 대체 계산 값으로 표시됩니다.',
            config.directionsEnabled ? 'success' : 'error'
        );
    }

    async function initTransportPage() {
        bindCarUi();

        const address = addressInput ? normalizeAddress(addressInput.value) : '';
        if (!config.hasSelectedAccom || !address) {
            setStatus('숙소를 선택하면 현재 위치와 주변 교통 거점 기준 이동 시간을 계산해 보여줍니다.', 'neutral');
            return;
        }

        if (!config.directionsEnabled) {
            setStatus('카카오 REST API 키가 없어 실시간 경로 대신 대체 계산 값을 표시합니다.', 'error');
        } else {
            setStatus('숙소 주소를 좌표로 변환하고 있습니다.', 'loading');
        }

        try {
            const geocode = await fetchGeocode(address);
            destinationCoords = {
                lat: geocode.lat,
                lng: geocode.lng
            };
            await loadRouteData();
        } catch (error) {
            setStatus(error.message || '숙소 주소를 좌표로 변환하지 못했습니다. 주소 또는 카카오 API 설정을 확인하세요.', 'error');
        }
    }

    window.initTransportPage = initTransportPage;

    document.addEventListener('DOMContentLoaded', function () {
        initTransportPage();
    });
})();
