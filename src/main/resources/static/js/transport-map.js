let map;
let accomLat = null;
let accomLng = null;
let accomMarker = null;
let startMarker = null;
let routeLine = null;
let currentLocationMarker = null;
let currentAccuracyCircle = null;
let transportMarkers = [];
let selectedRouteMode = "car";
let nearbyTransportState = { bus: [], subway: [] };
let lastRouteContext = null;
let lastRouteMetrics = null;

const DEFAULT_MAP_CENTER = [37.5665, 126.9780];
const DEFAULT_MAP_ZOOM = 12;

const MODE_META = {
  transit: { label: "대중교통", color: "#2563eb", dashArray: "8 10" },
  car: { label: "자동차", color: "#7b3ff2", dashArray: null },
  walk: { label: "도보", color: "#16a34a", dashArray: "10 8" }
};

const ROUTE_PROVIDERS = {
  driving: "https://routing.openstreetmap.de/routed-car/route/v1/driving",
  foot: "https://routing.openstreetmap.de/routed-foot/route/v1/driving"
};

document.addEventListener("DOMContentLoaded", init);

async function init() {
  bindRouteModeButtons();
  initMap();

  const address = getElement("accomLocation")?.value?.trim() || "";
  const accomName = getElement("accomName")?.value?.trim() || "";
  const accomImage = getElement("accomImage")?.value?.trim() || "";

  if (!address) {
    setStatus("도착지를 선택해 주세요.");
    setDistanceInfo("상단 도착지 드롭다운에서 숙소를 선택하면 교통 정보가 표시됩니다.");
    renderTransportGroups([], []);
    return;
  }

  try {
    await applyAccommodationDestination(accomName || address, address, accomImage);
  } catch (error) {
    console.error("교통 페이지 초기화 실패", error);
    setStatus("도착지 정보를 불러오지 못했습니다.");
    setDistanceInfo("다시 시도해 주세요.");
    renderTransportGroups([], []);
  }
}

function getElement(id) {
  return document.getElementById(id);
}

function initMap() {
  map = L.map("map", { zoomControl: false }).setView(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM);

  L.control.zoom({ position: "bottomright" }).addTo(map);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
  }).addTo(map);
}

function bindRouteModeButtons() {
  document.querySelectorAll(".route-mode[data-mode]").forEach(button => {
    button.addEventListener("click", () => {
      const mode = button.dataset.mode;
      if (!mode) {
        return;
      }

      selectedRouteMode = mode;
      syncRouteModeButtons();
      renderSelectedMode();
    });
  });

  syncRouteModeButtons();
}

function syncRouteModeButtons() {
  document.querySelectorAll(".route-mode[data-mode]").forEach(button => {
    button.classList.toggle("route-mode--active", button.dataset.mode === selectedRouteMode);
  });
}

async function geocodeAddress(address) {
  const response = await fetch(`/api/transport/geocode?address=${encodeURIComponent(address)}`, {
    headers: { Accept: "application/json" }
  });
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || "주소 좌표를 찾지 못했습니다.");
  }

  return {
    lat: Number(data.lat),
    lng: Number(data.lng)
  };
}

async function applyAccommodationDestination(name, address, imageUrl = "") {
  if (!address) {
    resetAccommodationSelection();
    return;
  }

  setStatus("도착지 위치를 확인하는 중입니다.");
  const geocode = await geocodeAddress(address);
  accomLat = geocode.lat;
  accomLng = geocode.lng;

  getElement("accomLocation").value = address;
  getElement("accomName").value = name || address;
  getElement("accomImage").value = imageUrl || "";

  syncAccommodationUi(name || address, address, imageUrl || "");
  updateAccommodationMarker();
  clearRouteLine();
  map.flyTo([accomLat, accomLng], 15, { duration: 0.5 });

  setStatus("주변 교통 정보를 불러오는 중입니다.");
  await loadNearbyTransport();
  setStatus("지도가 준비되었습니다.");
}

