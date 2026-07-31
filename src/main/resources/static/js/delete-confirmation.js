document.addEventListener("DOMContentLoaded", function () {
    "use strict";

    const modalElement = document.getElementById("deleteNoteModal");
    const form = document.getElementById("deleteNoteForm");
    const message = document.getElementById("deleteNoteMessage");

    if (!modalElement || !form || !message || typeof bootstrap === "undefined") {
        return;
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    const template = modalElement.dataset.messageTemplate || "Delete “{0}”?";

    document.addEventListener("click", function (event) {
        const trigger = event.target.closest("[data-delete-note]");
        if (!trigger) {
            return;
        }

        form.action = trigger.dataset.deleteUrl;
        message.textContent = template.replace("{0}", trigger.dataset.noteTitle || "");
        modal.show();
    });
});
