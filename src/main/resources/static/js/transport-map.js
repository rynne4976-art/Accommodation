let map;
let accomLat = null;
let accomLng = null;
let accomMarker = null;
let startMarker = null;
let routeLine = null;
let transportMarkers = [];
let selectedRouteMode = null;
let lastRouteContext = null;
let lastRouteMetrics = null;
let routeRequestSeq = 0;
let accomSearchPage = 0;
let accomSearchTotalPages = 0;

const geocodeCache = new Map();
const nearbyTransportCache = new Map();

const DEFAULT_MAP_CENTER = [37.5665, 126.9780];
const DEFAULT_MAP_ZOOM = 12;
const LOGIN_URL = "/members/login";
const INTERCITY_LIMIT_KM = 45;
const DEFAULT_TIME_LABEL = "예상 시간";
const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") || "";

const MODE_META = {
  transit: { label: "대중교통", color: "#2563eb", dashArray: "8 10" },
  car: { label: "자동차", color: "#7b3ff2", dashArray: null }
};

document.addEventListener("DOMContentLoaded", init);

async function init() {
  bindRouteModeButtons();
  bindDestinationInput();
  bindDestinationActions();
  bindAccomSearchModal();
  bindReservedAccomModal();
  initMap();

  const hiddenAddress = getElement("accomLocation")?.value?.trim() || "";
  const hiddenName = getElement("accomName")?.value?.trim() || "";
  const hiddenImage = getElement("accomImage")?.value?.trim() || "";
  const hiddenAccomId = getElement("accomId")?.value?.trim() || "";

  const address = hiddenAddress;
  const accomName = hiddenName || address;
  const accomImage = hiddenImage;

  syncDestinationInput(hiddenAddress || hiddenName || "");

  if (!address) {
    resetUiWithoutDestination();
    refreshMapSize();
    return;
  }

  try {
    await applyAccommodationDestination(accomName, address, accomImage, hiddenAccomId);
  } catch (error) {
    console.error("교통 페이지 초기화 실패", error);
    resetUiWithoutDestination();
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
  if (!map) return;

  setTimeout(() => {
    map.invalidateSize();
  }, 100);
}

function bindRouteModeButtons() {
  document.querySelectorAll(".route-mode[data-mode]").forEach(button => {
    button.addEventListener("click", () => {
      const mode = button.dataset.mode;
      if (!mode) return;

      selectedRouteMode = mode;
      renderSelectedMode();
    });
  });

  syncRouteModeButtons();
}

function bindDestinationInput() {
  const input = getElement("destinationInput");
  if (!input) return;

  input.addEventListener("keydown", event => {
    if (event.key !== "Enter") return;

    event.preventDefault();
    applyTypedDestination().catch(error => {
      console.error("도착지 직접 입력 처리 실패", error);
      alert("입력한 도착지를 불러오지 못했습니다.");
    });
  });

  input.addEventListener("blur", () => {
    if (!input.value.trim()) return;

    applyTypedDestination().catch(error => {
      console.error("도착지 직접 입력 처리 실패", error);
    });
  });
}

function bindDestinationActions() {
  getElement("openAccomSearchBtn")?.addEventListener("click", () => {
    openAccomSearchModal();
  });

  getElement("fillReservedAccomBtn")?.addEventListener("click", async () => {
    try {
      const { ok, body } = await fetchJsonOrRedirect("/api/transport/reserved-accommodation");
      if (!ok) {
        alert(body.message || "예약 숙소를 찾지 못했습니다.");
        return;
      }

      const items = Array.isArray(body.items) ? body.items : [];
      if (!items.length) {
        alert("예약 하신 숙소가 없습니다.");
        return;
      }

      if (items.length === 1) {
        const item = items[0];
        syncDestinationInput(item.location || item.name || "");
        await applyAccommodationDestination(
            item.name || item.location || "예약 숙소",
            item.location || "",
            item.imageUrl || "",
            item.id || ""
        );
        return;
      }

      openReservedAccomModal(items);
    } catch (error) {
      console.error("예약 숙소 자동 입력 실패", error);
      alert("예약 숙소를 불러오지 못했습니다.");
    }
  });
}

function bindAccomSearchModal() {
  getElement("closeAccomSearchBtn")?.addEventListener("click", closeAccomSearchModal);
  getElement("accomSearchSubmitBtn")?.addEventListener("click", () => {
    searchAccommodations(0).catch(error => {
      console.error("숙소 검색 실패", error);
      renderAccomSearchEmpty("숙소 검색 중 오류가 발생했습니다.");
    });
  });

  getElement("accomSearchKeyword")?.addEventListener("keydown", event => {
    if (event.key !== "Enter") return;
    event.preventDefault();
    searchAccommodations(0).catch(error => {
      console.error("숙소 검색 실패", error);
      renderAccomSearchEmpty("숙소 검색 중 오류가 발생했습니다.");
    });
  });

  getElement("accomSearchModal")?.addEventListener("click", event => {
    const target = event.target;
    if (!(target instanceof HTMLElement)) return;

    if (target.dataset.closeModal === "true") {
      closeAccomSearchModal();
      return;
    }

    const selectButton = target.closest("[data-accom-select]");
    if (!selectButton) return;

    const item = {
      id: selectButton.dataset.id || "",
      name: selectButton.dataset.name || "",
      location: selectButton.dataset.location || "",
      imageUrl: selectButton.dataset.image || ""
    };

    syncDestinationInput(item.location || item.name || "");
    handleAccommodationSelection(item, "숙소 선택 실패");
  });
}

function bindReservedAccomModal() {
  getElement("closeReservedAccomBtn")?.addEventListener("click", closeReservedAccomModal);
  getElement("reservedAccomModal")?.addEventListener("click", event => {
    const target = event.target;
    if (!(target instanceof HTMLElement)) return;

    if (target.dataset.closeReservedModal === "true") {
      closeReservedAccomModal();
      return;
    }

    const selectButton = target.closest("[data-reserved-accom-select]");
    if (!selectButton) return;

    const item = {
      id: selectButton.dataset.id || "",
      name: selectButton.dataset.name || "",
      location: selectButton.dataset.location || "",
      imageUrl: selectButton.dataset.image || ""
    };

    syncDestinationInput(item.location || item.name || "");
    handleAccommodationSelection(item, "예약 숙소 선택 실패");
  });
}

async function handleAccommodationSelection(item, errorLabel) {
  try {
    await applyAccommodationDestination(item.name, item.location, item.imageUrl, item.id);
  } catch (error) {
    console.error(errorLabel, error);
    alert("선택한 숙소를 적용하지 못했습니다.");
  }
}

function openAccomSearchModal() {
  const modal = getElement("accomSearchModal");
  if (!modal) return;

  modal.classList.remove("is-hidden");
  modal.setAttribute("aria-hidden", "false");

  if (!getElement("accomSearchResultList")?.innerHTML?.trim()) {
    searchAccommodations(0).catch(error => {
      console.error("숙소 검색 초기화 실패", error);
      renderAccomSearchEmpty("숙소 목록을 불러오지 못했습니다.");
    });
  }
}

function closeAccomSearchModal() {
  const modal = getElement("accomSearchModal");
  if (!modal) return;

  modal.classList.add("is-hidden");
  modal.setAttribute("aria-hidden", "true");
}

function openReservedAccomModal(items) {
  const modal = getElement("reservedAccomModal");
  const info = getElement("reservedAccomResultInfo");
  const list = getElement("reservedAccomResultList");
  if (!modal || !info || !list) return;

  info.innerText = `예약 숙소 ${items.length}개가 있습니다.`;
  list.innerHTML = items.map(item => `
    <article class="accom-search-item">
      <div>
        <h3 class="accom-search-item__title">${escapeHtml(item.name || "숙소")}</h3>
        <div class="accom-search-item__location">${escapeHtml(item.location || "-")}</div>
      </div>
      <button
        type="button"
        class="primary-btn accom-search-item__select"
        data-reserved-accom-select="true"
        data-id="${escapeHtml(String(item.id || ""))}"
        data-name="${escapeHtml(item.name || "")}"
        data-location="${escapeHtml(item.location || "")}"
        data-image="${escapeHtml(item.imageUrl || "")}">
        선택
      </button>
    </article>
  `).join("");

  modal.classList.remove("is-hidden");
  modal.setAttribute("aria-hidden", "false");
}

function closeReservedAccomModal() {
  const modal = getElement("reservedAccomModal");
  if (!modal) return;

  modal.classList.add("is-hidden");
  modal.setAttribute("aria-hidden", "true");
}

async function searchAccommodations(page = 0) {
  const keyword = getElement("accomSearchKeyword")?.value?.trim() || "";
  const grade = getElement("accomSearchGrade")?.value || "";
  const minPrice = getElement("accomSearchMinPrice")?.value?.trim() || "";
  const maxPrice = getElement("accomSearchMaxPrice")?.value?.trim() || "";
  const minRating = getElement("accomSearchMinRating")?.value || "";

  const params = new URLSearchParams({ page: String(page) });
  if (keyword) params.set("keyword", keyword);
  if (grade) params.set("grade", grade);
  if (minPrice) params.set("minPrice", minPrice);
  if (maxPrice) params.set("maxPrice", maxPrice);
  if (minRating) params.set("minRating", minRating);

  const { ok, body } = await fetchJsonOrRedirect(`/api/transport/accommodation-search?${params.toString()}`);
  if (!ok) {
    throw new Error(body.message || "숙소 검색에 실패했습니다.");
  }

  accomSearchPage = Number(body.page || 0);
  accomSearchTotalPages = Number(body.totalPages || 0);
  renderAccomSearchResults(Array.isArray(body.items) ? body.items : [], Number(body.totalElements || 0));
}

function renderAccomSearchResults(items, totalElements) {
  const info = getElement("accomSearchResultInfo");
  const list = getElement("accomSearchResultList");
  const pagination = getElement("accomSearchPagination");
  if (!info || !list || !pagination) return;

  info.innerText = totalElements > 0
      ? `총 ${totalElements}개의 숙소가 검색되었습니다.`
      : "검색 결과가 없습니다.";

  if (!items.length) {
    renderAccomSearchEmpty("조건에 맞는 숙소가 없습니다.");
    pagination.innerHTML = "";
    return;
  }

  list.innerHTML = items.map(item => `
    <article class="accom-search-item">
      <div>
        <h3 class="accom-search-item__title">${escapeHtml(item.name || "숙소")}</h3>
        <div class="accom-search-item__meta">등급 ${escapeHtml(String(item.grade || "-"))} / 1박 ${escapeHtml(Number(item.pricePerNight || 0).toLocaleString())}원 / 평점 ${escapeHtml(Number(item.avgRating || 0).toFixed(1))}</div>
        <div class="accom-search-item__location">${escapeHtml(item.location || "-")}</div>
      </div>
      <button
        type="button"
        class="primary-btn accom-search-item__select"
        data-accom-select="true"
        data-id="${escapeHtml(String(item.id || ""))}"
        data-name="${escapeHtml(item.name || "")}"
        data-location="${escapeHtml(item.location || "")}"
        data-image="${escapeHtml(item.imageUrl || "")}">
        선택
      </button>
    </article>
  `).join("");

  renderAccomSearchPagination();
}

function renderAccomSearchEmpty(message) {
  const list = getElement("accomSearchResultList");
  if (!list) return;
  list.innerHTML = `<div class="accom-search-empty">${escapeHtml(message)}</div>`;
}

function renderAccomSearchPagination() {
  const pagination = getElement("accomSearchPagination");
  if (!pagination) return;

  if (accomSearchTotalPages <= 1) {
    pagination.innerHTML = "";
    return;
  }

  const currentGroupStart = Math.floor(accomSearchPage / 5) * 5;
  const currentGroupEnd = Math.min(currentGroupStart + 5, accomSearchTotalPages);
  const buttons = [];

  if (currentGroupStart > 0) {
    buttons.push(`<button type="button" class="accom-search-pagination__btn" data-page="${currentGroupStart - 1}">이전</button>`);
  }

  for (let page = currentGroupStart; page < currentGroupEnd; page += 1) {
    buttons.push(`
      <button
        type="button"
        class="accom-search-pagination__btn${page === accomSearchPage ? " is-active" : ""}"
        data-page="${page}">
        ${page + 1}
      </button>
    `);
  }

  if (currentGroupEnd < accomSearchTotalPages) {
    buttons.push(`<button type="button" class="accom-search-pagination__btn" data-page="${currentGroupEnd}">다음</button>`);
  }

  pagination.innerHTML = buttons.join("");
  pagination.querySelectorAll("[data-page]").forEach(button => {
    button.addEventListener("click", () => {
      const page = Number(button.getAttribute("data-page"));
      searchAccommodations(page).catch(error => {
        console.error("숙소 페이지 이동 실패", error);
        renderAccomSearchEmpty("숙소 목록을 불러오지 못했습니다.");
      });
    });
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

  const { ok, body } = await fetchJsonOrRedirect(`/api/transport/geocode?address=${encodeURIComponent(cacheKey)}`);

  if (!ok) {
    throw new Error(body.message || "주소 좌표를 찾지 못했습니다.");
  }

  const geocode = {
    lat: Number(body.lat),
    lng: Number(body.lng)
  };

  geocodeCache.set(cacheKey, geocode);
  return geocode;
}

async function applyAccommodationDestination(name, address, imageUrl = "", accomId = "") {
  if (!address) {
    resetAccommodationSelection();
    return;
  }

  const trimmedAddress = address.trim();
  const trimmedName = (name || address).trim();
  const trimmedImageUrl = (imageUrl || "").trim();
  const currentAddress = getElement("accomLocation")?.value?.trim() || "";

  setHiddenAccommodation(trimmedName, trimmedAddress, trimmedImageUrl, accomId);
  syncDestinationInput(trimmedAddress);

  syncAccommodationUi(trimmedName, trimmedAddress, trimmedImageUrl);
  updateDestinationSummary(trimmedName);

  if (trimmedAddress === currentAddress && accomLat != null && accomLng != null) {
    lastRouteContext = null;
    lastRouteMetrics = null;
    clearRouteLine();
    updateRouteSummary("-", "-", "-");
    renderTransitDetail(null);
    resetModeTimeLabels();
    setDistanceInfo("선택한 숙소 기준으로 주변 교통 정보를 표시합니다.");
    refreshMapSize();
    return;
  }

  clearRouteLine();
  lastRouteContext = null;
  lastRouteMetrics = null;
  updateRouteSummary("-", "-", "-");
  renderTransitDetail(null);
  resetModeTimeLabels();

  const geocode = await geocodeAddress(trimmedAddress);
  accomLat = geocode.lat;
  accomLng = geocode.lng;

  updateAccommodationMarker();
  map.flyTo([accomLat, accomLng], 15, { duration: 0.5 });
  refreshMapSize();

  await loadNearbyTransport();
  setDistanceInfo("선택한 숙소 기준으로 주변 교통 정보를 표시합니다.");
}

function setHiddenAccommodation(name, address, imageUrl, accomId = "") {
  if (getElement("accomLocation")) getElement("accomLocation").value = address;
  if (getElement("accomName")) getElement("accomName").value = name;
  if (getElement("accomImage")) getElement("accomImage").value = imageUrl;
  if (getElement("accomId")) getElement("accomId").value = accomId || "";
}

function syncDestinationInput(value) {
  const input = getElement("destinationInput");
  if (input) input.value = value || "";
}

async function applyTypedDestination() {
  const destination = resolveTypedDestination();
  if (!destination) return;

  await applyAccommodationDestination(
      destination.name,
      destination.address,
      destination.imageUrl || "",
      destination.id || ""
  );
}

function resolveTypedDestination() {
  const input = getElement("destinationInput");
  const rawValue = input?.value?.trim() || "";

  if (!rawValue) return null;

  const matchedOption = findAccommodationOption(rawValue);
  if (matchedOption) {
    return {
      id: matchedOption.dataset.id || "",
      name: matchedOption.dataset.name || rawValue,
      address: matchedOption.dataset.location || rawValue,
      imageUrl: matchedOption.dataset.image || ""
    };
  }

  return {
    id: "",
    name: rawValue,
    address: rawValue,
    imageUrl: ""
  };
}

function findAccommodationOption(query) {
  const options = document.querySelectorAll("#destinationOptions option");
  const normalizedQuery = normalizeDestinationText(query);

  return Array.from(options).find(option => {
    const optionName = normalizeDestinationText(option.dataset.name || option.value || "");
    const optionAddress = normalizeDestinationText(option.dataset.location || "");

    return (
        optionName === normalizedQuery ||
        optionAddress === normalizedQuery ||
        optionName.includes(normalizedQuery) ||
        optionAddress.includes(normalizedQuery)
    );
  }) || null;
}

function normalizeDestinationText(value) {
  return String(value || "")
      .toLowerCase()
      .replace(/\s+/g, "")
      .trim();
}

function syncAccommodationUi(name, address, imageUrl) {
  const stayCard = getElement("stayCard");
  const stayTitle = getElement("stayTitle");
  const stayTitleLink = getElement("stayTitleLink");
  const accomAddress = getElement("accomAddress");
  const stayImage = getElement("stayImage");
  const stayIcon = getElement("stayIcon");
  const accomId = getElement("accomId")?.value?.trim() || "";

  if (stayCard) stayCard.classList.remove("is-hidden");
  if (stayTitle) stayTitle.innerText = name;
  if (stayTitleLink) stayTitleLink.href = accomId ? `/accom/${accomId}` : "#";
  if (accomAddress) accomAddress.innerText = address;

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

function resetUiWithoutDestination() {
  renderTransportGroups([], []);
  renderTransitDetail(null);
  updateDestinationSummary("-");
  setDistanceInfo("상단 도착지 입력창에 숙소를 입력하면 교통 정보가 표시됩니다.");
}

function resetRoutePlanner() {
  const startInput = getElement("startLocation");
  if (startInput) startInput.value = "";

  selectedRouteMode = null;
  syncRouteModeButtons();
  closeAccomSearchModal();
  closeReservedAccomModal();
  resetAccommodationSelection();
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
  renderTransitDetail(null);
  updateRouteSummary("-", "-", "-");
  updateDestinationSummary("-");
  resetModeTimeLabels();
  setHiddenAccommodation("", "", "", "");
  syncDestinationInput("");

  const stayCard = getElement("stayCard");
  const stayImage = getElement("stayImage");
  const stayIcon = getElement("stayIcon");
  const stayTitle = getElement("stayTitle");
  const stayTitleLink = getElement("stayTitleLink");
  const accomAddress = getElement("accomAddress");

  if (stayCard) stayCard.classList.remove("is-hidden");

  if (stayImage) {
    stayImage.classList.add("is-hidden");
    stayImage.removeAttribute("src");
  }

  if (stayIcon) stayIcon.classList.remove("is-hidden");
  if (stayTitle) stayTitle.innerText = "-";
  if (stayTitleLink) stayTitleLink.href = "#";
  if (accomAddress) accomAddress.innerText = "-";

  setDistanceInfo("상단 도착지 입력창에 숙소를 입력하면 교통 정보가 표시됩니다.");
  map.flyTo(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM, { duration: 0.5 });
  refreshMapSize();
}

function updateAccommodationMarker() {
  if (accomLat == null || accomLng == null) return;

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
    drawTransportMarkers(cached.items);
    renderTransportGroups(cached.bus, cached.subway);
    return;
  }

  const { ok, body } = await fetchJsonOrRedirect(`/api/transport/nearby?lat=${accomLat}&lng=${accomLng}`);
  if (!ok) {
    throw new Error(body.message || "주변 교통 정보를 불러오지 못했습니다.");
  }

  const items = Array.isArray(body.items) ? body.items : [];
  const busStops = items.filter(item => item.type === "bus");
  const subwayStations = items.filter(item => item.type === "subway");

  nearbyTransportCache.set(cacheKey, {
    items,
    bus: busStops,
    subway: subwayStations
  });

  drawTransportMarkers(items);
  renderTransportGroups(busStops, subwayStations);
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

    marker.bindTooltip(
        `${escapeHtml(item.name)}<br>${isBus ? "버스 정류장" : "지하철역"} / 도보 ${Number(item.walkMinutes)}분`,
        { direction: "top", offset: [0, -8] }
    );

    marker.on("click", () => activateTransportItem(item.type, item.name));
    transportMarkers.push({ marker, item });
  });
}

function renderTransportGroups(busStops, subwayStations) {
  renderTransportList("busList", busStops, "bus");
  renderTransportList("subwayList", subwayStations, "subway");

  if (getElement("busCountBadge")) {
    getElement("busCountBadge").innerText = String(busStops.length);
  }
  if (getElement("subwayCountBadge")) {
    getElement("subwayCountBadge").innerText = String(subwayStations.length);
  }
}

function renderTransportList(listId, items, type) {
  const listEl = getElement(listId);
  if (!listEl) return;

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
  if (!markerEntry) return;

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
  await ensureResolvedDestination();

  const start = getElement("startLocation")?.value?.trim() || "";
  const requestSeq = ++routeRequestSeq;

  if (!start) {
    alert("출발지를 선택해주세요.");
    return;
  }
  if (accomLat == null || accomLng == null) {
    alert("도착지를 선택해주세요.");
    return;
  }

  try {
    lastRouteContext = { lat: null, lng: null, label: start };
    lastRouteMetrics = null;
    resetModeTimeLabels();
    renderTransitDetail(null);
    clearRouteLine();

    const location = await geocodeAddress(start);
    if (requestSeq !== routeRequestSeq) return;

    setStartMarker(location.lat, location.lng, start);
    updateRouteSummary(start, "-", "-");
    map.flyTo([location.lat, location.lng], Math.max(map.getZoom(), 14), { duration: 0.6 });
    refreshMapSize();
    await calculateAndRenderRoutes({ lat: location.lat, lng: location.lng, label: start }, requestSeq);
  } catch (error) {
    if (requestSeq !== routeRequestSeq) return;
    console.error("길찾기 실패", error);
    alert("출발지를 찾을 수 없습니다.");
  }
}

async function ensureResolvedDestination() {
  const typedDestination = resolveTypedDestination();
  if (!typedDestination) return;

  const currentAddress = getElement("accomLocation")?.value?.trim() || "";
  if (typedDestination.address === currentAddress && accomLat != null && accomLng != null) {
    return;
  }

  await applyAccommodationDestination(
      typedDestination.name,
      typedDestination.address,
      typedDestination.imageUrl || "",
      typedDestination.id || ""
  );
}

async function calculateAndRenderRoutes(startContext, requestSeq = ++routeRequestSeq) {
  lastRouteContext = startContext;
  lastRouteMetrics = null;
  resetModeTimeLabels();
  renderTransitDetail(null);
  clearRouteLine();

  const straightDistanceKm = calculateStraightDistanceKm(
      startContext.lat,
      startContext.lng,
      accomLat,
      accomLng
  );

  const [carResult, transitResult] = await Promise.allSettled([
    fetchOsrmRoute(startContext.lat, startContext.lng, "driving"),
    fetchTransitRoute(startContext.lat, startContext.lng)
  ]);

  const metrics = buildRouteMetrics(
      startContext,
      carResult,
      transitResult,
      straightDistanceKm
  );

  if (requestSeq !== routeRequestSeq) {
    return;
  }

  if (!metrics) {
    renderTransitDetail(null);
    updateRouteSummary(startContext.label, "-", "-");
    resetModeTimeLabels();
    setDistanceInfo("경로를 찾지 못했습니다. 출발지 또는 도착지를 다시 확인해 주세요.");
    return;
  }

  lastRouteMetrics = metrics;
  renderModeDurations(metrics);

  if (!selectedRouteMode || !metrics[selectedRouteMode]?.minutes) {
    selectedRouteMode = getFirstAvailableMode(metrics) || "car";
  }

  renderSelectedMode();
}

function buildRouteMetrics(startContext, carResult, transitResult, straightDistanceKm) {
  const carRoute = carResult.status === "fulfilled" ? carResult.value : null;
  const transitRoute = transitResult.status === "fulfilled" ? transitResult.value : null;

  const baseDistanceKm = resolveDistanceKm(carRoute, transitRoute);
  const fallbackDistanceKm = baseDistanceKm > 0 ? baseDistanceKm : Number(straightDistanceKm.toFixed(2));

  return {
    startLabel: startContext.label,
    car: buildRoadMetrics(carRoute, fallbackDistanceKm) || buildEstimatedCarMetrics(fallbackDistanceKm),
    transit: buildTransitMetrics(fallbackDistanceKm, transitRoute)
  };
}

function resolveDistanceKm(carRoute, transitRoute) {
  if (carRoute?.distance) return Number((carRoute.distance / 1000).toFixed(2));
  if (transitRoute?.distanceKm) return Number(Number(transitRoute.distanceKm).toFixed(2));
  return 0;
}

function buildRoadMetrics(route, fallbackDistanceKm) {
  if (!route) {
    return null;
  }

  return {
    minutes: toRoundedMinutes(route.duration),
    distanceKm: Number((route.distance / 1000).toFixed(2)),
    geometry: route.geometry
  };
}

function buildEstimatedCarMetrics(distanceKm) {
  return {
    minutes: Math.max(3, Math.round((Math.max(distanceKm, 0.2) / 45) * 60)),
    distanceKm,
    geometry: null,
    estimated: true,
    message: "자동차 경로 API 응답이 없어 거리 기반 예상값을 표시합니다."
  };
}

async function fetchOsrmRoute(startLat, startLng, profile) {
  const { ok, body } = await fetchJsonOrRedirect(
      `/api/transport/road-route?profile=${encodeURIComponent(profile)}&sx=${startLng}&sy=${startLat}&ex=${accomLng}&ey=${accomLat}`
  );

  if (!ok || !body || body.available === false) {
    return null;
  }

  return body;
}

async function fetchTransitRoute(startLat, startLng) {
  const directDistanceKm = calculateStraightDistanceKm(startLat, startLng, accomLat, accomLng);

  if (directDistanceKm >= INTERCITY_LIMIT_KM) {
    const intercity = await fetchIntercityRecommendation(startLat, startLng);
    if (intercity) {
      return intercity;
    }
  }

  const tmapTransitRoute = await fetchTmapTransitRoute(startLat, startLng);
  if (tmapTransitRoute) {
    return tmapTransitRoute;
  }

  const webKey = getElement("odsayWebKey")?.value?.trim();
  if (webKey) {
    try {
      const directTransitRoute = await fetchTransitRouteFromWebKey(startLat, startLng, webKey);
      if (directTransitRoute) {
        return directTransitRoute;
      }
    } catch (error) {
      console.error("ODsay 웹키 직접 조회 실패", error);
    }
  }

  const { ok, body } = await fetchJsonOrRedirect(
      `/api/transport/transit-route?sx=${startLng}&sy=${startLat}&ex=${accomLng}&ey=${accomLat}`
  );

  if (!ok) {
    throw new Error(body.message || "대중교통 경로를 찾지 못했습니다.");
  }

  return normalizeServerTransitRoute(body);
}

async function fetchTmapTransitRoute(startLat, startLng) {
  try {
    const { ok, body } = await fetchJsonOrRedirect("/api/transport/tmap-transit-route", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        ...(CSRF_TOKEN && CSRF_HEADER ? { [CSRF_HEADER]: CSRF_TOKEN } : {})
      },
      body: JSON.stringify({
        startX: startLng,
        startY: startLat,
        endX: accomLng,
        endY: accomLat
      })
    });

    if (!ok || !body || body.available === false) return null;
    return normalizeServerTransitRoute({ ...body, source: "tmap" });
  } catch (error) {
    console.error("TMAP 대중교통 조회 실패", error);
    return null;
  }
}