function applySelectedDestination() {
  const select = getElement("destinationSelect");
  if (!select) {
    return;
  }

  const option = select.options[select.selectedIndex];
  const address = option?.dataset?.location || "";
  const name = option?.dataset?.name || option?.text || "";
  const imageUrl = option?.dataset?.image || "";

  if (!address) {
    resetAccommodationSelection();
    return;
  }

  applyAccommodationDestination(name, address, imageUrl).catch(error => {
    console.error("도착지 선택 실패", error);
    alert("숙소 정보를 불러오지 못했습니다.");
  });
}

function syncAccommodationUi(name, address, imageUrl) {
  const stayCard = getElement("stayCard");
  const stayTitle = getElement("stayTitle");
  const destinationSummaryCard = getElement("destinationSummaryCard");
  const accomAddress = getElement("accomAddress");
  const stayImage = getElement("stayImage");
  const stayIcon = getElement("stayIcon");

  if (stayCard) {
    stayCard.classList.remove("is-hidden");
  }
  if (destinationSummaryCard) {
    destinationSummaryCard.innerText = name;
  }
  if (stayTitle) {
    stayTitle.innerText = name;
  }
  if (accomAddress) {
    accomAddress.innerText = address;
  }
  if (stayImage) {
    if (imageUrl) {
      stayImage.src = imageUrl;
      stayImage.classList.remove("is-hidden");
    } else {
      stayImage.classList.add("is-hidden");
    }
  }
  if (stayIcon) {
    stayIcon.classList.toggle("is-hidden", Boolean(imageUrl));
  }
}

function resetAccommodationSelection() {
  accomLat = null;
  accomLng = null;
  lastRouteContext = null;
  lastRouteMetrics = null;

  if (accomMarker) {
    map.removeLayer(accomMarker);
    accomMarker = null;
  }

  clearTransportMarkers();
  clearRouteLine();
  renderTransportGroups([], []);
  updateRouteSummary("미설정", "-", "-");
  setModeTimeLabel("carTimeLabel", null, "자동차");
  setModeTimeLabel("walkTimeLabel", null, "도보");
  setModeTimeLabel("transitTimeLabel", null, "대중교통");

  getElement("accomLocation").value = "";
  getElement("accomName").value = "";
  getElement("accomImage").value = "";

  const stayCard = getElement("stayCard");
  const stayImage = getElement("stayImage");
  const stayIcon = getElement("stayIcon");
  const stayTitle = getElement("stayTitle");
  const destinationSummaryCard = getElement("destinationSummaryCard");
  const accomAddress = getElement("accomAddress");

  if (stayCard) {
    stayCard.classList.add("is-hidden");
  }
  if (stayImage) {
    stayImage.classList.add("is-hidden");
    stayImage.removeAttribute("src");
  }
  if (stayIcon) {
    stayIcon.classList.remove("is-hidden");
  }
  if (stayTitle) {
    stayTitle.innerText = "숙소를 선택해 주세요";
  }
  if (destinationSummaryCard) {
    destinationSummaryCard.innerText = "도착지를 선택해 주세요";
  }
  if (accomAddress) {
    accomAddress.innerText = "상단 도착지 드롭다운에서 숙소를 선택해 주세요.";
  }

  setStatus("도착지를 선택해 주세요.");
  setDistanceInfo("상단 도착지 드롭다운에서 숙소를 선택하면 교통 정보가 표시됩니다.");
  map.flyTo(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM, { duration: 0.5 });
}

function updateAccommodationMarker() {
  if (accomLat == null || accomLng == null) {
    return;
  }

  if (accomMarker) {
    map.removeLayer(accomMarker);
  }

  accomMarker = L.marker([accomLat, accomLng]).addTo(map);
  accomMarker.bindPopup("숙소 위치").openPopup();
}

