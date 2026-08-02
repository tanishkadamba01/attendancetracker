// ==========================================================================
// ATTENDANCE TRACKER LANDING PAGE INTERACTIVE JAVASCRIPT
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
    
    // ── 1. Mobile Menu Toggle ──
    const mobileBtn = document.getElementById('mobileMenuBtn');
    const mobileMenu = document.getElementById('mobileMenu');

    if (mobileBtn && mobileMenu) {
        mobileBtn.addEventListener('click', () => {
            mobileMenu.classList.toggle('active');
            const icon = mobileBtn.querySelector('i');
            if (icon) {
                if (mobileMenu.classList.contains('active')) {
                    icon.className = 'fa-solid fa-xmark';
                } else {
                    icon.className = 'fa-solid fa-bars';
                }
            }
        });

        // Close mobile menu when clicking a link
        document.querySelectorAll('.mobile-link').forEach(link => {
            link.addEventListener('click', () => {
                mobileMenu.classList.remove('active');
                const icon = mobileBtn.querySelector('i');
                if (icon) icon.className = 'fa-solid fa-bars';
            });
        });
    }

    // ── 2. Interactive Preview Tabs ──
    const tabBtns = document.querySelectorAll('.tab-btn');
    const panels = document.querySelectorAll('.showcase-panel');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');

            // Deactivate all tabs
            tabBtns.forEach(b => b.classList.remove('active'));
            panels.forEach(p => p.classList.remove('active'));

            // Activate target tab & panel
            btn.classList.add('active');
            const targetPanel = document.getElementById(targetId);
            if (targetPanel) {
                targetPanel.classList.add('active');
            }
        });
    });

    // ── 3. 3D Tilt Card Effect on Hero Phone ──
    const heroPhone = id => document.getElementById(id);
    const phoneCard = heroPhone('heroPhone');

    if (phoneCard) {
        document.addEventListener('mousemove', (e) => {
            const windowWidth = window.innerWidth;
            const windowHeight = window.innerHeight;
            
            if (windowWidth < 992) return; // Only apply on desktop screens

            const mouseX = e.clientX - windowWidth / 2;
            const mouseY = e.clientY - windowHeight / 2;

            const rotateX = (mouseY / windowHeight) * -12;
            const rotateY = (mouseX / windowWidth) * 12;

            phoneCard.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
        });
    }

    // ── 4. Scroll Reveal Animations ──
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const revealObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    const animatedElements = document.querySelectorAll('.feature-card, .preview-showcase, .comparison-container, .download-box');
    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'all 0.6s cubic-bezier(0.16, 1, 0.3, 1)';
        revealObserver.observe(el);
    });
});