async function fetchIntercityRecommendation(startLat, startLng) {
  return null;
}

function modePriority(type) {
  if (type === "train") return 0;
  if (type === "express") return 1;
  if (type === "intercity") return 2;
  return 9;
}

function normalizeIntercityItem(item) {
  const normalizedTitle = normalizeIntercityTitle(item?.type, item?.title, item?.detail);

  const steps = Array.isArray(item?.steps)
      ? item.steps.map(step => {
        const minutes = Number(step?.minutes || extractStepMinutes(step?.durationText));
        return {
          type: normalizeStepType(step?.type, step?.title),
          title: normalizeStepTitle(step?.type, step?.title),
          summary: step?.summary || "",
          instruction: step?.instruction || "",
          startName: step?.startName || "",
          endName: step?.endName || "",
          stationCount: Number(step?.stationCount || 0),
          minutes,
          durationText: formatStepDuration(minutes)
        };
      })
      : [];

  return {
    type: item?.type || "train",
    pathType: Number(item?.pathType || 0),
    totalTime: Number(item?.minutes || item?.totalTime || 0),
    payment: Number(item?.paymentAmount || parseFareAmount(item?.fare)),
    title: normalizedTitle,
    detail: item?.detail || "",
    firstStartStation: item?.startStation || "",
    lastEndStation: item?.endStation || "",
    steps
  };
}

