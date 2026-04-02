const params = new URLSearchParams(location.search);

if (params.has("logout")) {
    alert("로그아웃되었습니다.");
    params.delete("logout");
    history.replaceState({}, "", params.toString() ? `${location.pathname}?${params}` : location.pathname);
}

const $ = (id) => document.getElementById(id);
const tabs = document.querySelectorAll("[data-booking-tab]");
const slider = document.querySelector("[data-accom-slider]");

if (tabs.length) {
    const ui = {
        dateTrigger: $("dateTrigger"),
        guestTrigger: $("guestTrigger"),
        dateLabel: $("dateRangeLabel"),
        guestLabel: $("guestDisplay"),
        calendar: $("calendarPopup"),
        guest: $("guestPopup"),
        summary: $("calendarSummary"),
        leftTitle: $("monthTitleLeft"),
        rightTitle: $("monthTitleRight"),
        leftDays: $("monthDaysLeft"),
        rightDays: $("monthDaysRight"),
        adultCount: $("adultCount"),
        childCount: $("childCount"),
        roomCount: $("roomCount")
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
    const closePopups = () => [ui.calendar, ui.guest].forEach((el) => el.classList.remove("open"));

    function renderDate() {
        const text = !state.start
            ? "체크인 날짜 - 체크아웃 날짜"
            : !state.end
                ? `${formatDate(state.start)} - 체크아웃 날짜`
                : `${formatDate(state.start)} - ${formatDate(state.end)} (${Math.round((state.end - state.start) / 86400000)}박)`;

        ui.dateLabel.textContent = text;
        ui.summary.textContent = text;
        ui.dateTrigger.classList.toggle("is-placeholder", !state.start);
    }

    function renderGuest() {
        ui.adultCount.textContent = state.adult;
        ui.childCount.textContent = state.child;
        ui.roomCount.textContent = state.room;
        ui.guestLabel.textContent = `총 인원 ${state.adult + state.child}, 객실 ${state.room}`;
        ui.guestTrigger.classList.toggle("is-placeholder", state.adult + state.child === 0 && state.room === 1);
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
            if (!button.disabled) button.addEventListener("click", () => pickDate(date));

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

        if (state.end) ui.calendar.classList.remove("open");
        renderDate();
        renderCalendar();
    }

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            tabs.forEach((el) => el.classList.toggle("active", el === tab));
        });
    });

    [[ui.dateTrigger, ui.calendar], [ui.guestTrigger, ui.guest]].forEach(([trigger, popup]) => {
        trigger?.addEventListener("click", () => {
            const opened = popup.classList.contains("open");
            closePopups();
            if (!opened) popup.classList.add("open");
        });
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

    document.addEventListener("click", (e) => {
        if (!e.target.closest(".booking-shell")) closePopups();
    });

    state.start = addDays(baseToday, 4);
    state.end = addDays(baseToday, 5);
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
