document.addEventListener('DOMContentLoaded', () => {
    const sliders = document.querySelectorAll('.wish-slider');

    sliders.forEach((slider) => {
        const track = slider.querySelector('.wish-slider__track.is-scrollable');
        const prevButton = slider.querySelector('.wish-slider__control--prev');
        const nextButton = slider.querySelector('.wish-slider__control--next');

        if (!track || !prevButton || !nextButton) {
            return;
        }

        const getScrollAmount = () => {
            const firstCard = track.children[0];
            if (!firstCard) {
                return track.clientWidth;
            }

            const trackStyle = window.getComputedStyle(track);
            const cardGap = parseFloat(trackStyle.columnGap || trackStyle.gap) || 16;
            return firstCard.getBoundingClientRect().width + cardGap;
        };

        const updateButtons = () => {
            const maxScrollLeft = track.scrollWidth - track.clientWidth - 4;
            prevButton.disabled = track.scrollLeft <= 4;
            nextButton.disabled = track.scrollLeft >= maxScrollLeft;
        };

        prevButton.addEventListener('click', () => {
            track.scrollBy({ left: -getScrollAmount(), behavior: 'smooth' });
        });

        nextButton.addEventListener('click', () => {
            track.scrollBy({ left: getScrollAmount(), behavior: 'smooth' });
        });

        track.addEventListener('scroll', updateButtons, { passive: true });
        window.addEventListener('resize', updateButtons);
        updateButtons();
    });
});