function normalizeIntercityTitle(type, title, detail = "") {
  const raw = String(title || "").trim();
  const lower = raw.toLowerCase();
  const detailLower = String(detail || "").toLowerCase();

  if (type === "express") return "고속버스";
  if (type === "intercity") return "시외버스";

  if (
      lower.includes("ktx") ||
      lower.includes("srt") ||
      detailLower.includes("ktx") ||
      detailLower.includes("srt")
  ) {
    return "KTX/SRT";
  }

  if (lower.includes("itx")) return "ITX";
  if (raw.includes("무궁화")) return "무궁화호";
  if (raw.includes("새마을")) return "새마을호";
  if (raw.includes("누리로")) return "누리로";
  if (type === "train") return raw || "일반열차";

  return raw || "장거리 대중교통";
}

function normalizeStepType(type, title) {
  const rawType = String(type || "").toLowerCase();
  const rawTitle = String(title || "").toLowerCase();

  if (rawType === "train") return "train";
  if (rawType === "subway") return "subway";
  if (rawType === "walk") return "walk";
  if (rawType === "bus") {
    if (
        rawTitle.includes("ktx") ||
        rawTitle.includes("srt") ||
        rawTitle.includes("itx") ||
        rawTitle.includes("무궁화") ||
        rawTitle.includes("새마을")
    ) {
      return "train";
    }
    return "bus";
  }

  if (
      rawTitle.includes("ktx") ||
      rawTitle.includes("srt") ||
      rawTitle.includes("itx") ||
      rawTitle.includes("무궁화") ||
      rawTitle.includes("새마을")
  ) {
    return "train";
  }

  if (rawTitle.includes("지하철")) return "subway";
  if (rawTitle.includes("버스")) return "bus";
  return "walk";
}

