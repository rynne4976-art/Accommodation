document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".review-image-slider-shell").forEach((shell) => {
        const slider = shell.querySelector("[data-review-image-slider]");
        const prevButton = shell.querySelector("[data-review-image-prev]");
        const nextButton = shell.querySelector("[data-review-image-next]");

        if (!slider || !prevButton || !nextButton) {
            return;
        }

        const getScrollAmount = () => {
            const firstImage = slider.querySelector(".review-image-slider-item");
            if (!firstImage) {
                return 0;
            }

            const sliderStyle = window.getComputedStyle(slider);
            const gap = Number.parseFloat(sliderStyle.columnGap || sliderStyle.gap || "0");
            return (firstImage.getBoundingClientRect().width + gap) * 5;
        };

        prevButton.addEventListener("click", () => {
            slider.scrollBy({ left: -getScrollAmount(), behavior: "smooth" });
        });

        nextButton.addEventListener("click", () => {
            slider.scrollBy({ left: getScrollAmount(), behavior: "smooth" });
        });
    });

    const modal = document.getElementById("reviewImageViewerModal");
    const modalImage = document.getElementById("reviewImageViewerTarget");
    const prevModalButton = document.querySelector("[data-review-image-modal-prev]");
    const nextModalButton = document.querySelector("[data-review-image-modal-next]");
    const closeButtons = document.querySelectorAll("[data-review-image-close]");
    const groupedImages = new Map();
    let activeGroup = [];
    let activeIndex = 0;

    document.querySelectorAll("[data-review-modal-image]").forEach((image) => {
        const groupKey = image.getAttribute("data-review-modal-group") || "default";
        if (!groupedImages.has(groupKey)) {
            groupedImages.set(groupKey, []);
        }

        groupedImages.get(groupKey).push(image.getAttribute("data-review-modal-image"));

        image.addEventListener("click", () => {
            activeGroup = groupedImages.get(groupKey) || [];
            activeIndex = activeGroup.indexOf(image.getAttribute("data-review-modal-image"));
            openModalImage();
        });
    });

    function renderModalImage() {
        if (!modal || !modalImage || !activeGroup.length) {
            return;
        }

        modalImage.src = activeGroup[activeIndex];
        prevModalButton.style.display = activeGroup.length > 1 ? "inline-flex" : "none";
        nextModalButton.style.display = activeGroup.length > 1 ? "inline-flex" : "none";
    }

    function openModalImage() {
        if (!modal || !activeGroup.length) {
            return;
        }

        renderModalImage();
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.classList.add("modal-open");
    }

    function closeModalImage() {
        if (!modal) {
            return;
        }

        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.classList.remove("modal-open");
    }

    function moveModalImage(step) {
        if (!activeGroup.length) {
            return;
        }

        activeIndex = (activeIndex + step + activeGroup.length) % activeGroup.length;
        renderModalImage();
    }

    prevModalButton?.addEventListener("click", () => moveModalImage(-1));
    nextModalButton?.addEventListener("click", () => moveModalImage(1));
    closeButtons.forEach((button) => button.addEventListener("click", closeModalImage));

    document.addEventListener("keydown", (event) => {
        if (!modal?.classList.contains("is-open")) {
            return;
        }

        if (event.key === "Escape") {
            closeModalImage();
        }

        if (event.key === "ArrowLeft") {
            moveModalImage(-1);
        }

        if (event.key === "ArrowRight") {
            moveModalImage(1);
        }
    });
});