async function loadNearbyTransport() {
  if (accomLat == null || accomLng == null) {
    renderTransportGroups([], []);
    return;
  }

  const response = await fetch(`/api/transport/nearby?lat=${accomLat}&lng=${accomLng}`, {
    headers: { Accept: "application/json" }
  });
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || "주변 교통 정보를 불러오지 못했습니다.");
  }

  const items = Array.isArray(data.items) ? data.items : [];
  const busStops = items.filter(item => item.type === "bus");
  const subwayStations = items.filter(item => item.type === "subway");

  nearbyTransportState = { bus: busStops, subway: subwayStations };
  drawTransportMarkers(items);
  renderTransportGroups(busStops, subwayStations);

  if (!items.length) {
    setDistanceInfo("선택한 숙소 주변의 교통 정보를 찾지 못했습니다.");
    return;
  }

  setDistanceInfo(`주변 교통 ${items.length}곳을 지도에 표시했습니다.`);
}

function drawTransportMarkers(items) {
  clearTransportMarkers();

  items.forEach(item => {
    const isBus = item.type === "bus";
    const marker = L.marker([Number(item.lat), Number(item.lng)], {
      icon: L.divIcon({
        html: isBus ? "&#128652;" : "&#128647;",
        className: isBus ? "bus-marker" : "subway-marker",
        iconSize: [26, 26]
      })
    }).addTo(map);

    const typeLabel = isBus ? "버스 정류장" : "지하철역";
    marker.bindTooltip(
      `${escapeHtml(item.name)}<br>${typeLabel} / 도보 ${Number(item.walkMinutes)}분`,
      { direction: "top", offset: [0, -8] }
    );

    marker.on("click", () => activateTransportItem(item.type, item.name));
    transportMarkers.push({ marker, item });
  });
}

function renderTransportGroups(busStops, subwayStations) {
  renderTransportList("busList", busStops, "bus");
  renderTransportList("subwayList", subwayStations, "subway");

  const busCountBadge = getElement("busCountBadge");
  const subwayCountBadge = getElement("subwayCountBadge");
  if (busCountBadge) {
    busCountBadge.innerText = String(busStops.length);
  }
  if (subwayCountBadge) {
    subwayCountBadge.innerText = String(subwayStations.length);
  }
}

function renderTransportList(listId, items, type) {
  const listEl = getElement(listId);
  if (!listEl) {
    return;
  }

  const isBus = type === "bus";
  if (!items.length) {
    listEl.innerHTML = `<li class="transport-empty">${isBus ? "버스 정류장" : "지하철역"} 정보가 없습니다.</li>`;
    return;
  }

  listEl.innerHTML = items.map(item => `
    <li>
      <button type="button" class="transport-item" data-type="${type}" data-name="${escapeHtml(item.name)}">
        <span class="transport-item-icon transport-item-icon--${type}">
          ${isBus ? "&#128652;" : "&#128647;"}
        </span>
        <span class="transport-item-copy">
          <span class="transport-item-name">${escapeHtml(item.name)}</span>
          <span class="transport-item-meta">도보 ${Number(item.walkMinutes)}분 / ${Number(item.distance)}m</span>
        </span>
        <span class="transport-item-distance">${isBus ? "버스" : "지하철"}</span>
      </button>
    </li>
  `).join("");

  listEl.querySelectorAll(".transport-item").forEach(button => {
    button.addEventListener("click", () => activateTransportItem(button.dataset.type, button.dataset.name));
  });
}

function activateTransportItem(type, name) {
  const markerEntry = transportMarkers.find(entry => entry.item.type === type && entry.item.name === name);
  if (!markerEntry) {
    return;
  }

  document.querySelectorAll(".transport-item.is-active").forEach(element => {
    element.classList.remove("is-active");
  });

  document.querySelectorAll(`.transport-item[data-type="${type}"]`).forEach(button => {
    if (button.dataset.name === name) {
      button.classList.add("is-active");
      button.scrollIntoView({ block: "nearest", behavior: "smooth" });
    }
  });

  map.flyTo([Number(markerEntry.item.lat), Number(markerEntry.item.lng)], Math.max(map.getZoom(), 16), {
    duration: 0.6
  });
  markerEntry.marker.openTooltip();
}

function clearTransportMarkers() {
  transportMarkers.forEach(entry => map.removeLayer(entry.marker));
  transportMarkers = [];
}

