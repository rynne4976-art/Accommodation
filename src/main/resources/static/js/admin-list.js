document.addEventListener('submit', function (event) {
    const form = event.target.closest('[data-ajax-form]');
    if (!form) {
        return;
    }

    const submitter = event.submitter;
    if (submitter && submitter.hasAttribute('data-reset-search')) {
        form.reset();
    }

    if (form.dataset.ajaxFallback === 'true') {
        delete form.dataset.ajaxFallback;
        return;
    }

    event.preventDefault();

    const shell = form.closest('[data-ajax-list]');
    const method = (form.method || 'get').toLowerCase();

    if (method === 'get') {
        const url = new URL(form.action || window.location.href, window.location.origin);
        const formData = new FormData(form);

        for (const [key, value] of formData.entries()) {
            if (value) {
                url.searchParams.set(key, value);
            } else {
                url.searchParams.delete(key);
            }
        }

        url.searchParams.delete('page');
        updateAdminList(shell, url.toString(), true);
        return;
    }

    submitAdminAction(shell, form);
});

document.addEventListener('click', function (event) {
    const link = event.target.closest('[data-ajax-list] .pager a');
    if (!link) {
        return;
    }

    event.preventDefault();

    const shell = link.closest('[data-ajax-list]');
    updateAdminList(shell, link.href, true);
});

window.addEventListener('popstate', function () {
    const shell = document.querySelector('[data-ajax-list]');
    if (!shell) {
        return;
    }

    updateAdminList(shell, window.location.href, false);
});

async function updateAdminList(shell, url, pushState) {
    if (!shell) {
        return;
    }

    shell.classList.add('is-loading');

    try {
        const response = await fetch(url, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load admin list');
        }

        const html = await response.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const selector = shell.getAttribute('data-update-selector') || '[data-ajax-list]';
        const nextShell = doc.querySelector(selector);

        if (!nextShell) {
            throw new Error('Updated section not found');
        }

        shell.replaceWith(nextShell);

        if (pushState) {
            window.history.pushState({}, '', url);
        }
    } catch (error) {
        window.location.href = url;
    }
}

async function submitAdminAction(shell, form) {
    if (!shell || !form) {
        return;
    }

    shell.classList.add('is-loading');

    try {
        const response = await fetch(form.action, {
            method: (form.method || 'post').toUpperCase(),
            body: new FormData(form),
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to submit admin action');
        }

        const html = await response.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const selector = shell.getAttribute('data-update-selector') || '[data-ajax-list]';
        const nextShell = doc.querySelector(selector);

        if (!nextShell) {
            throw new Error('Updated section not found');
        }

        shell.replaceWith(nextShell);
        window.history.pushState({}, '', response.url);
    } catch (error) {
        form.dataset.ajaxFallback = 'true';
        form.requestSubmit();
    }
}
