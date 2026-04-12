(function () {
    const toggle = document.getElementById('chatbot-toggle');
    const panel = document.getElementById('chatbot-window');
    const backdrop = document.getElementById('chatbot-backdrop');
    const home = document.getElementById('chatbot-home');
    const close = document.getElementById('chatbot-close');
    const flow = document.getElementById('chatbot-flow');
    const messages = document.getElementById('chatbot-messages');
    const stage = document.getElementById('chatbot-stage');
    const quickActions = document.getElementById('chatbot-quick-actions');
    const form = document.getElementById('chatbot-form');
    const input = document.getElementById('chatbot-input');

    if (!toggle || !panel || !backdrop || !home || !close || !flow || !messages || !stage || !quickActions || !form || !input) {
        return;
    }

    const bookingPresets = {
        type: ['호텔', '리조트', '펜션', '모텔', '게스트하우스', '전체'],
        location: ['서울', '부산', '제주', '강릉', '경주', '직접 입력']
    };
    const curationLocations = ['서울', '부산', '제주', '강릉', '경주', '여수'];
    const curationPrices = [
        { title: '5만원 미만', subtitle: '초저가 중심', minPrice: null, maxPrice: 50000 },
        { title: '5만원 이상 ~ 10만원 미만', subtitle: '가성비 숙소', minPrice: 50000, maxPrice: 100000 },
        { title: '10만원 이상 ~ 15만원 미만', subtitle: '중간 가격대', minPrice: 100000, maxPrice: 150000 },
        { title: '15만원 이상 ~ 20만원 미만', subtitle: '프리미엄 입문', minPrice: 150000, maxPrice: 200000 },
        { title: '20만원 이상', subtitle: '고급 숙소', minPrice: 200000, maxPrice: null }
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
        comparison: emptyComparison()
    };

    const CATEGORY_COMMANDS = ['장바구니 담기', '맞춤 추천', '숙소 비교'];

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

    home.addEventListener('click', function () {
        goToCategorySelection();
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();

        // 비교 모드에서 2개 선택 완료 시 전송 버튼으로 비교 실행
        if (state.mode === 'comparison' && state.step === 'comparison-select') {
            if (state.comparison.selected.length === 2) {
                requestCompare(state.comparison.selected[0], state.comparison.selected[1]);
            }
            return;
        }

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
        clearMessages();
        renderFlow();
        pushMessage('bot', '무엇을 도와드릴까요? 장바구니 담기, 맞춤 추천, 숙소 비교를 지원합니다.');
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
                ['guests', '4 인원'],
                ['confirm', '5 확인']
            ], state.step);
            return;
        }

        if (state.mode === 'curation') {
            flow.innerHTML = buildFlow([
                ['curation-location', '1 지역'],
                ['curation-dates', '2 날짜'],
                ['curation-guests', '3 인원'],
                ['curation-price', '4 가격'],
                ['curation-result', '5 추천']
            ], state.step);
        }

        if (state.mode === 'comparison') {
            flow.innerHTML = buildFlow([
                ['comparison-select', '1 숙소 선택'],
                ['comparison-result', '2 비교 결과']
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
            { title: '장바구니 담기', subtitle: '날짜·지역·유형 조건으로 숙소 찾기' },
            { title: '맞춤 추천', subtitle: '지역·날짜·가격대로 숙소 추천' },
            { title: '숙소 비교', subtitle: '찜·최근 숙소 2개 나란히 비교' }
        ], function (item) {
            pushMessage('user', item.title);
            selectMode(item.title);
        }));
    }

    function selectMode(label) {
        clearStage();
        clearQuickActions();

        if (label === '장바구니 담기') {
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
    }

    function handleInput(value) {
        const input = (value || '').trim();

        if (!input) {
            return;
        }

        // 모드 선택 전 상태
        if (!state.mode) {
            if (CATEGORY_COMMANDS.includes(input)) {
                selectMode(input);
                return;
            }
            requestAssistantReply(input);
            return;
        }

        if (state.mode === 'booking') {
            handleBookingInput(input);
            return;
        }

        if (state.mode === 'curation') {
            handleCurationInput(input);
            return;
        }

        if (state.mode === 'comparison') {
            pushMessage('bot', '숙소 비교는 카드에서 2개를 선택한 뒤 진행해주세요.');
            return;
        }

        requestAssistantReply(input);
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
                renderStepNavigation();
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

        if (state.step === 'guests') {
            const guestCount = parseGuestCountInput(value);
            if (!guestCount) {
                pushMessage('bot', '인원 수를 숫자로 입력해주세요. 예: 4명');
                return;
            }
            applyBookingGuests(guestCount, 0, state.booking.roomCount || 1);
            return;
        }

        if (state.step === 'confirm') {
            if (value === '장바구니 담기') {
                requestBookingAssistant();
                return;
            }
            if (value === '날짜 수정') {
                state.step = 'dates';
                renderFlow();
                pushMessage('bot', '날짜를 다시 선택해주세요.');
                renderDateStage(applyBookingDates, state.booking.checkInDate, state.booking.checkOutDate);
                return;
            }
            if (value === '인원 수정') {
                state.step = 'guests';
                renderFlow();
                pushMessage('bot', '투숙 인원과 객실 수를 다시 선택해주세요.');
                renderBookingGuestStage();
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

        if (state.step === 'curation-guests') {
            const guestCount = parseGuestCountInput(value);
            if (!guestCount) {
                pushMessage('bot', '인원 수를 숫자로 입력해주세요. 예: 2명');
                return;
            }
            applyCurationGuests(guestCount, 0, state.curation.roomCount || 1);
            return;
        }

        if (state.step === 'curation-result') {
            requestCurationRecommendations();
        }
    }

    function renderBookingTypeStage() {
        input.placeholder = '숙소 유형을 입력하거나 선택하세요.';
        clearStage();
        renderStepNavigation();
        stage.appendChild(buildCardGrid(bookingPresets.type.map(function (title) {
            return { title: title, subtitle: '유형 선택' };
        }), function (item) {
            pushMessage('user', item.title);
            handleBookingInput(item.title);
        }));
    }

    function renderBookingLocationStage() {
        input.placeholder = '지역을 입력하거나 선택하세요.';
        clearStage();
        renderStepNavigation();
        stage.appendChild(buildCardGrid(bookingPresets.location.map(function (title) {
            return { title: title, subtitle: '지역 선택' };
        }), function (item) {
            pushMessage('user', item.title);
            handleBookingInput(item.title);
        }));
    }

    function renderCurationLocationStage() {
        input.placeholder = '추천받을 지역을 입력하거나 선택하세요.';
        clearStage();
        renderStepNavigation();
        stage.appendChild(buildCardGrid(curationLocations.map(function (title) {
            return { title: title, subtitle: '추천 지역' };
        }), function (item) {
            pushMessage('user', item.title);
            handleCurationInput(item.title);
        }));
    }

    function renderCurationPriceStage() {
        input.placeholder = '가격대를 선택하세요.';
        clearStage();
        renderStepNavigation();
        stage.appendChild(buildCardGrid(curationPrices, function (item) {
            pushMessage('user', item.title);
            state.curation.priceLabel = item.title;
            state.curation.minPrice = item.minPrice;
            state.curation.maxPrice = item.maxPrice;
            state.step = 'curation-result';
            renderFlow();
            requestCurationRecommendations();
        }));
    }

    function renderDateStage(applyHandler, currentCheckInDate, currentCheckOutDate) {
        clearStage();
        renderStepNavigation();
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
        state.step = 'guests';
        renderFlow();
        pushMessage('bot', buildGuestPrompt(state.booking.type));
        renderBookingGuestStage();
    }

    function applyCurationDates(checkInDate, checkOutDate) {
        if (!isValidDateRange(checkInDate, checkOutDate)) {
            pushMessage('bot', '체크아웃은 체크인보다 뒤 날짜여야 합니다.');
            return;
        }

        state.curation.checkInDate = checkInDate;
        state.curation.checkOutDate = checkOutDate;
        state.step = 'curation-guests';
        renderFlow();
        pushMessage('bot', '추천받을 인원을 선택해주세요.');
        renderCurationGuestStage();
    }

    function renderBookingConfirmStage() {
        clearStage();
        input.placeholder = '장바구니 담기 또는 날짜/인원 수정';
        renderStepNavigation();

        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = [
            summaryItem('유형', state.booking.type || '-'),
            summaryItem('지역', state.booking.location || '-'),
            summaryItem('체크인', state.booking.checkInDate || '-'),
            summaryItem('체크아웃', state.booking.checkOutDate || '-'),
            summaryItem('인원', formatGuestSummary(state.booking.adultCount, state.booking.childCount)),
            summaryItem('객실', formatRoomSummary(state.booking.roomCount))
        ].join('');

        stage.appendChild(summary);
        setContextualQuickActions(['장바구니 담기', '날짜 수정', '인원 수정', '다시 선택'], function (value) {
            if (value === '뒤로가기') {
                goBackOneStep();
                return;
            }
            handleBookingInput(value);
        });
    }

    function renderBookingGuestStage() {
        const limits = getGuestLimitsForType(state.booking.type);
        clearStage();
        renderStepNavigation();
        input.placeholder = '투숙 인원과 객실 수를 선택하세요.';

        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = [
            summaryItem('유형', state.booking.type || '-'),
            summaryItem('가능 인원', limits.min + '명 ~ ' + limits.max + '명')
        ].join('');
        stage.appendChild(summary);
        stage.appendChild(buildOccupancyInputBox({
            minTotalGuests: limits.min,
            maxTotalGuests: limits.max,
            selectedAdults: state.booking.adultCount || Math.max(1, limits.min),
            selectedChildren: state.booking.childCount || 0,
            selectedRooms: state.booking.roomCount || 1,
            onApply: function (adultCount, childCount, roomCount) {
                pushMessage('user', formatOccupancySummary(adultCount, childCount, roomCount));
                applyBookingGuests(adultCount, childCount, roomCount);
            }
        }));
    }

    function goToCategorySelection() {
        state.mode = '';
        state.step = 'category';
        state.booking = emptyBooking();
        state.curation = emptyCuration();
        state.comparison = emptyComparison();
        renderFlow();
        pushMessage('bot', '원하는 기능을 다시 선택해주세요.');
        renderCategoryStage();
    }

    function renderCurationGuestStage() {
        clearStage();
        renderStepNavigation();
        input.placeholder = '추천받을 인원을 선택하세요.';

        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = [
            summaryItem('지역', state.curation.location || '-'),
            summaryItem('가능 인원', '1명 ~ 10명')
        ].join('');
        stage.appendChild(summary);
        stage.appendChild(buildOccupancyInputBox({
            minTotalGuests: 1,
            maxTotalGuests: 10,
            selectedAdults: state.curation.adultCount || 2,
            selectedChildren: state.curation.childCount || 0,
            selectedRooms: state.curation.roomCount || 1,
            onApply: function (adultCount, childCount, roomCount) {
                pushMessage('user', formatOccupancySummary(adultCount, childCount, roomCount));
                applyCurationGuests(adultCount, childCount, roomCount);
            }
        }));
    }

    async function requestCurationRecommendations() {
        try {
            clearStage();
            renderStepNavigation();
            pushMessage('bot', '선택한 조건으로 AI 맞춤 추천을 생성하고 있습니다.');

            const response = await fetch('/api/chatbot/assistant?message=' + encodeURIComponent(buildCurationAssistantPrompt()), {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (!response.ok) {
                throw new Error('assistant request failed');
            }

            const data = await response.json();
            const filteredRecommendations = applyCurationFilters(data.recommendations || []);

            renderRecommendationResult({
                interpretedNeeds: [
                    '지역: ' + (state.curation.location || '-'),
                    '체크인: ' + (state.curation.checkInDate || '-'),
                    '체크아웃: ' + (state.curation.checkOutDate || '-'),
                    '인원: ' + formatGuestSummary(state.curation.adultCount, state.curation.childCount),
                    '객실: ' + formatRoomSummary(state.curation.roomCount),
                    '가격대: ' + (state.curation.priceLabel || '가격 무관')
                ],
                recommendations: filteredRecommendations
            });
        } catch (error) {
            console.error(error);
            pushMessage('bot', '맞춤 추천 API 응답이 없어 검색 결과 페이지로 바로 안내합니다.');
            renderRecommendationFallback();
            setContextualQuickActions(['검색 결과로 이동', '다시 시도', '다시 기능 선택'], function (value) {
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
        const minPrice = state.curation.minPrice;
        const maxPrice = state.curation.maxPrice;
        if (minPrice != null || maxPrice != null) {
            return items.filter(function (item) {
                if (typeof item.pricePerNight !== 'number') {
                    return false;
                }
                if (minPrice != null && item.pricePerNight < minPrice) {
                    return false;
                }
                if (maxPrice != null && item.pricePerNight >= maxPrice) {
                    return false;
                }
                return true;
            });
        }
        return items;
    }

    function renderRecommendationResult(data) {
        clearStage();

        const needs = Array.isArray(data.interpretedNeeds) ? data.interpretedNeeds : [];
        const recommendations = dedupeRecommendations(Array.isArray(data.recommendations) ? data.recommendations : []);

        input.placeholder = '원하는 기능을 선택하거나 직접 입력하세요.';

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

        setContextualQuickActions(['조건 다시 선택', '검색 결과로 이동', '다시 기능 선택'], function (value) {
            if (value === '뒤로가기') {
                goBackOneStep();
                return;
            }
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
        clearStage();
        input.placeholder = '원하는 기능을 선택하거나 직접 입력하세요.';

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
            checkOutDate: '',
            adultCount: 0,
            childCount: 0,
            roomCount: 1
        };
    }

    function emptyCuration() {
        return {
            location: '',
            checkInDate: '',
            checkOutDate: '',
            adultCount: 0,
            childCount: 0,
            roomCount: 1,
            priceLabel: '',
            minPrice: null,
            maxPrice: null
        };
    }

    function requestAssistantReply(message) {
        fetch('/api/chatbot/assistant?message=' + encodeURIComponent(message), {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('assistant request failed');
                }
                return response.json();
            })
            .then(function (data) {
                pushMessage('bot', data.answer || '추천 결과를 확인해주세요.');
                renderRecommendationResult({
                    interpretedNeeds: [],
                    recommendations: Array.isArray(data.recommendations) ? data.recommendations : []
                });
            })
            .catch(function (error) {
                console.error(error);
                pushMessage('bot', 'AI 추천을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
            });
    }

    function requestBookingAssistant() {
        const params = new URLSearchParams({
            location: state.booking.location || '',
            accomType: state.booking.type || '',
            checkInDate: state.booking.checkInDate || '',
            checkOutDate: state.booking.checkOutDate || '',
            adultCount: String(state.booking.adultCount || 1),
            childCount: String(state.booking.childCount || 0),
            roomCount: String(state.booking.roomCount || 1)
        });

        fetch('/api/chatbot/cart-candidates?' + params.toString(), {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('assistant request failed');
                }
                return response.json();
            })
            .then(function (data) {
                const recommendations = Array.isArray(data.recommendations) ? data.recommendations : [];
                const actionType = data.actionType || '';

                pushMessage('bot', data.answer || '장바구니 요청 결과를 확인해주세요.');

                if (actionType === 'login_required') {
                    clearStage();
                    setContextualQuickActions(['처음으로'], function () {
                        goToCategorySelection();
                    });
                    return;
                }

                if (actionType === 'cart_candidate_single' && data.selectedAccomId) {
                    const selected = recommendations.find(function (item) {
                        return item.accomId === data.selectedAccomId;
                    });
                    submitBookingCartSelection(data.selectedAccomId, selected ? selected.accomName : '선택한 업소');
                    return;
                }

                if (actionType === 'cart_candidate_select' && recommendations.length) {
                    renderBookingCandidateSelection(recommendations);
                    return;
                }

                renderRecommendationResult({
                    interpretedNeeds: buildBookingFailureNeeds(),
                    recommendations: recommendations
                });
            })
            .catch(function (error) {
                console.error(error);
                pushMessage('bot', '장바구니 후보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
            });
    }

    function buildBookingFailureNeeds() {
        return [
            '지역: ' + (state.booking.location || '-'),
            '유형: ' + (state.booking.type || '-'),
            '체크인: ' + (state.booking.checkInDate || '-'),
            '체크아웃: ' + (state.booking.checkOutDate || '-'),
            '인원: ' + formatGuestSummary(state.booking.adultCount, state.booking.childCount),
            '객실: ' + formatRoomSummary(state.booking.roomCount)
        ];
    }

    function renderBookingCandidateSelection(items) {
        clearStage();
        renderStepNavigation();
        input.placeholder = '숙박 업소를 선택해 주세요.';

        const summary = document.createElement('div');
        summary.className = 'chatbot-summary';
        summary.innerHTML = buildBookingFailureNeeds().map(function (item) {
            return summaryItem('조건', item);
        }).join('');
        stage.appendChild(summary);

        const grid = document.createElement('div');
        grid.className = 'chatbot-accom-grid';

        items.slice(0, 3).forEach(function (item) {
            const stars = Array.from({ length: item.grade || 0 }, function () {
                return '<span class="star filled">&#9733;</span>';
            }).join('');

            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'accom-card card chatbot-accom-select-card';
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
                        '<span class="reserve-btn">선택</span>' +
                    '</div>' +
                '</div>';

            card.addEventListener('click', function () {
                submitBookingCartSelection(item.accomId, item.accomName);
            });

            grid.appendChild(card);
        });

        stage.appendChild(grid);
        setContextualQuickActions(['다시 선택', '처음으로'], function (value) {
            if (value === '다시 선택') {
                renderBookingConfirmStage();
                return;
            }
            goToCategorySelection();
        });
    }

    function submitBookingCartSelection(accomId, accomName) {
        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken = csrfTokenMeta ? csrfTokenMeta.content : readCookieValue('XSRF-TOKEN');
        const csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.content : 'X-XSRF-TOKEN';
        const headers = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        };

        if (csrfToken) {
            headers[csrfHeaderName] = csrfToken;
        }

        fetch('/cart', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                accomId: accomId,
                checkInDate: state.booking.checkInDate,
                checkOutDate: state.booking.checkOutDate,
                adultCount: state.booking.adultCount || 1,
                childCount: state.booking.childCount || 0,
                roomCount: state.booking.roomCount || 1
            })
        })
            .then(function (response) {
                if (response.ok) {
                    return response.text();
                }
                if (response.status === 401) {
                    throw new Error('로그인이 필요합니다. 다시 로그인한 뒤 시도해 주세요.');
                }
                if (response.status === 403) {
                    throw new Error('보안 토큰이 만료되었거나 권한이 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.');
                }
                return response.text().then(function (message) {
                    throw new Error(message || '장바구니에 담지 못했습니다.');
                });
            })
            .then(function () {
                clearStage();
                clearQuickActions();
                pushMessage('bot', (accomName || '선택한 업소') + '을(를) 장바구니에 담았습니다. 장바구니에서 바로 확인해 보세요.');
                setContextualQuickActions(['장바구니 보기', '처음으로'], function (value) {
                    if (value === '장바구니 보기') {
                        window.location.href = '/cart';
                        return;
                    }
                    goToCategorySelection();
                });
            })
            .catch(function (error) {
                console.error(error);
                pushMessage('bot', '선택한 업소를 장바구니에 담지 못했습니다. ' + (error.message || '다른 업소를 선택해 주세요.'));
            });
    }

    function readCookieValue(name) {
        const encodedName = encodeURIComponent(name) + '=';
        const parts = document.cookie ? document.cookie.split('; ') : [];

        for (let i = 0; i < parts.length; i += 1) {
            if (parts[i].indexOf(encodedName) === 0) {
                return decodeURIComponent(parts[i].substring(encodedName.length));
            }
        }

        return '';
    }

    function buildCurationAssistantPrompt() {
        return [
            (state.curation.location || '지역 미정') + ' 숙소 추천해줘',
            state.curation.checkInDate ? '체크인 ' + state.curation.checkInDate : '',
            state.curation.checkOutDate ? '체크아웃 ' + state.curation.checkOutDate : '',
            state.curation.adultCount ? '성인 ' + state.curation.adultCount + '명' : '',
            state.curation.childCount ? '아동 ' + state.curation.childCount + '명' : '',
            state.curation.roomCount ? '객실 ' + state.curation.roomCount + '개' : '',
            state.curation.priceLabel ? '가격대는 ' + state.curation.priceLabel : '',
            '정확히 일치하는 숙소가 없으면 선택한 필터와 가장 가까운 업소를 찾아 추천하고, 어떤 조건이 비슷한지 같이 알려줘'
        ].filter(Boolean).join(', ');
    }

    function getGuestLimitsForType(typeLabel) {
        const accomType = typeMap[typeLabel] || '';
        if (accomType === 'MOTEL' || accomType === 'GUESTHOUSE') {
            return { min: 1, max: 6, label: typeLabel || '해당 숙소 유형' };
        }
        return { min: 2, max: 10, label: typeLabel || '해당 숙소 유형' };
    }

    function buildGuestPrompt(typeLabel) {
        const limits = getGuestLimitsForType(typeLabel);
        return (typeLabel || '숙소') + '은(는) ' + limits.min + '명부터 ' + limits.max + '명까지 선택할 수 있습니다. 투숙 인원과 객실 수를 선택해주세요.';
    }

    function buildGuestSelectBox(options) {
        const box = document.createElement('div');
        box.className = 'chatbot-date-box';

        const field = document.createElement('label');
        field.className = 'chatbot-date-field';
        field.innerHTML = '<span>' + escapeHtml(options.label || '투숙 인원') + '</span>';

        const select = document.createElement('select');
        select.className = 'chatbot-guest-select';
        for (let value = options.min; value <= options.max; value += 1) {
            const option = document.createElement('option');
            option.value = String(value);
            option.textContent = value + '명';
            option.selected = value === options.selectedValue;
            select.appendChild(option);
        }
        field.appendChild(select);

        const apply = document.createElement('button');
        apply.type = 'button';
        apply.className = 'chatbot-date-apply';
        apply.textContent = options.applyLabel || '적용';
        apply.addEventListener('click', function () {
            options.onApply(Number(select.value));
        });

        box.appendChild(field);
        box.appendChild(apply);
        return box;
    }

    function buildOccupancyInputBox(options) {
        const box = document.createElement('div');
        box.className = 'chatbot-date-box';

        const fields = document.createElement('div');
        fields.className = 'chatbot-date-fields chatbot-date-fields--occupancy';

        const adultField = document.createElement('label');
        adultField.className = 'chatbot-date-field';
        adultField.innerHTML = '<span>성인</span>';

        const adultInput = document.createElement('input');
        adultInput.type = 'number';
        adultInput.min = '1';
        adultInput.max = String(options.maxTotalGuests);
        adultInput.step = '1';
        adultInput.className = 'chatbot-guest-input';
        adultInput.value = String(options.selectedAdults || 1);
        adultField.appendChild(adultInput);

        const childField = document.createElement('label');
        childField.className = 'chatbot-date-field';
        childField.innerHTML = '<span>아동</span>';

        const childInput = document.createElement('input');
        childInput.type = 'number';
        childInput.min = '0';
        childInput.max = String(options.maxTotalGuests - 1);
        childInput.step = '1';
        childInput.className = 'chatbot-guest-input';
        childInput.value = String(options.selectedChildren || 0);
        childField.appendChild(childInput);

        const roomField = document.createElement('label');
        roomField.className = 'chatbot-date-field';
        roomField.innerHTML = '<span>객실 수</span>';

        const roomInput = document.createElement('input');
        roomInput.type = 'number';
        roomInput.min = '1';
        roomInput.step = '1';
        roomInput.className = 'chatbot-guest-input';
        roomInput.value = String(options.selectedRooms || 1);
        roomField.appendChild(roomInput);

        fields.appendChild(adultField);
        fields.appendChild(childField);
        fields.appendChild(roomField);

        const apply = document.createElement('button');
        apply.type = 'button';
        apply.className = 'chatbot-date-apply';
        apply.textContent = '조건 적용';
        apply.addEventListener('click', function () {
            options.onApply(Number(adultInput.value), Number(childInput.value), Number(roomInput.value));
        });

        box.appendChild(fields);
        box.appendChild(apply);
        return box;
    }

    function parseGuestCountInput(value) {
        const match = String(value || '').match(/(\d{1,2})\s*(명|인)?/);
        return match ? Number(match[1]) : 0;
    }

    function formatGuestSummary(adultCount, childCount) {
        const adults = Number(adultCount) || 0;
        const children = Number(childCount) || 0;
        const total = adults + children;
        if (!total) {
            return '-';
        }
        if (!children) {
            return '총 ' + total + '명';
        }
        return '성인 ' + adults + '명, 아동 ' + children + '명';
    }

    function formatRoomSummary(roomCount) {
        return (Number(roomCount) || 1) + '개';
    }

    function calculateNightCount(checkInDate, checkOutDate) {
        if (!isValidDateRange(checkInDate, checkOutDate)) {
            return 0;
        }
        const start = new Date(checkInDate + 'T00:00:00');
        const end = new Date(checkOutDate + 'T00:00:00');
        return Math.round((end - start) / 86400000);
    }

    function formatOccupancySummary(adultCount, childCount, roomCount) {
        const parts = ['성인 ' + adultCount + '명'];
        if (childCount > 0) {
            parts.push('아동 ' + childCount + '명');
        }
        parts.push('객실 ' + roomCount + '개');
        return parts.join(' / ');
    }

    function dedupeRecommendations(items) {
        const seen = new Set();
        return items.filter(function (item) {
            const key = item && item.accomId != null
                ? 'id:' + item.accomId
                : 'name:' + (item && item.accomName ? item.accomName : '');
            if (seen.has(key)) {
                return false;
            }
            seen.add(key);
            return true;
        });
    }

    function applyBookingGuests(adultCount, childCount, roomCount) {
        const limits = getGuestLimitsForType(state.booking.type);
        const totalGuestCount = adultCount + childCount;
        if (adultCount < 1) {
            pushMessage('bot', '성인은 최소 1명 이상 선택해야 합니다.');
            return;
        }
        if (totalGuestCount < limits.min || totalGuestCount > limits.max) {
            pushMessage('bot', limits.label + '은(는) ' + limits.min + '명부터 ' + limits.max + '명까지 선택할 수 있습니다.');
            return;
        }
        if (roomCount < 1) {
            pushMessage('bot', '객실 수는 1개 이상 선택해야 합니다.');
            return;
        }
        state.booking.adultCount = adultCount;
        state.booking.childCount = childCount;
        state.booking.roomCount = roomCount;
        state.step = 'confirm';
        renderFlow();
        pushMessage('bot', '조건을 확인하고 바로 장바구니 담기를 진행할 수 있습니다.');
        renderBookingConfirmStage();
    }

    function applyCurationGuests(adultCount, childCount, roomCount) {
        const totalGuestCount = adultCount + childCount;
        if (adultCount < 1) {
            pushMessage('bot', '성인은 최소 1명 이상 선택해야 합니다.');
            return;
        }
        if (totalGuestCount < 1 || totalGuestCount > 10) {
            pushMessage('bot', '맞춤 추천 인원은 1명부터 10명까지 선택할 수 있습니다.');
            return;
        }
        if (roomCount < 1) {
            pushMessage('bot', '객실 수는 1개 이상 선택해야 합니다.');
            return;
        }
        state.curation.adultCount = adultCount;
        state.curation.childCount = childCount;
        state.curation.roomCount = roomCount;
        state.step = 'curation-price';
        renderFlow();
        pushMessage('bot', '마지막으로 원하는 가격대를 골라주세요.');
        renderCurationPriceStage();
    }

    function renderStepNavigation() {
        if (!state.mode) {
            clearQuickActions();
            return;
        }
        setQuickActions(['뒤로가기'], function () {
            goBackOneStep();
        });
    }

    function setContextualQuickActions(items, handler) {
        const labels = items.slice();
        if (getPreviousStepLabel() && labels.indexOf('뒤로가기') < 0) {
            labels.unshift('뒤로가기');
        }
        setQuickActions(labels, handler);
    }

    function getPreviousStepLabel() {
        if (state.mode === 'booking') {
            if (state.step === 'type') return 'category';
            if (state.step === 'location') return 'type';
            if (state.step === 'dates') return 'location';
            if (state.step === 'guests') return 'dates';
            if (state.step === 'confirm') return 'guests';
        }
        if (state.mode === 'curation') {
            if (state.step === 'curation-location') return 'category';
            if (state.step === 'curation-dates') return 'curation-location';
            if (state.step === 'curation-guests') return 'curation-dates';
            if (state.step === 'curation-price') return 'curation-guests';
            if (state.step === 'curation-result') return 'curation-price';
        }
        if (state.mode === 'comparison') {
            if (state.step === 'comparison-select') return 'category';
            if (state.step === 'comparison-result') return 'comparison-select';
        }
        return '';
    }

    function goBackOneStep() {
        if (getPreviousStepLabel() === 'category') {
            goToCategorySelection();
            return;
        }
        if (state.mode === 'booking') {
            if (state.step === 'location') {
                state.step = 'type';
                renderFlow();
                pushMessage('bot', '숙소 유형을 다시 선택해주세요.');
                renderBookingTypeStage();
                return;
            }
            if (state.step === 'dates') {
                state.step = 'location';
                renderFlow();
                pushMessage('bot', '지역을 다시 선택해주세요.');
                renderBookingLocationStage();
                return;
            }
            if (state.step === 'guests') {
                state.step = 'dates';
                renderFlow();
                pushMessage('bot', '날짜를 다시 선택해주세요.');
                renderDateStage(applyBookingDates, state.booking.checkInDate, state.booking.checkOutDate);
                return;
            }
            if (state.step === 'confirm') {
                state.step = 'guests';
                renderFlow();
                pushMessage('bot', '투숙 인원과 객실 수를 다시 선택해주세요.');
                renderBookingGuestStage();
            }
            return;
        }
        if (state.mode === 'comparison') {
            if (state.step === 'comparison-result') {
                state.step = 'comparison-select';
                renderFlow();
                pushMessage('bot', '비교할 숙소 2개를 다시 선택해주세요.');
                fetchSelectableAccoms();
            }
            return;
        }
        if (state.mode === 'curation') {
            if (state.step === 'curation-dates') {
                state.step = 'curation-location';
                renderFlow();
                pushMessage('bot', '추천 지역을 다시 선택해주세요.');
                renderCurationLocationStage();
                return;
            }
            if (state.step === 'curation-guests') {
                state.step = 'curation-dates';
                renderFlow();
                pushMessage('bot', '여행 날짜를 다시 선택해주세요.');
                renderDateStage(applyCurationDates, state.curation.checkInDate, state.curation.checkOutDate);
                return;
            }
            if (state.step === 'curation-price' || state.step === 'curation-result') {
                state.step = 'curation-guests';
                renderFlow();
                pushMessage('bot', '추천 인원을 다시 선택해주세요.');
                renderCurationGuestStage();
            }
            return;
        }
    }

    function emptyComparison() {
        return { selected: [] };
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
            renderStepNavigation();

            // 수정된 체크 로직: 데이터가 없거나, 배열이 비어있을 경우
            if (!data || !Array.isArray(data) || data.length === 0) {
                pushMessage('bot', '현재 비교 가능한 숙소(찜 목록 또는 최근 본 숙소)가 없습니다. 먼저 숙소를 둘러보시겠어요?');
                setContextualQuickActions(['처음으로', '숙소 검색하러 가기'], function (v) {
                    if (v === '처음으로') {
                        goToCategorySelection();
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
            setContextualQuickActions(['다시 시도', '처음으로'], function (v) {
                if (v === '다시 시도') { fetchSelectableAccoms(); } else { goToCategorySelection(); }
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
            setContextualQuickActions(['다시 선택', '처음으로'], function (v) {
                if (v === '다시 선택') {
                    state.comparison = emptyComparison();
                    state.step = 'comparison-select';
                    renderFlow();
                    fetchSelectableAccoms();
                } else {
                    goToCategorySelection();
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

        // 체크인 단순 표시
        const checkInRow = document.createElement('div');
        checkInRow.className = 'chatbot-metric-row';
        checkInRow.innerHTML =
            '<div class="chatbot-metric-row__left"><span class="chatbot-metric-value">' + escapeHtml(L.checkInTime || '-') + '</span></div>' +
            '<div class="chatbot-metric-row__label">체크인</div>' +
            '<div class="chatbot-metric-row__right"><span class="chatbot-metric-value">' + escapeHtml(R.checkInTime || '-') + '</span></div>';
        metricsEl.appendChild(checkInRow);

// 체크아웃 단순 표시 (체크인과 동일한 형식)
const checkOutRow = document.createElement('div');
checkOutRow.className = 'chatbot-metric-row';
checkOutRow.innerHTML =
    '<div class="chatbot-metric-row__left"><span class="chatbot-metric-value">' + escapeHtml(L.checkOutTime || '-') + '</span></div>' +
    '<div class="chatbot-metric-row__label">체크아웃</div>' +
    '<div class="chatbot-metric-row__right"><span class="chatbot-metric-value">' + escapeHtml(R.checkOutTime || '-') + '</span></div>';
metricsEl.appendChild(checkOutRow);

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

        setContextualQuickActions(['다시 비교', '처음으로'], function (v) {
            if (v === '다시 비교') {
                state.comparison = emptyComparison();
                state.step = 'comparison-select';
                renderFlow();
                pushMessage('bot', '비교할 숙소 2개를 다시 선택해주세요.');
                fetchSelectableAccoms();
            } else {
                goToCategorySelection();
            }
        });
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
        const lCheckOut = L.checkOutTime || '';
        const rCheckOut = R.checkOutTime || '';
        if (lCheckOut && rCheckOut && lCheckOut !== rCheckOut && lCheckOut !== '정보 없음' && rCheckOut !== '정보 없음') {
            const later = lCheckOut > rCheckOut ? L : R;
            tips.push({ icon: '🛌', text: '느긋한 체크아웃을 원한다면 → ' + later.accomName + ' (체크아웃 ' + (lCheckOut > rCheckOut ? lCheckOut : rCheckOut) + ')' });
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
