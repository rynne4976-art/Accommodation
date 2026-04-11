let map;
let accomLat = null;
let accomLng = null;
let accomMarker = null;
let startMarker = null;
let routeLine = null;
let transportMarkers = [];
let selectedRouteMode = "car";
let lastRouteContext = null;
let lastRouteMetrics = null;
let accomSearchPage = 0;
let accomSearchTotalPages = 0;

const geocodeCache = new Map();
const nearbyTransportCache = new Map();

const DEFAULT_MAP_CENTER = [37.5665, 126.9780];
const DEFAULT_MAP_ZOOM = 12;
const LOGIN_URL = "/members/login";
const WALK_LIMIT_KM = 12;
const INTERCITY_LIMIT_KM = 45;

const MODE_META = {
  transit: { label: "대중교통", color: "#2563eb", dashArray: "8 10" },
  car: { label: "자동차", color: "#7b3ff2", dashArray: null },
  walk: { label: "도보", color: "#16a34a", dashArray: "10 8" }
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
    handleAccommodationSelection(item, closeAccomSearchModal, "숙소 선택 실패");
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
    handleAccommodationSelection(item, closeReservedAccomModal, "예약 숙소 선택 실패");
  });
}

async function handleAccommodationSelection(item, closeModalFn, errorLabel) {
  try {
    await applyAccommodationDestination(item.name, item.location, item.imageUrl, item.id);
    closeModalFn();
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
    if (lastRouteContext) {
      await calculateAndRenderRoutes(lastRouteContext);
    }
    refreshMapSize();
    return;
  }

  clearRouteLine();
  lastRouteMetrics = null;
  updateRouteSummary("-", "-", "-");
  renderTransitDetail(null);
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

  if (lastRouteContext) {
    await calculateAndRenderRoutes(lastRouteContext);
  } else {
    setDistanceInfo("선택한 숙소 기준으로 주변 교통 정보를 표시합니다.");
  }
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

  selectedRouteMode = "car";
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
  setModeTimeLabel("carTimeLabel", null, "자동차");
  setModeTimeLabel("walkTimeLabel", null, "도보");
  setModeTimeLabel("transitTimeLabel", null, "대중교통");
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

  if (!start) {
    alert("출발지를 선택해주세요.");
    return;
  }
  if (accomLat == null || accomLng == null) {
    alert("도착지를 선택해주세요.");
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
  await ensureResolvedDestination();

  if (accomLat == null || accomLng == null) {
    alert("도착지를 선택해주세요.");
    return;
  }

  try {
    const myAddress = await fetchMyPageAddress();
    if (!isUsableAddress(myAddress)) {
      alert("마이페이지에 등록된 주소가 없습니다.");
      return;
    }

    if (getElement("startLocation")) {
      getElement("startLocation").value = myAddress;
    }

    const location = await geocodeAddress(myAddress);
    setStartMarker(location.lat, location.lng, "내 주소");
    updateRouteSummary(myAddress, "-", "-");
    map.flyTo([location.lat, location.lng], Math.max(map.getZoom(), 14), { duration: 0.6 });
    refreshMapSize();

    await calculateAndRenderRoutes({
      lat: location.lat,
      lng: location.lng,
      label: myAddress
    });
  } catch (error) {
    console.error("내 주소 경로 계산 실패", error);
    alert("마이페이지 주소로 경로를 찾을 수 없습니다.");
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

async function fetchMyPageAddress() {
  const hidden = getElement("myPageAddress")?.value?.trim() || "";
  if (isUsableAddress(hidden) && !isAddressLabelOnly(hidden)) {
    return hidden;
  }

  try {
    const { ok, body } = await fetchJsonOrRedirect("/api/transport/my-address");
    if (ok && isUsableAddress(body?.address) && !isAddressLabelOnly(body?.address)) {
      return body.address.trim();
    }
  } catch (error) {
    console.warn("내 주소 API 조회 실패, 마이페이지 HTML 파싱으로 재시도합니다.", error);
  }

  const response = await fetch("/members/mypage", {
    method: "GET",
    credentials: "same-origin",
    headers: { Accept: "text/html" }
  });

  if (!response.ok) {
    throw new Error("마이페이지를 불러오지 못했습니다.");
  }

  const parsed = parseAddressFromMyPageHtml(await response.text());
  if (!isUsableAddress(parsed) || isAddressLabelOnly(parsed)) {
    throw new Error("마이페이지 주소를 찾을 수 없습니다.");
  }

  return parsed.trim();
}

function parseAddressFromMyPageHtml(html) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");

  const candidates = [
    ".mypage-item",
    ".info-row",
    ".profile-row",
    "tr",
    "li",
    ".form-row",
    ".user-info-row"
  ];

  for (const selector of candidates) {
    for (const item of doc.querySelectorAll(selector)) {
      const label =
          item.querySelector(".mypage-item-label, .label, th, dt, .title")?.textContent?.trim() || "";

      const value =
          item.querySelector(".value, strong, td:last-child, dd, p")?.textContent?.trim() || "";

      if (
          ["주소", "기본 주소", "도로명 주소", "집 주소", "회원 주소"].includes(label) &&
          isUsableAddress(value) &&
          !isAddressLabelOnly(value)
      ) {
        return value;
      }
    }
  }

  const text = doc.body?.innerText || "";
  const lines = text
      .split("\n")
      .map(line => line.trim())
      .filter(Boolean);

  for (const line of lines) {
    if (isUsableAddress(line) && !isAddressLabelOnly(line)) {
      if (/(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)/.test(line)) {
        return line;
      }
    }
  }

  return "";
}

function isAddressLabelOnly(value) {
  const normalized = String(value || "").replace(/\s+/g, "").trim();
  return [
    "주소",
    "기본주소",
    "도로명주소",
    "집주소",
    "회원주소",
    "address",
    "Address"
  ].includes(normalized);
}

function isUsableAddress(value) {
  const text = String(value || "").trim();
  if (!text) return false;
  if (text.includes("입력된 정보 없음") || text.includes("없음")) return false;
  if (isAddressLabelOnly(text)) return false;

  return /(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)/.test(text)
      || /(로|길|동|읍|면|리)/.test(text);
}

async function calculateAndRenderRoutes(startContext) {
  lastRouteContext = startContext;

  const straightDistanceKm = calculateStraightDistanceKm(
      startContext.lat,
      startContext.lng,
      accomLat,
      accomLng
  );

  const [carResult, walkResult, transitResult] = await Promise.allSettled([
    fetchOsrmRoute(startContext.lat, startContext.lng, "driving"),
    straightDistanceKm > WALK_LIMIT_KM
        ? Promise.reject(new Error("도보는 가까운 거리만 선택해 주세요."))
        : fetchOsrmRoute(startContext.lat, startContext.lng, "foot"),
    fetchTransitRoute(startContext.lat, startContext.lng)
  ]);

  const metrics = buildRouteMetrics(
      startContext,
      carResult,
      walkResult,
      transitResult,
      straightDistanceKm
  );

  if (!metrics) {
    renderTransitDetail(null);
    updateRouteSummary(startContext.label, "-", "-");
    setModeTimeLabel("carTimeLabel", null, "자동차");
    setModeTimeLabel("walkTimeLabel", null, "도보");
    setModeTimeLabel("transitTimeLabel", null, "대중교통");
    setDistanceInfo("경로를 찾지 못했습니다. 출발지 또는 도착지를 다시 확인해 주세요.");
    alert("경로를 찾지 못했습니다. 출발지 또는 도착지를 다시 확인해 주세요.");
    return;
  }

  lastRouteMetrics = metrics;
  renderModeDurations(metrics);

  if (!metrics[selectedRouteMode]?.minutes) {
    selectedRouteMode = getFirstAvailableMode(metrics) || "car";
  }

  renderSelectedMode();
}

function buildRouteMetrics(startContext, carResult, walkResult, transitResult, straightDistanceKm) {
  const carRoute = carResult.status === "fulfilled" ? carResult.value : null;
  const walkRoute = walkResult.status === "fulfilled" ? walkResult.value : null;
  const transitRoute = transitResult.status === "fulfilled" ? transitResult.value : null;

  if (!carRoute && !walkRoute && !transitRoute) {
    return null;
  }

  const baseDistanceKm = resolveDistanceKm(carRoute, walkRoute, transitRoute);

  return {
    startLabel: startContext.label,
    car: buildRoadMetrics(carRoute, baseDistanceKm),
    walk: buildWalkMetrics(walkRoute, baseDistanceKm, straightDistanceKm),
    transit: buildTransitMetrics(baseDistanceKm, transitRoute)
  };
}

function resolveDistanceKm(carRoute, walkRoute, transitRoute) {
  if (carRoute?.distance) return Number((carRoute.distance / 1000).toFixed(2));
  if (walkRoute?.distance) return Number((walkRoute.distance / 1000).toFixed(2));
  if (transitRoute?.distanceKm) return Number(Number(transitRoute.distanceKm).toFixed(2));
  return 0;
}

function buildRoadMetrics(route, fallbackDistanceKm) {
  if (!route) {
    return {
      minutes: null,
      distanceKm: fallbackDistanceKm,
      geometry: null
    };
  }

  return {
    minutes: toRoundedMinutes(route.duration),
    distanceKm: Number((route.distance / 1000).toFixed(2)),
    geometry: route.geometry
  };
}

function buildWalkMetrics(route, fallbackDistanceKm, straightDistanceKm) {
  if (straightDistanceKm > WALK_LIMIT_KM) {
    return {
      minutes: null,
      distanceKm: Number(straightDistanceKm.toFixed(2)),
      geometry: null,
      message: "가까운 거리만 선택해 주세요."
    };
  }

  return buildRoadMetrics(route, fallbackDistanceKm);
}

async function fetchOsrmRoute(startLat, startLng, profile) {
  const { ok, body } = await fetchJsonOrRedirect(
      `/api/transport/road-route?profile=${encodeURIComponent(profile)}&sx=${startLng}&sy=${startLat}&ex=${accomLng}&ey=${accomLat}`
  );

  if (!ok) {
    throw new Error(body.message || "경로를 찾지 못했습니다.");
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

async function fetchIntercityRecommendation(startLat, startLng) {
  try {
    const { ok, body } = await fetchJsonOrRedirect(
        `/api/transport/intercity-recommend?sx=${startLng}&sy=${startLat}&ex=${accomLng}&ey=${accomLat}`
    );

    if (!ok) return null;

    const items = Array.isArray(body.items) ? body.items : [];
    if (!items.length) return null;

    const candidates = items
        .map(normalizeIntercityItem)
        .filter(item => item && Number.isFinite(Number(item.totalTime)))
        .filter(item => item.totalTime > 0)
        .sort((a, b) => {
          const diff = a.totalTime - b.totalTime;
          if (Math.abs(diff) <= 20) {
            const score = modePriority(a.type) - modePriority(b.type);
            if (score !== 0) return score;
          }
          return diff;
        });

    if (!candidates.length) return null;

    const best = candidates[0];

    return {
      totalTime: Number(best.totalTime || 0),
      payment: Number(best.payment || 0),
      busTransitCount: best.type === "train" ? 0 : best.type === "subway" ? 0 : 1,
      subwayTransitCount: best.type === "subway" ? 1 : 0,
      firstStartStation: best.firstStartStation || "",
      lastEndStation: best.lastEndStation || "",
      totalWalk: 0,
      distanceKm: 0,
      pathType: Number(best.pathType || (best.type === "train" ? 11 : best.type === "express" ? 12 : 14)),
      title: best.title || buildTransitTitle(best.steps),
      detail: best.detail || "",
      steps: best.steps
    };
  } catch (error) {
    console.error("intercity recommendation failed", error);
    return null;
  }
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
  return normalizeIntercityTitle(type, title, "");
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
        minutes,
        durationText: `${distance}m · ${formatStepDuration(minutes)}`
      };
    }

    if (type === "bus") {
      const busNo = lane.busNo || lane.name || "버스";
      return {
        type,
        title: `버스 ${busNo}`,
        summary: `${startName} -> ${endName}`,
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
        minutes: Math.max(1, sectionTime),
        durationText: `${stationCount > 0 ? `${stationCount}개 역 · ` : ""}${formatStepDuration(sectionTime)}`
      };
    }

    const subwayName = lane.name || "지하철";
    return {
      type: "subway",
      title: subwayName,
      summary: `${startName} -> ${endName}`,
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
    return {
      minutes: null,
      distanceKm,
      geometry: null,
      detail: "ODsay route unavailable",
      title: "",
      steps: [],
      payment: 0,
      alternatives: []
    };
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
    geometry: null,
    detail: detailParts.join(" / "),
    title: transitRoute.title || buildTransitTitle(transitRoute.steps),
    firstStartStation: transitRoute.firstStartStation || "",
    steps: transitRoute.steps,
    payment: transitRoute.payment,
    alternatives: Array.isArray(transitRoute.alternatives) ? transitRoute.alternatives : []
  };
}

function toRoundedMinutes(seconds) {
  const numericSeconds = Number(seconds);
  if (!Number.isFinite(numericSeconds) || numericSeconds <= 0) {
    return null;
  }
  return Math.max(1, Math.round(numericSeconds / 60));
}

function renderModeDurations(metrics) {
  setModeTimeLabel("carTimeLabel", metrics.car.minutes, "자동차");
  setModeTimeLabel("walkTimeLabel", metrics.walk.minutes, metrics.walk.message || "도보");
  setModeTimeLabel("transitTimeLabel", metrics.transit.minutes, "대중교통");
}

function setModeTimeLabel(elementId, minutes, fallbackLabel) {
  const element = getElement(elementId);
  if (!element) return;

  element.innerText = minutes == null ? `${fallbackLabel} 정보 없음` : `약 ${minutes}분`;
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
  meta.innerText = formatTransitHeadline(metrics.minutes);
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
    return metrics.title || "대중교통 경로";
  }
  return `${MODE_META[mode].label} 경로`;
}

function buildRouteDetailSummary(mode, metrics) {
  if (mode === "transit") {
    if (metrics.firstStartStation) return `${metrics.firstStartStation} 출발`;
    if (lastRouteContext?.label) return `${lastRouteContext.label} 출발`;
    return "최적 출발지";
  }
  if (lastRouteContext?.label) return `${lastRouteContext.label} 출발`;
  return "출발지";
}

function renderRouteDetailExtras(mode, metrics) {
  if (mode !== "transit") return "";
  return renderTransitAlternatives(metrics.alternatives);
}

function renderTransitSteps(steps) {
  if (!Array.isArray(steps) || !steps.length) {
    return `
      <article class="transit-step">
        <span class="transit-step__badge transit-step__badge--walk">안내</span>
        <div class="transit-step__copy">
          <strong>상세 구간 정보가 없습니다.</strong>
          <span>대중교통 응답에 세부 구간이 포함되지 않았습니다.</span>
        </div>
      </article>
    `;
  }

  return steps.map(step => {
    const normalizedType = normalizeStepType(step.type, step.title);
    const normalizedTitle = normalizeStepTitle(step.type, step.title);

    return `
      <article class="transit-step">
        <span class="transit-step__badge transit-step__badge--${normalizedType}">${getTransitBadgeLabel(normalizedType)}</span>
        <div class="transit-step__copy">
          <strong>${escapeHtml(normalizedTitle)}</strong>
          <span>${escapeHtml(step.summary || "")} · ${escapeHtml(step.durationText || "")}</span>
        </div>
      </article>
    `;
  }).join("");
}

function renderTransitStepBar(steps) {
  if (!Array.isArray(steps) || !steps.length) {
    return "";
  }

  const parsedSteps = steps.map(step => {
    const normalizedType = normalizeStepType(step.type, step.title);
    return {
      type: normalizedType,
      minutes: Number(step.minutes || extractStepMinutes(step.durationText)),
      label: shortenStepLabel(normalizeStepTitle(step.type, step.title))
    };
  });

  const totalMinutes = parsedSteps.reduce((sum, step) => sum + Math.max(step.minutes, 1), 0);

  return parsedSteps.map(step => {
    const width = totalMinutes > 0 ? Math.max((step.minutes / totalMinutes) * 100, 12) : 20;
    return `
      <span class="transit-step-bar__segment transit-step-bar__segment--${step.type}" style="width:${width}%">
        <span class="transit-step-bar__time">${step.minutes}분</span>
        <span class="transit-step-bar__label">${escapeHtml(step.label)}</span>
      </span>
    `;
  }).join("");
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

function shortenStepLabel(text) {
  const value = String(text || "").trim();
  if (value.length <= 10) return value;
  return `${value.slice(0, 10)}…`;
}

function getTransitBadgeLabel(type) {
  if (type === "bus") return "버스";
  if (type === "train") return "기차";
  if (type === "subway") return "지하철";
  return "도보";
}

function drawSelectedRouteLine(geometry, mode) {
  clearRouteLine();

  const lineGeometry = mode === "transit" ? lastRouteMetrics?.car?.geometry : geometry;
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

async function fetchJsonOrRedirect(url) {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { Accept: "application/json" }
  });

  const contentType = response.headers.get("content-type") || "";
  if (response.redirected || contentType.includes("text/html")) {
    window.location.href = `${LOGIN_URL}?redirectUrl=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    throw new Error("로그인이 필요합니다.");
  }

  const body = await response.json();
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
    steps: Array.isArray(data.steps) ? data.steps : [],
    alternatives: dedupeTransitAlternatives(Array.isArray(data.alternatives) ? data.alternatives : [])
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
        type: item?.type || "bus",
        title: item?.title || "대중교통",
        totalTime: Number(item?.totalTime || item?.minutes || 0),
        payment: Number(item?.payment || item?.paymentAmount || 0),
        pathType: Number(item?.pathType || 0),
        detail: item?.detail || "",
        steps: Array.isArray(item?.steps) ? item.steps : []
      }))
      .filter(item => Number.isFinite(item.totalTime) && item.totalTime > 0)
      .sort((a, b) => a.totalTime - b.totalTime)
      .filter(item => {
        const key = `${item.type}:${item.title}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      })
      .slice(0, 5);
}

function renderTransitAlternatives(alternatives) {
  if (!Array.isArray(alternatives) || !alternatives.length) return "";

  return `
    <div class="transit-alt-grid">
      ${alternatives.map(item => `
        <article class="transit-alt-card">
          <span class="transit-alt-card__type">${escapeHtml(item.title || "대중교통")}</span>
          <strong class="transit-alt-card__time">${escapeHtml(formatStepDuration(Number(item.totalTime || 0)))}</strong>
        </article>
      `).join("")}
    </div>
  `;
}
