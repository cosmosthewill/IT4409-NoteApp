document.addEventListener("DOMContentLoaded", function () {
    "use strict";

    const app = document.getElementById("notes-app");
    const form = document.getElementById("notes-filter-form");
    const search = document.getElementById("note-search");
    const category = document.getElementById("category-filter");
    const grid = document.getElementById("note-grid");
    const emptyState = document.getElementById("empty-state");
    const emptyTitle = document.getElementById("empty-state-title");
    const emptyText = document.getElementById("empty-state-text");
    const emptyCreateAction = document.getElementById("empty-create-action");
    const loadMoreWrap = document.getElementById("load-more-wrap");
    const loadMore = document.getElementById("load-more");
    const loadingIndicator = document.getElementById("loading-indicator");
    const searchSpinner = document.getElementById("search-spinner");
    const loadError = document.getElementById("load-error");
    const retryLoad = document.getElementById("retry-load");
    const sentinel = document.getElementById("scroll-sentinel");
    const resultsStatus = document.getElementById("results-status");
    let notesFragment = grid?.querySelector(".notes-fragment");

    if (!app || !form || !search || !category || !grid || !notesFragment) {
        return;
    }

    let currentPage = Number(notesFragment.dataset.page || 0);
    let hasNext = notesFragment.dataset.hasNext === "true";
    let isLoading = false;
    let activeController = null;
    let debounceTimer = null;
    let lastFailedAction = null;

    function hasFilters() {
        return search.value.trim() !== "" || category.value !== "";
    }

    function setLoading(loading, replacing) {
        isLoading = loading;
        loadingIndicator.classList.toggle("d-none", !loading || replacing);
        searchSpinner.classList.toggle("d-none", !loading || !replacing);
        loadMore.disabled = loading;
    }

    function updateEmptyState(cardCount) {
        const isEmpty = cardCount === 0;
        emptyState.classList.toggle("d-none", !isEmpty);
        if (!isEmpty) {
            return;
        }

        const filtered = hasFilters();
        emptyTitle.textContent = filtered ? app.dataset.noResultsTitle : app.dataset.emptyTitle;
        emptyText.textContent = filtered ? app.dataset.noResultsText : app.dataset.emptyText;
        emptyCreateAction.classList.toggle("d-none", filtered);
    }

    function updateLoadControls() {
        loadMoreWrap.classList.toggle("d-none", !hasNext || isLoading);
    }

    function syncAddressBar() {
        const url = new URL(window.location.href);
        const query = search.value.trim();
        if (query) {
            url.searchParams.set("q", query);
        } else {
            url.searchParams.delete("q");
        }
        if (category.value) {
            url.searchParams.set("category", category.value);
        } else {
            url.searchParams.delete("category");
        }
        url.searchParams.delete("page");
        window.history.replaceState({}, "", url);
    }

    function endpoint(page) {
        const url = new URL("/notes/fragments", window.location.origin);
        const query = search.value.trim();
        if (query) {
            url.searchParams.set("q", query);
        }
        if (category.value) {
            url.searchParams.set("category", category.value);
        }
        url.searchParams.set("page", String(page));
        return url;
    }

    async function loadPage(page, replace) {
        if (isLoading && !replace) {
            return;
        }

        if (replace && activeController) {
            activeController.abort();
        }

        const controller = new AbortController();
        activeController = controller;
        setLoading(true, replace);
        loadError.classList.add("d-none");
        lastFailedAction = {page, replace};

        try {
            const response = await fetch(endpoint(page), {
                signal: controller.signal,
                headers: {"X-Requested-With": "XMLHttpRequest"}
            });
            if (!response.ok) {
                throw new Error(`Request failed with ${response.status}`);
            }

            const html = await response.text();
            const documentFragment = new DOMParser().parseFromString(html, "text/html");
            const fragment = documentFragment.querySelector(".notes-fragment");
            if (!fragment) {
                throw new Error("Missing note fragment");
            }

            const cards = Array.from(fragment.querySelectorAll(".note-card-column"));
            if (replace) {
                grid.replaceChildren(fragment);
                notesFragment = fragment;
            } else {
                cards.forEach((card) => notesFragment.appendChild(card));
            }

            currentPage = Number(fragment.dataset.page || page);
            hasNext = fragment.dataset.hasNext === "true";
            const totalCards = notesFragment.querySelectorAll(".note-card-column").length;
            updateEmptyState(totalCards);
            resultsStatus.textContent = String(totalCards);
            syncAddressBar();
            lastFailedAction = null;
        } catch (error) {
            if (error.name !== "AbortError") {
                loadError.classList.remove("d-none");
                resultsStatus.textContent = app.dataset.loadError;
            }
        } finally {
            if (activeController === controller) {
                activeController = null;
                setLoading(false, replace);
                updateLoadControls();
            }
        }
    }

    function resetAndLoad() {
        currentPage = 0;
        hasNext = false;
        loadPage(0, true);
    }

    search.addEventListener("input", function () {
        window.clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(resetAndLoad, 300);
    });

    category.addEventListener("change", function () {
        window.clearTimeout(debounceTimer);
        resetAndLoad();
    });

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        window.clearTimeout(debounceTimer);
        resetAndLoad();
    });

    loadMore.addEventListener("click", function () {
        if (hasNext) {
            loadPage(currentPage + 1, false);
        }
    });

    retryLoad.addEventListener("click", function () {
        if (lastFailedAction) {
            loadPage(lastFailedAction.page, lastFailedAction.replace);
        }
    });

    if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver((entries) => {
            if (entries.some((entry) => entry.isIntersecting) && hasNext && !isLoading) {
                loadPage(currentPage + 1, false);
            }
        }, {rootMargin: "240px 0px"});
        observer.observe(sentinel);
    }

    updateLoadControls();
});
