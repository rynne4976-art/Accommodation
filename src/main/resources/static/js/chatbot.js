(function () {
    const toggle = document.getElementById('chatbot-toggle');
    const panel = document.getElementById('chatbot-window');
    const backdrop = document.getElementById('chatbot-backdrop');
    const close = document.getElementById('chatbot-close');
    const flow = document.getElementById('chatbot-flow');
    const messages = document.getElementById('chatbot-messages');
    const stage = document.getElementById('chatbot-stage');
    const quickActions = document.getElementById('chatbot-quick-actions');
    const form = document.getElementById('chatbot-form');
    const input = document.getElementById('chatbot-input');

    if (!toggle || !panel || !backdrop || !close || !flow || !messages || !stage || !quickActions || !form || !input) {
        return;
    }

    const bookingPresets = {
        type: ['호텔', '리조트', '펜션', '모텔', '게스트하우스', '전체'],
        location: ['서울', '부산', '제주', '강릉', '경주', '직접 입력']
    };
    const curationLocations = ['서울', '부산', '제주', '강릉', '경주', '여수'];
    const curationPrices = [
        { title: '10만원 이하', subtitle: '가성비 중심', maxPrice: 100000 },
        { title: '20만원 이하', subtitle: '중간 가격대', maxPrice: 200000 },
        { title: '30만원 이하', subtitle: '프리미엄 포함', maxPrice: 300000 },
        { title: '가격 무관', subtitle: '조건 우선 추천', maxPrice: null }
    ];
    const typeMap = {
        '호텔': 'HOTEL',
        '리조트': 'RESORT',
        '펜션': 'PENSION',
        '모텔': 'MOTEL',
        '게스트하우스': 'GUESTHOUSE',
        '전체': ''
    };

    const state = {
        initialized: false,
        mode: '',
        step: 'category',
        booking: emptyBooking(),
        curation: emptyCuration(),
        comparison: emptyComparison(),
        activity: emptyActivity()
    };

    toggle.addEventListener('click', function () {
        const willOpen = panel.classList.contains('is-hidden');
        panel.classList.toggle('is-hidden', !willOpen);
        backdrop.classList.toggle('is-hidden', !willOpen);
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

    backdrop.addEventListener('click', function () {
        panel.classList.add('is-hidden');
        backdrop.classList.add('is-hidden');
        toggle.setAttribute('aria-expanded', 'false');
        state.initialized = false;
    });

    close.addEventListener('click', function () {
        panel.classList.add('is-hidden');
        backdrop.classList.add('is-hidden');
        toggle.setAttribute('aria-expanded', 'false');
        state.initialized = false;
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
        state.mode = '';
        state.step = 'category';
        state.booking = emptyBooking();
        state.curation = emptyCuration();
        state.comparison = emptyComparison();
        state.activity = emptyActivity();
        clearMessages();
        renderFlow();
        pushMessage('bot', '무엇을 도와드릴까요? 예약 검색과 조건 기반 맞춤 숙소 추천을 지원합니다.');
        renderCategoryStage();
    }

    function renderFlow() {
        if (!state.mode) {
            flow.innerHTML = '<div class="chatbot-flow__step is-active">기능 선택</div>';
            return;
        }

        if (state.mode === 'booking') {
            flow.innerHTML = buildFlow([
                ['type', '1 유형'],
                ['location', '2 지역'],
                ['dates', '3 날짜'],
                ['confirm', '4 확인']
            ], state.step);
            return;
        }

        if (state.mode === 'curation') {
            flow.innerHTML = buildFlow([
                ['curation-location', '1 지역'],
                ['curation-dates', '2 날짜'],
                ['curation-price', '3 가격'],
                ['curation-result', '4 추천']
            ], state.step);
        }

        if (state.mode === 'comparison') {
            flow.innerHTML = buildFlow([
                ['comparison-select', '1 숙소 선택'],
                ['comparison-result', '2 비교 결과']
            ], state.step);
        }

        if (state.mode === 'activity') {
            flow.innerHTML = buildFlow([
                ['activity-taste', '1 취향 선택'],
                ['activity-region', '2 지역 선택'],
                ['activity-result', '3 추천 결과']
            ], state.step);
        }
    }

    function buildFlow(steps, activeStep) {
        return steps.map(function (item) {
            const stepName = item[0];
            const label = item[1];
            return '<div class="chatbot-flow__step' + (stepName === activeStep ? ' is-active' : '') + '">' + label + '</div>';
        }).join('');
    }

    function renderCategoryStage() {
        clearStage();
        clearQuickActions();
        input.placeholder = '원하는 기능을 선택하거나 직접 입력하세요.';
        stage.appendChild(buildCardGrid([
            { title: '예약하기', subtitle: '날짜·지역·유형 기반 검색' },
            { title: '맞춤 추천', subtitle: '지역·날짜·가격대로 숙소 추천' },
            { title: '숙소 비교', subtitle: '찜·최근 숙소 2개 나란히 비교' },
            { title: '즐길거리 탐색', subtitle: '취향별 행사·축제·관광지 추천' }
        ], function (item) {
            pushMessage('user', item.title);
            selectMode(item.title);
        }));
    }

    function selectMode(label) {
        clearStage();
        clearQuickActions();

        if (label === '예약하기') {
            state.mode = 'booking';
            state.step = 'type';
            state.booking = emptyBooking();
            renderFlow();
            pushMessage('bot', '숙소 유형을 먼저 선택해주세요.');
            renderBookingTypeStage();
            return;
        }

        if (label === '맞춤 추천') {
            state.mode = 'curation';
            state.step = 'curation-location';
            state.curation = emptyCuration();
            renderFlow();
            pushMessage('bot', '먼저 여행 지역을 골라주세요.');
            renderCurationLocationStage();
            return;
        }

        if (label === '숙소 비교') {
            state.mode = 'comparison';
            state.step = 'comparison-select';
            state.comparison = emptyComparison();
            renderFlow();
            pushMessage('bot', '찜 목록과 최근 본 숙소에서 비교할 숙소 2개를 골라주세요.');
            fetchSelectableAccoms();
            return;
        }

        if (label === '즐길거리 탐색') {
            state.mode = 'activity';
            state.step = 'activity-taste';
            state.activity = emptyActivity();
            renderFlow();
            pushMessage('bot', '어떤 취향의 즐길거리를 찾고 계신가요?');
            renderActivityTasteStage();
        }
    }

    function handleInput(value) {
        if (!state.mode) {
            selectMode(value);
            return;
        }

        if (state.mode === 'booking') {
            handleBookingInput(value);
            return;
        }

        if (state.mode === 'comparison' || state.mode === 'activity') {
            return;
        }

        if (state.mode === 'curation') {
            handleCurationInput(value);
        }
    }

    function handleBookingInput(value) {
        if (state.step === 'type') {
            state.booking.type = value;
            state.step = 'location';
            renderFlow();
            pushMessage('bot', '지역을 선택해주세요.');
            renderBookingLocationStage();
            return;
        }

        if (state.step === 'location') {
            if (value === '직접 입력') {
                clearStage();
                clearQuickActions();
                pushMessage('bot', '희망 지역명을 직접 입력해주세요.');
                return;
            }

            state.booking.location = value;
            state.step = 'dates';
            renderFlow();
            pushMessage('bot', '체크인과 체크아웃 날짜를 선택해주세요.');
            renderDateStage(applyBookingDates, state.booking.checkInDate, state.booking.checkOutDate);
            return;
        }

        if (state.step === 'dates' && !state.booking.location) {
            state.booking.location = value;
            pushMessage('bot', '이제 날짜를 선택해주세요.');
            renderDateStage(applyBookingDates, state.booking.checkInDate, state.booking.checkOutDate);
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
                renderDateStage(applyBookingDates, state.booking.checkInDate, state.booking.checkOutDate);
                return;
            }
            resetChat();
        }
    }

    function handleCurationInput(value) {
        if (state.step === 'curation-location') {
            state.curation.location = value;
            state.step = 'curation-dates';
            renderFlow();
            pushMessage('bot', '이제 여행 날짜를 선택해주세요.');
            renderDateStage(applyCurationDates, state.curation.checkInDate, state.curation.checkOutDate);
            return;
        }

        if (state.step === 'curation-result') {
            requestCurationRecommendations();
        }
    }

    function renderBookingTypeStage() {
        input.placeholder = '숙소 유형을 입력하거나 선택하세요.';
        clearQuickActions();
        clearStage();
        stage.appendChild(buildCardGrid(bookingPresets.type.map(function (title) {
            return { title: title, subtitle: '유형 선택' };
        }), function (item) {
            pushMessage('user', item.title);
            handleBookingInput(item.title);
        }));
    }

    function renderBookingLocationStage() {
        input.placeholder = '지역을 입력하거나 선택하세요.';
        clearQuickActions();
        clearStage();
        stage.appendChild(buildCardGrid(bookingPresets.location.map(function (title) {
            return { title: title, subtitle: '지역 선택' };
        }), function (item) {
            pushMessage('user', item.title);
            handleBookingInput(item.title);
        }));
    }

    function renderCurationLocationStage() {
        input.placeholder = '추천받을 지역을 입력하거나 선택하세요.';
        clearQuickActions();
        clearStage();
        stage.appendChild(buildCardGrid(curationLocations.map(function (title) {
            return { title: title, subtitle: '추천 지역' };
        }), function (item) {
            pushMessage('user', item.title);
            handleCurationInput(item.title);
        }));
    }

    function renderCurationPriceStage() {
        input.placeholder = '가격대를 선택하세요.';
        clearQuickActions();
        clearStage();
        stage.appendChild(buildCardGrid(curationPrices, function (item) {
            pushMessage('user', item.title);
            state.curation.priceLabel = item.title;
            state.curation.maxPrice = item.maxPrice;
            state.step = 'curation-result';
            renderFlow();
            requestCurationRecommendations();
        }));
    }

    function renderDateStage(applyHandler, currentCheckInDate, currentCheckOutDate) {
        clearQuickActions();
        clearStage();
        input.placeholder = '날짜를 선택해주세요.';

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
        checkInInput.value = currentCheckInDate || '';
        checkInInput.min = formatDate(new Date());
        checkInField.appendChild(checkInInput);

        const checkOutField = document.createElement('label');
        checkOutField.className = 'chatbot-date-field';
        checkOutField.innerHTML = '<span>체크아웃</span>';

        const checkOutInput = document.createElement('input');
        checkOutInput.type = 'date';
        checkOutInput.value = currentCheckOutDate || '';
        checkOutInput.min = currentCheckInDate || formatDate(addDays(new Date(), 1));
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
            applyHandler(checkInInput.value, checkOutInput.value);
        });

        box.appendChild(shortcuts);
        box.appendChild(fields);
        box.appendChild(apply);
        stage.appendChild(box);
    }

    function applyBookingDates(checkInDate, checkOutDate) {
        if (!isValidDateRange(checkInDate, checkOutDate)) {
            pushMessage('bot', '체크아웃은 체크인보다 뒤 날짜여야 합니다.');
            return;
        }

        state.booking.checkInDate = checkInDate;
        state.booking.checkOutDate = checkOutDate;
        state.step = 'confirm';
        renderFlow();
        pushMessage('bot', '조건을 확인하고 검색할 수 있습니다.');
        renderBookingConfirmStage();
    }

    function applyCurationDates(checkInDate, checkOutDate) {
        if (!isValidDateRange(checkInDate, checkOutDate)) {
            pushMessage('bot', '체크아웃은 체크인보다 뒤 날짜여야 합니다.');
            return;
        }

        state.curation.checkInDate = checkInDate;
        state.curation.checkOutDate = checkOutDate;
        state.step = 'curation-price';
        renderFlow();
        pushMessage('bot', '마지막으로 원하는 가격대를 골라주세요.');
        renderCurationPriceStage();
    }

    function renderBookingConfirmStage() {
        clearStage();
        input.placeholder = '검색하기 또는 날짜 수정';

        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = [
            summaryItem('유형', state.booking.type || '-'),
            summaryItem('지역', state.booking.location || '-'),
            summaryItem('체크인', state.booking.checkInDate || '-'),
            summaryItem('체크아웃', state.booking.checkOutDate || '-')
        ].join('');

        stage.appendChild(summary);
        setQuickActions(['검색하기', '날짜 수정', '다시 선택'], function (value) {
            pushMessage('user', value);
            handleBookingInput(value);
        });
    }

    async function requestCurationRecommendations() {
        try {
            clearStage();
            clearQuickActions();
            pushMessage('bot', '선택한 조건으로 맞춤 숙소를 찾고 있습니다.');

            const query = buildCurationQuery();
            const response = await fetch('/api/chatbot/recommendations?query=' + encodeURIComponent(query), {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (!response.ok) {
                throw new Error('recommendation request failed');
            }

            const data = await response.json();
            const filteredRecommendations = applyCurationFilters(data.recommendations || []);

            renderRecommendationResult({
                query: query,
                interpretedNeeds: [
                    '지역: ' + (state.curation.location || '-'),
                    '체크인: ' + (state.curation.checkInDate || '-'),
                    '체크아웃: ' + (state.curation.checkOutDate || '-'),
                    '가격대: ' + (state.curation.priceLabel || '가격 무관')
                ],
                recommendations: filteredRecommendations
            });
        } catch (error) {
            console.error(error);
            pushMessage('bot', '맞춤 추천 API 응답이 없어 검색 결과 페이지로 바로 안내합니다.');
            renderRecommendationFallback();
            setQuickActions(['검색 결과로 이동', '다시 시도', '다시 기능 선택'], function (value) {
                if (value === '검색 결과로 이동') {
                    redirectToCurationSearch();
                    return;
                }
                if (value === '다시 시도') {
                    requestCurationRecommendations();
                    return;
                }
                resetChat();
            });
        }
    }

    function applyCurationFilters(items) {
        const maxPrice = state.curation.maxPrice;
        if (maxPrice == null) {
            return items;
        }

        const filtered = items.filter(function (item) {
            return typeof item.pricePerNight === 'number' && item.pricePerNight <= maxPrice;
        });

        return filtered.length ? filtered : items;
    }

    function renderRecommendationResult(data) {
        const needs = Array.isArray(data.interpretedNeeds) ? data.interpretedNeeds : [];
        const recommendations = Array.isArray(data.recommendations) ? data.recommendations : [];

        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = (needs.length ? needs : ['조건을 넓게 해석해 추천했습니다.']).map(function (item) {
            return summaryItem('조건', item);
        }).join('');
        stage.appendChild(summary);

        if (!recommendations.length) {
            const empty = document.createElement('div');
            empty.className = 'chatbot-summary';
            empty.style.marginTop = '10px';
            empty.innerHTML = summaryItem('안내', '조건에 맞는 추천 숙소가 없습니다.');
            stage.appendChild(empty);
        } else {
            const grid = document.createElement('div');
            grid.className = 'chatbot-accom-grid';

            recommendations.forEach(function (item) {
                const stars = Array.from({ length: item.grade || 0 }, function () {
                    return '<span class="star filled">&#9733;</span>';
                }).join('');

                const card = document.createElement('a');
                card.href = '/accom/' + item.accomId;
                card.className = 'accom-card card';
                card.innerHTML =
                    '<div class="accom-card__visual">' +
                        (item.imgUrl
                            ? '<img class="accom-card__image" src="' + escapeHtml(item.imgUrl) + '" alt="' + escapeHtml(item.accomName) + '">'
                            : '<div class="accom-card__image-placeholder"><span>' + escapeHtml(String(item.grade || '')) + '</span></div>') +
                    '</div>' +
                    '<div class="card-body accom-info">' +
                        '<div class="accom-card__badge-row">' +
                            '<span class="accom-card__kind">' + escapeHtml(item.accomType || '숙소') + '</span>' +
                            '<span class="accom-card__grade-stars">' + stars + '</span>' +
                        '</div>' +
                        '<h3 class="accom-card__title">' + escapeHtml(item.accomName) + '</h3>' +
                        '<div class="accom-card__rating">' +
                            '<span class="accom-card__score">' + formatRating(item.avgRating) + '</span>' +
                            '<div class="accom-card__rating-copy">' +
                                '<span class="accom-card__review-link">' + (item.reviewCount || 0) + '개 후기</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="accom-card__meta">' +
                            '<div class="accom-card__location"><span>' + escapeHtml(item.location || '') + '</span></div>' +
                            '<div class="accom-card__desc"><span>' + escapeHtml((item.reasons || []).join(' / ')) + '</span></div>' +
                        '</div>' +
                        '<div class="card-bottom accom-card__bottom">' +
                            '<div class="accom-card__price-wrap">' +
                                '<span class="accom-card__price-label">1박 기준</span>' +
                                '<strong class="price">' + formatPrice(item.pricePerNight) + '</strong>' +
                            '</div>' +
                            '<span class="reserve-btn">상세보기</span>' +
                        '</div>' +
                    '</div>';

                grid.appendChild(card);
            });

            stage.appendChild(grid);
        }

        setQuickActions(['조건 다시 선택', '검색 결과로 이동', '다시 기능 선택'], function (value) {
            if (value === '조건 다시 선택') {
                state.step = 'curation-location';
                state.curation = emptyCuration();
                renderFlow();
                pushMessage('bot', '지역부터 다시 선택해주세요.');
                renderCurationLocationStage();
                return;
            }
            if (value === '검색 결과로 이동') {
                redirectToCurationSearch();
                return;
            }
            resetChat();
        });
    }

    function renderRecommendationFallback() {
        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = [
            summaryItem('지역', state.curation.location || '-'),
            summaryItem('체크인', state.curation.checkInDate || '-'),
            summaryItem('체크아웃', state.curation.checkOutDate || '-'),
            summaryItem('가격대', state.curation.priceLabel || '가격 무관')
        ].join('');
        stage.appendChild(summary);
    }

    function buildCardGrid(items, callback) {
        const grid = document.createElement('div');
        grid.className = 'chatbot-card-grid';

        items.forEach(function (item) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'chatbot-card-button';
            button.innerHTML = '<strong>' + escapeHtml(item.title) + '</strong><span>' + escapeHtml(item.subtitle) + '</span>';
            button.addEventListener('click', function () {
                callback(item);
            });
            grid.appendChild(button);
        });

        return grid;
    }

    function buildCurationQuery() {
        const parts = [];
        if (state.curation.location) {
            parts.push(state.curation.location);
        }
        if (state.curation.priceLabel && state.curation.priceLabel !== '가격 무관') {
            parts.push(state.curation.priceLabel);
        }
        return parts.join(' ');
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

    function redirectToCurationSearch() {
        const params = new URLSearchParams();
        if (state.curation.location) {
            params.set('searchQuery', state.curation.location);
        }
        if (state.curation.checkInDate) {
            params.set('checkInDate', state.curation.checkInDate);
        }
        if (state.curation.checkOutDate) {
            params.set('checkOutDate', state.curation.checkOutDate);
        }
        window.location.href = '/searchList?' + params.toString();
    }

    function summaryItem(label, value) {
        return '<div class="chatbot-summary__item"><span>' + escapeHtml(label) + '</span><strong>' + escapeHtml(value) + '</strong></div>';
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

    function emptyCuration() {
        return {
            location: '',
            checkInDate: '',
            checkOutDate: '',
            priceLabel: '',
            maxPrice: null
        };
    }

    function emptyComparison() {
        return { selected: [] };
    }

    function emptyActivity() {
        return { keyword: '', keywordLabel: '', region: '' };
    }

    // ── 숙소 비교 ──────────────────────────────────────

    async function fetchSelectableAccoms() {
        // ... (기존 로딩 로직)
        try {
            const res = await fetch('/api/chatbot/selectable-accoms', {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (!res.ok) throw new Error('failed');

            const data = await res.json();
            clearStage();

            // 수정된 체크 로직: 데이터가 없거나, 배열이 비어있을 경우
            if (!data || !Array.isArray(data) || data.length === 0) {
                pushMessage('bot', '현재 비교 가능한 숙소(찜 목록 또는 최근 본 숙소)가 없습니다. 먼저 숙소를 둘러보시겠어요?');
                setQuickActions(['처음으로', '숙소 검색하러 가기'], function (v) {
                    if (v === '처음으로') {
                        resetChat();
                    } else {
                        window.location.href = '/searchList'; // 검색 페이지로 유도
                    }
                });
                return;
        }

        renderSelectableGrid(data);

        } catch (e) {
            clearStage();
            pushMessage('bot', '목록을 불러오지 못했습니다. 다시 시도해주세요.');
            setQuickActions(['다시 시도', '처음으로'], function (v) {
                if (v === '다시 시도') { fetchSelectableAccoms(); } else { resetChat(); }
            });
        }
    }

    function renderSelectableGrid(accoms) {
        const bar = document.createElement('div');
        bar.className = 'chatbot-compare-bar';

        const counter = document.createElement('span');
        counter.className = 'chatbot-compare-counter';
        counter.textContent = '0 / 2 선택';

        const compareBtn = document.createElement('button');
        compareBtn.type = 'button';
        compareBtn.className = 'chatbot-compare-btn';
        compareBtn.textContent = '비교하기';
        compareBtn.disabled = true;

        bar.appendChild(counter);
        bar.appendChild(compareBtn);
        stage.appendChild(bar);

        const grid = document.createElement('div');
        grid.className = 'chatbot-selectable-grid';

        accoms.forEach(function (accom) {
            const card = document.createElement('div');
            card.className = 'chatbot-selectable-card';
            card.dataset.accomId = String(accom.accomId);

            const sourceLabel = accom.source === 'wish' ? '❤️ 찜' : '🕐 최근';
            const sourceClass = accom.source === 'wish'
                ? 'chatbot-selectable-card__source chatbot-selectable-card__source--wish'
                : 'chatbot-selectable-card__source chatbot-selectable-card__source--recent';

            const stars = Array.from({ length: accom.grade || 0 }, function () { return '★'; }).join('');

            card.innerHTML =
                (accom.imgUrl
                    ? '<img class="chatbot-selectable-card__img" src="' + escapeHtml(accom.imgUrl) + '" alt="' + escapeHtml(accom.accomName) + '">'
                    : '<div class="chatbot-selectable-card__placeholder">🏨</div>') +
                '<span class="' + sourceClass + '">' + sourceLabel + '</span>' +
                '<div class="chatbot-selectable-card__check">✓</div>' +
                '<div class="chatbot-selectable-card__body">' +
                    '<p class="chatbot-selectable-card__name">' + escapeHtml(accom.accomName) + '</p>' +
                    '<span class="chatbot-selectable-card__meta">' +
                        escapeHtml(accom.location || '') + (stars ? ' · ' + stars : '') +
                        ' · ★' + formatRating(accom.avgRating) +
                    '</span>' +
                    '<p class="chatbot-selectable-card__price">' + formatPrice(accom.pricePerNight) + ' / 박</p>' +
                '</div>';

            card.addEventListener('click', function () {
                const idx = state.comparison.selected.findIndex(function (s) {
                    return s.accomId === accom.accomId;
                });

                if (idx >= 0) {
                    state.comparison.selected.splice(idx, 1);
                    card.classList.remove('is-selected');
                } else {
                    if (state.comparison.selected.length >= 2) return;
                    state.comparison.selected.push(accom);
                    card.classList.add('is-selected');
                }

                const count = state.comparison.selected.length;
                counter.textContent = count + ' / 2 선택';
                compareBtn.disabled = count < 2;

                grid.querySelectorAll('.chatbot-selectable-card').forEach(function (c) {
                    if (count >= 2 && !c.classList.contains('is-selected')) {
                        c.classList.add('is-dimmed');
                    } else {
                        c.classList.remove('is-dimmed');
                    }
                });
            });

            grid.appendChild(card);
        });

        stage.appendChild(grid);

        compareBtn.addEventListener('click', function () {
            if (state.comparison.selected.length < 2) return;
            requestCompare(state.comparison.selected[0], state.comparison.selected[1]);
        });
    }

    async function requestCompare(left, right) {
        clearStage();
        clearQuickActions();
        pushMessage('bot', left.accomName + ' vs ' + right.accomName + ' 비교를 시작합니다.');

        try {
            const res = await fetch(
                '/api/chatbot/compare?leftId=' + left.accomId + '&rightId=' + right.accomId,
                { headers: { 'X-Requested-With': 'XMLHttpRequest' } }
            );
            if (!res.ok) throw new Error('failed');
            const data = await res.json();

            state.step = 'comparison-result';
            renderFlow();
            renderComparisonResult(data, left, right);
        } catch (e) {
            pushMessage('bot', '비교 정보를 불러오지 못했습니다.');
            setQuickActions(['다시 선택', '처음으로'], function (v) {
                if (v === '다시 선택') {
                    state.comparison = emptyComparison();
                    state.step = 'comparison-select';
                    renderFlow();
                    fetchSelectableAccoms();
                } else {
                    resetChat();
                }
            });
        }
    }

    function renderComparisonResult(data, leftAccom, rightAccom) {
        const L = data.left;
        const R = data.right;
        if (!L || !R) {
            pushMessage('bot', '비교할 숙소 정보를 찾을 수 없습니다.');
            return;
        }

        // VS 헤더
        const vsHeader = document.createElement('div');
        vsHeader.className = 'chatbot-vs-header';

        function sideEl(accom, item) {
            const el = document.createElement('div');
            el.className = 'chatbot-vs-side';
            const imgHtml = accom.imgUrl
                ? '<img src="' + escapeHtml(accom.imgUrl) + '" alt="' + escapeHtml(item.accomName) + '">'
                : '<div class="chatbot-vs-side__placeholder">🏨</div>';
            el.innerHTML = imgHtml + '<span class="chatbot-vs-side__name">' + escapeHtml(item.accomName) + '</span>';
            return el;
        }

        const vsBadge = document.createElement('div');
        vsBadge.className = 'chatbot-vs-badge';
        vsBadge.textContent = 'VS';

        vsHeader.appendChild(sideEl(leftAccom, L));
        vsHeader.appendChild(vsBadge);
        vsHeader.appendChild(sideEl(rightAccom, R));
        stage.appendChild(vsHeader);

        // 메트릭 비교
        const metricsEl = document.createElement('div');
        metricsEl.className = 'chatbot-metrics';

        function metricRow(label, lVal, rVal, lDisplay, rDisplay, compareType) {
            // compareType: 'low' (낮을수록 좋음: 가격), 'high' (높을수록 좋음: 평점, 등급 등)
            let lWins = false;
            let rWins = false;

            if (lVal === rVal) {
                lWins = true;
                rWins = true;
            } else if (compareType === 'low') {
                lWins = lVal < rVal;
                rWins = rVal < lVal;
            } else {
                lWins = lVal > rVal;
                rWins = rVal > lVal;
            }

            const lPct = lVal > 0 || rVal > 0 ? Math.round(lVal / (lVal + rVal) * 100) : 50;
            const rPct = 100 - lPct;

            const row = document.createElement('div');
            row.className = 'chatbot-metric-row';
            row.innerHTML =
                '<div class="chatbot-metric-row__left">' +
                    '<span class="chatbot-metric-value' + (lWins ? ' is-winner' : '') + '">' + escapeHtml(lDisplay) + '</span>' +
                    '<div class="chatbot-metric-bar-wrap"><div class="chatbot-metric-bar' + (lWins ? ' is-winner' : '') + '" style="width:' + lPct + '%"></div></div>' +
                '</div>' +
                '<div class="chatbot-metric-row__label">' + escapeHtml(label) + '</div>' +
                '<div class="chatbot-metric-row__right">' +
                    '<span class="chatbot-metric-value' + (rWins ? ' is-winner' : '') + '">' + escapeHtml(rDisplay) + '</span>' +
                    '<div class="chatbot-metric-bar-wrap"><div class="chatbot-metric-bar' + (rWins ? ' is-winner' : '') + '" style="width:' + rPct + '%"></div></div>' +
                '</div>';
            return row;
        }

        // 호출 부분 수정
        const lPrice = L.pricePerNight || 0;
        const rPrice = R.pricePerNight || 0;
        metricsEl.appendChild(metricRow('가격', lPrice, rPrice, formatPrice(L.pricePerNight), formatPrice(R.pricePerNight), 'low'));

        const lRating = (L.avgRating || 0) * 10;
        const rRating = (R.avgRating || 0) * 10;
        metricsEl.appendChild(metricRow('평점', lRating, rRating, '★ ' + formatRating(L.avgRating), '★ ' + formatRating(R.avgRating), 'high'));

        const lGrade = L.grade || 0;
        const rGrade = R.grade || 0;
        metricsEl.appendChild(metricRow('등급', lGrade, rGrade,
            Array.from({ length: lGrade }, function () { return '★'; }).join('') || '-',
            Array.from({ length: rGrade }, function () { return '★'; }).join('') || '-', 'high'));

        const lGuest = L.guestCount || 0;
        const rGuest = R.guestCount || 0;
        metricsEl.appendChild(metricRow('수용인원', lGuest, rGuest, lGuest + '명', rGuest + '명', 'high'));

        // 체크인/아웃 단순 표시
        const infoRow = document.createElement('div');
        infoRow.className = 'chatbot-metric-row';
        infoRow.innerHTML =
            '<div class="chatbot-metric-row__left"><span class="chatbot-metric-value">' + escapeHtml(L.checkInTime || '-') + '</span></div>' +
            '<div class="chatbot-metric-row__label">체크인</div>' +
            '<div class="chatbot-metric-row__right"><span class="chatbot-metric-value">' + escapeHtml(R.checkInTime || '-') + '</span></div>';
        metricsEl.appendChild(infoRow);

        stage.appendChild(metricsEl);

        // 이런 여행자에게 추천
        const tips = buildTips(L, R);
        const tipsEl = document.createElement('div');
        tipsEl.className = 'chatbot-recommendation-tips';
        tipsEl.innerHTML = '<p class="chatbot-recommendation-tips__title">이런 여행자에게 추천해요</p>' +
            tips.map(function (t) {
                return '<div class="chatbot-recommendation-tip"><span class="chatbot-recommendation-tip__icon">' + t.icon + '</span><span>' + escapeHtml(t.text) + '</span></div>';
            }).join('');
        stage.appendChild(tipsEl);

        // CTA
        const actions = document.createElement('div');
        actions.className = 'chatbot-vs-actions';
        [L, R].forEach(function (item) {
            const a = document.createElement('a');
            a.href = '/accom/' + item.accomId;
            a.className = 'chatbot-vs-action-btn';
            a.textContent = item.accomName + ' 보기';
            actions.appendChild(a);
        });
        stage.appendChild(actions);

        setQuickActions(['다시 비교', '처음으로'], function (v) {
            if (v === '다시 비교') {
                state.comparison = emptyComparison();
                state.step = 'comparison-select';
                renderFlow();
                pushMessage('bot', '비교할 숙소 2개를 다시 선택해주세요.');
                fetchSelectableAccoms();
            } else {
                resetChat();
            }
        });
    }

    // ── 즐길거리 탐색 ─────────────────────────────────────────────────────

    var TASTE_OPTIONS = [
        { keyword: '자연/힐링',    label: '자연/힐링',    icon: '🌿', desc: '공원·바다·산·힐링' },
        { keyword: '문화/역사',    label: '문화/역사',    icon: '🏛️', desc: '박물관·유적·미술관' },
        { keyword: '액티브/레포츠', label: '액티브/레포츠', icon: '🏄', desc: '서핑·클라이밍·체험' },
        { keyword: '축제/행사',    label: '축제/행사',    icon: '🎉', desc: '축제·이벤트·공연' },
        { keyword: '야경/감성',    label: '야경/감성',    icon: '🌃', desc: '야경·뷰포인트·감성' },
        { keyword: '음식/체험',    label: '음식/체험',    icon: '🍜', desc: '맛집·시장·공방' }
    ];

    var ACTIVITY_REGIONS = ['서울', '부산', '제주', '강릉', '경주'];

    function renderActivityTasteStage() {
        clearStage();
        clearQuickActions();
        input.placeholder = '취향을 선택해주세요.';

        const grid = document.createElement('div');
        grid.className = 'chatbot-taste-grid';

        TASTE_OPTIONS.forEach(function (taste) {
            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'chatbot-taste-card';
            card.innerHTML =
                '<span class="chatbot-taste-card__icon">' + taste.icon + '</span>' +
                '<span class="chatbot-taste-card__label">' + escapeHtml(taste.label) + '</span>' +
                '<span class="chatbot-taste-card__desc">' + escapeHtml(taste.desc) + '</span>';
            card.addEventListener('click', function () {
                state.activity.keyword = taste.keyword;
                state.activity.keywordLabel = taste.label;
                pushMessage('user', taste.icon + ' ' + taste.label);
                state.step = 'activity-region';
                renderFlow();
                pushMessage('bot', '어느 지역에서 즐기실 건가요?');
                renderActivityRegionStage();
            });
            grid.appendChild(card);
        });

        stage.appendChild(grid);
    }

    function renderActivityRegionStage() {
        clearStage();
        clearQuickActions();
        input.placeholder = '지역을 선택해주세요.';

        stage.appendChild(buildCardGrid(
            ACTIVITY_REGIONS.map(function (r) { return { title: r, subtitle: r + ' 지역 탐색' }; }),
            function (item) {
                state.activity.region = item.title;
                pushMessage('user', item.title);
                state.step = 'activity-result';
                renderFlow();
                pushMessage('bot', item.title + '의 ' + state.activity.keywordLabel + ' 즐길거리를 찾고 있습니다...');
                fetchActivityResults();
            }
        ));
    }

    async function fetchActivityResults() {
        clearStage();
        clearQuickActions();

        try {
            var url = '/api/chatbot/activities'
                + '?keyword=' + encodeURIComponent(state.activity.keyword)
                + '&region=' + encodeURIComponent(state.activity.region);

            var res = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
            if (!res.ok) throw new Error('failed');
            var data = await res.json();

            if (!data.length) {
                pushMessage('bot', '조건에 맞는 즐길거리를 찾지 못했습니다. 다른 조건을 선택해보세요.');
                setQuickActions(['취향 다시 선택', '처음으로'], function (v) {
                    if (v === '취향 다시 선택') {
                        state.activity = emptyActivity();
                        state.step = 'activity-taste';
                        renderFlow();
                        pushMessage('bot', '어떤 취향의 즐길거리를 찾고 계신가요?');
                        renderActivityTasteStage();
                    } else {
                        resetChat();
                    }
                });
                return;
            }

            renderActivityResults(data);
        } catch (e) {
            pushMessage('bot', '즐길거리 정보를 불러오지 못했습니다. 다시 시도해주세요.');
            setQuickActions(['다시 시도', '처음으로'], function (v) {
                if (v === '다시 시도') { fetchActivityResults(); } else { resetChat(); }
            });
        }
    }

    function renderActivityResults(items) {
        var festivals = items.filter(function (i) { return i.period && i.period.trim(); });
        var others = items.filter(function (i) { return !i.period || !i.period.trim(); });

        // 진행 중인 행사 섹션
        if (festivals.length) {
            var festHeader = document.createElement('div');
            festHeader.className = 'chatbot-activity-section-header';
            festHeader.innerHTML =
                '<span class="chatbot-activity-section-header__icon">🎉</span>' +
                '<span class="chatbot-activity-section-header__title">현재 진행 중인 행사·축제</span>';
            stage.appendChild(festHeader);

            var festGrid = document.createElement('div');
            festGrid.className = 'chatbot-activity-grid';
            festivals.forEach(function (item) { festGrid.appendChild(buildActivityCard(item)); });
            stage.appendChild(festGrid);
        }

        // 추천 즐길거리 섹션
        if (others.length) {
            var tasteIcon = TASTE_OPTIONS.find(function (t) { return t.keyword === state.activity.keyword; });
            var icon = tasteIcon ? tasteIcon.icon : '✨';

            var otherHeader = document.createElement('div');
            otherHeader.className = 'chatbot-activity-section-header';
            otherHeader.style.marginTop = festivals.length ? '10px' : '0';
            otherHeader.innerHTML =
                '<span class="chatbot-activity-section-header__icon">' + icon + '</span>' +
                '<span class="chatbot-activity-section-header__title">' + escapeHtml(state.activity.keywordLabel) + ' 추천 즐길거리</span>';
            stage.appendChild(otherHeader);

            var otherGrid = document.createElement('div');
            otherGrid.className = 'chatbot-activity-grid';
            others.forEach(function (item) { otherGrid.appendChild(buildActivityCard(item)); });
            stage.appendChild(otherGrid);
        }

        setQuickActions([
            '다른 취향 탐색',
            state.activity.region + ' 숙소 보기',
            '처음으로'
        ], function (v) {
            if (v === '다른 취향 탐색') {
                state.activity = emptyActivity();
                state.step = 'activity-taste';
                renderFlow();
                pushMessage('bot', '어떤 취향의 즐길거리를 찾고 계신가요?');
                renderActivityTasteStage();
            } else if (v === state.activity.region + ' 숙소 보기') {
                window.location.href = '/searchList?searchQuery=' + encodeURIComponent(state.activity.region);
            } else {
                resetChat();
            }
        });
    }

    function buildActivityCard(item) {
        var a = document.createElement('a');
        a.href = item.linkUrl || '#';
        a.target = '_blank';
        a.rel = 'noopener noreferrer';
        a.className = 'chatbot-activity-card';

        var categoryBadgeClass = 'chatbot-activity-card__badge ';
        if (item.category === '행사/축제') categoryBadgeClass += 'chatbot-activity-card__badge--festival';
        else if (item.category === '문화시설') categoryBadgeClass += 'chatbot-activity-card__badge--culture';
        else if (item.category === '레포츠') categoryBadgeClass += 'chatbot-activity-card__badge--sports';
        else categoryBadgeClass += 'chatbot-activity-card__badge--spot';

        var imgHtml = item.imageUrl
            ? '<img class="chatbot-activity-card__img" src="' + escapeHtml(item.imageUrl) + '" alt="' + escapeHtml(item.title) + '" loading="lazy">'
            : '<div class="chatbot-activity-card__placeholder">🗺️</div>';

        var badgesHtml = '<div class="chatbot-activity-card__badges">'
            + '<span class="' + categoryBadgeClass + '">' + escapeHtml(item.category || '즐길거리') + '</span>'
            + (item.ongoing ? '<span class="chatbot-activity-card__badge chatbot-activity-card__badge--ongoing">● 진행중</span>' : '')
            + '</div>';

        var periodHtml = item.period
            ? '<span class="chatbot-activity-card__period">📅 ' + escapeHtml(item.period) + '</span>'
            : '';

        a.innerHTML = imgHtml + badgesHtml
            + '<div class="chatbot-activity-card__body">'
            + '<p class="chatbot-activity-card__title">' + escapeHtml(item.title) + '</p>'
            + periodHtml
            + '<p class="chatbot-activity-card__address">📍 ' + escapeHtml(item.address || '') + '</p>'
            + '<span class="chatbot-activity-card__link">자세히 보기 →</span>'
            + '</div>';

        return a;
    }

    function buildTips(L, R) {
        const tips = [];
        const lPrice = L.pricePerNight || 0;
        const rPrice = R.pricePerNight || 0;
        const lRating = L.avgRating || 0;
        const rRating = R.avgRating || 0;

        if (lPrice !== rPrice) {
            const cheaper = lPrice < rPrice ? L : R;
            tips.push({ icon: '💰', text: '가성비를 원한다면 → ' + cheaper.accomName + ' (' + formatPrice(cheaper.pricePerNight) + '/박)' });
        }
        if (Math.abs(lRating - rRating) >= 0.1) {
            const better = lRating >= rRating ? L : R;
            tips.push({ icon: '⭐', text: '만족도 우선이라면 → ' + better.accomName + ' (평점 ' + formatRating(better.avgRating) + ')' });
        }
        const lGuest = L.guestCount || 0;
        const rGuest = R.guestCount || 0;
        if (lGuest !== rGuest) {
            const bigger = lGuest >= rGuest ? L : R;
            tips.push({ icon: '👥', text: '인원이 많다면 → ' + bigger.accomName + ' (' + bigger.guestCount + '명 수용)' });
        }
        if (!tips.length) {
            tips.push({ icon: '✨', text: '두 숙소 모두 조건이 비슷합니다. 취향에 따라 선택하세요.' });
        }
        return tips;
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

    function setQuickActions(items, handler) {
        clearQuickActions();

        items.forEach(function (label) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'chatbot-quick-action';
            button.textContent = label;
            button.addEventListener('click', function () {
                pushMessage('user', label);
                handler(label);
            });
            quickActions.appendChild(button);
        });
    }

    function clearQuickActions() {
        quickActions.innerHTML = '';
    }

    function isValidDateRange(checkInDate, checkOutDate) {
        return isDate(checkInDate) && isDate(checkOutDate) && checkOutDate > checkInDate;
    }

    function isDate(value) {
        return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value));
    }

    function formatPrice(value) {
        return typeof value === 'number' ? value.toLocaleString('ko-KR') + '원' : '-';
    }

    function formatRating(value) {
        return typeof value === 'number' ? value.toFixed(1) : '0.0';
    }

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();