async function searchRoute() {
  const start = getElement("startLocation").value.trim();

  if (!start) {
    alert("출발지를 먼저 입력해 주세요.");
    return;
  }

  if (accomLat == null || accomLng == null) {
    alert("도착 숙소를 먼저 선택해 주세요.");
    return;
  }

  try {
    setStatus("출발지를 찾는 중입니다.");
    const location = await geocodeAddress(start);
    setStartMarker(location.lat, location.lng, "출발 위치");
    updateRouteSummary(start, "-", "-");
    await calculateAndRenderRoutes({ lat: location.lat, lng: location.lng, label: start });
  } catch (error) {
    console.error("길찾기 실패", error);
    setStatus("출발지를 찾지 못했습니다.");
    alert("출발지를 찾을 수 없습니다.");
  }
}

function useMyLocation() {
  if (accomLat == null || accomLng == null) {
    alert("도착 숙소를 먼저 선택해 주세요.");
    return;
  }

  if (!navigator.geolocation) {
    alert("브라우저가 위치 기능을 지원하지 않습니다.");
    return;
  }

  setStatus("현재 위치를 확인하는 중입니다.");

  navigator.geolocation.getCurrentPosition(
    async position => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;
      const accuracy = position.coords.accuracy || 0;

      drawCurrentLocation(lat, lng, accuracy);
      setStartMarker(lat, lng, "내 위치");
      map.flyTo([lat, lng], Math.max(map.getZoom(), 16), { duration: 0.6 });

      try {
        await calculateAndRenderRoutes({ lat, lng, label: "내 위치" });
      } catch (error) {
        console.error("현재 위치 경로 계산 실패", error);
        setStatus("현재 위치는 찾았지만 경로 계산에 실패했습니다.");
      }
    },
    error => {
      console.error("현재 위치 확인 실패", error);
      setStatus("현재 위치를 확인하지 못했습니다.");
      alert("위치 권한을 허용한 뒤 다시 시도해 주세요.");
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0
    }
  );
}

async function calculateAndRenderRoutes(startContext) {
  lastRouteContext = startContext;

  const [carRoute, walkRoute, transitRoute] = await Promise.all([
    fetchOsrmRoute(startContext.lat, startContext.lng, "driving"),
    fetchOsrmRoute(startContext.lat, startContext.lng, "foot"),
    fetchTransitRoute(startContext.lat, startContext.lng)
  ]);

  lastRouteMetrics = buildRouteMetrics(startContext, carRoute, walkRoute, transitRoute);
  renderModeDurations(lastRouteMetrics);
  renderSelectedMode();
}

async function fetchOsrmRoute(startLat, startLng, profile) {
  const baseUrl = ROUTE_PROVIDERS[profile];
  if (!baseUrl) {
    throw new Error("지원하지 않는 경로 프로필입니다.");
  }

  const url = `${baseUrl}/${startLng},${startLat};${accomLng},${accomLat}?overview=full&geometries=geojson`;
  const response = await fetch(url);
  const data = await response.json();

  if (!response.ok || !data.routes || !data.routes.length) {
    throw new Error("경로를 찾지 못했습니다.");
  }

  return data.routes[0];
}

async function fetchTransitRoute(startLat, startLng) {
  const webKey = getElement("odsayWebKey")?.value?.trim();
  if (webKey) {
    return fetchTransitRouteFromOdsay(startLat, startLng, webKey);
  }

  const response = await fetch(
    `/api/transport/transit-route?sx=${startLng}&sy=${startLat}&ex=${accomLng}&ey=${accomLat}`,
    { headers: { Accept: "application/json" } }
  );
  const data = await response.json();
  return response.ok ? data : null;
}

