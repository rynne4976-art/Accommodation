let map;
let accomLat = null;
let accomLng = null;
let accomMarker = null;
let startMarker = null;
let routeLine = null;
let transportMarkers = [];
let selectedRouteMode = "car";
let nearbyTransportState = { bus: [], subway: [] };
let lastRouteContext = null;
let lastRouteMetrics = null;
const geocodeCache = new Map();
const nearbyTransportCache = new Map();

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
  bindDestinationSelect();
  initMap();

  const select = getElement("destinationSelect");
  const selectedOption = select?.options?.[select.selectedIndex];

  const selectedAddress = selectedOption?.dataset?.location?.trim() || "";
  const selectedName = selectedOption?.dataset?.name?.trim() || "";
  const selectedImage = selectedOption?.dataset?.image?.trim() || "";

  const hiddenAddress = getElement("accomLocation")?.value?.trim() || "";
  const hiddenName = getElement("accomName")?.value?.trim() || "";
  const hiddenImage = getElement("accomImage")?.value?.trim() || "";

  const address = selectedAddress || hiddenAddress;
  const accomName = selectedName || hiddenName || address;
  const accomImage = selectedImage || hiddenImage;

  if (!address) {
    setDistanceInfo("상단 도착지 드롭다운에서 숙소를 선택하면 교통 정보가 표시됩니다.");
    renderTransportGroups([], []);
    updateDestinationSummary("숙소를 선택해 주세요.");
    refreshMapSize();
    return;
  }

  try {
    await applyAccommodationDestination(accomName, address, accomImage);
  } catch (error) {
    console.error("교통 페이지 초기화 실패", error);
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

  refreshMapSize();
  window.addEventListener("resize", refreshMapSize);
}

function refreshMapSize() {
  if (!map) {
    return;
  }

  setTimeout(() => {
    map.invalidateSize();
  }, 100);
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

function bindDestinationSelect() {
  const select = getElement("destinationSelect");
  if (!select) {
    return;
  }

  select.addEventListener("change", () => {
    applySelectedDestination();
  });
}

function syncRouteModeButtons() {
  document.querySelectorAll(".route-mode[data-mode]").forEach(button => {
    button.classList.toggle("route-mode--active", button.dataset.mode === selectedRouteMode);
  });
}

async function geocodeAddress(address) {
  const cacheKey = address.trim();
  if (geocodeCache.has(cacheKey)) {
    return geocodeCache.get(cacheKey);
  }

  const response = await fetch(`/api/transport/geocode?address=${encodeURIComponent(address)}`, {
    headers: { Accept: "application/json" }
  });
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || "주소 좌표를 찾지 못했습니다.");
  }

  const geocode = {
    lat: Number(data.lat),
    lng: Number(data.lng)
  };

  geocodeCache.set(cacheKey, geocode);
  return geocode;
}

