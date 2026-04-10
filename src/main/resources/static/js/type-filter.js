(() => {
    const syncSharedFields = () => {
        document.querySelectorAll("[data-shared-field]").forEach((field) => {
            if (!(field instanceof HTMLInputElement)) {
                return;
            }

            document.querySelectorAll(`[data-shared-field="${field.dataset.sharedField}"]`).forEach((peer) => {
                if (peer instanceof HTMLInputElement && peer !== field) {
                    peer.value = field.value;
                }
            });
        });
    };

    const updateFilterChipState = (form) => {
        form.querySelectorAll(".type-filter-chip").forEach((chip) => {
            chip.classList.toggle("is-selected", Boolean(chip.querySelector("input:checked")));
        });
    };

    const applyPriceRange = (form, selectedValue) => {
        let minPrice = "";
        let maxPrice = "";

        switch (selectedValue) {
            case "under-50000":
                maxPrice = "50000";
                break;
            case "50000-100000":
                minPrice = "50000";
                maxPrice = "100000";
                break;
            case "100000-150000":
                minPrice = "100000";
                maxPrice = "150000";
                break;
            case "150000-200000":
                minPrice = "150000";
                maxPrice = "200000";
                break;
            case "over-200000":
                minPrice = "200000";
                break;
            default:
                break;
        }

        const minInput = form.querySelector('input[name="minPrice"]');
        const maxInput = form.querySelector('input[name="maxPrice"]');

        if (minInput) {
            minInput.value = minPrice;
        }

        if (maxInput) {
            maxInput.value = maxPrice;
        }
    };

    const buildQueryString = (form) => {
        const formData = new FormData(form);
        const params = new URLSearchParams();

        formData.forEach((value, key) => {
            const normalized = typeof value === "string" ? value.trim() : value;
            if (normalized !== "") {
                params.set(key, normalized);
            }
        });

        return params.toString();
    };

    const replaceResults = async (form) => {
        const queryString = buildQueryString(form);
        const requestUrl = `${form.action}${queryString ? `?${queryString}` : ""}`;
        const response = await fetch(requestUrl, {
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            }
        });

        if (!response.ok) {
            throw new Error("TYPE_FILTER_REQUEST_FAILED");
        }

        const html = await response.text();
        const parsed = new DOMParser().parseFromString(html, "text/html");
        const nextResults = parsed.getElementById("typeListResults");
        const currentResults = document.getElementById("typeListResults");

        if (!nextResults || !currentResults) {
            throw new Error("TYPE_FILTER_RESULTS_MISSING");
        }

        currentResults.innerHTML = nextResults.innerHTML;
        window.history.replaceState({}, "", requestUrl);
        document.dispatchEvent(new CustomEvent("typeList:updated"));
    };

    document.querySelectorAll(".type-filter-form").forEach((form) => {
        form.addEventListener("change", async (event) => {
            const target = event.target;
            if (!(target instanceof HTMLInputElement) || target.type !== "radio") {
                return;
            }

            updateFilterChipState(form);

            if (target.name === "priceRange") {
                applyPriceRange(form, target.value);
            }

            syncSharedFields();

            try {
                await replaceResults(form);
            } catch (_) {
                form.requestSubmit();
            }
        });
    });

    document.querySelectorAll(".booking-bar").forEach((form) => {
        form.addEventListener("submit", () => {
            syncSharedFields();
        });
    });
})();
