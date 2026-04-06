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
});