function normalizeStepTitle(type, title) {
  if (normalizeStepType(type, title) === "bus") {
    return normalizeBusRouteName(title);
  }
  return normalizeIntercityTitle(type, title, "");
}

function normalizeBusRouteName(value) {
  const raw = String(value || "")
      .trim()
      .replace(/\s+/g, " ")
      .replace(/^버스\s*버스\s*/, "버스 ");
  if (!raw) return "버스";
  if (/^(버스|간선버스|지선버스|광역버스|마을버스|공항버스|고속버스|시외버스)\b/.test(raw)) {
    return raw;
  }
  if (raw.includes("버스")) {
    return raw;
  }
  return `버스 ${raw}`;
}

function parseFareAmount(value) {
  const digits = String(value || "").replace(/[^0-9]/g, "");
  return digits ? Number(digits) : 0;
}

async function fetchTransitRouteFromWebKey(startLat, startLng, webKey) {
  const candidates = [
    buildOdsayWebUrl("searchPubTransPathT", startLng, startLat, null, webKey),
    buildOdsayWebUrl("searchPubTransPathR", startLng, startLat, "0", webKey),
    buildOdsayWebUrl("searchPubTransPathR", startLng, startLat, "1", webKey)
  ];

  const paths = [];
  for (const url of candidates) {
    try {
      const response = await fetch(url, { headers: { Accept: "application/json" } });
      const data = await response.json();
      const errorMessage = extractOdsayErrorMessage(data);

      if (!response.ok || errorMessage) {
        continue;
      }

      const resultPaths = Array.isArray(data?.result?.path) ? data.result.path : [];
      paths.push(...resultPaths);
    } catch (error) {
      console.error("ODsay 웹키 후보 조회 실패", error);
    }
  }

  if (!paths.length) {
    return null;
  }

  return normalizeOdsayTransitRoute({ result: { path: paths } });
}

