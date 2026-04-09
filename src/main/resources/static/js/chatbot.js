(function () {
    const toggle = document.getElementById('chatbot-toggle');
    const panel = document.getElementById('chatbot-window');
    const close = document.getElementById('chatbot-close');
    const flow = document.getElementById('chatbot-flow');
    const messages = document.getElementById('chatbot-messages');
    const stage = document.getElementById('chatbot-stage');
    const quickActions = document.getElementById('chatbot-quick-actions');
    const form = document.getElementById('chatbot-form');
    const input = document.getElementById('chatbot-input');

    if (!toggle || !panel || !close || !flow || !messages || !stage || !quickActions || !form || !input) {
        return;
    }

    const steps = ['type', 'location', 'dates', 'confirm'];

    const state = {
        initialized: false,
        step: 'type',
        booking: emptyBooking()
    };

    const presets = {
        type: ['호텔', '리조트', '펜션', '모텔', '게스트하우스', '전체'],
        location: ['서울', '부산', '제주', '강릉', '경주', '직접 입력']
    };

    const typeMap = {
        '호텔': 'HOTEL',
        '리조트': 'RESORT',
        '펜션': 'PENSION',
        '모텔': 'MOTEL',
        '게스트하우스': 'GUESTHOUSE',
        '전체': ''
    };

    toggle.addEventListener('click', function () {
        const willOpen = panel.classList.contains('is-hidden');
        panel.classList.toggle('is-hidden', !willOpen);
        toggle.setAttribute('aria-expanded', String(willOpen));

        if (willOpen && !state.initialized) {
            initializeChat();
        }

        if (willOpen) {
            window.setTimeout(function () {
                input.focus();
            }, 60);
        }
    });

    close.addEventListener('click', function () {
        panel.classList.add('is-hidden');
        toggle.setAttribute('aria-expanded', 'false');
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        const value = input.value.trim();
        if (!value) {
            return;
        }
        input.value = '';
        pushMessage('user', value);
        handleInput(value);
    });

    function initializeChat() {
        state.initialized = true;
        state.step = 'type';
        state.booking = emptyBooking();
        renderFlow();
        clearMessages();
        pushMessage('bot', '숙소 유형을 선택해주세요.');
        renderTypeStage();
    }

    function handleInput(value) {
        if (state.step === 'type') {
            state.booking.type = value;
            state.step = 'location';
            renderFlow();
            pushMessage('bot', '지역을 선택해주세요.');
            renderLocationStage();
            return;
        }

        if (state.step === 'location') {
            if (value === '직접 입력') {
                clearStage();
                clearQuickActions();
                pushMessage('bot', '원하는 지역명을 입력해주세요.');
                return;
            }

            state.booking.location = value;
            state.step = 'dates';
            renderFlow();
            pushMessage('bot', '체크인과 체크아웃 날짜를 선택해주세요.');
            renderDateStage();
            return;
        }

        if (state.step === 'dates') {
            if (!state.booking.location) {
                state.booking.location = value;
                pushMessage('bot', '이제 날짜를 선택해주세요.');
                renderDateStage();
            }
            return;
        }

        if (state.step === 'confirm') {
            if (value === '검색하기') {
                redirectToSearch();
                return;
            }

            if (value === '날짜 수정') {
                state.step = 'dates';
                renderFlow();
                pushMessage('bot', '날짜를 다시 선택해주세요.');
                renderDateStage();
                return;
            }

            resetChat();
        }
    }

    function renderFlow() {
        const labels = {
            type: '1 유형',
            location: '2 지역',
            dates: '3 날짜',
            confirm: '4 확인'
        };

        flow.innerHTML = steps.map(function (stepName) {
            return '<div class="chatbot-flow__step' +
                (state.step === stepName ? ' is-active' : '') +
                '">' + labels[stepName] + '</div>';
        }).join('');
    }

    function renderTypeStage() {
        clearQuickActions();
        clearStage();
        stage.appendChild(buildCardGrid(presets.type, '빠른 선택', handleSelection));
    }

    function renderLocationStage() {
        clearQuickActions();
        clearStage();
        stage.appendChild(buildCardGrid(presets.location, '지역 선택', handleSelection));
    }

    function renderDateStage() {
        clearQuickActions();
        clearStage();

        const box = document.createElement('div');
        box.className = 'chatbot-date-box';

        const shortcuts = document.createElement('div');
        shortcuts.className = 'chatbot-date-shortcuts';

        [
            { label: '오늘 체크인', offset: 0, nights: 1 },
            { label: '내일 체크인', offset: 1, nights: 1 },
            { label: '2박 3일', offset: 0, nights: 2 }
        ].forEach(function (preset) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'chatbot-date-shortcut';
            button.textContent = preset.label;
            button.addEventListener('click', function () {
                const range = buildRange(preset.offset, preset.nights);
                checkInInput.value = range.checkInDate;
                checkOutInput.value = range.checkOutDate;
            });
            shortcuts.appendChild(button);
        });

        const fields = document.createElement('div');
        fields.className = 'chatbot-date-fields';

        const checkInField = document.createElement('label');
        checkInField.className = 'chatbot-date-field';
        checkInField.innerHTML = '<span>체크인</span>';

        const checkInInput = document.createElement('input');
        checkInInput.type = 'date';
        checkInInput.value = state.booking.checkInDate;
        checkInInput.min = formatDate(new Date());
        checkInField.appendChild(checkInInput);

        const checkOutField = document.createElement('label');
        checkOutField.className = 'chatbot-date-field';
        checkOutField.innerHTML = '<span>체크아웃</span>';

        const checkOutInput = document.createElement('input');
        checkOutInput.type = 'date';
        checkOutInput.value = state.booking.checkOutDate;
        checkOutInput.min = state.booking.checkInDate || formatDate(addDays(new Date(), 1));
        checkOutField.appendChild(checkOutInput);

        checkInInput.addEventListener('change', function () {
            checkOutInput.min = checkInInput.value || formatDate(addDays(new Date(), 1));
            if (checkOutInput.value && checkOutInput.value <= checkInInput.value) {
                checkOutInput.value = '';
            }
        });

        fields.appendChild(checkInField);
        fields.appendChild(checkOutField);

        const apply = document.createElement('button');
        apply.type = 'button';
        apply.className = 'chatbot-date-apply';
        apply.textContent = '날짜 적용';
        apply.addEventListener('click', function () {
            applyDates(checkInInput.value, checkOutInput.value);
        });

        box.appendChild(shortcuts);
        box.appendChild(fields);
        box.appendChild(apply);
        stage.appendChild(box);
    }

    function renderConfirmStage() {
        clearStage();
        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = [
            summaryItem('유형', state.booking.type || '-'),
            summaryItem('지역', state.booking.location || '-'),
            summaryItem('체크인', state.booking.checkInDate || '-'),
            summaryItem('체크아웃', state.booking.checkOutDate || '-')
        ].join('');
        stage.appendChild(summary);
        setQuickActions(['검색하기', '날짜 수정', '다시 선택']);
    }

    function applyDates(checkInDate, checkOutDate) {
        if (!isDate(checkInDate) || !isDate(checkOutDate) || checkOutDate <= checkInDate) {
            pushMessage('bot', '체크아웃은 체크인 이후 날짜여야 합니다.');
            return;
        }

        state.booking.checkInDate = checkInDate;
        state.booking.checkOutDate = checkOutDate;
        state.step = 'confirm';
        renderFlow();
        pushMessage('bot', '조건을 확인하고 검색해주세요.');
        renderConfirmStage();
    }

    function buildCardGrid(items, subtitle, callback) {
        const grid = document.createElement('div');
        grid.className = 'chatbot-card-grid';

        items.forEach(function (label) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'chatbot-card-button';
            button.innerHTML = '<strong>' + label + '</strong><span>' + subtitle + '</span>';
            button.addEventListener('click', function () {
                callback(label);
            });
            grid.appendChild(button);
        });

        return grid;
    }

    function handleSelection(label) {
        pushMessage('user', label);
        handleInput(label);
    }

    function summaryItem(label, value) {
        return '<div class="chatbot-summary__item"><span>' + label + '</span><strong>' + value + '</strong></div>';
    }

    function buildRange(offset, nights) {
        const start = addDays(new Date(), offset);
        const end = addDays(start, nights);
        return {
            checkInDate: formatDate(start),
            checkOutDate: formatDate(end)
        };
    }

    function addDays(date, days) {
        const copy = new Date(date);
        copy.setDate(copy.getDate() + days);
        return copy;
    }

    function formatDate(date) {
        return [
            date.getFullYear(),
            String(date.getMonth() + 1).padStart(2, '0'),
            String(date.getDate()).padStart(2, '0')
        ].join('-');
    }

    function redirectToSearch() {
        const params = new URLSearchParams();

        if (state.booking.location) {
            params.set('searchQuery', state.booking.location);
        }
        if (state.booking.checkInDate) {
            params.set('checkInDate', state.booking.checkInDate);
        }
        if (state.booking.checkOutDate) {
            params.set('checkOutDate', state.booking.checkOutDate);
        }

        const accomType = typeMap[state.booking.type] || '';
        if (accomType) {
            params.set('accomType', accomType);
        }

        window.location.href = '/searchList?' + params.toString();
    }

    function resetChat() {
        flow.innerHTML = '';
        clearMessages();
        clearStage();
        clearQuickActions();
        state.initialized = false;
        initializeChat();
    }

    function emptyBooking() {
        return {
            type: '',
            location: '',
            checkInDate: '',
            checkOutDate: ''
        };
    }

    function pushMessage(role, text) {
        const item = document.createElement('div');
        item.className = 'chatbot-message chatbot-message--' + role;

        const bubble = document.createElement('div');
        bubble.className = 'chatbot-message__bubble';
        bubble.textContent = text;

        item.appendChild(bubble);
        messages.appendChild(item);
        messages.scrollTop = messages.scrollHeight;
    }

    function clearMessages() {
        messages.innerHTML = '';
    }

    function clearStage() {
        stage.innerHTML = '';
    }

    function setQuickActions(items) {
        clearQuickActions();
        items.forEach(function (label) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'chatbot-quick-action';
            button.textContent = label;
            button.addEventListener('click', function () {
                pushMessage('user', label);
                handleInput(label);
            });
            quickActions.appendChild(button);
        });
    }

    function clearQuickActions() {
        quickActions.innerHTML = '';
    }

    function isDate(value) {
        return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value));
    }
})();
