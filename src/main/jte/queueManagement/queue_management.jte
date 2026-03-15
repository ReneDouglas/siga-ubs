@import br.com.tecsus.sigaubs.dtos.MedicalProceduresTotalDTO
@import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO
@import br.com.tecsus.sigaubs.dtos.ProcedureTypeTotalDTO
@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.entities.Specialty
@import br.com.tecsus.sigaubs.enums.ProcedureType
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@import java.util.List
@param List<BasicHealthUnit> basicHealthUnits
@param List<Specialty> specialties
@param Page<PatientOpenAppointmentDTO> consultasPage
@param Page<PatientOpenAppointmentDTO> examesPage
@param Page<PatientOpenAppointmentDTO> cirurgiasPage
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param List<ProcedureTypeTotalDTO> totalProceduresType = null
@param List<MedicalProceduresTotalDTO> totalMedicalProcedures = null
@param String hide = "hidden"
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Fila de Espera", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-1 bg-slate-100 my-8 flex-col space-y-4">

            <%-- Panel 1 - Filas de espera --%>
            <div class="w-full">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1" class="block bg-gray-700 text-white p-4 cursor-pointer">
                    Filas de espera
                </label>
                <div class="peer-checked:max-h-[2000px] max-h-0 overflow-hidden transition-max-height duration-500 ease-in-out">
                    <div class="p-4 bg-white border border-gray-300">
                        <div class="flex">
                            <form action="/queue-management/search" method="get" class="border border-black w-1/2">
                                <div class="flex w-full justify-start">
                                    <div class="mb-4 p-4">
                                        <label for="ubs" class="inline-flex text-sm font-medium text-gray-700">UBS:</label>
                                        <select id="ubs" name="basicHealthUnit"
                                                class="w-full mt-1 inline-flex p-2 bg-white border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500"
                                                required>
                                            <option value="">Selecione</option>
                                            @for(var ubs : basicHealthUnits)
                                            <option value="${ubs.getId()}" selected="${selectedUBS != null && selectedUBS.equals(ubs.getId())}">${ubs.getName()}</option>
                                            @endfor
                                        </select>
                                    </div>
                                    <div class="mb-4 p-4">
                                        <label for="specialty" class="inline-flex text-sm font-medium text-gray-700">Especialidade:</label>
                                        <select id="specialty" name="specialty"
                                                class="w-full mt-1 inline-flex p-2 bg-white border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500"
                                                required>
                                            <option value="">Selecione</option>
                                            @for(var specialty : specialties)
                                            <option value="${specialty.getId()}" selected="${selectedSpecialty != null && selectedSpecialty.equals(specialty.getId())}">${specialty.getTitle()}</option>
                                            @endfor
                                        </select>
                                    </div>
                                    <div class="ml-4 mt-11 flex justify-start h-1/2">
                                        <button type="submit"
                                                class="px-4 p-1.5 bg-blue-600 text-white font-medium rounded-md shadow-lg hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:ring-offset-2 transition-colors duration-300">
                                            Pesquisar
                                        </button>
                                    </div>
                                </div>
                            </form>
                            <div id="statistics-panel"
                                 class="border border-black w-1/2 grid grid-cols-2 ${totalProceduresType == null || totalProceduresType.isEmpty() ? "hidden" : ""}"
                                 onmouseenter="showStatics(event)" onmouseleave="hideStatics()">
                                @if(totalProceduresType != null)
                                @for(var type : totalProceduresType)
                                <span class="font-semibold text-gray-700 text-right">${type.procedureType().getDescription()}</span>
                                <span class="px-4 py-2 text-gray-700 bg-gray-300 rounded-lg">${type.total()} pacientes</span>
                                @endfor
                                @endif
                            </div>
                        </div>
                        <%-- Abas --%>
                        <div class="flex space-x-4 border-b border-gray-300 mb-4 shadow-xl">
                            <button class="tab-btn w-1/3 py-2 text-center bg-gray-300 hover:bg-gray-300 focus:outline-none"
                                    onclick="showTab('consultas', this)">
                                Consultas
                            </button>
                            <button class="tab-btn w-1/3 py-2 text-center bg-gray-200 hover:bg-gray-300 focus:outline-none"
                                    onclick="showTab('exames', this)">
                                Exames
                            </button>
                            <button class="tab-btn w-1/3 py-2 text-center bg-gray-200 hover:bg-gray-300 focus:outline-none"
                                    onclick="showTab('cirurgias', this)">
                                Cirurgias
                            </button>
                        </div>
                        <%-- Conteúdo das abas --%>
                        <div id="contentPanel">
                            @template.queueManagement.queueFragments.queue_tabs_consultas(consultasPage = consultasPage, selectedUBS = selectedUBS, selectedSpecialty = selectedSpecialty)
                            @template.queueManagement.queueFragments.queue_tabs_exames(examesPage = examesPage, selectedUBS = selectedUBS, selectedSpecialty = selectedSpecialty, hide = hide)
                            @template.queueManagement.queueFragments.queue_tabs_cirurgias(cirurgiasPage = cirurgiasPage, selectedUBS = selectedUBS, selectedSpecialty = selectedSpecialty, hide = hide)
                        </div>
                    </div>
                </div>
            </div>

            <%-- Panel 2 - Histórico --%>
            <div class="w-full">
                <input type="checkbox" id="panel2" class="peer hidden" checked>
                <label for="panel2" class="block bg-gray-700 text-white p-4 cursor-pointer">
                    Histórico
                </label>
                <div class="peer-checked:max-h-[2000px] max-h-0 overflow-hidden transition-max-height duration-500 ease-in-out">
                    <div class="p-4 bg-white border border-gray-300">
                    </div>
                </div>
            </div>

        </div>
        <div id="patientAppointment-modal"></div>
        <div id="default-modal" tabindex="-1" aria-hidden="true"
             class="hidden overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-40 justify-center items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full">
            <div class="relative p-4 w-full max-w-2xl max-h-full">
                <div class="relative bg-white rounded-lg shadow dark:bg-gray-700">
                    <div class="flex items-center justify-between p-4 md:p-5 border-b rounded-t dark:border-gray-600">
                        <h3 class="text-xl font-semibold text-gray-900 dark:text-white">Observações</h3>
                        <button type="button"
                                onclick="this.parentElement.parentElement.parentElement.parentElement.classList.add('hidden')"
                                class="text-gray-400 bg-transparent hover:bg-gray-200 hover:text-gray-900 rounded-lg text-sm w-8 h-8 ms-auto inline-flex justify-center items-center dark:hover:bg-gray-600 dark:hover:text-white"
                                data-modal-hide="default-modal">
                            <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6"/>
                            </svg>
                            <span class="sr-only">Close modal</span>
                        </button>
                    </div>
                    <div class="p-4 md:p-5 space-y-4">
                        <p class="text-base leading-relaxed text-gray-900 dark:text-gray-100" id="modal-observacoes"></p>
                    </div>
                </div>
            </div>
        </div>
        <div id="marcacoes-stats"
             class="hidden absolute z-50 w-1/3 p-6 bg-white rounded-lg shadow-xl border border-gray-200 mt-2">
            <div class="grid grid-cols-2 gap-8">
                <div>
                    <h4 class="text-xl font-bold text-gray-900 mb-4">Exames</h4>
                    <div class="grid grid-cols-2">
                        @if(totalMedicalProcedures != null)
                        @for(var exame : totalMedicalProcedures)
                        @if(exame.type() == ProcedureType.EXAME)
                        <span class="text-gray-700 flex-1">${exame.description()}</span>
                        <span class="text-gray-700 flex-1 text-right">${exame.total()}</span>
                        @endif
                        @endfor
                        @endif
                    </div>
                </div>
                <div>
                    <h4 class="text-xl font-bold text-gray-900 mb-4">Cirurgias</h4>
                    <div class="grid grid-cols-2">
                        @if(totalMedicalProcedures != null)
                        @for(var cirurgia : totalMedicalProcedures)
                        @if(cirurgia.type() == ProcedureType.CIRURGIA)
                        <span class="text-gray-700 flex-1">${cirurgia.description()}</span>
                        <span class="text-gray-700 flex-1 text-right">${cirurgia.total()}</span>
                        @endif
                        @endfor
                        @endif
                    </div>
                </div>
            </div>
        </div>
        <input type="hidden" id="csrfToken" name="${csrf.parameterName()}" value="${csrf.token()}"/>
    </section>