function buildOdsayWebUrl(apiName, startLng, startLat, searchType, webKey) {
  const params = new URLSearchParams({
    apiKey: webKey,
    SX: String(startLng),
    SY: String(startLat),
    EX: String(accomLng),
    EY: String(accomLat),
    OPT: "0",
    lang: "0"
  });

  if (searchType != null) {
    params.set("SearchType", searchType);
  }

  return `https://api.odsay.com/v1/api/${apiName}?${params.toString()}`;
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
  const paths = Array.isArray(data?.result?.path) ? data.result.path : [];
  const alternatives = paths
      .map(path => {
        const info = path?.info;
        if (!info || !Number.isFinite(Number(info?.totalTime))) return null;
        const steps = normalizeTransitSteps(path, info);
        return {
          type: inferAlternativeType(path, info, steps),
          title: resolveAlternativeTitle(path, info, steps),
          totalTime: Number(info.totalTime),
          payment: Number(info.payment || 0),
          pathType: Number(path.pathType || 0),
          detail: buildAlternativeDetail(info),
          steps
        };
      })
      .filter(Boolean);
  const path = paths
      .filter(candidate => Number.isFinite(Number(candidate?.info?.totalTime)))
      .sort((left, right) => Number(left.info.totalTime) - Number(right.info.totalTime))[0];

  const info = path?.info;
  if (!info || !Number.isFinite(Number(info.totalTime))) {
    return null;
  }

  const steps = normalizeTransitSteps(path, info);

  return {
    totalTime: Number(info.totalTime),
    payment: Number(info.payment || 0),
    busTransitCount: Number(info.busTransitCount || 0),
    subwayTransitCount: Number(info.subwayTransitCount || 0),
    firstStartStation: info.firstStartStation || "",
    lastEndStation: info.lastEndStation || "",
    totalWalk: Number(info.totalWalk || 0),
    distanceKm: Number(info.totalDistance || 0) / 1000,
    pathType: Number(path.pathType || 0),
    steps,
    title: resolveAlternativeTitle(path, info, steps),
    alternatives: dedupeTransitAlternatives(alternatives)
  };
}

function normalizeTransitSteps(path, info) {
  const subPaths = Array.isArray(path?.subPath) ? path.subPath : [];
  if (!subPaths.length) {
    return buildFallbackTransitSteps(path, info);
  }

  return subPaths.map(step => {
    const lane = Array.isArray(step.lane) && step.lane.length ? step.lane[0] : {};
    const type = inferTransitStepType(step, lane, Number(step.trafficType));
    const stationCount = Number(step.stationCount || 0);
    const sectionTime = Number(step.sectionTime || 0);
    const distance = Number(step.distance || 0);
    const startName = step.startName || "출발 지점";
    const endName = step.endName || "도착 지점";

    if (type === "walk") {
      const minutes = Math.max(1, sectionTime || Math.round(distance / 70) || 1);
      return {
        type,
        title: "도보 이동",
        summary: `${startName} -> ${endName}`,
        instruction: `${startName}에서 ${endName}까지 도보 이동`,
        startName,
        endName,
        stationCount: 0,
        minutes,
        durationText: `${distance}m · ${formatStepDuration(minutes)}`
      };
    }

    if (type === "bus") {
      const busNo = normalizeBusRouteName(lane.busNo || lane.name || "버스");
      return {
        type,
        title: busNo,
        summary: `${startName} -> ${endName}`,
        instruction: `${startName}에서 ${busNo} 승차 후 ${endName}에서 하차`,
        startName,
        endName,
        stationCount,
        minutes: Math.max(1, sectionTime),
        durationText: `${stationCount}정거장 · ${formatStepDuration(sectionTime)}`
      };
    }

    if (type === "train") {
      const trainName = lane.name || lane.trainClass || "열차";
      return {
        type,
        title: normalizeStepTitle("train", trainName),
        summary: `${startName} -> ${endName}`,
        instruction: `${startName}에서 ${normalizeStepTitle("train", trainName)} 승차 후 ${endName}에서 하차`,
        startName,
        endName,
        stationCount,
        minutes: Math.max(1, sectionTime),
        durationText: `${stationCount > 0 ? `${stationCount}개 역 · ` : ""}${formatStepDuration(sectionTime)}`
      };
    }

    const subwayName = lane.name || "지하철";
    return {
      type: "subway",
    title: subwayName,
    summary: `${startName} -> ${endName}`,
    instruction: `${startName}에서 ${subwayName} 승차 후 ${endName}에서 하차`,
    startName,
    endName,
    stationCount,
    minutes: Math.max(1, sectionTime),
    durationText: `${stationCount}개 역 · ${formatStepDuration(sectionTime)}`
  };
  });
}

function inferTransitStepType(step, lane, trafficType) {
  if (trafficType === 3 && !lane?.name && !lane?.trainClass && !lane?.busNo) {
    return "walk";
  }
  if (trafficType === 2) return "bus";
  if (trafficType === 1) return "subway";
  if (trafficType === 3 || lane?.trainClass) return "train";
  if (trafficType === 4 || trafficType === 6) return "bus";

  const laneName = String(lane?.name || "").toLowerCase();
  if (lane?.busNo || laneName.includes("버스")) return "bus";
  if (
      laneName.includes("ktx") ||
      laneName.includes("srt") ||
      laneName.includes("itx") ||
      laneName.includes("무궁화") ||
      laneName.includes("새마을")
  ) {
    return "train";
  }
  if (laneName.includes("지하철") || laneName.includes("subway") || laneName.includes("metro")) {
    return "subway";
  }
  return "walk";
}

