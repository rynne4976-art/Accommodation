(() => {
    const buttons = document.querySelectorAll("[data-wish-button][data-accom-id]");
    if (!buttons.length) {
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");

    function moveToLoginWithRedirect() {
        const redirectUrl = `${window.location.pathname}${window.location.search}`;
        window.location.href = `/members/login?redirectUrl=${encodeURIComponent(redirectUrl)}`;
    }

    async function syncWishButtons() {
        const isWishPage = Boolean(document.querySelector("[data-wish-content]"));
        if (isWishPage) {
            return;
        }

        try {
            const response = await fetch("/wish", {
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            if (!response.ok || response.redirected || response.url.includes("/members/login")) {
                return;
            }

            const contentType = response.headers.get("content-type") || "";
            if (!contentType.includes("text/html")) {
                return;
            }

            const html = await response.text();
            const doc = new DOMParser().parseFromString(html, "text/html");
            const wishedIds = new Set(
                Array.from(doc.querySelectorAll("[data-wish-button][data-accom-id]"))
                    .map((node) => node.getAttribute("data-accom-id"))
                    .filter(Boolean)
            );

            buttons.forEach((button) => {
                const wished = wishedIds.has(button.getAttribute("data-accom-id"));
                button.classList.toggle("is-active", wished);
                button.setAttribute("aria-pressed", wished ? "true" : "false");
            });
        } catch (error) {
            console.error(error);
        }
    }

    async function toggleWish(button) {
        const accomId = button.getAttribute("data-accom-id");
        const wished = button.classList.contains("is-active");
        const method = wished ? "DELETE" : "POST";
        const headers = {};

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        button.disabled = true;

        try {
            const response = await fetch(`/wish/${accomId}`, {
                method,
                headers
            });

            if (response.status === 401 || response.redirected || response.url.includes("/members/login")) {
                moveToLoginWithRedirect();
                return;
            }

            if (!response.ok) {
                throw new Error("wish request failed");
            }

            const contentType = response.headers.get("content-type") || "";
            if (!contentType.includes("application/json")) {
                throw new Error("unexpected response type");
            }

            const result = await response.json();
            button.classList.toggle("is-active", result.wished);
            button.setAttribute("aria-pressed", result.wished ? "true" : "false");

            const card = button.closest("[data-wish-card]");
            if (card && !result.wished) {
                card.remove();
                const remainingCards = document.querySelectorAll("[data-wish-card]");
                document.querySelectorAll("[data-wish-count]").forEach((node) => {
                    node.textContent = String(remainingCards.length);
                });

                const content = document.querySelector("[data-wish-content]");
                const empty = document.querySelector("[data-wish-empty]");
                if (content && empty && remainingCards.length === 0) {
                    content.hidden = true;
                    empty.hidden = false;
                }
            } else {
                document.querySelectorAll("[data-wish-count]").forEach((node) => {
                    node.textContent = String(result.wishCount ?? 0);
                });
            }
        } catch (error) {
            console.error(error);
            alert("찜 처리 중 오류가 발생했습니다.");
        } finally {
            button.disabled = false;
        }
    }

    buttons.forEach((button) => {
        button.addEventListener("click", () => {
            toggleWish(button);
        });
    });

    syncWishButtons();
})();