async function fetchTransitRouteFromOdsay(startLat, startLng, webKey) {
  const url =
    "https://api.odsay.com/v1/api/searchPubTransPathT"
    + `?apiKey=${encodeURIComponent(webKey)}`
    + `&SX=${startLng}`
    + `&SY=${startLat}`
    + `&EX=${accomLng}`
    + `&EY=${accomLat}`
    + "&lang=0";

  const response = await fetch(url, { headers: { Accept: "application/json" } });
  const data = await response.json();
  const errorMessage = extractOdsayErrorMessage(data);

  if (!response.ok || errorMessage) {
    console.error("ODsay transit fetch failed", errorMessage || data);
    return null;
  }

  return normalizeOdsayTransitRoute(data);
}

function extractOdsayErrorMessage(data) {
  const errorNode = data?.error;
  if (Array.isArray(errorNode) && errorNode.length > 0) {
    return errorNode[0]?.message || "ODsay API error";
  }

  if (errorNode?.msg) {
    return errorNode.msg;
  }

  return "";
}

function normalizeOdsayTransitRoute(data) {
  const path = data?.result?.path?.[0];
  const info = path?.info;

  if (!info || !Number.isFinite(Number(info.totalTime))) {
    return null;
  }

  return {
    totalTime: Number(info.totalTime),
    payment: Number(info.payment || 0),
    busTransitCount: Number(info.busTransitCount || 0),
    subwayTransitCount: Number(info.subwayTransitCount || 0),
    firstStartStation: info.firstStartStation || "",
    lastEndStation: info.lastEndStation || "",
    totalWalk: Number(info.totalWalk || 0),
    pathType: Number(path.pathType || 0)
  };
}

function buildRouteMetrics(startContext, carRoute, walkRoute, transitRoute) {
  const carMinutes = Math.max(1, Math.round(carRoute.duration / 60));
  const walkMinutes = Math.max(1, Math.round(walkRoute.duration / 60));
  const distanceKm = Number((carRoute.distance / 1000).toFixed(2));

  return {
    startLabel: startContext.label,
    distanceKm,
    car: {
      minutes: carMinutes,
      distanceKm,
      geometry: carRoute.geometry
    },
    walk: {
      minutes: walkMinutes,
      distanceKm: Number((walkRoute.distance / 1000).toFixed(2)),
      geometry: walkRoute.geometry
    },
    transit: buildTransitMetrics(distanceKm, transitRoute)
  };
}

function buildTransitMetrics(distanceKm, transitRoute) {
  if (!transitRoute || !Number.isFinite(Number(transitRoute.totalTime))) {
    return {
      minutes: null,
      distanceKm,
      geometry: null,
      detail: "ODsay 경로 없음"
    };
  }

  const detailParts = [];
  if (Number(transitRoute.busTransitCount) > 0) {
    detailParts.push(`버스 ${Number(transitRoute.busTransitCount)}회`);
  }
  if (Number(transitRoute.subwayTransitCount) > 0) {
    detailParts.push(`지하철 ${Number(transitRoute.subwayTransitCount)}회`);
  }
  if (Number(transitRoute.totalWalk) > 0) {
    detailParts.push(`도보 ${Number(transitRoute.totalWalk)}m`);
  }
  if (transitRoute.firstStartStation) {
    detailParts.push(`${transitRoute.firstStartStation} 출발`);
  }

  return {
    minutes: Math.max(1, Number(transitRoute.totalTime)),
    distanceKm,
    geometry: null,
    detail: detailParts.join(" / ")
  };
}

function renderModeDurations(metrics) {
  setModeTimeLabel("carTimeLabel", metrics.car.minutes, "자동차");
  setModeTimeLabel("walkTimeLabel", metrics.walk.minutes, "도보");
  setModeTimeLabel("transitTimeLabel", metrics.transit.minutes, "대중교통");
}

function setModeTimeLabel(elementId, minutes, fallbackLabel) {
  const element = getElement(elementId);
  if (!element) {
    return;
  }

  element.innerText = minutes == null ? `${fallbackLabel} 정보 없음` : `약 ${minutes}분`;
}