function buildFallbackTransitSteps(path, info) {
  const pathType = Number(path?.pathType || 0);
  const title = buildFallbackTransitTitle(pathType, info);
  const startName = info?.firstStartStation || "출발지";
  const endName = info?.lastEndStation || "도착지";
  const totalTime = Number(info?.totalTime || 0);

  return [{
    type: pathType >= 11 ? "train" : "bus",
    title,
    summary: `${startName} -> ${endName}`,
    instruction: `${startName}에서 ${title} 승차 후 ${endName}에서 하차`,
    startName,
    endName,
    stationCount: 0,
    minutes: Math.max(1, totalTime),
    durationText: formatStepDuration(totalTime)
  }];
}

function buildFallbackTransitTitle(pathType, info) {
  if (pathType === 11) return "KTX/SRT";
  if (pathType === 12) return "고속버스";
  if (pathType === 13) return "항공";
  if (pathType === 14) return "시외버스";
  return info?.firstStartStation ? `${info.firstStartStation} 출발` : "대중교통 경로";
}

function buildTransitMetrics(distanceKm, transitRoute) {
  if (!transitRoute || !Number.isFinite(Number(transitRoute.totalTime))) {
    return buildEstimatedTransitMetrics(distanceKm, "ODsay API 응답이 없어 거리 기반 예상값을 표시합니다.");
  }

  const detailParts = [];
  if (transitRoute.detail) detailParts.push(transitRoute.detail);
  if (transitRoute.busTransitCount > 0) detailParts.push(`버스 ${transitRoute.busTransitCount}회`);
  if (transitRoute.subwayTransitCount > 0) detailParts.push(`지하철 ${transitRoute.subwayTransitCount}회`);
  if (transitRoute.totalWalk > 0) detailParts.push(`도보 ${transitRoute.totalWalk}m`);
  if (transitRoute.firstStartStation) detailParts.push(`${transitRoute.firstStartStation} 출발`);

  return {
    minutes: Math.max(1, Number(transitRoute.totalTime)),
    distanceKm: transitRoute.distanceKm > 0 ? Number(transitRoute.distanceKm.toFixed(2)) : distanceKm,
    geometry: transitRoute.geometry || null,
    detail: detailParts.join(" / "),
    title: transitRoute.title || buildTransitTitle(transitRoute.steps),
    pathType: Number(transitRoute.pathType || 0),
    firstStartStation: transitRoute.firstStartStation || "",
    steps: transitRoute.steps,
    payment: transitRoute.payment,
    alternatives: Array.isArray(transitRoute.alternatives) ? transitRoute.alternatives : [],
    lookupTimeText: formatCurrentLookupTime(),
    estimated: Boolean(transitRoute.estimated)
  };
}

function buildEstimatedTransitMetrics(distanceKm, detail) {
  const main = resolveEstimatedTransitMain(distanceKm);
  const accessMinutes = distanceKm >= 10 ? 12 : 8;
  const egressMinutes = distanceKm >= 10 ? 10 : 6;
  const mainMinutes = Math.max(main.minMinutes, Math.round((Math.max(distanceKm, 0.2) / main.speedKmh) * 60));
  const totalMinutes = accessMinutes + mainMinutes + main.transferMinutes + egressMinutes;

  return {
    minutes: totalMinutes,
    distanceKm,
    geometry: null,
    detail,
    title: main.title,
    pathType: main.pathType || 0,
    firstStartStation: "",
    steps: [],
    payment: 0,
    alternatives: [],
    lookupTimeText: formatCurrentLookupTime(),
    estimated: true
  };
}

function resolveEstimatedTransitMain(distanceKm) {
  if (distanceKm >= 100) {
    return { type: "train", title: "장거리 대중교통 예상", pathType: 11, speedKmh: 190, minMinutes: 35, transferMinutes: 12, startHub: "출발지 주변 주요역/터미널", endHub: "도착지 주변 주요역/터미널", transferSummary: "실제 KTX/ITX/고속버스 배차 확인 필요" };
  }
  if (distanceKm >= 70) {
    return { type: "train", title: "장거리 대중교통 예상", pathType: 11, speedKmh: 120, minMinutes: 30, transferMinutes: 14, startHub: "출발지 주변 주요역/터미널", endHub: "도착지 주변 주요역/터미널", transferSummary: "실제 열차/고속버스 배차 확인 필요" };
  }
  if (distanceKm >= 45) {
    return { type: "bus", title: "장거리 대중교통 예상", pathType: 12, speedKmh: 75, minMinutes: 35, transferMinutes: 16, startHub: "출발지 주변 터미널/역", endHub: "도착지 주변 터미널/역", transferSummary: "실제 고속버스/철도 배차 확인 필요" };
  }
  if (distanceKm >= 12) {
    return { type: "subway", title: "지역 대중교통 예상", pathType: 3, speedKmh: 32, minMinutes: 12, transferMinutes: 8, startHub: "출발지 주변 정류장/역", endHub: "도착지 주변 정류장/역", transferSummary: "실제 버스/지하철 노선 확인 필요" };
  }
  return { type: "bus", title: "지역 대중교통 예상", pathType: 2, speedKmh: 24, minMinutes: 8, transferMinutes: 6, startHub: "출발지 주변 정류장", endHub: "도착지 주변 정류장", transferSummary: "실제 버스 노선 확인 필요" };
}

function buildEstimatedTransitStep(type, title, instruction, minutes) {
  return {
    type,
    title,
    summary: instruction,
    instruction,
    minutes,
    durationText: formatStepDuration(minutes)
  };
}

function formatCurrentLookupTime() {
  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).format(new Date());
}

function toRoundedMinutes(seconds) {
  const numericSeconds = Number(seconds);
  if (!Number.isFinite(numericSeconds) || numericSeconds <= 0) {
    return null;
  }
  return Math.max(1, Math.round(numericSeconds / 60));
}

function renderModeDurations(metrics) {
  setModeTimeLabel("carTimeLabel", metrics.car.minutes);
  setModeTimeLabel("transitTimeLabel", metrics.transit.minutes);
}

function setModeTimeLabel(elementId, minutes) {
  const element = getElement(elementId);
  if (!element) return;

  element.innerText = minutes == null ? DEFAULT_TIME_LABEL : `약 ${minutes}분`;
}

function resetModeTimeLabels() {
  setModeTimeLabel("carTimeLabel", null);
  setModeTimeLabel("transitTimeLabel", null);
}

function renderSelectedMode() {
  syncRouteModeButtons();

  if (!lastRouteMetrics || !lastRouteContext) {
    renderTransitDetail(null, null);
    return;
  }

  const selectedMetrics = lastRouteMetrics[selectedRouteMode];
  if (!selectedMetrics) return;

  updateRouteSummary(
      lastRouteMetrics.startLabel,
      selectedMetrics.distanceKm > 0 ? `${selectedMetrics.distanceKm.toFixed(2)}km` : "-",
      selectedMetrics.minutes == null
          ? `${MODE_META[selectedRouteMode].label} 정보 없음`
          : `${MODE_META[selectedRouteMode].label} 약 ${selectedMetrics.minutes}분`
  );

  if (selectedMetrics.minutes == null) {
    setDistanceInfo(selectedMetrics.message || "선택한 이동 수단의 예상 시간을 계산하지 못했습니다.");
    renderTransitDetail(selectedRouteMode, null);
    clearRouteLine();
    return;
  }

  if (selectedRouteMode === "transit") {
    const detail = selectedMetrics.detail ? ` / ${selectedMetrics.detail}` : "";
    setDistanceInfo(`총 이동 거리 ${selectedMetrics.distanceKm.toFixed(2)}km / 예상 소요 시간 약 ${selectedMetrics.minutes}분${detail}`);
    renderTransitDetail(selectedRouteMode, selectedMetrics);
  } else {
    setDistanceInfo(`총 이동 거리 ${selectedMetrics.distanceKm.toFixed(2)}km / ${MODE_META[selectedRouteMode].label} 기준 약 ${selectedMetrics.minutes}분`);
    renderTransitDetail(selectedRouteMode, selectedMetrics);
  }

  drawSelectedRouteLine(selectedMetrics.geometry, selectedRouteMode);
}

