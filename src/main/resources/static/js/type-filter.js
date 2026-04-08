document.querySelectorAll(".type-filter-form").forEach((form) => {
    form.addEventListener("change", (event) => {
        const target = event.target;
        if (!(target instanceof HTMLInputElement) || target.type !== "radio") {
            return;
        }

        form.querySelectorAll(".type-filter-chip").forEach((chip) => {
            chip.classList.toggle("is-selected", Boolean(chip.querySelector("input:checked")));
        });

        if (target.name !== "priceRange") {
            return;
        }

        let minPrice = "";
        let maxPrice = "";

        switch (target.value) {
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
    });
});
