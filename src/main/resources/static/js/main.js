const params = new URLSearchParams(location.search);

if (params.has("logout")) {
    alert("로그아웃되었습니다.");
    params.delete("logout");
    history.replaceState({}, "", params.toString() ? `${location.pathname}?${params}` : location.pathname);
}

const $ = (id) => document.getElementById(id);
const slider = document.querySelector("[data-accom-slider]");
const syncInputValue = (input, nextValue) => {
    if (!input) {
        return;
    }

    const normalizedValue = nextValue ?? "";
    if (input.value === normalizedValue) {
        return;
    }

    input.value = normalizedValue;
    input.dispatchEvent(new Event("change", { bubbles: true }));
};

if ($("dateTrigger")) {
    const ui = {
        dateTrigger: $("dateTrigger"),
        guestTrigger: $("guestTrigger"),
        dateLabel: $("dateRangeLabel"),
        checkInInput: $("checkInDateInput"),
        checkOutInput: $("checkOutDateInput"),
        guestLabel: $("guestDisplay"),
        calendar: $("calendarPopup"),
        guest: $("guestPopup"),
        summary: $("calendarSummary"),
        calendarConfirm: $("calendarConfirmBtn"),
        guestConfirm: $("guestConfirmBtn"),
        leftTitle: $("monthTitleLeft"),
        rightTitle: $("monthTitleRight"),
        leftDays: $("monthDaysLeft"),
        rightDays: $("monthDaysRight"),
        adultCount: $("adultCount"),
        childCount: $("childCount"),
        roomCount: $("roomCount"),
        adultInput: $("adultCountInput"),
        childInput: $("childCountInput"),
        roomInput: $("roomCountInput")
    };

    const weekdays = ["일", "월", "화", "수", "목", "금", "토"];
    const today = new Date();
    const baseToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const state = {
        adult: 0,
        child: 0,
        room: 1,
        start: null,
        end: null,
        month: new Date(baseToday.getFullYear(), baseToday.getMonth(), 1)
    };

    const addDays = (date, days) => new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
    const sameDate = (a, b) => a && b && a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
    const formatDate = (date) => `${date.getMonth() + 1}.${String(date.getDate()).padStart(2, "0")} ${weekdays[date.getDay()]}`;
    const formatIsoDate = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    const parseIsoDate = (value) => {
        if (!value) {
            return null;
        }

        const [year, month, day] = value.split("-").map(Number);
        if (!year || !month || !day) {
            return null;
        }

        return new Date(year, month - 1, day);
    };
    const parseCount = (value, fallback) => {
        const parsed = Number.parseInt(value, 10);
        return Number.isNaN(parsed) ? fallback : parsed;
    };
    const closePopups = () => [ui.calendar, ui.guest].forEach((el) => el.classList.remove("open"));
    const emptyDateText = "날짜를 선택해주세요!";
    const selectingDateText = "체크인 날짜 - 체크아웃 날짜";

    function renderDate() {
        const hasCompleteRange = Boolean(state.start && state.end);
        const isCalendarOpen = ui.calendar.classList.contains("open");
        const triggerText = hasCompleteRange
            ? `${formatDate(state.start)} - ${formatDate(state.end)} (${Math.round((state.end - state.start) / 86400000)}박)`
            : isCalendarOpen
                ? selectingDateText
                : emptyDateText;
        const summaryText = !state.start
            ? selectingDateText
            : !state.end
                ? `${formatDate(state.start)} - 체크아웃 날짜`
                : triggerText;

        ui.dateLabel.textContent = triggerText;
        ui.summary.textContent = summaryText;
        ui.dateTrigger.classList.toggle("is-placeholder", !hasCompleteRange);
        syncInputValue(ui.checkInInput, state.start ? formatIsoDate(state.start) : "");
        syncInputValue(ui.checkOutInput, state.end ? formatIsoDate(state.end) : "");
    }

    function renderGuest() {
        ui.adultCount.textContent = state.adult;
        ui.childCount.textContent = state.child;
        ui.roomCount.textContent = state.room;
        ui.guestLabel.textContent = `총 인원 ${state.adult + state.child}, 객실 ${state.room}`;
        ui.guestTrigger.classList.toggle("is-placeholder", state.adult + state.child === 0 && state.room === 1);
        syncInputValue(ui.adultInput, String(state.adult));
        syncInputValue(ui.childInput, String(state.child));
        syncInputValue(ui.roomInput, String(state.room));
    }

    function buildMonth(container, title, month) {
        container.innerHTML = "";
        title.textContent = `${month.getFullYear()}년 ${month.getMonth() + 1}월`;

        const firstDay = new Date(month.getFullYear(), month.getMonth(), 1).getDay();
        const lastDate = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();

        for (let i = 0; i < firstDay; i += 1) {
            const blank = document.createElement("span");
            blank.className = "calendar-day muted";
            container.appendChild(blank);
        }

        for (let day = 1; day <= lastDate; day += 1) {
            const date = new Date(month.getFullYear(), month.getMonth(), day);
            const button = document.createElement("button");
            button.type = "button";
            button.className = "calendar-day";
            button.textContent = String(day);

            if (date.getDay() === 0) button.classList.add("sunday");
            if (date.getDay() === 6) button.classList.add("saturday");
            if (date.getTime() < baseToday.getTime()) {
                button.classList.add("disabled");
                button.disabled = true;
            }
            if (state.start && state.end && date > state.start && date < state.end) button.classList.add("in-range");
            if (sameDate(date, state.start) || sameDate(date, state.end)) button.classList.add("selected");
            if (!button.disabled) {
                button.addEventListener("click", (event) => {
                    event.stopPropagation();
                    pickDate(date);
                });
            }

            container.appendChild(button);
        }
    }

    function renderCalendar() {
        buildMonth(ui.leftDays, ui.leftTitle, state.month);
        buildMonth(ui.rightDays, ui.rightTitle, new Date(state.month.getFullYear(), state.month.getMonth() + 1, 1));
    }

    function pickDate(date) {
        if (!state.start || state.end) {
            state.start = date;
            state.end = null;
        } else if (date < state.start) {
            state.start = date;
        } else {
            state.end = sameDate(date, state.start) ? addDays(date, 1) : date;
        }

        ui.calendar.classList.add("open");
        renderDate();
        renderCalendar();
    }

    [[ui.dateTrigger, ui.calendar], [ui.guestTrigger, ui.guest]].forEach(([trigger, popup]) => {
        trigger?.addEventListener("click", () => {
            const opened = popup.classList.contains("open");
            closePopups();
            if (!opened) popup.classList.add("open");
            if (popup === ui.calendar) renderDate();
        });
    });

    ui.calendar?.addEventListener("click", (event) => {
        event.stopPropagation();
    });

    ui.guest?.addEventListener("click", (event) => {
        event.stopPropagation();
    });

    [["prevMonthBtn", -1], ["nextMonthBtn", 1]].forEach(([id, diff]) => {
        $(id)?.addEventListener("click", () => {
            state.month = new Date(state.month.getFullYear(), state.month.getMonth() + diff, 1);
            renderCalendar();
        });
    });

    $("calendarResetBtn")?.addEventListener("click", () => {
        state.start = null;
        state.end = null;
        state.month = new Date(baseToday.getFullYear(), baseToday.getMonth(), 1);
        renderDate();
        renderCalendar();
    });

    ui.calendarConfirm?.addEventListener("click", () => {
        if (!state.start || !state.end) {
            alert("체크인과 체크아웃 날짜를 모두 선택해 주세요.");
            return;
        }

        renderDate();
        ui.calendar.classList.remove("open");
    });

    [["adult", 0], ["child", 0], ["room", 1]].forEach(([key, min]) => {
        $(`${key}MinusBtn`)?.addEventListener("click", () => {
            state[key] = Math.max(min, state[key] - 1);
            renderGuest();
        });
        $(`${key}PlusBtn`)?.addEventListener("click", () => {
            state[key] += 1;
            renderGuest();
        });
    });

    ui.guestConfirm?.addEventListener("click", () => {
        renderGuest();
        ui.guest.classList.remove("open");
    });

    document.addEventListener("click", (e) => {
        if (!e.target.closest(".booking-shell")) {
            closePopups();
            renderDate();
        }
    });

    state.start = parseIsoDate(ui.checkInInput?.value);
    state.end = parseIsoDate(ui.checkOutInput?.value);
    state.adult = Math.max(0, parseCount(ui.adultInput?.value, 0));
    state.child = Math.max(0, parseCount(ui.childInput?.value, 0));
    state.room = Math.max(1, parseCount(ui.roomInput?.value, 1));
    if (state.start) {
        state.month = new Date(state.start.getFullYear(), state.start.getMonth(), 1);
    }

    renderDate();
    renderGuest();
    renderCalendar();
}

if (slider) {
    const amount = () => Math.max(slider.clientWidth * 0.72, 280);
    document.querySelector("[data-accom-prev]")?.addEventListener("click", () => {
        slider.scrollBy({ left: -amount(), behavior: "smooth" });
    });
    document.querySelector("[data-accom-next]")?.addEventListener("click", () => {
        slider.scrollBy({ left: amount(), behavior: "smooth" });
    });
}
