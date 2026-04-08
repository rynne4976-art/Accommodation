(() => {
  let initialized = false;

  // 화면의 안내 문구를 바꿔주는 공통 함수
  function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
  }

  // 서버에서 받은 좌표 객체({lat,lng})를 네이버 지도 좌표 타입으로 변환
  function toLatLng(point) {
    return new naver.maps.LatLng(point.lat, point.lng);
  }

  // 경로 전체가 보이도록 지도 화면 범위를 자동 조정
  function fitToPath(map, pathLatLng) {
    if (!pathLatLng.length) return;
    const bounds = pathLatLng.reduce((b, p) => b.extend(p), new naver.maps.LatLngBounds(pathLatLng[0], pathLatLng[0]));
    map.fitBounds(bounds, { top: 40, right: 40, bottom: 40, left: 40 });
  }

  // API 호출 후 JSON으로 변환 (실패 시 에러 처리)
  async function fetchJson(url) {
    const res = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!res.ok) {
      const text = await res.text().catch(() => "");
      throw new Error(text || `HTTP ${res.status}`);
    }
    return await res.json();
  }

  async function fetchDrivingRoute(start, goal, sourceLabel) {
    const qs = new URLSearchParams({
      startLat: String(start.lat),
      startLng: String(start.lng),
      goalLat: String(goal.lat),
      goalLng: String(goal.lng),
      source: sourceLabel || "출발지",
    });
    return await fetchJson(`/transport/directions/driving?${qs.toString()}`);
  }

  function drawRouteOnMap(map, start, goal, route) {
    const startLatLng = toLatLng(start);
    const goalLatLng = toLatLng(goal);

    // 출발지/도착지 마커를 지도에 표시
    new naver.maps.Marker({ position: startLatLng, map });
    new naver.maps.Marker({ position: goalLatLng, map });

    // 서버에서 받은 실제 도로 경로(path)를 선으로 표시
    const pathLatLng = (route.path || []).map(toLatLng);
    if (pathLatLng.length) {
      new naver.maps.Polyline({
        map,
        path: pathLatLng,
        strokeColor: "#7a5cff",
        strokeWeight: 5,
        strokeOpacity: 0.9,
      });
      fitToPath(map, pathLatLng);
    } else {
      // fallback: at least show both points
      const bounds = new naver.maps.LatLngBounds(startLatLng, startLatLng);
      bounds.extend(goalLatLng);
      map.fitBounds(bounds, { top: 40, right: 40, bottom: 40, left: 40 });
    }
  }

  function createMap(elId, center) {
    return new naver.maps.Map(elId, {
      center: new naver.maps.LatLng(center.lat, center.lng),
      zoom: 13,
    });
  }

  // 숙소 주소(문자열)를 좌표(lat/lng)로 변환
  function geocodeAddress(address) {
    return new Promise((resolve, reject) => {
      naver.maps.Service.geocode({ query: address }, (status, response) => {
        if (status !== naver.maps.Service.Status.OK) {
          reject(new Error("주소 변환 실패"));
          return;
        }
        const item = response?.v2?.addresses?.[0];
        if (!item) {
          reject(new Error("주소 결과 없음"));
          return;
        }
        resolve({ lat: parseFloat(item.y), lng: parseFloat(item.x) });
      });
    });
  }

  // 브라우저의 현재 위치 권한을 이용해 사용자 좌표를 가져옴
  function getCurrentPosition() {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("geolocation 미지원"));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        (err) => reject(err),
        // 모바일 GPS 기준으로 정확도를 높이고 캐시 좌표 사용을 줄임
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
      );
    });
  }

  async function init() {
    // 1) 화면에 출력된 숙소 주소 읽기
    const address =
      (document.getElementById("accomLocation")?.value || "").trim() ||
      (document.getElementById("accomAddress")?.innerText || "").trim();
    if (!address) {
      setText("distanceInfo", "숙소 주소가 없습니다.");
      setText("subwayInfo", "숙소 주소가 없습니다.");
      setText("busInfo", "숙소 주소가 없습니다.");
      return;
    }

    // 2) 숙소 주소를 좌표로 변환
    const accom = await geocodeAddress(address);

    let user;
    try {
      // 3) 모바일 GPS 기반 현재 위치 사용
      user = await getCurrentPosition();
    } catch (e) {
      setText("distanceInfo", "GPS 위치 권한이 필요합니다.");
      setText("subwayInfo", "GPS 위치 권한이 필요합니다.");
      setText("busInfo", "GPS 위치 권한이 필요합니다.");
      // 권한이 없을 때도 지도 프레임은 보이게 유지
      createMap("mapToAccom", accom);
      createMap("mapToSubway", accom);
      createMap("mapToBus", accom);
      return;
    }

    // 4) 목적지별 지도 3개 생성
    const mapToAccom = createMap("mapToAccom", accom);
    const mapToSubway = createMap("mapToSubway", user);
    const mapToBus = createMap("mapToBus", user);

    // 5) 현재 위치 -> 숙소 경로(마커 + 선) 표시
    try {
      const routeToAccom = await fetchDrivingRoute(user, accom, "현재 위치");
      drawRouteOnMap(mapToAccom, user, accom, routeToAccom);
      setText(
        "distanceInfo",
        `약 ${routeToAccom.distanceKm.toFixed(2)} km · ${routeToAccom.durationMinutes}분`
      );
    } catch (e) {
      setText("distanceInfo", "경로 조회 실패");
    }

    // 6) 현재 위치 기준 가장 가까운 지하철 검색 후 경로 표시
    try {
      const subway = await fetchJson(`/api/transport/subway?lat=${user.lat}&lng=${user.lng}`);
      if (subway?.distance >= 0) {
        setText("subwayInfo", `${subway.name} (약 ${subway.distance}m)`);
        const subwayPos = { lat: subway.lat, lng: subway.lng };
        const routeToSubway = await fetchDrivingRoute(user, subwayPos, "현재 위치");
        drawRouteOnMap(mapToSubway, user, subwayPos, routeToSubway);
      } else {
        setText("subwayInfo", "주변 지하철역을 찾지 못했습니다.");
      }
    } catch (e) {
      setText("subwayInfo", "지하철 검색 실패");
    }

    // 7) 현재 위치 기준 가장 가까운 버스정류장 검색 후 경로 표시
    try {
      const bus = await fetchJson(`/api/transport/bus?lat=${user.lat}&lng=${user.lng}`);
      if (bus?.distance >= 0) {
        setText("busInfo", `${bus.name} (약 ${bus.distance}m)`);
        const busPos = { lat: bus.lat, lng: bus.lng };
        const routeToBus = await fetchDrivingRoute(user, busPos, "현재 위치");
        drawRouteOnMap(mapToBus, user, busPos, routeToBus);
      } else {
        setText("busInfo", "주변 버스정류장을 찾지 못했습니다.");
      }
    } catch (e) {
      setText("busInfo", "버스 검색 실패");
    }
  }

  function bootstrap() {
    if (initialized) return;
    initialized = true;
    init().catch(() => {
      setText("distanceInfo", "초기화 실패");
      setText("subwayInfo", "초기화 실패");
      setText("busInfo", "초기화 실패");
    });
  }

  // accomDtl 페이지와 동일하게 네이버 지도 callback 방식으로 초기화
  window.initTransportMap = bootstrap;

  // callback이 호출되지 않는 환경에서도 한 번 더 시도
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
      if (window.naver && window.naver.maps) {
        bootstrap();
      }
    }, { once: true });
  } else if (window.naver && window.naver.maps) {
    bootstrap();
  }
})();

