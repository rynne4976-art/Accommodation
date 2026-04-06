document.addEventListener("DOMContentLoaded", () => {
    const slider = document.querySelector("[data-review-slider]");
    const prevButton = document.querySelector("[data-review-prev]");
    const nextButton = document.querySelector("[data-review-next]");

    if (!slider || !prevButton || !nextButton) {
        return;
    }

    const getScrollAmount = () => {
        const firstCard = slider.querySelector(".review-list-card");
        if (!firstCard) {
            return 0;
        }

        const sliderStyle = window.getComputedStyle(slider);
        const gap = Number.parseFloat(sliderStyle.columnGap || sliderStyle.gap || "0");
        return firstCard.getBoundingClientRect().width + gap;
    };

    prevButton.addEventListener("click", () => {
        slider.scrollBy({ left: -getScrollAmount(), behavior: "smooth" });
    });

    nextButton.addEventListener("click", () => {
        slider.scrollBy({ left: getScrollAmount(), behavior: "smooth" });
    });
});
