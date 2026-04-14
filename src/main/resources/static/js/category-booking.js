(function () {
    const configEl = document.getElementById('categoryBookingConfig');
    const modal = document.getElementById('categoryBookingModal');
    const cartNotice = document.getElementById('categoryCartNotice');

    if (!configEl || !modal) {
        return;
    }

    const state = {
        accomId: null,
        accomName: '',
        pricePerNight: 0,
        roomCount: 0,
        guestCount: 1,
        accomType: '',
        checkInTime: '',
        checkOutTime: '',
        remainingRooms: null,
        operationDays: new Set(),
        soldOutDays: new Set(),
        operationStartDate: null,
        operationEndDate: null,
        currentMonth: null,
        selectedCheckIn: null,
        selectedCheckOut: null,
        previewCheckOut: null,
        latestRemainingRooms: null,
        monthlyRoomsCache: {},
        dailyRooms: {},
        lastAvailabilityKey: '',
        availabilityController: null,
        openRequestId: 0
    };

    const bookingMetaCache = {};
    const isAuthenticated = String(configEl.dataset.isAuthenticated) === 'true';
    const loginUrl = configEl.dataset.loginUrl || '/members/login';
    const csrfHeader = configEl.dataset.csrfHeader;
    const csrfToken = configEl.dataset.csrfToken;
    const today = new Date();
    const baseToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const defaultAdultCount = getDefaultAdultCountByAccomType(accomType);

    const elements = {
        accomName: document.getElementById('categoryBookingAccomName'),
        title: document.getElementById('categoryBookingTitle'),
        checkTimeText: document.getElementById('modalCheckTimeText'),
        roomCountText: document.getElementById('modalRoomCountText'),
        checkInDate: document.getElementById('modalCheckInDate'),
        checkOutDate: document.getElementById('modalCheckOutDate'),
        summaryCheckIn: document.getElementById('modalSummaryCheckIn'),
        summaryCheckOut: document.getElementById('modalSummaryCheckOut'),
        summaryNights: document.getElementById('modalSummaryNights'),
        summaryCheckInTime: document.getElementById('modalSummaryCheckInTime'),
        summaryCheckOutTime: document.getElementById('modalSummaryCheckOutTime'),
        summaryRemaining: document.getElementById('modalSummaryRemaining'),
        adultCount: document.getElementById('modalAdultCount'),
        childCount: document.getElementById('modalChildCount'),
        roomCount: document.getElementById('modalRoomCount'),
        guestHint: document.getElementById('modalGuestHint'),
        surchargeGuide: document.getElementById('modalSurchargeGuide'),
        basePrice: document.getElementById('modalBasePrice'),
        surchargeRow: document.getElementById('modalSurchargeRow'),
        surchargePrice: document.getElementById('modalSurchargePrice'),
        totalPrice: document.getElementById('modalTotalPrice'),
        cartBtn: document.getElementById('modalCartBtn'),
        orderBtn: document.getElementById('modalOrderBtn'),
        cartStayBtn: document.getElementById('categoryCartStayBtn'),
        cartGoBtn: document.getElementById('categoryCartGoBtn'),
        calendarGrid: document.getElementById('modalCalendarGrid'),
        calendarMonthLabel: document.getElementById('modalCalendarMonthLabel'),
        prevMonthBtn: document.getElementById('modalPrevMonthBtn'),
        nextMonthBtn: document.getElementById('modalNextMonthBtn')
    };

    const checkInDateInput = document.getElementById('checkInDateInput');
    const checkOutDateInput = document.getElementById('checkOutDateInput');

    function formatNumber(value) {
        return Number(value || 0).toLocaleString('ko-KR');
    }

    function parseDate(value) {
        if (!value) {
            return null;
        }

        const [year, month, day] = value.split('-').map(Number);
        if (!year || !month || !day) {
            return null;
        }

        return new Date(year, month - 1, day);
    }

    function parseLocalDate(value) {
        return parseDate(value);
    }

    function formatDateObj(date) {
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    function formatDate(year, month, day) {
        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }

    function firstDayOfMonth(value) {
        const date = parseLocalDate(value);
        return date ? new Date(date.getFullYear(), date.getMonth(), 1) : null;
    }

    function getNights() {
        if (!state.selectedCheckIn || !state.selectedCheckOut) {
            return 0;
        }

        const checkIn = parseLocalDate(state.selectedCheckIn);
        const checkOut = parseLocalDate(state.selectedCheckOut);
        if (!checkIn || !checkOut) {
            return 0;
        }

        return Math.round((checkOut.getTime() - checkIn.getTime()) / 86400000);
    }

    function getDefaultAdultCountByAccomType(accomType) {
        if (!accomType) {
            return 1;
        }

        const normalizedType = String(accomType).trim().toUpperCase();

        if (normalizedType === 'HOTEL' || normalizedType === 'RESORT' || normalizedType === 'PENSION') {
            return 2;
        }

        if (normalizedType === 'MOTEL' || normalizedType === 'GUESTHOUSE') {
            return 1;
        }

        return 1;
    }

    function getAdultCount() {
        return Math.max(1, Number(elements.adultCount.value || defaultAdultCount));
    }

    function getChildCount() {
        return Math.max(0, Number(elements.childCount.value || 0));
    }

    function getRoomCount() {
        return Math.max(1, Number(elements.roomCount.value || 1));
    }

    function isSmallType() {
        return state.accomType === 'MOTEL' || state.accomType === 'GUESTHOUSE';
    }

    function getEffectiveMaxGuests() {
        const typeMaxGuests = isSmallType() ? 6 : 10;
        return Math.min(Number(state.guestCount || 1), typeMaxGuests);
    }

    function getTypeMinGuests() {
        return isSmallType() ? 1 : 2;
    }

    function getSurchargeThreshold() {
        return isSmallType() ? 2 : 4;
    }

    function calcSurchargePerNight(adultCount, childCount) {
        const totalGuests = adultCount + childCount;
        const threshold = getSurchargeThreshold();
        if (totalGuests <= threshold) {
            return 0;
        }

        const excessTotal = totalGuests - threshold;
        const excessAdults = Math.max(0, adultCount - threshold);
        const excessChildren = excessTotal - excessAdults;

        return Math.floor(Number(state.pricePerNight) * 0.10 * excessAdults)
            + Math.floor(Number(state.pricePerNight) * 0.05 * excessChildren);
    }

    function redirectToLogin() {
        const redirectUrl = `${window.location.pathname}${window.location.search}`;
        window.location.href = `${loginUrl}?redirectUrl=${encodeURIComponent(redirectUrl)}`;
    }

    function moveToMyPageEditForReservationInfo() {
        window.location.href = '/members/mypage/edit?reservationInfoRequired=true';
    }

    function handleReservationInfoRequiredMessage(message) {
        if (message && message.includes('예약을 위해 연락처와 주소를 먼저 입력해주세요.')) {
            moveToMyPageEditForReservationInfo();
            return true;
        }

        return false;
    }

    function getHeaders() {
        const headers = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            Accept: 'application/json'
        };

        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        return headers;
    }

    function setAvailabilityBadge(el, text, mod) {
        const cls = mod ? `category-booking-remaining-badge ${mod}` : 'category-booking-remaining-badge';
        el.innerHTML = `<span class="${cls}">${text}</span>`;
    }

    function updateRemainingBadge(value) {
        const target = elements.summaryRemaining;

        if (value === null || value === undefined || value === '') {
            setAvailabilityBadge(target, '-', '');
            return;
        }

        if (Number(value) <= 0) {
            setAvailabilityBadge(target, '마감', 'is-soldout');
            return;
        }

        if (Number(value) < 15) {
            setAvailabilityBadge(target, `${value}실 남음`, 'is-available');
            return;
        }

        setAvailabilityBadge(target, '예약가능', 'is-default');
    }

    function setStockLabelState(label, text, isOpen) {
        if (!label) {
            return;
        }

        label.textContent = text;
        label.classList.remove('is-hidden');
        label.classList.toggle('is-open', Boolean(isOpen));
        label.classList.toggle('is-soldout', !isOpen);
    }

    async function updateStockLabels() {
        const stockLabels = Array.from(document.querySelectorAll('[data-stock-label="true"]'));
        if (!stockLabels.length) {
            return;
        }

        const checkIn = checkInDateInput?.value || '';
        const checkOut = checkOutDateInput?.value || '';

        if (!checkIn || !checkOut) {
            stockLabels.forEach((label) => {
                label.classList.add('is-hidden');
            });
            return;
        }

        const checkInDate = parseDate(checkIn);
        const checkOutDate = parseDate(checkOut);
        if (!checkInDate || !checkOutDate || checkOutDate.getTime() <= checkInDate.getTime()) {
            stockLabels.forEach((label) => label.classList.add('is-hidden'));
            return;
        }

        await Promise.all(stockLabels.map(async (label) => {
            const accomId = Number(label.dataset.accomId || 0);
            if (!accomId) {
                setStockLabelState(label, '확인 불가', false);
                return;
            }

            try {
                const response = await fetch(`/orders/accom/${accomId}/availability?checkInDate=${encodeURIComponent(checkIn)}&checkOutDate=${encodeURIComponent(checkOut)}`);
                if (!response.ok) {
                    setStockLabelState(label, '확인 불가', false);
                    return;
                }

                const data = await response.json();
                const remainingRooms = typeof data.remainingRooms === 'number' ? data.remainingRooms : null;
                if (remainingRooms === null) {
                    setStockLabelState(label, '확인 불가', false);
                } else if (remainingRooms <= 0) {
                    setStockLabelState(label, '예약 마감', false);
                } else {
                    setStockLabelState(label, `남은객실 ${formatNumber(remainingRooms)}개`, true);
                }
            } catch (_) {
                setStockLabelState(label, '확인 불가', false);
            }
        }));
    }

    async function fetchBookingMeta(accomId) {
        if (bookingMetaCache[accomId]) {
            return bookingMetaCache[accomId];
        }

        const response = await fetch(`/orders/accom/${accomId}/booking-meta`, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                Accept: 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('booking meta fetch failed');
        }

        const data = await response.json();
        bookingMetaCache[accomId] = data;
        return data;
    }

    async function fetchMonthlyRooms(year, month) {
        const key = `${state.accomId}:${year}-${month}`;
        if (state.monthlyRoomsCache[key]) {
            state.dailyRooms = state.monthlyRoomsCache[key];
            renderCalendar();
            return;
        }

        try {
            const res = await fetch(`/orders/accom/${state.accomId}/monthly-availability?year=${year}&month=${month}`);
            if (res.ok) {
                const data = await res.json();
                state.monthlyRoomsCache[key] = data;
                state.dailyRooms = data;
            }
        } catch (_) {
            state.dailyRooms = {};
        }
        renderCalendar();
    }

    function initCalendarMonth() {
        if (state.selectedCheckIn) {
            state.currentMonth = parseLocalDate(state.selectedCheckIn);
            state.currentMonth.setDate(1);
            return;
        }
        if (state.operationStartDate) {
            state.currentMonth = parseLocalDate(state.operationStartDate);
            state.currentMonth.setDate(1);
            return;
        }
        state.currentMonth = new Date(baseToday.getFullYear(), baseToday.getMonth(), 1);
    }

    function buildStayDates(checkIn, checkOut) {
        const result = [];
        let cursor = parseLocalDate(checkIn);
        const out = parseLocalDate(checkOut);
        while (cursor < out) {
            result.push(formatDateObj(cursor));
            cursor.setDate(cursor.getDate() + 1);
        }
        return result;
    }

    function buildInnerRange(checkIn, checkOut) {
        return buildStayDates(checkIn, checkOut).filter((date) => date !== checkIn);
    }

    function isBetweenSelectedRange(dateString) {
        return state.selectedCheckIn && state.selectedCheckOut && dateString > state.selectedCheckIn && dateString < state.selectedCheckOut;
    }

    function isBeforeToday(dateString) {
        return parseLocalDate(dateString) < baseToday;
    }

    function isDateClosedByCheckInTime(dateString) {
        if (!state.checkInTime) {
            return false;
        }

        const now = new Date();
        if (dateString !== formatDateObj(now)) {
            return false;
        }

        const [hour, minute] = state.checkInTime.split(':').map(Number);
        return now >= new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute, 0);
    }

    function isDateUnavailable(dateString) {
        return isBeforeToday(dateString)
            || !state.operationDays.has(dateString)
            || state.soldOutDays.has(dateString)
            || isDateClosedByCheckInTime(dateString);
    }

    function clearPreviewRange(resetValue = true) {
        elements.calendarGrid.querySelectorAll('.calendar-day.is-preview').forEach((cell) => {
            cell.classList.remove('is-preview');
            const badge = cell.querySelector('.calendar-day__badge');
            if (badge && badge.dataset.originalText !== undefined) {
                badge.textContent = badge.dataset.originalText;
                delete badge.dataset.originalText;
            }
        });

        if (resetValue) {
            state.previewCheckOut = null;
        }
    }

    function applyPreviewRange() {
        clearPreviewRange(false);
        if (!state.selectedCheckIn || state.selectedCheckOut || !state.previewCheckOut) {
            return;
        }

        buildInnerRange(state.selectedCheckIn, state.previewCheckOut).forEach((dateString) => {
            const cell = elements.calendarGrid.querySelector(`.calendar-day[data-date="${dateString}"]`);
            if (cell && !cell.classList.contains('is-selected') && !cell.classList.contains('is-in-range')) {
                cell.classList.add('is-preview');
                const badge = cell.querySelector('.calendar-day__badge');
                if (badge) {
                    badge.dataset.originalText = badge.textContent;
                }
            }
        });
    }

    function handleDateHover(dateString) {
        if (!state.selectedCheckIn || state.selectedCheckOut || dateString <= state.selectedCheckIn || isDateUnavailable(dateString)) {
            clearPreviewRange();
            return;
        }

        const stayDates = buildStayDates(state.selectedCheckIn, dateString);
        if (stayDates.some((date) => isDateUnavailable(date))) {
            clearPreviewRange();
            return;
        }

        state.previewCheckOut = dateString;
        applyPreviewRange();
    }

    function selectDate(dateString) {
        if (isDateClosedByCheckInTime(dateString)) {
            alert('당일 체크인 가능 시간이 지나 선택할 수 없습니다.');
            return;
        }

        if (state.selectedCheckIn === dateString && !state.selectedCheckOut) {
            state.selectedCheckIn = null;
            state.selectedCheckOut = null;
            state.previewCheckOut = null;
            renderCalendar();
            updateSummary();
            return;
        }

        if (state.selectedCheckIn === dateString && state.selectedCheckOut) {
            state.selectedCheckIn = null;
            state.selectedCheckOut = null;
            state.previewCheckOut = null;
            renderCalendar();
            updateSummary();
            return;
        }

        if (state.selectedCheckOut === dateString) {
            state.selectedCheckOut = null;
            state.previewCheckOut = null;
            renderCalendar();
            updateSummary();
            return;
        }

        if (!state.selectedCheckIn || (state.selectedCheckIn && state.selectedCheckOut)) {
            if (isDateUnavailable(dateString)) {
                return;
            }
            state.selectedCheckIn = dateString;
            state.selectedCheckOut = null;
            state.previewCheckOut = null;
            renderCalendar();
            updateSummary();
            return;
        }

        if (dateString < state.selectedCheckIn) {
            if (isDateUnavailable(dateString)) {
                return;
            }
            state.selectedCheckIn = dateString;
            state.selectedCheckOut = null;
            state.previewCheckOut = null;
            renderCalendar();
            updateSummary();
            return;
        }

        if (dateString > state.selectedCheckIn) {
            if (isDateUnavailable(dateString)) {
                return;
            }

            const stayDates = buildStayDates(state.selectedCheckIn, dateString);
            if (stayDates.some((date) => isDateUnavailable(date))) {
                alert('선택한 범위 안에 운영하지 않는 날짜 또는 예약 불가 날짜가 포함되어 있습니다.');
                return;
            }

            state.selectedCheckOut = dateString;
            state.previewCheckOut = null;
            renderCalendar();
            updateSummary();
        }
    }

    function renderCalendar() {
        const grid = elements.calendarGrid;
        grid.innerHTML = '';

        const year = state.currentMonth.getFullYear();
        const month = state.currentMonth.getMonth();
        elements.calendarMonthLabel.textContent = `${year}년 ${month + 1}월`;

        const firstDate = new Date(year, month, 1);
        const lastDate = new Date(year, month + 1, 0);
        const startWeekday = firstDate.getDay();
        const daysInMonth = lastDate.getDate();

        for (let i = 0; i < startWeekday; i += 1) {
            const emptyCell = document.createElement('div');
            emptyCell.className = 'calendar-day is-other-month';
            grid.appendChild(emptyCell);
        }

        for (let day = 1; day <= daysInMonth; day += 1) {
            const dateString = formatDate(year, month + 1, day);
            const cell = document.createElement('button');
            cell.type = 'button';
            cell.className = 'calendar-day';
            cell.dataset.date = dateString;

            const isPastDate = isBeforeToday(dateString);
            const isOperationDay = state.operationDays.has(dateString);
            const isSoldOut = state.soldOutDays.has(dateString);
            const isClosedByCheckInTime = isDateClosedByCheckInTime(dateString);
            const isToday = dateString === formatDateObj(new Date());
            const dow = new Date(year, month, day).getDay();
            const selectable = !isPastDate && isOperationDay && !isSoldOut && !isClosedByCheckInTime;

            if (isPastDate) {
                cell.classList.add('is-disabled', 'is-past');
            } else if (!isOperationDay || isClosedByCheckInTime) {
                cell.classList.add('is-disabled');
            }
            if (isSoldOut) {
                cell.classList.remove('is-disabled', 'is-past');
                cell.classList.add('is-soldout');
            }
            if (isToday) {
                cell.classList.add('is-today');
            }
            if (dow === 0) {
                cell.classList.add('is-sunday');
            }
            if (dow === 6) {
                cell.classList.add('is-saturday');
            }
            if (state.selectedCheckIn === dateString || state.selectedCheckOut === dateString) {
                cell.classList.add('is-selected');
            } else if (isBetweenSelectedRange(dateString)) {
                cell.classList.add('is-in-range');
            }

            const num = document.createElement('div');
            num.className = 'calendar-day__num';
            if (isToday) {
                num.classList.add('is-today');
            }
            num.textContent = day;
            cell.appendChild(num);

            const badge = document.createElement('div');
            badge.className = 'calendar-day__badge';
                if (isSoldOut) {
                    badge.textContent = '마감';
                } else if (isPastDate) {
                    badge.textContent = '지난 날짜';
                } else if (isClosedByCheckInTime || !isOperationDay) {
                    badge.textContent = '비운영';
                } else if (state.selectedCheckIn === dateString) {
                badge.textContent = '체크인';
            } else if (state.selectedCheckOut === dateString) {
                badge.textContent = '체크아웃';
            } else if (isBetweenSelectedRange(dateString)) {
                badge.textContent = '선택';
            } else {
                const remaining = state.dailyRooms[dateString];
                if (typeof remaining === 'number') {
                    badge.textContent = `${remaining}실 남음`;
                    badge.classList.add(remaining < 3 ? 'is-low-stock' : 'is-available');
                } else {
                    badge.classList.add('is-available');
                    badge.textContent = '예약가능';
                }
            }
            cell.appendChild(badge);

            if (selectable || state.selectedCheckIn === dateString || state.selectedCheckOut === dateString) {
                cell.addEventListener('click', () => selectDate(dateString));
                cell.addEventListener('mouseenter', () => handleDateHover(dateString));
                cell.addEventListener('mouseleave', () => clearPreviewRange());
            }

            grid.appendChild(cell);
        }
    }

    function fillGuestGuide() {
        const effectiveMax = getEffectiveMaxGuests();
        const surchargeFromPerson = isSmallType() ? 3 : 5;

        elements.guestHint.textContent = `최대 ${effectiveMax}명까지 가능합니다.`;
        elements.guestHint.classList.remove('error');
        elements.surchargeGuide.innerHTML =
            `<span><strong>${surchargeFromPerson}명 이상</strong>부터 성인 1인당 1박 추가 요금 <strong>10%</strong>, 아동 1인당 <strong>5%</strong> 추가 요금이 부과됩니다.</span>`;
    }

    async function updateAvailability() {
        if (!state.selectedCheckIn || !state.selectedCheckOut) {
            state.latestRemainingRooms = null;
            updateRemainingBadge(null);
            state.lastAvailabilityKey = '';
            return;
        }

        const key = `${state.selectedCheckIn}_${state.selectedCheckOut}`;
        if (key === state.lastAvailabilityKey) {
            return;
        }
        state.lastAvailabilityKey = key;

        if (state.availabilityController) {
            state.availabilityController.abort();
        }

        state.availabilityController = new AbortController();

        try {
            const response = await fetch(
                `/orders/accom/${state.accomId}/availability?checkInDate=${encodeURIComponent(state.selectedCheckIn)}&checkOutDate=${encodeURIComponent(state.selectedCheckOut)}`,
                { signal: state.availabilityController.signal, headers: { 'X-Requested-With': 'XMLHttpRequest', Accept: 'application/json' } }
            );

            if (!response.ok) {
                updateRemainingBadge(null);
                return;
            }

            const data = await response.json();
            const remaining = typeof data.remainingRooms === 'number' ? data.remainingRooms : null;
            state.latestRemainingRooms = remaining;
            updateRemainingBadge(remaining);
            onGuestChange();
        } catch (error) {
            state.latestRemainingRooms = null;
            if (error.name !== 'AbortError') {
                updateRemainingBadge(null);
            }
        }
    }

    function onGuestChange() {
        const adultCount = getAdultCount();
        const childCount = getChildCount();
        const roomCount = getRoomCount();
        const totalGuests = adultCount + childCount;
        const effectiveMax = getEffectiveMaxGuests();
        const typeMinGuests = getTypeMinGuests();

        let hintText = `최대 ${effectiveMax}명까지 가능합니다.`;
        let error = false;

        if (adultCount < 1) {
            hintText = '성인은 최소 1명 이상이어야 합니다.';
            error = true;
        } else if (roomCount < 1) {
            hintText = '객실 수는 최소 1실 이상이어야 합니다.';
            error = true;
        } else if (totalGuests < typeMinGuests) {
            hintText = `이 숙소 유형은 최소 ${typeMinGuests}인 이상이어야 합니다.`;
            error = true;
        } else if (totalGuests > effectiveMax) {
            hintText = `최대 ${effectiveMax}명까지 가능합니다.`;
            error = true;
        } else if (state.latestRemainingRooms !== null && roomCount > state.latestRemainingRooms) {
            hintText = `잔여 객실은 ${state.latestRemainingRooms}실입니다.`;
            error = true;
        }

        elements.guestHint.textContent = hintText;
        elements.guestHint.classList.toggle('error', error);
        updateSummary();
    }

    function updateSummary() {
        const beforeCheckIn = state.selectedCheckIn;
        const beforeCheckOut = state.selectedCheckOut;
        if (state.selectedCheckIn && isDateUnavailable(state.selectedCheckIn)) {
            state.selectedCheckIn = null;
            state.selectedCheckOut = null;
            state.previewCheckOut = null;
        }
        if (state.selectedCheckOut && isDateUnavailable(state.selectedCheckOut)) {
            state.selectedCheckOut = null;
        }
        const selectionChanged = beforeCheckIn !== state.selectedCheckIn || beforeCheckOut !== state.selectedCheckOut;

        elements.summaryCheckIn.textContent = state.selectedCheckIn || '선택 전';
        elements.summaryCheckOut.textContent = state.selectedCheckOut || '선택 전';
        elements.checkInDate.value = state.selectedCheckIn || '';
        elements.checkOutDate.value = state.selectedCheckOut || '';

        const nights = getNights();
        elements.summaryNights.textContent = `${nights}박`;

        const adultCount = getAdultCount();
        const childCount = getChildCount();
        const roomCount = getRoomCount();
        const baseTotal = Number(state.pricePerNight) * nights * roomCount;
        const surchargePerNight = calcSurchargePerNight(adultCount, childCount);
        const surchargeTotal = surchargePerNight * nights * roomCount;
        const total = baseTotal + surchargeTotal;

        elements.basePrice.textContent = `${formatNumber(baseTotal)}원`;

        if (surchargeTotal > 0) {
            elements.surchargeRow.style.display = '';
            elements.surchargePrice.textContent = `${formatNumber(surchargeTotal)}원`;
        } else {
            elements.surchargeRow.style.display = 'none';
            elements.surchargePrice.textContent = '0원';
        }

        elements.totalPrice.textContent = `${formatNumber(total)}원`;
        if (selectionChanged) {
            renderCalendar();
        }
        updateAvailability();
    }

    function getBookingSelection() {
        if (!state.selectedCheckIn || !state.selectedCheckOut) {
            alert('체크인과 체크아웃 날짜를 선택해 주세요.');
            return null;
        }

        const nights = getNights();
        if (nights <= 0) {
            alert('체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.');
            return null;
        }

        const adultCount = getAdultCount();
        const childCount = getChildCount();
        const roomCount = getRoomCount();
        const totalGuests = adultCount + childCount;
        const effectiveMax = getEffectiveMaxGuests();
        const typeMinGuests = getTypeMinGuests();

        if (adultCount < 1) {
            alert('성인은 최소 1명 이상이어야 합니다.');
            return null;
        }
        if (roomCount < 1) {
            alert('객실 수는 최소 1실 이상이어야 합니다.');
            return null;
        }
        if (totalGuests < typeMinGuests) {
            alert(`이 숙소 유형은 최소 ${typeMinGuests}인 이상이어야 합니다.`);
            return null;
        }
        if (totalGuests > effectiveMax) {
            alert(`최대 ${effectiveMax}명까지 가능합니다.`);
            return null;
        }
        if (state.latestRemainingRooms !== null && roomCount > state.latestRemainingRooms) {
            alert(`예약 가능한 객실 수를 초과했습니다. 잔여 객실은 ${state.latestRemainingRooms}실입니다.`);
            return null;
        }

        return { adultCount, childCount, roomCount };
    }

    async function fetchRemainingRooms() {
        const response = await fetch(
            `/orders/accom/${state.accomId}/availability?checkInDate=${encodeURIComponent(state.selectedCheckIn)}&checkOutDate=${encodeURIComponent(state.selectedCheckOut)}`,
            { headers: { 'X-Requested-With': 'XMLHttpRequest', Accept: 'application/json' } }
        );

        if (!response.ok) {
            throw new Error('availability fetch failed');
        }

        const data = await response.json();
        return typeof data.remainingRooms === 'number' ? data.remainingRooms : null;
    }

    async function submitCart() {
        const selection = getBookingSelection();
        if (!selection) {
            return;
        }

        try {
            const remainingRooms = await fetchRemainingRooms();
            if (remainingRooms !== null && selection.roomCount > remainingRooms) {
                state.latestRemainingRooms = remainingRooms;
                onGuestChange();
                alert(`예약 가능한 객실 수를 초과했습니다. 잔여 객실은 ${remainingRooms}실입니다.`);
                return;
            }

            const response = await fetch('/cart', {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({
                    accomId: state.accomId,
                    checkInDate: state.selectedCheckIn,
                    checkOutDate: state.selectedCheckOut,
                    adultCount: selection.adultCount,
                    childCount: selection.childCount,
                    roomCount: selection.roomCount
                })
            });

            if (response.ok) {
                closeModal();
                openCartNotice();
                return;
            }

            const message = await response.text();
            if (handleReservationInfoRequiredMessage(message)) {
                return;
            }
            alert(`장바구니에 담지 못했습니다.\n이유: ${message}`);
        } catch (error) {
            alert(`장바구니에 담지 못했습니다.\n이유: ${error.message}`);
        }
    }

    async function submitOrder() {
        const selection = getBookingSelection();
        if (!selection) {
            return;
        }

        try {
            const remainingRooms = await fetchRemainingRooms();
            if (remainingRooms !== null && selection.roomCount > remainingRooms) {
                state.latestRemainingRooms = remainingRooms;
                onGuestChange();
                alert(`예약 가능한 객실 수를 초과했습니다. 잔여 객실은 ${remainingRooms}실입니다.`);
                return;
            }

            const response = await fetch('/order', {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({
                    accomId: state.accomId,
                    checkInDate: state.selectedCheckIn,
                    checkOutDate: state.selectedCheckOut,
                    adultCount: selection.adultCount,
                    childCount: selection.childCount,
                    roomCount: selection.roomCount
                })
            });

            if (response.ok) {
                const orderId = await response.json();
                window.location.href = `/orders/${orderId}`;
                return;
            }

            const message = await response.text();
            if (handleReservationInfoRequiredMessage(message)) {
                return;
            }
            alert(`예약에 실패했습니다: ${message}`);
        } catch (error) {
            alert(`오류가 발생했습니다: ${error.message}`);
        }
    }

    async function moveMonth(offset) {
        const moved = new Date(state.currentMonth.getFullYear(), state.currentMonth.getMonth() + offset, 1);
        if (state.operationStartDate && moved < firstDayOfMonth(state.operationStartDate)) {
            return;
        }
        if (state.operationEndDate && moved > firstDayOfMonth(state.operationEndDate)) {
            return;
        }

        state.currentMonth = moved;
        state.dailyRooms = {};
        await fetchMonthlyRooms(state.currentMonth.getFullYear(), state.currentMonth.getMonth() + 1);
        renderCalendar();
    }

    async function openModal(button) {
        const requestId = ++state.openRequestId;
        state.accomId = Number(button.dataset.accomId || 0);
        state.accomName = button.dataset.accomName || '';
        state.pricePerNight = Number(button.dataset.price || 0);
        state.roomCount = Number(button.dataset.roomCount || 0);
        state.guestCount = Number(button.dataset.guestCount || 1);
        state.accomType = button.dataset.accomType || '';
        state.monthlyRoomsCache = {};
        state.dailyRooms = {};
        state.previewCheckOut = null;
        state.latestRemainingRooms = null;
        state.lastAvailabilityKey = '';

        if (elements.title) {
            elements.title.textContent = '예약 옵션 선택';
        }
        if (elements.accomName) {
            elements.accomName.textContent = state.accomName;
        }
        elements.roomCountText.textContent = `${formatNumber(state.roomCount)}개`;
        elements.adultCount.value = '1';
        elements.childCount.value = '0';
        elements.roomCount.value = '1';
        elements.cartBtn.hidden = false;
        elements.orderBtn.hidden = false;

        try {
            const meta = await fetchBookingMeta(state.accomId);
            if (requestId !== state.openRequestId) {
                return;
            }
            state.operationDays = new Set(meta.operationDays || []);
            state.soldOutDays = new Set(meta.soldOutDays || []);
            state.operationStartDate = meta.operationStartDate || null;
            state.operationEndDate = meta.operationEndDate || null;
            state.checkInTime = meta.checkInTime || button.dataset.checkInTime || '미등록';
            state.checkOutTime = meta.checkOutTime || button.dataset.checkOutTime || '미등록';
        } catch (_) {
            if (requestId !== state.openRequestId) {
                return;
            }
            state.operationDays = new Set();
            state.soldOutDays = new Set();
            state.operationStartDate = null;
            state.operationEndDate = null;
            state.checkInTime = button.dataset.checkInTime || '미등록';
            state.checkOutTime = button.dataset.checkOutTime || '미등록';
        }

        elements.checkTimeText.textContent = `${state.checkInTime || '미등록'} / ${state.checkOutTime || '미등록'}`;
        elements.summaryCheckInTime.textContent = state.checkInTime || '미등록';
        elements.summaryCheckOutTime.textContent = state.checkOutTime || '미등록';

        state.selectedCheckIn = checkInDateInput?.value || null;
        state.selectedCheckOut = checkOutDateInput?.value || null;

        if (state.selectedCheckIn && isDateUnavailable(state.selectedCheckIn)) {
            state.selectedCheckIn = null;
            state.selectedCheckOut = null;
        }
        if (state.selectedCheckOut && isDateUnavailable(state.selectedCheckOut)) {
            state.selectedCheckOut = null;
        }

        initCalendarMonth();
        await fetchMonthlyRooms(state.currentMonth.getFullYear(), state.currentMonth.getMonth() + 1);
        if (requestId !== state.openRequestId) {
            return;
        }
        renderCalendar();
        fillGuestGuide();
        updateRemainingBadge(null);
        updateSummary();

        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        elements.orderBtn.focus();
    }

    function closeModal() {
        state.openRequestId += 1;
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
    }

    function openCartNotice() {
        if (!cartNotice) {
            return;
        }

        cartNotice.classList.add('is-open');
        cartNotice.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        elements.cartStayBtn?.focus();
    }

    function closeCartNotice() {
        if (!cartNotice) {
            return;
        }

        cartNotice.classList.remove('is-open');
        cartNotice.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
    }

    document.addEventListener('click', (event) => {
        const button = event.target.closest('.js-open-booking');
        if (!button) {
            return;
        }

        if (!isAuthenticated) {
            redirectToLogin();
            return;
        }

        openModal(button);
    });

    document.querySelectorAll('.js-booking-close').forEach((button) => {
        button.addEventListener('click', closeModal);
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && modal.classList.contains('is-open')) {
            closeModal();
        }
        if (event.key === 'Escape' && cartNotice?.classList.contains('is-open')) {
            closeCartNotice();
        }
    });

    elements.prevMonthBtn?.addEventListener('click', () => moveMonth(-1));
    elements.nextMonthBtn?.addEventListener('click', () => moveMonth(1));
    elements.adultCount?.addEventListener('input', onGuestChange);
    elements.childCount?.addEventListener('input', onGuestChange);
    elements.roomCount?.addEventListener('input', onGuestChange);
    elements.cartBtn?.addEventListener('click', submitCart);
    elements.orderBtn?.addEventListener('click', submitOrder);
    elements.cartStayBtn?.addEventListener('click', closeCartNotice);
    elements.cartGoBtn?.addEventListener('click', () => {
        window.location.href = '/cart';
    });
    checkInDateInput?.addEventListener('change', updateStockLabels);
    checkOutDateInput?.addEventListener('change', updateStockLabels);
    document.addEventListener('typeList:updated', updateStockLabels);
    cartNotice?.querySelector('.category-cart-notice__backdrop')?.addEventListener('click', closeCartNotice);

    updateStockLabels();
})();