function getFirstAvailableMode(metrics) {
  return ["car", "transit"].find(mode => metrics[mode]?.minutes != null) || null;
}

function renderTransitDetail(mode, metrics) {
  const panel = getElement("transitDetailPanel");
  const title = getElement("transitDetailTitle");
  const meta = getElement("transitDetailMeta");
  const summary = getElement("transitDetailSummary");
  const eyebrow = panel?.querySelector(".transit-detail-panel__eyebrow");

  if (!panel || !title || !meta || !summary || !eyebrow) return;

  if (!mode || !metrics || metrics.minutes == null) {
    eyebrow.innerText = "이동 상세";
    title.innerText = "추천 경로를 불러오면 이동 수단이 표시됩니다";
    meta.innerText = "";
    summary.innerText = "최적 출발지가 계산되면 이곳에 표시됩니다.";
    return;
  }

  eyebrow.innerText = `${MODE_META[mode].label} 상세`;
  title.innerText = buildRouteDetailTitle(mode, metrics);
  meta.innerText = mode === "transit" && metrics.lookupTimeText
      ? `${formatTransitHeadline(metrics.minutes)} · ${metrics.lookupTimeText} 조회`
      : formatTransitHeadline(metrics.minutes);
  summary.innerHTML = escapeHtml(buildRouteDetailSummary(mode, metrics)) + renderRouteDetailExtras(mode, metrics);
}

function formatTransitHeadline(minutes) {
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  const timeText = hour > 0 ? `${hour}시간 ${minute}분` : `${minute}분`;
  return timeText;
}

function buildTransitTitle(steps) {
  const vehicleTitles = (steps || [])
      .filter(step => normalizeStepType(step.type, step.title) !== "walk")
      .map(step => normalizeStepTitle(step.type, step.title))
      .filter(Boolean)
      .filter((title, index, arr) => arr.indexOf(title) === index);

  return vehicleTitles.length ? vehicleTitles.join(" → ") : "추천 대중교통 경로";
}

function buildRouteDetailTitle(mode, metrics) {
  if (mode === "transit") {
    if (metrics.estimated) {
      return `예상 경로 · ${metrics.title || "대중교통"}`;
    }
    return `가장 빠른 경로 · ${metrics.title || "대중교통 경로"}`;
  }
  return `${MODE_META[mode].label} 경로`;
}

function buildRouteDetailSummary(mode, metrics) {
  if (mode === "transit") {
    if (isLongDistanceTransit(metrics) && metrics.estimated) {
      return "";
    }
    if (metrics.estimated) {
      return metrics.detail || "정확한 대중교통 경로를 불러오지 못해 예상 시간만 표시합니다.";
    }
    if (metrics.firstStartStation) return `${metrics.firstStartStation} 출발`;
    if (lastRouteContext?.label) return `${lastRouteContext.label} 출발`;
    return "최적 출발지";
  }
  if (lastRouteContext?.label) return `${lastRouteContext.label} 출발`;
  return "출발지";
}

function renderRouteDetailExtras(mode, metrics) {
  if (mode !== "transit") return "";
  if (isLongDistanceTransit(metrics) && metrics.estimated) return renderLongDistanceTransitResult(metrics);
  if (metrics.estimated) return "";
  const alternatives = isLongDistanceTransit(metrics) ? [] : metrics.alternatives;
  return renderTransitSteps(metrics.steps) + renderTransitAlternatives(alternatives);
}

function renderLongDistanceTransitResult(metrics) {
  const start = lastRouteContext?.label || "출발지";
  const destination = resolveDestinationLabel();
  const routeText = `${start} → ${destination}`;
  const accuracyText = metrics.estimated
      ? "네이버 길찾기 수치에 맞춘 장거리 대중교통 예상 시간"
      : "장거리 대중교통 최종 선택 경로";

  return `
    <article class="long-transit-result">
      <span class="long-transit-result__badge">${escapeHtml(metrics.title || "장거리 대중교통")}</span>
      <strong class="long-transit-result__time">${escapeHtml(formatTransitHeadline(metrics.minutes))}</strong>
      <span class="long-transit-result__route">${escapeHtml(routeText)}</span>
      <span class="long-transit-result__note">${escapeHtml(accuracyText)}</span>
    </article>
  `;
}

function resolveDestinationLabel() {
  const name = getElement("accomName")?.value?.trim();
  const address = getElement("accomLocation")?.value?.trim();
  const summary = getElement("destinationSummaryCard")?.innerText?.trim();
  return name || summary || address || "도착지";
}

function isLongDistanceTransit(metrics) {
  return Number(metrics?.distanceKm || 0) >= INTERCITY_LIMIT_KM
      || Number(metrics?.pathType || 0) >= 11
      || String(metrics?.title || "").includes("KTX")
      || String(metrics?.title || "").includes("ITX")
      || String(metrics?.title || "").includes("SRT")
      || String(metrics?.title || "").includes("고속버스")
      || String(metrics?.title || "").includes("시외버스");
}

function extractStepMinutes(text) {
  const match = String(text || "").match(/(\d+)/);
  return match ? Number(match[1]) : 1;
}

function formatStepDuration(minutes) {
  const numericMinutes = Number(minutes || 0);
  if (numericMinutes <= 0) return "0분";

  const hour = Math.floor(numericMinutes / 60);
  const minute = numericMinutes % 60;
  return hour > 0 ? `${hour}시간 ${minute}분` : `${minute}분`;
}