</main>
</body>
<script>
    function showStatics(event) {
        const panel = document.getElementById('statistics-panel');
        if (panel.classList.contains('hidden')) return;
        const modal = document.getElementById('marcacoes-stats');
        modal.classList.remove('hidden');
        let left = event.clientX;
        let top = event.clientY;
        const modalWidth = modal.offsetWidth;
        const modalHeight = modal.offsetHeight;
        if (left + modalWidth > window.innerWidth) left = window.innerWidth - modalWidth - 10;
        if (top + modalHeight > window.innerHeight) top = window.innerHeight - modalHeight - 10;
        modal.style.left = left + "px";
        modal.style.top = top + "px";
    }

    function hideStatics() {
        document.getElementById('marcacoes-stats').classList.add('hidden');
    }

    document.addEventListener('mousemove', function (e) {
        const modal = document.getElementById('marcacoes-stats');
        if (!modal.classList.contains('hidden')) {
            const panel = document.getElementById('statistics-panel');
            if (panel.classList.contains('hidden')) { hideStatics(); return; }
            const panelRect = panel.getBoundingClientRect();
            if (e.clientX < panelRect.left || e.clientX > panelRect.right ||
                e.clientY < panelRect.top || e.clientY > panelRect.bottom) {
                hideStatics();
            }
        }
    });

    function showTab(tabId, clickedBtn) {
        document.querySelectorAll('.tab-content').forEach(function (content) {
            content.classList.add('hidden');
        });
        document.getElementById(tabId).classList.remove('hidden');
        clickedBtn.closest('.flex').querySelectorAll('.tab-btn').forEach(function (btn) {
            btn.classList.remove('bg-gray-300');
            btn.classList.add('bg-gray-200');
        });
        clickedBtn.classList.remove('bg-gray-200');
        clickedBtn.classList.add('bg-gray-300');
    }

    function showModalTab(tabId, clickedBtn) {
        document.getElementById('patient-details').classList.add('hidden');
        document.getElementById('appointment-details').classList.add('hidden');
        document.getElementById('patientHistory-details').classList.add('hidden');
        document.getElementById(tabId).classList.remove('hidden');
        clickedBtn.closest('.flex').querySelectorAll('.modal-tab-btn').forEach(function (btn) {
            btn.classList.remove('bg-slate-300');
            btn.classList.add('bg-slate-200');
        });
        clickedBtn.classList.remove('bg-slate-200');
        clickedBtn.classList.add('bg-slate-300');
    }

    function openModal(observacoes) {
        let modal = document.getElementById("default-modal");
        modal.querySelector("#modal-observacoes").innerText = observacoes;
        modal.classList.remove("hidden");
        modal.classList.add("flex");
        modal.classList.add("opacity-100");
    }

    function closeModal() {
        let modal = document.getElementById("patientAppointment-modal");
        modal.classList.add("opacity-0");
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }

    function handleContemplation() {
        let reasonText = "";
        Swal.fire({
            title: "Digite o motivo da contemplação",
            input: "textarea",
            inputPlaceholder: "Motivo...",
            showCancelButton: true,
            confirmButtonText: "Enviar",
            cancelButtonText: "Cancelar",
            inputAttributes: { maxlength: "255" },
            inputValidator: (value) => {
                if (!value) return "Você precisa digitar um motivo.";
                reasonText = value;
            }
        }).then(function (result) {
            if (result.isConfirmed) {
                Swal.fire({
                    title: "Digite a sua senha",
                    input: "password",
                    showCancelButton: true,
                    confirmButtonText: "Enviar",
                    cancelButtonText: "Cancelar",
                    inputValidator: (value) => {
                        if (!value) return "Você precisa digitar a senha.";
                        return new Promise((resolve) => {
                            const formData = new URLSearchParams();
                            formData.append('password', value);
                            formData.append(document.getElementById('csrfToken').name, document.getElementById('csrfToken').value);
                            fetch('/systemUser-management/validate', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                body: formData
                            }).then(response => {
                                if (response.ok) resolve();
                                else response.text().then(errorText => resolve(errorText));
                            }).catch(error => resolve('Erro ao validar a senha. Tente novamente.'));
                        });
                    }
                }).then(function (passwordResult) {
                    if (passwordResult.isConfirmed) {
                        document.getElementById("reason").value = reasonText;
                        document.getElementById("contemplationForm").submit();
                    }
                });
            }
        });
    }

    function handleConfirmation() {
        Swal.fire({
            title: "Confirmar contemplação",
            text: "Tem certeza que deseja continuar?",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Sim",
            cancelButtonText: "Cancelar"
        }).then(function (result) {
            if (result.isConfirmed) {
                document.getElementById("confirmForm").submit();
            }
        });
    }

    document.addEventListener("htmx:confirm", function (evt) {
        if (!evt.detail.target.hasAttribute('hx-confirm')) return;
        evt.preventDefault();
        Swal.fire({
            title: "Cancelar contemplação",
            text: evt.detail.question,
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Sim",
            cancelButtonText: "Cancelar"
        }).then(function (result) {
            if (result.isConfirmed) {
                Swal.fire({
                    title: "Digite o motivo do cancelamento",
                    input: "textarea",
                    inputPlaceholder: "Motivo...",
                    showCancelButton: true,
                    confirmButtonText: "Enviar",
                    cancelButtonText: "Cancelar",
                    inputAttributes: { maxlength: "255" },
                    inputValidator: (value) => { if (!value) return "Você precisa digitar um motivo!" }
                }).then(function (textResult) {
                    if (textResult.isConfirmed && textResult.value) {
                        evt.detail.issueRequest(true);
                    }
                });
            }
        });
    });
</script>
</html>
