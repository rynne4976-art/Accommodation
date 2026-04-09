(function () {
    'use strict';

    const app = window.accomFormApp;
    const config = window.accomFormConfig || {};
    const operationStartDateInput = document.getElementById('operationStartDate');
    const operationEndDateInput = document.getElementById('operationEndDate');
    const operationDateListBox = document.getElementById('operationDateListBox');
    const selectAllOperationDatesBtn = document.getElementById('selectAllOperationDatesBtn');
    const clearAllOperationDatesBtn = document.getElementById('clearAllOperationDatesBtn');

    if (!app || !operationStartDateInput || !operationEndDateInput || !operationDateListBox) {
        return;
    }

    let selectedDateSet = new Set(config.initialSelectedDates || []);
    let rangeAnchorDate = null;

    function formatDate(date) {
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const dd = String(date.getDate()).padStart(2, '0');
        return yyyy + '-' + mm + '-' + dd;
    }

    function getTodayString() {
        const now = new Date();
        return formatDate(new Date(now.getFullYear(), now.getMonth(), now.getDate()));
    }

    function filterSelectedDatesByRange(startValue, endValue) {
        if (!startValue || !endValue) {
            selectedDateSet.clear();
            rangeAnchorDate = null;
            return;
        }

        selectedDateSet = new Set(Array.from(selectedDateSet).filter(function (dateValue) {
            return dateValue >= startValue && dateValue <= endValue;
        }));
    }

    function isHoliday(date) {
        const monthDay = String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
        const solarHolidayMap = new Set([
            '01-01',
            '03-01',
            '05-05',
            '06-06',
            '08-15',
            '10-03',
            '10-09',
            '12-25'
        ]);

        return solarHolidayMap.has(monthDay);
    }

    function selectDateRange(startValue, endValue) {
        const from = startValue <= endValue ? startValue : endValue;
        const to = startValue <= endValue ? endValue : startValue;
        let cursor = new Date(from + 'T00:00:00');
        const last = new Date(to + 'T00:00:00');

        while (cursor <= last) {
            selectedDateSet.add(formatDate(cursor));
            cursor.setDate(cursor.getDate() + 1);
        }
    }

    function isOperationDateListValid() {
        return app.getCheckedOperationDates().length > 0;
    }

    function updateOperationDateState() {
        operationDateListBox.classList.toggle('is-invalid', !isOperationDateListValid());
    }

    function renderOperationDates() {
        const startValue = operationStartDateInput.value;
        const endValue = operationEndDateInput.value;

        operationDateListBox.innerHTML = '';

        if (!startValue || !endValue) {
            operationDateListBox.innerHTML = '<p class="operation-date-placeholder">운영 시작일과 종료일을 먼저 입력해 주세요.</p>';
            updateOperationDateState();
            return;
        }

        const startDate = new Date(startValue);
        const endDate = new Date(endValue);

        if (startDate > endDate) {
            operationDateListBox.innerHTML = '<p class="operation-date-placeholder is-error">운영 종료일은 운영 시작일보다 빠를 수 없습니다.</p>';
            updateOperationDateState();
            return;
        }

        filterSelectedDatesByRange(startValue, endValue);

        const savedSelectedDates = new Set(selectedDateSet);
        const dayLabels = ['일', '월', '화', '수', '목', '금', '토'];
        const monthDates = [];
        let currentMonth = new Date(startDate.getFullYear(), startDate.getMonth(), 1);
        const lastMonth = new Date(endDate.getFullYear(), endDate.getMonth(), 1);

        while (currentMonth <= lastMonth) {
            monthDates.push(new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1));
            currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1);
        }

        monthDates.forEach(function (monthDate) {
            const monthCard = document.createElement('section');
            monthCard.className = 'operation-calendar-card';

            const monthTitle = document.createElement('h4');
            monthTitle.className = 'operation-calendar-month';
            monthTitle.textContent = monthDate.getFullYear() + '년 ' + (monthDate.getMonth() + 1) + '월';
            monthCard.appendChild(monthTitle);

            const weekdayRow = document.createElement('div');
            weekdayRow.className = 'operation-calendar-weekdays';
            dayLabels.forEach(function (label) {
                const item = document.createElement('span');
                item.textContent = label;
                weekdayRow.appendChild(item);
            });
            monthCard.appendChild(weekdayRow);

            const grid = document.createElement('div');
            grid.className = 'operation-calendar-grid';

            const firstDay = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
            const lastDay = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);

            for (let i = 0; i < firstDay.getDay(); i += 1) {
                const emptyCell = document.createElement('span');
                emptyCell.className = 'operation-calendar-cell is-empty';
                grid.appendChild(emptyCell);
            }

            for (let day = 1; day <= lastDay.getDate(); day += 1) {
                const date = new Date(monthDate.getFullYear(), monthDate.getMonth(), day);
                const yyyy = date.getFullYear();
                const mm = String(date.getMonth() + 1).padStart(2, '0');
                const dd = String(date.getDate()).padStart(2, '0');
                const dateValue = yyyy + '-' + mm + '-' + dd;

                const label = document.createElement('label');
                label.className = 'operation-calendar-label';

                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.name = 'operationDateList';
                checkbox.value = dateValue;
                checkbox.checked = savedSelectedDates.has(dateValue);
                checkbox.className = 'operation-calendar-input';
                checkbox.disabled = dateValue < startValue || dateValue > endValue;

                label.addEventListener('click', function (event) {
                    event.preventDefault();

                    if (checkbox.disabled) {
                        return;
                    }

                    if (!rangeAnchorDate) {
                        selectedDateSet.clear();
                        rangeAnchorDate = dateValue;
                        selectedDateSet.add(dateValue);
                    } else if (dateValue < rangeAnchorDate) {
                        selectedDateSet.clear();
                        rangeAnchorDate = dateValue;
                        selectedDateSet.add(dateValue);
                    } else {
                        selectedDateSet.clear();
                        selectDateRange(rangeAnchorDate, dateValue);
                        rangeAnchorDate = null;
                    }

                    renderOperationDates();
                    updateOperationDateState();
                });

                const cell = document.createElement('span');
                cell.className = 'operation-calendar-cell';
                cell.textContent = String(day);
                cell.dataset.dayOfWeek = String(date.getDay());
                if (date.getDay() === 0) {
                    cell.classList.add('is-sunday');
                }
                if (date.getDay() === 6) {
                    cell.classList.add('is-saturday');
                }
                if (isHoliday(date)) {
                    cell.classList.add('is-holiday');
                }

                if (checkbox.disabled) {
                    label.classList.add('is-disabled');
                    checkbox.checked = false;
                }

                if (checkbox.checked) {
                    label.classList.add('is-selected');
                }

                label.appendChild(checkbox);
                label.appendChild(cell);
                grid.appendChild(label);
            }

            monthCard.appendChild(grid);
            operationDateListBox.appendChild(monthCard);
        });

        updateOperationDateState();
    }

    if (selectAllOperationDatesBtn) {
        selectAllOperationDatesBtn.addEventListener('click', function () {
            rangeAnchorDate = null;
            document.querySelectorAll('input[name="operationDateList"]').forEach(function (input) {
                if (input.disabled) {
                    return;
                }
                input.checked = true;
                selectedDateSet.add(input.value);
                input.closest('.operation-calendar-label')?.classList.add('is-selected');
            });
            updateOperationDateState();
        });
    }

    if (clearAllOperationDatesBtn) {
        clearAllOperationDatesBtn.addEventListener('click', function () {
            rangeAnchorDate = null;
            document.querySelectorAll('input[name="operationDateList"]').forEach(function (input) {
                input.checked = false;
                selectedDateSet.delete(input.value);
                input.closest('.operation-calendar-label')?.classList.remove('is-selected');
            });
            updateOperationDateState();
        });
    }

    operationStartDateInput.addEventListener('change', function () {
        const today = getTodayString();
        if (operationStartDateInput.value && operationStartDateInput.value < today) {
            operationStartDateInput.value = today;
        }
        operationEndDateInput.min = operationStartDateInput.value || today;
        if (operationEndDateInput.value && operationStartDateInput.value && operationEndDateInput.value < operationStartDateInput.value) {
            operationEndDateInput.value = operationStartDateInput.value;
        }
        rangeAnchorDate = null;
        app.setFocusedInvalid('operationStartDate', !app.validateField('operationStartDate'));
        app.setFocusedInvalid('operationEndDate', !app.validateField('operationEndDate'));
        renderOperationDates();
    });

    operationEndDateInput.addEventListener('change', function () {
        const today = getTodayString();
        operationEndDateInput.min = operationStartDateInput.value || today;
        if (operationEndDateInput.value && operationEndDateInput.value < operationEndDateInput.min) {
            operationEndDateInput.value = operationEndDateInput.min;
        }
        rangeAnchorDate = null;
        app.setFocusedInvalid('operationStartDate', !app.validateField('operationStartDate'));
        app.setFocusedInvalid('operationEndDate', !app.validateField('operationEndDate'));
        renderOperationDates();
    });

    (function initDateBounds() {
        const today = getTodayString();
        operationStartDateInput.min = today;
        operationEndDateInput.min = operationStartDateInput.value || today;

        if (operationStartDateInput.value && operationStartDateInput.value < today) {
            operationStartDateInput.value = today;
        }

        if (operationEndDateInput.value && operationEndDateInput.value < operationEndDateInput.min) {
            operationEndDateInput.value = operationEndDateInput.min;
        }
    })();

    renderOperationDates();
})();