function drawSelectedRouteLine(geometry, mode) {
  clearRouteLine();

  const lineGeometry = geometry;
  if (!lineGeometry) return;

  routeLine = L.geoJSON(lineGeometry, {
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
  if (getElement("startSummary")) getElement("startSummary").innerText = startText || "-";
  if (getElement("distanceSummary")) getElement("distanceSummary").innerText = distanceText;
  if (getElement("durationSummary")) getElement("durationSummary").innerText = durationText;
}

function updateDestinationSummary(text) {
  if (getElement("destinationSummaryCard")) {
    getElement("destinationSummaryCard").innerText = text || "-";
  }
}

function setPresetStart(place) {
  if (getElement("startLocation")) {
    getElement("startLocation").value = place;
  }
  if (accomLat != null && accomLng != null) {
    searchRoute();
  }
}

function focusAccommodation() {
  if (!map || accomLat == null || accomLng == null) return;

  map.flyTo([accomLat, accomLng], 15, { duration: 0.5 });
  if (accomMarker) accomMarker.openPopup();
  refreshMapSize();
}

function setDistanceInfo(text) {
  if (getElement("distanceInfo")) {
    getElement("distanceInfo").innerText = text;
  }
}

function calculateStraightDistanceKm(startLat, startLng, endLat, endLng) {
  const earthRadiusKm = 6371;
  const dLat = toRadians(endLat - startLat);
  const dLng = toRadians(endLng - startLng);
  const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(toRadians(startLat)) *
      Math.cos(toRadians(endLat)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusKm * c;
}

function toRadians(degrees) {
  return degrees * (Math.PI / 180);
}

function escapeHtml(value) {
  return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
}

async function fetchJsonOrRedirect(url, options = {}) {
  const headers = {
    Accept: "application/json",
    ...(options.headers || {})
  };
  const response = await fetch(url, {
    ...options,
    credentials: "same-origin",
    headers
  });

  const contentType = response.headers.get("content-type") || "";
  if (response.redirected || contentType.includes("text/html")) {
    return {
      ok: false,
      body: { message: "로그인 또는 서버 응답 문제로 기본 예상값을 표시합니다." }
    };
  }

  let body = {};
  try {
    body = await response.json();
  } catch (error) {
    body = { message: "응답을 읽지 못해 기본 예상값을 표시합니다." };
  }
  return { ok: response.ok, body };
}

function normalizeServerTransitRoute(data) {
  if (!data || data.available === false) {
    return null;
  }

  return {
    totalTime: Number(data.totalTime || data.minutes || 0),
    payment: Number(data.payment || data.paymentAmount || 0),
    busTransitCount: Number(data.busTransitCount || 0),
    subwayTransitCount: Number(data.subwayTransitCount || 0),
    firstStartStation: data.firstStartStation || "",
    lastEndStation: data.lastEndStation || "",
    totalWalk: Number(data.totalWalk || 0),
    distanceKm: Number(data.distanceKm || 0),
    pathType: Number(data.pathType || 0),
    title: data.title || "",
    detail: data.detail || "",
    geometry: data.geometry || null,
    steps: Array.isArray(data.steps) ? data.steps : [],
    alternatives: dedupeTransitAlternatives(Array.isArray(data.alternatives) ? data.alternatives : []),
    estimated: Boolean(data.estimated)
  };
}

function inferAlternativeType(path, info, steps) {
  const pathType = Number(path?.pathType || 0);
  if (pathType === 11) return "train";
  if (pathType === 12) return "express";
  if (pathType === 14) return "intercity";
  if (steps.some(step => normalizeStepType(step.type, step.title) === "train")) return "train";
  if (Number(info?.subwayTransitCount || 0) > 0 && Number(info?.busTransitCount || 0) === 0) return "subway";
  return "bus";
}

function resolveAlternativeTitle(path, info, steps) {
  const pathType = Number(path?.pathType || 0);
  if (pathType === 11) {
    const trainStep = steps.find(step => normalizeStepType(step.type, step.title) === "train");
    return trainStep?.title || "열차";
  }
  if (pathType === 12) return "고속버스";
  if (pathType === 14) return "시외버스";
  if (Number(info?.subwayTransitCount || 0) > 0 && Number(info?.busTransitCount || 0) > 0) return "버스 + 지하철";
  if (Number(info?.subwayTransitCount || 0) > 0) return "지하철";
  if (Number(info?.busTransitCount || 0) > 0) return "버스";
  return "대중교통";
}

function buildAlternativeDetail(info) {
  const parts = [];
  if (Number(info?.busTransitCount || 0) > 0) parts.push(`버스 ${Number(info.busTransitCount)}회`);
  if (Number(info?.subwayTransitCount || 0) > 0) parts.push(`지하철 ${Number(info.subwayTransitCount)}회`);
  if (Number(info?.totalWalk || 0) > 0) parts.push(`도보 ${Number(info.totalWalk)}m`);
  return parts.join(" / ");
}

function dedupeTransitAlternatives(alternatives) {
  const seen = new Set();
  return alternatives
      .map(item => ({
        type: normalizeStepType(item?.type, item?.title),
        title: item?.title || "대중교통",
        totalTime: Number(item?.totalTime || item?.minutes || 0),
        payment: Number(item?.payment || item?.paymentAmount || 0),
        pathType: Number(item?.pathType || 0),
        detail: item?.detail || "",
        firstStartStation: item?.firstStartStation || "",
        lastEndStation: item?.lastEndStation || "",
        steps: normalizeTransitStepList(item?.steps)
      }))
      .filter(item => Number.isFinite(item.totalTime) && item.totalTime > 0)
      .sort((a, b) => a.totalTime - b.totalTime)
      .filter(item => {
        const routeText = item.steps
            .map(step => `${step.title}:${step.startName || step.summary}:${step.endName || ""}`)
            .join("|");
        const key = `${item.type}:${item.title}:${item.totalTime}:${routeText}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      })
      .slice(0, 5)
      .map((item, index) => ({ ...item, fastest: index === 0 }));
}

function renderTransitSteps(steps) {
  if (!Array.isArray(steps) || !steps.length) return "";

  return `
    <ol class="transit-step-list">
      ${steps.map((step, index) => {
        const stepType = normalizeStepType(step.type, step.title);
        return `
          <li class="transit-step transit-step--${escapeHtml(stepType)}">
            <span class="transit-step__index">${index + 1}</span>
            <span class="transit-step__body">
              <strong class="transit-step__title">${escapeHtml(step.title || step.type || "이동")}</strong>
              <span class="transit-step__summary">${escapeHtml(step.instruction || step.summary || "")}</span>
              <span class="transit-step__time">${escapeHtml(step.durationText || formatStepDuration(Number(step.minutes || 0)))}</span>
              ${Number(step.stationCount || 0) > 0 ? `<span class="transit-step__station">${escapeHtml(`${Number(step.stationCount)}개 정류장/역 이동`)}</span>` : ""}
            </span>
          </li>
        `;
      }).join("")}
    </ol>
  `;
}

function renderTransitAlternatives(alternatives) {
  if (!Array.isArray(alternatives) || !alternatives.length) return "";

  return `
    <div class="transit-alt-section">
      <div class="transit-alt-section__head">
        <strong>경로 후보</strong>
        <span>빠른 순</span>
      </div>
      <div class="transit-alt-grid">
      ${alternatives.map(item => `
        <article class="transit-alt-card${item.fastest ? " is-fastest" : ""}">
          <span class="transit-alt-card__type">
            ${item.fastest ? '<span class="transit-alt-card__badge">가장 빠름</span>' : ""}
            ${escapeHtml(item.title || "대중교통")}
          </span>
          <strong class="transit-alt-card__time">${escapeHtml(formatStepDuration(Number(item.totalTime || 0)))}</strong>
          <span class="transit-alt-card__detail">${escapeHtml(item.detail || buildTransitTitle(item.steps))}</span>
          <span class="transit-alt-card__route">${escapeHtml(buildAlternativeStationText(item))}</span>
          ${renderAlternativeStepPreview(item.steps)}
        </article>
      `).join("")}
      </div>
    </div>
  `;
}

function buildAlternativeStationText(item) {
  if (item?.firstStartStation && item?.lastEndStation) {
    return `${item.firstStartStation} → ${item.lastEndStation}`;
  }
  if (Array.isArray(item?.steps) && item.steps.length) {
    const first = item.steps[0]?.summary || "";
    return first || "";
  }
  return "";
}

function normalizeTransitStepList(steps) {
  if (!Array.isArray(steps)) return [];
  return steps.map(step => {
    const minutes = Number(step?.minutes || extractStepMinutes(step?.durationText));
    return {
      type: normalizeStepType(step?.type, step?.title),
      title: normalizeStepTitle(step?.type, step?.title),
      summary: step?.summary || "",
      instruction: step?.instruction || "",
      startName: step?.startName || "",
      endName: step?.endName || "",
      stationCount: Number(step?.stationCount || 0),
      minutes,
      durationText: step?.durationText || formatStepDuration(minutes)
    };
  });
}

function renderAlternativeStepPreview(steps) {
  if (!Array.isArray(steps) || !steps.length) return "";
  return `
    <ol class="transit-alt-steps">
      ${steps.map((step, index) => {
        const stepType = normalizeStepType(step.type, step.title);
        const instruction = step.instruction || step.summary || "";
        return `
          <li class="transit-alt-step transit-alt-step--${escapeHtml(stepType)}">
            <span class="transit-alt-step__dot">${index + 1}</span>
            <span class="transit-alt-step__text">
              <strong>${escapeHtml(step.title || "이동")}</strong>
              <span>${escapeHtml(instruction)}</span>
            </span>
          </li>
        `;
      }).join("")}
    </ol>
  `;
}
