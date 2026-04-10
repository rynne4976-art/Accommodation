document.addEventListener('DOMContentLoaded', () => {
    const tabNav = document.querySelector('.accom-detail-tab-nav');
    const tabLinks = Array.from(document.querySelectorAll('.accom-detail-tab-nav .tab-nav-link'));
    const scrollTopButton = document.querySelector('[data-scroll-top]');

    if (!tabNav || tabLinks.length === 0) {
        return;
    }

    const getScrollOffset = () => tabNav.offsetHeight + 28;

    tabLinks.forEach((link) => {
        link.addEventListener('click', (event) => {
            const targetId = link.getAttribute('href');
            if (!targetId || !targetId.startsWith('#')) {
                return;
            }

            const section = document.querySelector(targetId);
            if (!section) {
                return;
            }

            event.preventDefault();

            const targetTop = section.getBoundingClientRect().top + window.scrollY - getScrollOffset();
            window.scrollTo({
                top: Math.max(targetTop, 0),
                behavior: 'smooth'
            });
        });
    });

    if (scrollTopButton) {
        scrollTopButton.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }
});
