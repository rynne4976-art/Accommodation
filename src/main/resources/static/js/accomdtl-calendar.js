(function () {
    'use strict';

    const calendarRoot = document.getElementById('operationCalendar');
    const calendarConfig = window.accomDtlCalendarConfig || {};

    if (!calendarRoot || !calendarConfig.startDate || !calendarConfig.endDate) {
        return;
    }

    const startDate = new Date(calendarConfig.startDate + 'T00:00:00');
    const endDate = new Date(calendarConfig.endDate + 'T00:00:00');
    const operationDates = new Set(calendarConfig.operationDates || []);
    const dayLabels = ['일', '월', '화', '수', '목', '금', '토'];
    const months = [];

    function toDateKey(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }

    function normalizeDate(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    let currentMonth = new Date(startDate.getFullYear(), startDate.getMonth(), 1);
    const lastMonth = new Date(endDate.getFullYear(), endDate.getMonth(), 1);

    while (currentMonth <= lastMonth) {
        months.push(new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1));
        currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1);
    }

    months.forEach(function (monthDate) {
        const calendarCard = document.createElement('section');
        calendarCard.className = 'operation-calendar-card';

        const monthTitle = document.createElement('h4');
        monthTitle.className = 'operation-calendar-month';
        monthTitle.textContent = monthDate.getFullYear() + '년 ' + (monthDate.getMonth() + 1) + '월';
        calendarCard.appendChild(monthTitle);

        const weekdayRow = document.createElement('div');
        weekdayRow.className = 'operation-calendar-weekdays';
        dayLabels.forEach(function (label) {
            const item = document.createElement('span');
            item.textContent = label;
            if (label === '토') {
                item.classList.add('is-saturday');
            } else if (label === '일') {
                item.classList.add('is-sunday');
            }
            weekdayRow.appendChild(item);
        });
        calendarCard.appendChild(weekdayRow);

        const grid = document.createElement('div');
        grid.className = 'operation-calendar-grid';

        const firstDay = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
        const lastDay = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);
        const leadingBlankCount = firstDay.getDay();

        for (let i = 0; i < leadingBlankCount; i += 1) {
            const emptyCell = document.createElement('span');
            emptyCell.className = 'operation-calendar-cell is-empty';
            grid.appendChild(emptyCell);
        }

        for (let day = 1; day <= lastDay.getDate(); day += 1) {
            const date = new Date(monthDate.getFullYear(), monthDate.getMonth(), day);
            const cell = document.createElement('span');
            const dateKey = toDateKey(date);
            const inRange = normalizeDate(date) >= normalizeDate(startDate)
                && normalizeDate(date) <= normalizeDate(endDate);

            cell.className = 'operation-calendar-cell';
            cell.textContent = String(day);

            if (date.getDay() === 6) {
                cell.classList.add('is-saturday');
            } else if (date.getDay() === 0) {
                cell.classList.add('is-sunday');
            }

            if (!inRange) {
                cell.classList.add('is-outside-range');
            } else if (operationDates.has(dateKey)) {
                cell.classList.add('is-active');
                cell.title = '운영일';
            } else {
                cell.classList.add('is-in-range');
            }

            grid.appendChild(cell);
        }

        calendarCard.appendChild(grid);
        calendarRoot.appendChild(calendarCard);
    });
})();
