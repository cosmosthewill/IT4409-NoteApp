document.addEventListener("DOMContentLoaded", function () {
    "use strict";

    const content = document.getElementById("content");
    const counter = document.getElementById("content-counter");
    if (!content || !counter) {
        return;
    }

    const updateCounter = function () {
        counter.textContent = `${content.value.length} / ${content.maxLength}`;
    };

    content.addEventListener("input", updateCounter);
    updateCounter();
});