async function applyAccommodationDestination(name, address, imageUrl = "") {
  if (!address) {
    resetAccommodationSelection();
    return;
  }

  const trimmedAddress = address.trim();
  const trimmedName = (name || address).trim();
  const trimmedImageUrl = (imageUrl || "").trim();

  const currentAddress = getElement("accomLocation")?.value?.trim() || "";

  if (getElement("accomLocation")) {
    getElement("accomLocation").value = trimmedAddress;
  }
  if (getElement("accomName")) {
    getElement("accomName").value = trimmedName;
  }
  if (getElement("accomImage")) {
    getElement("accomImage").value = trimmedImageUrl;
  }

  const select = getElement("destinationSelect");
  if (select) {
    Array.from(select.options).forEach(option => {
      const optionAddress = option.dataset.location?.trim() || "";
      option.selected = optionAddress === trimmedAddress;
    });
  }

  syncAccommodationUi(trimmedName, trimmedAddress, trimmedImageUrl);
  updateDestinationSummary(trimmedName);

  if (trimmedAddress === currentAddress && accomLat != null && accomLng != null) {
    refreshMapSize();
    return;
  }

  lastRouteContext = null;
  lastRouteMetrics = null;
  clearRouteLine();
  updateRouteSummary("미설정", "-", "-");
  setModeTimeLabel("carTimeLabel", null, "자동차");
  setModeTimeLabel("walkTimeLabel", null, "도보");
  setModeTimeLabel("transitTimeLabel", null, "대중교통");

  const geocode = await geocodeAddress(trimmedAddress);
  accomLat = geocode.lat;
  accomLng = geocode.lng;

  updateAccommodationMarker();
  map.flyTo([accomLat, accomLng], 15, { duration: 0.5 });
  refreshMapSize();

  await loadNearbyTransport();
  setDistanceInfo(`선택한 숙소 기준으로 주변 교통 정보를 표시합니다.`);
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
  const accomAddress = getElement("accomAddress");
  const stayImage = getElement("stayImage");
  const stayIcon = getElement("stayIcon");

  if (stayCard) {
    stayCard.classList.remove("is-hidden");
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
      stayImage.removeAttribute("src");
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

  if (startMarker) {
    map.removeLayer(startMarker);
    startMarker = null;
  }

  clearTransportMarkers();
  clearRouteLine();
  renderTransportGroups([], []);
  updateRouteSummary("미설정", "-", "-");
  updateDestinationSummary("숙소를 선택해 주세요.");
  setModeTimeLabel("carTimeLabel", null, "자동차");
  setModeTimeLabel("walkTimeLabel", null, "도보");
  setModeTimeLabel("transitTimeLabel", null, "대중교통");

  if (getElement("accomLocation")) getElement("accomLocation").value = "";
  if (getElement("accomName")) getElement("accomName").value = "";
  if (getElement("accomImage")) getElement("accomImage").value = "";

  const stayCard = getElement("stayCard");
  const stayImage = getElement("stayImage");
  const stayIcon = getElement("stayIcon");
  const stayTitle = getElement("stayTitle");
  const accomAddress = getElement("accomAddress");
  const select = getElement("destinationSelect");

  if (select) {
    select.selectedIndex = 0;
  }

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
  if (accomAddress) {
    accomAddress.innerText = "상단 도착지 드롭다운에서 숙소를 선택해 주세요.";
  }

  setDistanceInfo("상단 도착지 드롭다운에서 숙소를 선택하면 교통 정보가 표시됩니다.");
  map.flyTo(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM, { duration: 0.5 });
  refreshMapSize();
}

function updateAccommodationMarker() {
  if (accomLat == null || accomLng == null) {
    return;
  }

  if (accomMarker) {
    map.removeLayer(accomMarker);
  }

  accomMarker = L.marker([accomLat, accomLng]).addTo(map);
  accomMarker.bindPopup("숙소 위치");
}

async function loadNearbyTransport() {
  if (accomLat == null || accomLng == null) {
    renderTransportGroups([], []);
    return;
  }

  const cacheKey = `${accomLat.toFixed(5)},${accomLng.toFixed(5)}`;
  if (nearbyTransportCache.has(cacheKey)) {
    const cached = nearbyTransportCache.get(cacheKey);
    nearbyTransportState = { bus: cached.bus, subway: cached.subway };
    drawTransportMarkers(cached.items);
    renderTransportGroups(cached.bus, cached.subway);
    setDistanceInfo(
        cached.items.length
            ? `주변 교통 ${cached.items.length}곳을 지도에 표시했습니다.`
            : "선택한 숙소 주변의 교통 정보를 찾지 못했습니다."
    );
    refreshMapSize();
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

  nearbyTransportCache.set(cacheKey, {
    items,
    bus: busStops,
    subway: subwayStations
  });

  nearbyTransportState = { bus: busStops, subway: subwayStations };
  drawTransportMarkers(items);
  renderTransportGroups(busStops, subwayStations);

  if (!items.length) {
    setDistanceInfo("선택한 숙소 주변의 교통 정보를 찾지 못했습니다.");
    refreshMapSize();
    return;
  }

  setDistanceInfo(`주변 교통 ${items.length}곳을 지도에 표시했습니다.`);
  refreshMapSize();
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
  refreshMapSize();
}

function clearTransportMarkers() {
  transportMarkers.forEach(entry => map.removeLayer(entry.marker));
  transportMarkers = [];
}

async function searchRoute() {
  const start = getElement("startLocation")?.value?.trim() || "";

  if (!start) {
    alert("출발지를 먼저 입력해 주세요.");
    return;
  }

  if (accomLat == null || accomLng == null) {
    alert("도착 숙소를 먼저 선택해 주세요.");
    return;
  }

  try {
    const location = await geocodeAddress(start);
    setStartMarker(location.lat, location.lng, start);
    updateRouteSummary(start, "-", "-");
    map.flyTo([location.lat, location.lng], Math.max(map.getZoom(), 14), { duration: 0.6 });
    refreshMapSize();
    await calculateAndRenderRoutes({ lat: location.lat, lng: location.lng, label: start });
  } catch (error) {
    console.error("길찾기 실패", error);
    alert("출발지를 찾을 수 없습니다.");
  }
}

async function useMyAddress() {
  if (accomLat == null || accomLng == null) {
    alert("도착 숙소를 먼저 선택해 주세요.");
    return;
  }

  const myAddress = getElement("myPageAddress")?.value?.trim() || "";

  if (!myAddress) {
    alert("마이페이지에 저장된 주소가 없습니다.");
    return;
  }

  const startInput = getElement("startLocation");
  if (startInput) {
    startInput.value = myAddress;
  }

  try {
    const location = await geocodeAddress(myAddress);
    setStartMarker(location.lat, location.lng, "내 주소");
    updateRouteSummary(myAddress, "-", "-");
    map.flyTo([location.lat, location.lng], Math.max(map.getZoom(), 14), { duration: 0.6 });
    refreshMapSize();
    await calculateAndRenderRoutes({ lat: location.lat, lng: location.lng, label: myAddress });
  } catch (error) {
    console.error("내 주소 경로 계산 실패", error);
    alert("마이페이지 주소로 경로를 찾을 수 없습니다.");
  }
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
      selectedMetrics.minutes == null
          ? `${MODE_META[selectedRouteMode].label} 정보 없음`
          : `${MODE_META[selectedRouteMode].label} 약 ${selectedMetrics.minutes}분`
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
  refreshMapSize();
}

function clearRouteLine() {
  if (routeLine) {
    map.removeLayer(routeLine);
    routeLine = null;
  }
}

function setStartMarker(lat, lng, label) {
  if (startMarker) {
    map.removeLayer(startMarker);
  }

  startMarker = L.marker([lat, lng]).addTo(map);
  startMarker.bindPopup(label);
}

function updateRouteSummary(startText, distanceText, durationText) {
  const startSummary = getElement("startSummary");
  const distanceSummary = getElement("distanceSummary");
  const durationSummary = getElement("durationSummary");

  if (startSummary) startSummary.innerText = startText || "미설정";
  if (distanceSummary) distanceSummary.innerText = distanceText;
  if (durationSummary) durationSummary.innerText = durationText;
}

function updateDestinationSummary(text) {
  const destinationSummaryCard = getElement("destinationSummaryCard");
  if (destinationSummaryCard) {
    destinationSummaryCard.innerText = text || "숙소를 선택해 주세요.";
  }
}

function setPresetStart(place) {
  const startInput = getElement("startLocation");
  if (startInput) {
    startInput.value = place;
  }
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
  refreshMapSize();
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