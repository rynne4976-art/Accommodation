(function () {
    'use strict';

    const config = window.accomDtlConfig || {};

    let naverMap = null;
    let naverMarker = null;
    let naverInfoWindow = null;
    let mapInitialized = false;
    let initRequested = false;

    function showMapError(message) {
        const mapErrorBox = document.getElementById('naverMapErrorMessage');
        if (!mapErrorBox) {
            return;
        }

        mapErrorBox.style.display = 'block';
        mapErrorBox.textContent = message;
        console.error('[accomdtl-map]', message);
    }

    function hideMapError() {
        const mapErrorBox = document.getElementById('naverMapErrorMessage');
        if (!mapErrorBox) {
            return;
        }

        mapErrorBox.style.display = 'none';
        mapErrorBox.textContent = '';
    }

    function getMapContainer() {
        return document.getElementById('naverMap');
    }

    function getLocationInput() {
        return document.getElementById('accomLocation');
    }

    function normalizeAddress(value) {
        return (value || '').trim().replace(/\s+/g, ' ');
    }

    function buildMapQueries(rawAddress) {
        const queries = [];

        function pushQuery(value) {
            const normalized = normalizeAddress(value);
            if (normalized && !queries.includes(normalized)) {
                queries.push(normalized);
            }
        }

        const cleanedAddress = normalizeAddress(rawAddress);
        pushQuery(cleanedAddress);

        const withoutPostcode = cleanedAddress.replace(/^\(\d{5}\)\s*/, '').trim();
        pushQuery(withoutPostcode);

        if (withoutPostcode.includes(',')) {
            pushQuery(withoutPostcode.split(',')[0]);
        }

        const parts = withoutPostcode.split(' ').filter(Boolean);

        for (let endIndex = parts.length - 1; endIndex >= Math.max(parts.length - 3, 1); endIndex -= 1) {
            pushQuery(parts.slice(0, endIndex).join(' '));
        }

        return queries;
    }

    function ensureMapInstance() {
        const mapContainer = getMapContainer();

        if (!mapContainer) {
            console.warn('[accomdtl-map] 지도 컨테이너가 없습니다.');
            return false;
        }

        if (!window.naver || !naver.maps) {
            console.warn('[accomdtl-map] naver.maps 가 아직 준비되지 않았습니다.');
            return false;
        }

        if (!naverMap) {
            const defaultPoint = new naver.maps.LatLng(37.5665, 126.9780);

            naverMap = new naver.maps.Map(mapContainer, {
                center: defaultPoint,
                zoom: 15
            });

            console.log('[accomdtl-map] 지도 객체 생성 완료');
        }

        return true;
    }

    function applyMapResult(point, addressLabel) {
        if (!ensureMapInstance()) {
            showMapError(config.mapScriptLoadFailedMessage || '네이버 지도 스크립트를 불러오지 못했습니다.');
            return;
        }

        hideMapError();

        naverMap.setCenter(point);

        if (naverMarker) {
            naverMarker.setMap(null);
        }

        naverMarker = new naver.maps.Marker({
            position: point,
            map: naverMap
        });

        if (naverInfoWindow) {
            naverInfoWindow.close();
        }

        naverInfoWindow = new naver.maps.InfoWindow({
            content:
                '<div class="naver-map-info-window">' +
                '<strong>' + (config.mapInfoTitle || '숙소 위치') + '</strong>' +
                '<p>' + addressLabel + '</p>' +
                '</div>'
        });

        naverInfoWindow.open(naverMap, naverMarker);

        mapInitialized = true;

        setTimeout(function () {
            try {
                naver.maps.Event.trigger(naverMap, 'resize');
                naverMap.setCenter(point);
                console.log('[accomdtl-map] resize 및 center 재적용 완료');
            } catch (error) {
                console.error('[accomdtl-map] resize 처리 중 오류', error);
            }
        }, 300);
    }

    function geocodeAddressQueries(queries, index) {
        if (!window.naver || !naver.maps || !naver.maps.Service) {
            showMapError(config.mapScriptLoadFailedMessage || '네이버 지도 스크립트를 불러오지 못했습니다.');
            return;
        }

        if (!queries || queries.length === 0) {
            showMapError(config.mapResolveFailedMessage || '주소를 기반으로 지도를 표시할 수 없습니다.');
            return;
        }

        if (index >= queries.length) {
            showMapError(config.mapResolveFailedMessage || '주소를 기반으로 지도를 표시할 수 없습니다.');
            return;
        }

        console.log('[accomdtl-map] geocode 시도:', queries[index]);

        naver.maps.Service.geocode(
            {
                query: queries[index]
            },
            function (status, response) {
                console.log('[accomdtl-map] geocode status:', status, 'query:', queries[index], 'response:', response);

                if (status !== naver.maps.Service.Status.OK) {
                    geocodeAddressQueries(queries, index + 1);
                    return;
                }

                const items = response && response.v2 ? response.v2.addresses : null;

                if (!items || items.length === 0) {
                    geocodeAddressQueries(queries, index + 1);
                    return;
                }

                const first = items[0];
                const lat = parseFloat(first.y);
                const lng = parseFloat(first.x);

                if (Number.isNaN(lat) || Number.isNaN(lng)) {
                    console.warn('[accomdtl-map] 좌표 변환 실패:', first);
                    geocodeAddressQueries(queries, index + 1);
                    return;
                }

                const point = new naver.maps.LatLng(lat, lng);
                const roadAddress = first.roadAddress || first.jibunAddress || queries[index];

                applyMapResult(point, roadAddress);
            }
        );
    }

    function tryInitMap() {
        const locationInput = getLocationInput();
        const mapContainer = getMapContainer();

        console.log('[accomdtl-map] tryInitMap 실행');

        if (!mapContainer) {
            console.warn('[accomdtl-map] 지도 컨테이너 없음');
            return;
        }

        if (!locationInput || !locationInput.value || normalizeAddress(locationInput.value) === '') {
            showMapError(config.mapLocationMissingMessage || '등록된 숙소 위치 정보가 없습니다.');
            return;
        }

        if (!window.naver || !naver.maps || !naver.maps.Service) {
            console.warn('[accomdtl-map] 네이버 지도 SDK 미준비 상태');
            return;
        }

        if (mapInitialized) {
            console.log('[accomdtl-map] 이미 초기화 완료됨');
            return;
        }

        if (!ensureMapInstance()) {
            showMapError(config.mapScriptLoadFailedMessage || '네이버 지도 스크립트를 불러오지 못했습니다.');
            return;
        }

        hideMapError();
        geocodeAddressQueries(buildMapQueries(locationInput.value), 0);
    }

    window.initNaverMap = function () {
        console.log('[accomdtl-map] SDK callback initNaverMap 호출됨');
        initRequested = true;
        tryInitMap();
    };

    window.navermap_authFailure = function () {
        showMapError(config.mapAuthFailedMessage || '네이버 지도 인증에 실패했습니다.');
    };

    document.addEventListener('DOMContentLoaded', function () {
        console.log('[accomdtl-map] DOMContentLoaded');

        setTimeout(function () {
            if (window.naver && naver.maps && naver.maps.Service) {
                console.log('[accomdtl-map] DOMContentLoaded 이후 수동 초기화 시도');
                tryInitMap();
            }
        }, 200);
    });

    window.addEventListener('load', function () {
        console.log('[accomdtl-map] window.load');

        const mapContainer = getMapContainer();
        if (!mapContainer) {
            return;
        }

        setTimeout(function () {
            if (!mapInitialized && window.naver && naver.maps && naver.maps.Service) {
                console.log('[accomdtl-map] load 이후 수동 초기화 재시도');
                tryInitMap();
                return;
            }

            if (!mapInitialized && (!window.naver || !naver.maps)) {
                showMapError(config.mapScriptLoadFailedMessage || '네이버 지도 스크립트를 불러오지 못했습니다.');
            }
        }, 500);

        setTimeout(function () {
            if (!mapInitialized && initRequested) {
                console.warn('[accomdtl-map] callback 후에도 초기화 미완료');
            }
        }, 2000);
    });
})();