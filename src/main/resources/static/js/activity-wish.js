(() => {
    const buttons = document.querySelectorAll('[data-activity-wish-button][data-activity-key]');
    if (!buttons.length) {
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    function moveToLoginWithRedirect() {
        const redirectUrl = `${window.location.pathname}${window.location.search}`;
        window.location.href = `/members/login?redirectUrl=${encodeURIComponent(redirectUrl)}`;
    }

    async function toggleWish(button) {
        const activityKey = button.getAttribute('data-activity-key');
        const wished = button.classList.contains('is-active');
        const method = wished ? 'DELETE' : 'POST';
        const headers = {};

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        if (!wished) {
            headers['Content-Type'] = 'application/json';
        }

        button.disabled = true;

        try {
            const response = await fetch(
                wished ? `/activity-wish/${encodeURIComponent(activityKey)}` : '/activity-wish',
                {
                    method,
                    headers,
                    body: wished ? undefined : JSON.stringify({
                        activityKey,
                        title: button.getAttribute('data-title'),
                        imageUrl: button.getAttribute('data-image-url'),
                        address: button.getAttribute('data-address'),
                        period: button.getAttribute('data-period'),
                        detailUrl: button.getAttribute('data-detail-url'),
                        externalUrl: button.getAttribute('data-external-url'),
                        category: button.getAttribute('data-category'),
                        tel: button.getAttribute('data-tel'),
                        regionName: button.getAttribute('data-region-name')
                    })
                }
            );

            if (response.status === 401 || response.redirected || response.url.includes('/members/login')) {
                moveToLoginWithRedirect();
                return;
            }

            if (!response.ok) {
                throw new Error('activity wish request failed');
            }

            const result = await response.json();
            button.classList.toggle('is-active', result.wished);
            button.setAttribute('aria-pressed', result.wished ? 'true' : 'false');

            const card = button.closest('[data-activity-wish-card]');
            if (card && !result.wished) {
                card.remove();

                const remainingCards = document.querySelectorAll('[data-activity-wish-card]');
                const section = document.querySelector('.wish-section--activities');
                const totalCountEl = document.querySelector('.wish-page__count');
                const activityCountHead = section?.querySelector('.wish-section__head span');
                const accomCount = document.querySelectorAll('[data-wish-card]').length;

                if (activityCountHead) {
                    activityCountHead.textContent = `${remainingCards.length}개`;
                }

                if (totalCountEl) {
                    totalCountEl.textContent = `${accomCount + remainingCards.length}개`;
                }

                if (section && remainingCards.length === 0) {
                    section.hidden = true;
                }

                const empty = document.querySelector('[data-wish-empty]');
                if (empty && accomCount + remainingCards.length === 0) {
                    empty.hidden = false;
                }
            }
        } catch (error) {
            console.error(error);
            alert('찜 처리 중 오류가 발생했습니다.');
        } finally {
            button.disabled = false;
        }
    }

    buttons.forEach((button) => {
        button.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            toggleWish(button);
        });
    });
})();