function renderSelectedMode() {
  if (!lastRouteMetrics || !lastRouteContext) {
    syncRouteModeButtons();
    return;
  }

  syncRouteModeButtons();

  const selectedMetrics = lastRouteMetrics[selectedRouteMode];
  if (!selectedMetrics) {
    return;
  }

  updateRouteSummary(
    lastRouteMetrics.startLabel,
    `${selectedMetrics.distanceKm.toFixed(2)}km`,
    selectedMetrics.minutes == null ? `${MODE_META[selectedRouteMode].label} 정보 없음` : `${MODE_META[selectedRouteMode].label} 약 ${selectedMetrics.minutes}분`
  );

  if (selectedMetrics.minutes == null) {
    setDistanceInfo("선택한 이동 수단의 예상 시간을 계산하지 못했습니다.");
    clearRouteLine();
    return;
  }

  if (selectedRouteMode === "transit") {
    const transitDetail = lastRouteMetrics.transit.detail ? ` / ${lastRouteMetrics.transit.detail}` : "";
    setDistanceInfo(`총 이동 거리 ${selectedMetrics.distanceKm.toFixed(2)}km / 예상 소요 시간 약 ${selectedMetrics.minutes}분${transitDetail}`);
  } else {
    setDistanceInfo(`총 이동 거리 ${selectedMetrics.distanceKm.toFixed(2)}km / ${MODE_META[selectedRouteMode].label} 기준 약 ${selectedMetrics.minutes}분`);
  }

  drawSelectedRouteLine(selectedMetrics.geometry, selectedRouteMode);
  setStatus(`${MODE_META[selectedRouteMode].label} 기준 경로 안내가 준비되었습니다.`);
}

function drawSelectedRouteLine(geometry, mode) {
  clearRouteLine();

  const fallbackGeometry = mode === "transit" ? lastRouteMetrics?.car?.geometry : geometry;
  if (!fallbackGeometry) {
    return;
  }

  routeLine = L.geoJSON(fallbackGeometry, {
    style: {
      color: MODE_META[mode].color,
      weight: 5,
      opacity: 0.9,
      dashArray: MODE_META[mode].dashArray || null
    }
  }).addTo(map);

  map.fitBounds(routeLine.getBounds(), { padding: [28, 28] });
}

function clearRouteLine() {
  if (routeLine) {
    map.removeLayer(routeLine);
    routeLine = null;
  }
}

function drawCurrentLocation(lat, lng, accuracy) {
  if (currentLocationMarker) {
    map.removeLayer(currentLocationMarker);
  }
  if (currentAccuracyCircle) {
    map.removeLayer(currentAccuracyCircle);
  }

  currentLocationMarker = L.marker([lat, lng], {
    icon: L.divIcon({
      html: "&#128205;",
      className: "current-location-marker",
      iconSize: [22, 22]
    })
  }).addTo(map);

  currentLocationMarker.bindPopup("현재 위치").openPopup();

  if (accuracy > 0) {
    currentAccuracyCircle = L.circle([lat, lng], {
      radius: accuracy,
      color: "#0f766e",
      fillColor: "#14b8a6",
      fillOpacity: 0.08,
      weight: 1
    }).addTo(map);
  }
}

function setStartMarker(lat, lng, label) {
  if (startMarker) {
    map.removeLayer(startMarker);
  }

  startMarker = L.marker([lat, lng]).addTo(map);
  startMarker.bindPopup(label).openPopup();
}

function updateRouteSummary(startText, distanceText, durationText) {
  getElement("startSummary").innerText = startText || "미설정";
  getElement("distanceSummary").innerText = distanceText;
  getElement("durationSummary").innerText = durationText;
}

function setPresetStart(place) {
  getElement("startLocation").value = place;
  searchRoute();
}

function focusAccommodation() {
  if (!map || accomLat == null || accomLng == null) {
    return;
  }

  map.flyTo([accomLat, accomLng], 15, { duration: 0.5 });
  if (accomMarker) {
    accomMarker.openPopup();
  }
}

function setStatus(text) {
  const statusText = getElement("statusText");
  if (statusText) {
    statusText.innerText = text;
  }
}

function setDistanceInfo(text) {
  const distanceInfo = getElement("distanceInfo");
  if (distanceInfo) {
    distanceInfo.innerText = text;
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}
