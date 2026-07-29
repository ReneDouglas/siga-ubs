(function () {
    "use strict";

    const byId = (id) => document.getElementById(id);

    function csrfToken() {
        return byId("csrfToken") || document.querySelector('input[name="_csrf"]');
    }

    function ensureCsrf(form) {
        if (!form || (form.method || "").toLowerCase() === "get") return;
        const token = csrfToken();
        if (!token || form.querySelector(`input[name="${CSS.escape(token.name)}"]`)) return;
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = token.name;
        input.value = token.value;
        form.appendChild(input);
    }

    function submitForm(id) {
        const form = byId(id);
        if (!form) return;
        ensureCsrf(form);
        form.submit();
    }

    function closeModal(id) {
        const modal = byId(id);
        if (!modal) return;
        modal.classList.add("hidden", "opacity-0");
        modal.classList.remove("flex", "opacity-100");
    }

    function showObservation(button) {
        const row = button.closest("tr") || button.parentElement;
        const source = row ? row.querySelector("[data-observation-source]") : null;
        const modal = byId("default-modal");
        const destination = modal ? modal.querySelector("#modal-observacoes") : null;
        if (!modal || !destination) return;
        destination.textContent = source ? source.textContent : "";
        modal.classList.remove("hidden", "opacity-0");
        modal.classList.add("flex", "opacity-100");
    }

    function showTab(button, modal) {
        const target = byId(button.dataset.tabTarget);
        if (!target) return;
        const contentSelector = modal
            ? "#patient-details, #appointment-details, #status-details, #patientHistory-details"
            : ".tab-content";
        document.querySelectorAll(contentSelector).forEach((content) => content.classList.add("hidden"));
        target.classList.remove("hidden");

        const selector = modal ? ".modal-tab-btn" : ".tab-btn";
        const group = button.closest(".flex") || document;
        const grayTheme = button.classList.contains("bg-gray-200") || button.classList.contains("bg-gray-300");
        const selected = grayTheme ? "bg-gray-300" : "bg-slate-300";
        const idle = grayTheme ? "bg-gray-200" : "bg-slate-200";
        group.querySelectorAll(selector).forEach((tab) => {
            tab.classList.remove(selected);
            tab.classList.add(idle);
        });
        button.classList.remove(idle);
        button.classList.add(selected);
    }

    function confirmation(title, text) {
        return Swal.fire({
            title,
            text,
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Sim",
            cancelButtonText: "Cancelar"
        });
    }

    async function confirmForm(button) {
        const result = await confirmation("Confirmar contemplação", "Tem certeza que deseja continuar?");
        if (result.isConfirmed) submitForm(button.dataset.formId);
    }

    async function cancelContemplation(button) {
        const result = await confirmation("Cancelar contemplação", "Tem certeza que deseja cancelar?");
        if (!result.isConfirmed) return;
        const reason = await Swal.fire({
            title: "Digite o motivo do cancelamento",
            input: "textarea",
            inputPlaceholder: "Motivo...",
            showCancelButton: true,
            confirmButtonText: "Enviar",
            cancelButtonText: "Cancelar",
            inputAttributes: {maxlength: "500"},
            inputValidator: (value) => value && value.trim() ? undefined : "Você precisa digitar um motivo!"
        });
        if (!reason.isConfirmed || !reason.value) return;
        const form = byId(button.dataset.formId);
        const input = form ? form.querySelector('[name="reason"]') : null;
        if (input) input.value = reason.value.trim();
        submitForm(button.dataset.formId);
    }

    async function manualContemplation(button) {
        const form = byId(button.dataset.formId);
        if (!form) return;
        const reason = await Swal.fire({
            title: "Digite o motivo da contemplação",
            input: "textarea",
            inputPlaceholder: "Motivo...",
            showCancelButton: true,
            confirmButtonText: "Enviar",
            cancelButtonText: "Cancelar",
            inputAttributes: {maxlength: "500"},
            inputValidator: (value) => value && value.trim() ? undefined : "Você precisa digitar um motivo."
        });
        if (!reason.isConfirmed) return;

        const password = await Swal.fire({
            title: "Digite a sua senha",
            input: "password",
            showCancelButton: true,
            confirmButtonText: "Enviar",
            cancelButtonText: "Cancelar",
            inputValidator: async (value) => {
                if (!value) return "Você precisa digitar a senha.";
                const token = csrfToken();
                const body = new URLSearchParams({
                    password: value,
                    action: "MANUAL_CONTEMPLATION",
                    objectId: form.querySelector('[name="appointmentId"]').value
                });
                if (token) body.append(token.name, token.value);
                try {
                    const response = await fetch("/systemUser-management/validate", {
                        method: "POST",
                        headers: {"Content-Type": "application/x-www-form-urlencoded"},
                        body
                    });
                    return response.ok ? undefined : (await response.text() || "Senha inválida.");
                } catch (_) {
                    return "Erro ao validar a senha. Tente novamente.";
                }
            }
        });
        if (!password.isConfirmed) return;
        form.querySelector('[name="reason"]').value = reason.value.trim();
        submitForm(button.dataset.formId);
    }

    function tenantSwalClasses(confirmButtonClass) {
        return {
            popup: "rounded-lg",
            title: "text-slate-800",
            confirmButton: confirmButtonClass || "text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300",
            cancelButton: "text-sm inline-block rounded-lg bg-slate-100 py-2.5 px-6 text-slate-700 cursor-pointer hover:bg-slate-200 transition duration-300 ml-2"
        };
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    async function changeTenantSlug(button) {
        const currentSlug = button.dataset.tenantSlug || "";
        const result = await Swal.fire({
            title: "Alterar slug do tenant",
            icon: "warning",
            width: "42rem",
            buttonsStyling: false,
            showCancelButton: true,
            confirmButtonText: "Alterar slug",
            cancelButtonText: "Cancelar",
            customClass: tenantSwalClasses(),
            html: `<div class="text-left text-sm text-slate-700">
                <div class="mb-4 rounded-lg bg-slate-100 px-4 py-3">
                    <span class="block text-xs font-semibold uppercase tracking-wide text-slate-500">Slug atual</span>
                    <span class="mt-1 inline-flex rounded-lg bg-white px-3 py-1 font-mono text-sm text-slate-900">${escapeHtml(currentSlug)}</span>
                </div>
                <div class="mb-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-amber-800">
                    <span class="mb-1 block font-semibold">Atenção antes de alterar</span>
                    <span class="block">A URL será alterada imediatamente e as sessões ativas serão encerradas.</span>
                </div>
                <label for="swalNewSlug" class="mb-1 block font-semibold">Novo slug</label>
                <input id="swalNewSlug" class="swal2-input !mx-0 !mt-0 !mb-3 !h-auto !w-full rounded-lg px-4 py-2 text-sm" placeholder="exemplo-tenant">
                <label for="swalSlugConfirmation" class="mb-1 block font-semibold">Confirme o novo slug</label>
                <input id="swalSlugConfirmation" class="swal2-input !mx-0 !mt-0 !mb-0 !h-auto !w-full rounded-lg px-4 py-2 text-sm" placeholder="repita o novo slug">
            </div>`,
            focusConfirm: false,
            preConfirm: () => {
                const newSlug = byId("swalNewSlug").value.trim();
                const confirmationValue = byId("swalSlugConfirmation").value.trim();
                if (!/^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/.test(newSlug)) {
                    Swal.showValidationMessage("Use apenas letras minúsculas, números e hífen.");
                    return false;
                }
                if (newSlug !== confirmationValue || newSlug === currentSlug) {
                    Swal.showValidationMessage("A confirmação deve coincidir e o slug deve ser diferente.");
                    return false;
                }
                return {newSlug, confirmationValue};
            }
        });
        if (!result.isConfirmed) return;
        byId("slugTenantId").value = button.dataset.tenantId;
        byId("newSlug").value = result.value.newSlug;
        byId("slugConfirmation").value = result.value.confirmationValue;
        submitForm("tenantSlugForm");
    }

    async function updateTenantState(button, maintenance) {
        const result = await Swal.fire({
            title: maintenance ? "Colocar tenant em manutenção" : "Desabilitar tenant",
            icon: "warning",
            width: "40rem",
            buttonsStyling: false,
            showCancelButton: true,
            confirmButtonText: maintenance ? "Ativar manutenção" : "Desabilitar",
            cancelButtonText: "Cancelar",
            customClass: tenantSwalClasses(maintenance ? undefined :
                "text-sm inline-block rounded-lg bg-red-600 py-2.5 px-6 text-white cursor-pointer hover:bg-red-500"),
            input: "textarea",
            inputLabel: maintenance ? "Mensagem de manutenção" : "Motivo da desabilitação",
            inputAttributes: {maxlength: "500"},
            showLoaderOnConfirm: true
        });
        if (!result.isConfirmed) return;
        const prefix = maintenance ? "maintenance" : "disable";
        byId(`${prefix}TenantId`).value = button.dataset.tenantId;
        byId(maintenance ? "maintenanceMessage" : "disabledReason").value = (result.value || "").trim();
        submitForm(maintenance ? "tenantMaintenanceForm" : "tenantDisableForm");
    }

    function toggleSidebar() {
        const sidebar = document.querySelector("[data-sidebar]");
        const backdrop = byId("sidebar-backdrop");
        if (!sidebar) return;
        sidebar.classList.toggle("hidden");
        sidebar.classList.toggle("flex");
        if (backdrop) backdrop.classList.toggle("hidden");
    }

    function addSpecialtyProcedure() {
        const nameInput = byId("procedure");
        const typeSelect = byId("procedureType");
        const table = byId("procedureTable");
        if (!nameInput || !typeSelect || !table) return;
        const name = nameInput.value.trim();
        const type = typeSelect.options[typeSelect.selectedIndex]?.text || "";
        if (!name || !typeSelect.value) {
            window.alert("Preencha o procedimento e o tipo antes de adicionar.");
            return;
        }
        const row = document.createElement("tr");
        [name, type, ""].forEach((textValue) => {
            const cell = document.createElement("td");
            cell.className = "border-b text-sm py-2 pl-4";
            cell.textContent = textValue;
            row.appendChild(cell);
        });
        table.querySelector("tbody").appendChild(row);
        const form = table.closest("form");
        const items = JSON.parse(form.dataset.procedures || "[]");
        items.push({description: name, procedureType: type});
        form.dataset.procedures = JSON.stringify(items);
        nameInput.value = "";
        nameInput.disabled = false;
        typeSelect.selectedIndex = 0;
    }

    function initializePage() {
        const selectedMonth = byId("selectedMonth");
        const monthControl = document.querySelector('input[type="month"]');
        if (monthControl && !monthControl.value) {
            const now = new Date();
            monthControl.value = selectedMonth?.value ||
                `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
        }

        const specialty = byId("specialty");
        if (specialty) specialty.addEventListener("change", () => {
            const procedureType = byId("procedureType");
            const medicalProcedure = byId("medicalProcedure");
            const priority = byId("priority");
            if (procedureType) {
                procedureType.disabled = !specialty.value;
                procedureType.value = "";
            }
            [medicalProcedure, priority].forEach((select) => {
                if (!select) return;
                select.disabled = true;
                select.replaceChildren(new Option("Selecione", ""));
            });
        });

        const procedureType = byId("procedureType");
        const procedureName = byId("procedure");
        if (procedureType && procedureName && byId("addProcedureBtn")) {
            procedureType.addEventListener("change", () => {
                const consultation = procedureType.value === "Consulta";
                procedureName.value = consultation ? "-" : "";
                procedureName.disabled = consultation;
            });
        }

        const sidebar = document.querySelector("[data-sidebar]");
        if (sidebar) {
            sidebar.addEventListener("mouseenter", () => {
                if (!window.matchMedia("(min-width: 768px)").matches) return;
                sidebar.classList.replace("w-16", "w-64");
                sidebar.querySelectorAll("[data-sidebar-label]").forEach((label) => label.classList.remove("hidden"));
            });
            sidebar.addEventListener("mouseleave", () => {
                if (!window.matchMedia("(min-width: 768px)").matches) return;
                sidebar.classList.replace("w-64", "w-16");
                sidebar.querySelectorAll("[data-sidebar-label]").forEach((label) => label.classList.add("hidden"));
            });
        }
    }

    document.addEventListener("click", (event) => {
        const button = event.target.closest("[data-action]");
        if (!button) {
            if (!event.target.closest("#dropdown") && !event.target.closest("#patientSearch") &&
                !event.target.closest("#systemUserSearch")) {
                byId("dropdown")?.classList.add("hidden");
            }
            return;
        }
        const action = button.dataset.action;
        if (action === "toggle-sidebar") toggleSidebar();
        if (action === "remove-parent") button.parentElement?.remove();
        if (action === "dismiss-alert") button.closest('[role="alert"]')?.remove();
        if (action === "close-modal") closeModal(button.dataset.modalId);
        if (action === "show-observation") showObservation(button);
        if (action === "show-tab") showTab(button, false);
        if (action === "show-modal-tab") showTab(button, true);
        if (action === "confirm-form") confirmForm(button);
        if (action === "cancel-contemplation") cancelContemplation(button);
        if (action === "manual-contemplation") manualContemplation(button);
        if (action === "autocomplete-patient") {
            const input = byId("patientSearch");
            if (input) input.value = button.textContent.trim();
            byId("dropdown")?.classList.add("hidden");
        }
        if (action === "tenant-slug") changeTenantSlug(button);
        if (action === "tenant-maintenance") updateTenantState(button, true);
        if (action === "tenant-disable") updateTenantState(button, false);
        if (action === "save-medical-slots") {
            confirmation("Confirmação", "Você deseja salvar as alterações?")
                .then((result) => result.isConfirmed && submitForm(button.dataset.formId));
        }
        if (action === "previous-page" && window.goToPreviousPage) window.goToPreviousPage(event);
        if (action === "next-page" && window.goToNextPage) window.goToNextPage(event);
    });

    document.addEventListener("input", (event) => {
        if (event.target.matches("#patientSearch, #systemUserSearch")) {
            byId("dropdown")?.classList.remove("hidden");
        }
    });

    document.addEventListener("submit", (event) => {
        ensureCsrf(event.target);
        const procedures = event.target.querySelector("#proceduresJson");
        if (procedures) procedures.value = event.target.dataset.procedures || "[]";
    }, true);

    document.addEventListener("click", (event) => {
        if (event.target.closest("#addProcedureBtn")) addSpecialtyProcedure();
    });

    document.addEventListener("htmx:afterRequest", (event) => {
        const form = event.detail.elt?.closest?.("[data-reset-after-request]");
        if (!form || event.detail.elt.tagName === "SELECT" || !event.detail.successful) return;
        form.reset();
        const month = form.querySelector('input[type="month"]');
        if (month) {
            const now = new Date();
            month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
        }
    });

    document.addEventListener("mouseover", (event) => {
        const container = event.target.closest("[data-tooltip-container]");
        if (!container) return;
        container.querySelector("[data-tooltip]")?.classList.remove("hidden");
    });
    document.addEventListener("mousemove", (event) => {
        const container = event.target.closest("[data-tooltip-container]");
        const tooltip = container?.querySelector("[data-tooltip]");
        if (!tooltip) return;
        tooltip.style.left = `${event.clientX + 270 > innerWidth ? event.clientX - 270 : event.clientX + 10}px`;
        tooltip.style.top = `${event.clientY + 230 > innerHeight ? event.clientY - 230 : event.clientY + 10}px`;
    });
    document.addEventListener("mouseout", (event) => {
        const container = event.target.closest("[data-tooltip-container]");
        if (container && !container.contains(event.relatedTarget)) {
            container.querySelector("[data-tooltip]")?.classList.add("hidden");
        }
    });

    document.addEventListener("DOMContentLoaded", initializePage);
})();
