(function () {
    "use strict";

    const storageKey = "note-app-theme";
    const systemQuery = window.matchMedia("(prefers-color-scheme: dark)");

    function selectedTheme() {
        const stored = localStorage.getItem(storageKey);
        return ["light", "dark", "system"].includes(stored) ? stored : "system";
    }

    function resolvedTheme(theme) {
        return theme === "system" ? (systemQuery.matches ? "dark" : "light") : theme;
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute("data-bs-theme", resolvedTheme(theme));
        document.documentElement.dataset.themePreference = theme;
    }

    function updateSelection(theme) {
        document.querySelectorAll("[data-theme-value]").forEach((button) => {
            const active = button.dataset.themeValue === theme;
            button.classList.toggle("active", active);
            button.setAttribute("aria-pressed", String(active));
        });
    }

    applyTheme(selectedTheme());

    document.addEventListener("DOMContentLoaded", function () {
        updateSelection(selectedTheme());
        document.addEventListener("click", function (event) {
            const option = event.target.closest("[data-theme-value]");
            if (!option) {
                return;
            }
            const theme = option.dataset.themeValue;
            localStorage.setItem(storageKey, theme);
            applyTheme(theme);
            updateSelection(theme);
        });
    });

    systemQuery.addEventListener("change", function () {
        if (selectedTheme() === "system") {
            applyTheme("system");
        }
    });
})();
