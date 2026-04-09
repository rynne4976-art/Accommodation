(function () {
    'use strict';

    const config = window.accomFormConfig || {};
    const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
    const today = parseIsoDate(config.today) || startOfDay(new Date());

    function $(id) {
        return document.getElementById(id);
    }

    function startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    function parseIsoDate(value) {
        if (!value) {
            return null;
        }

        const parts = value.split('-').map(Number);
        if (parts.length !== 3 || parts.some(Number.isNaN)) {
            return null;
        }

        return new Date(parts[0], parts[1] - 1, parts[2]);
    }

    function formatIsoDate(date) {
        return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
    }

    function formatDisplayDate(date) {
        return date.getMonth() + '.' + String(date.getDate()).padStart(2, '0') + ' ' + weekdays[date.getDay()];
    }

    function formatRangeText(startDate, endDate) {
        if (!startDate && !endDate) {
            return '';
        }
        if (startDate && !endDate) {
            return formatDisplayDate(startDate) + ' - 운영 종료일';
        }
        return formatDisplayDate(startDate) + ' - ' + formatDisplayDate(endDate);
    }

    function parseTime(value) {
        return /^\d{2}:\d{2}$/.test(value || '') ? value : null;
    }

    function formatDisplayTime(value) {
        const parsed = parseTime(value);
        if (!parsed) {
            return '';
        }

        const [hourText, minuteText] = parsed.split(':');
        const hour = Number(hourText);
        const meridiem = hour < 12 ? '오전' : '오후';
        const displayHour = hour % 12 === 0 ? 12 : hour % 12;
        return meridiem + ' ' + displayHour + ':' + minuteText;
    }

    function closeAllPickers(exceptId) {
        document.querySelectorAll('.picker-popup.open').forEach(function (popup) {
            if (popup.id !== exceptId) {
                popup.classList.remove('open');
            }
        });

        document.querySelectorAll('.picker-trigger.is-open').forEach(function (trigger) {
            if (trigger.getAttribute('aria-controls') !== exceptId) {
                trigger.classList.remove('is-open');
            }
        });
    }

    function setTriggerText(trigger, label, placeholder, value) {
        if (!trigger || !label) {
            return;
        }

        const hasValue = Boolean(value);
        label.textContent = hasValue ? value : placeholder;
        trigger.classList.toggle('is-placeholder', !hasValue);
    }

    function syncDateConstraints() {
        const startInput = $('operationStartDate');
        const endInput = $('operationEndDate');

        if (!startInput || !endInput) {
            return;
        }

        const startDate = parseIsoDate(startInput.value);
        const endDate = parseIsoDate(endInput.value);

        if (startDate && startDate < today) {
            startInput.value = formatIsoDate(today);
        }

        if (endDate && startInput.value && endInput.value < startInput.value) {
            endInput.value = startInput.value;
        }
    }

    function initOperationPeriodPicker() {
        const startInput = $('operationStartDate');
        const endInput = $('operationEndDate');
        const trigger = $('operationPeriodTrigger');
        const label = $('operationPeriodLabel');
        const popup = $('operationPeriodPopup');
        const summary = $('operationPeriodSummary');
        const leftTitle = $('operationPeriodMonthTitleLeft');
        const rightTitle = $('operationPeriodMonthTitleRight');
        const leftDays = $('operationPeriodMonthDaysLeft');
        const rightDays = $('operationPeriodMonthDaysRight');
        const prevBtn = $('operationPeriodPrevMonthBtn');
        const nextBtn = $('operationPeriodNextMonthBtn');
        const resetBtn = $('operationPeriodResetBtn');

        if (!startInput || !endInput || !trigger || !label || !popup) {
            return;
        }

        trigger.setAttribute('aria-controls', popup.id);

        const state = {
            month: new Date((parseIsoDate(startInput.value) || today).getFullYear(), (parseIsoDate(startInput.value) || today).getMonth(), 1),
            selectingEnd: Boolean(startInput.value && !endInput.value)
        };

        function renderText() {
            const startDate = parseIsoDate(startInput.value);
            const endDate = parseIsoDate(endInput.value);
            const display = formatRangeText(startDate, endDate);
            setTriggerText(trigger, label, '운영 시작일 - 운영 종료일', display);
            if (summary) {
                summary.textContent = display || '운영 시작일 - 운영 종료일';
            }
        }

        function isInRange(date, startDate, endDate) {
            return startDate && endDate && date > startDate && date < endDate;
        }

        function buildMonth(container, titleEl, monthDate) {
            container.innerHTML = '';
            titleEl.textContent = monthDate.getFullYear() + '년 ' + (monthDate.getMonth() + 1) + '월';

            const firstDay = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1).getDay();
            const lastDate = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0).getDate();
            const startDate = parseIsoDate(startInput.value);
            const endDate = parseIsoDate(endInput.value);

            for (let i = 0; i < firstDay; i += 1) {
                const blank = document.createElement('span');
                blank.className = 'calendar-day muted';
                container.appendChild(blank);
            }

            for (let day = 1; day <= lastDate; day += 1) {
                const date = new Date(monthDate.getFullYear(), monthDate.getMonth(), day);
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'calendar-day';
                button.textContent = String(day);

                if (date.getDay() === 0) {
                    button.classList.add('sunday');
                }
                if (date.getDay() === 6) {
                    button.classList.add('saturday');
                }
                if (date < today) {
                    button.classList.add('disabled');
                    button.disabled = true;
                }
                if (startDate && formatIsoDate(date) === formatIsoDate(startDate)) {
                    button.classList.add('selected');
                }
                if (endDate && formatIsoDate(date) === formatIsoDate(endDate)) {
                    button.classList.add('selected');
                }
                if (isInRange(date, startDate, endDate)) {
                    button.classList.add('in-range');
                }

                if (!button.disabled) {
                    button.addEventListener('click', function (event) {
                        event.stopPropagation();

                        if (!startInput.value || endInput.value || !state.selectingEnd) {
                            startInput.value = formatIsoDate(date);
                            endInput.value = '';
                            state.selectingEnd = true;
                        } else if (date < parseIsoDate(startInput.value)) {
                            startInput.value = formatIsoDate(date);
                        } else {
                            endInput.value = formatIsoDate(date);
                            state.selectingEnd = false;
                            popup.classList.remove('open');
                            trigger.classList.remove('is-open');
                        }

                        syncDateConstraints();
                        renderText();
                        renderCalendar();
                        startInput.dispatchEvent(new Event('change', { bubbles: true }));
                        endInput.dispatchEvent(new Event('change', { bubbles: true }));
                    });
                }

                container.appendChild(button);
            }
        }

        function renderCalendar() {
            buildMonth(leftDays, leftTitle, state.month);
            buildMonth(rightDays, rightTitle, new Date(state.month.getFullYear(), state.month.getMonth() + 1, 1));
        }

        trigger.addEventListener('click', function (event) {
            event.stopPropagation();
            const isOpen = popup.classList.contains('open');
            closeAllPickers(isOpen ? null : popup.id);
            popup.classList.toggle('open', !isOpen);
            trigger.classList.toggle('is-open', !isOpen);
            const anchor = parseIsoDate(startInput.value) || today;
            state.month = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
            renderText();
            renderCalendar();
        });

        popup.addEventListener('click', function (event) {
            event.stopPropagation();
        });

        prevBtn?.addEventListener('click', function () {
            state.month = new Date(state.month.getFullYear(), state.month.getMonth() - 1, 1);
            renderCalendar();
        });

        nextBtn?.addEventListener('click', function () {
            state.month = new Date(state.month.getFullYear(), state.month.getMonth() + 1, 1);
            renderCalendar();
        });

        resetBtn?.addEventListener('click', function () {
            startInput.value = '';
            endInput.value = '';
            state.selectingEnd = false;
            renderText();
            renderCalendar();
            startInput.dispatchEvent(new Event('change', { bubbles: true }));
            endInput.dispatchEvent(new Event('change', { bubbles: true }));
        });

        startInput.addEventListener('change', renderText);
        endInput.addEventListener('change', renderText);

        renderText();
        renderCalendar();
    }

    function buildTimeOptions() {
        const options = [];

        for (let hour = 0; hour < 24; hour += 1) {
            for (let minute = 0; minute < 60; minute += 10) {
                const value = String(hour).padStart(2, '0') + ':' + String(minute).padStart(2, '0');
                options.push(value);
            }
        }

        return options;
    }

    function initTimePicker(fieldId, title) {
        const input = $(fieldId);
        const trigger = $(fieldId + 'Trigger');
        const label = $(fieldId + 'Label');
        const popup = $(fieldId + 'Popup');
        const meridiemBox = $(fieldId + 'Meridiem');
        const hourList = $(fieldId + 'HourList');
        const minuteList = $(fieldId + 'MinuteList');
        const summary = $(fieldId + 'Summary');
        const resetBtn = $(fieldId + 'ResetBtn');

        if (!input || !trigger || !label || !popup || !meridiemBox || !hourList || !minuteList) {
            return;
        }

        trigger.setAttribute('aria-controls', popup.id);

        const state = {
            meridiem: 'AM',
            hour: '09',
            minute: '00'
        };

        function syncStateFromInput() {
            const value = parseTime(input.value);
            if (!value) {
                state.meridiem = 'AM';
                state.hour = '09';
                state.minute = '00';
                return;
            }

            const [hourText, minuteText] = value.split(':');
            const hour24 = Number(hourText);
            state.meridiem = hour24 < 12 ? 'AM' : 'PM';
            const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12;
            state.hour = String(hour12).padStart(2, '0');
            state.minute = minuteText;
        }

        function syncInputFromState() {
            const hour12 = Number(state.hour);
            let hour24 = hour12 % 12;
            if (state.meridiem === 'PM') {
                hour24 += 12;
            }
            input.value = String(hour24).padStart(2, '0') + ':' + state.minute;
        }

        function renderText() {
            const display = formatDisplayTime(input.value);
            setTriggerText(trigger, label, '시간을 선택해 주세요.', display);
            if (summary) {
                summary.textContent = display ? title + ' ' + display : '시간을 선택해 주세요.';
            }
        }

        function renderOptions() {
            meridiemBox.querySelectorAll('[data-meridiem]').forEach(function (button) {
                button.classList.toggle('is-selected', button.dataset.meridiem === state.meridiem);
            });

            hourList.innerHTML = '';
            minuteList.innerHTML = '';

            for (let hour = 1; hour <= 12; hour += 1) {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'time-option';
                button.textContent = String(hour).padStart(2, '0');
                if (state.hour === String(hour).padStart(2, '0')) {
                    button.classList.add('is-selected');
                }
                button.addEventListener('click', function (event) {
                    event.stopPropagation();
                    state.hour = String(hour).padStart(2, '0');
                    syncInputFromState();
                    renderText();
                    renderOptions();
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                });
                hourList.appendChild(button);
            }

            for (let minute = 0; minute < 60; minute += 10) {
                const minuteText = String(minute).padStart(2, '0');
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'time-option';
                button.textContent = minuteText;
                if (state.minute === minuteText) {
                    button.classList.add('is-selected');
                }
                button.addEventListener('click', function (event) {
                    event.stopPropagation();
                    state.minute = minuteText;
                    syncInputFromState();
                    renderText();
                    renderOptions();
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                });
                minuteList.appendChild(button);
            }

            scrollSelectedIntoView(hourList);
            scrollSelectedIntoView(minuteList);
        }

        function scrollSelectedIntoView(listEl) {
            const selected = listEl.querySelector('.time-option.is-selected');
            if (selected) {
                selected.scrollIntoView({ block: 'center' });
            }
        }

        trigger.addEventListener('click', function (event) {
            event.stopPropagation();
            const isOpen = popup.classList.contains('open');
            closeAllPickers(isOpen ? null : popup.id);
            popup.classList.toggle('open', !isOpen);
            trigger.classList.toggle('is-open', !isOpen);
            syncStateFromInput();
            renderText();
            renderOptions();
        });

        popup.addEventListener('click', function (event) {
            event.stopPropagation();
        });

        resetBtn?.addEventListener('click', function () {
            input.value = '';
            syncStateFromInput();
            renderText();
            renderOptions();
            input.dispatchEvent(new Event('change', { bubbles: true }));
        });

        meridiemBox.querySelectorAll('[data-meridiem]').forEach(function (button) {
            button.addEventListener('click', function (event) {
                event.stopPropagation();
                state.meridiem = button.dataset.meridiem;
                syncInputFromState();
                renderText();
                renderOptions();
                input.dispatchEvent(new Event('change', { bubbles: true }));
            });
        });

        input.addEventListener('change', function () {
            syncStateFromInput();
            renderText();
            renderOptions();
        });

        syncStateFromInput();
        renderText();
        renderOptions();
    }

    initOperationPeriodPicker();
    initTimePicker('checkInTime', '체크인 시간');
    initTimePicker('checkOutTime', '체크아웃 시간');

    document.addEventListener('click', function (event) {
        if (!event.target.closest('.form-group')) {
            closeAllPickers();
        }
    });
})();
