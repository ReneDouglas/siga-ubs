@import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO
@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.entities.MedicalProcedure
@import br.com.tecsus.sigaubs.entities.Specialty
@import br.com.tecsus.sigaubs.enums.ProcedureType
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@import java.util.List
@param Page<PatientOpenAppointmentDTO> queuePage
@param List<BasicHealthUnit> basicHealthUnits
@param List<Specialty> specialties
@param List<MedicalProcedure> procedures
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param Long selectedMedicalProcedure = null
@param String selectedProcedureType = null
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
        <div class="flex flex-col mx-4 justify-start items-center">
            @template.fragments.loading()

            <%-- Panel 1 - Filas de espera --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1"
                    class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">patient_list</span>Filas de espera
                    </div>
                </label>
                <div
                    class="flex flex-col justify-center peer-checked:max-h-[2000px] max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div class="py-4 px-4">
                        <form hx-get="/queue-management/v2/search" method="get"
                            hx-target="#queue-datatable" hx-swap="outerHTML" hx-indicator=".animate-pulse">
                            <div
                                class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2 gap-x-4 w-full items-end">
                                <div class="flex flex-col w-full">
                                    <label class="text-sm" for="ubs">UBS:</label>
                                    @if(secHelper.hasAnyRole("ADMIN", "SMS"))
                                    <%-- Select editável para ADMIN/SMS --%>
                                    <select
                                        class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="ubs" name="basicHealthUnit">
                                        <option value="">Selecione</option>
                                        @for(var ubs : basicHealthUnits)
                                        <option value="${ubs.getId()}" selected="${ubs.getId().equals(selectedUBS)}">${ubs.getName()}</option>
                                        @endfor
                                    </select>
                                    @else
                                    <%-- Select desabilitado para demais roles --%>
                                    <select
                                        class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none bg-slate-100 cursor-not-allowed"
                                        id="ubs" disabled>
                                        @for(var ubs : basicHealthUnits)
                                        <option value="${ubs.getId()}" selected="${ubs.getId().equals(selectedUBS)}">${ubs.getName()}</option>
                                        @endfor
                                    </select>
                                    <%-- Hidden input para garantir submissão do valor nos requests HTMX --%>
                                    <input type="hidden" name="basicHealthUnit" value="${selectedUBS}"/>
                                    @endif
                                </div>
                                <div class="flex flex-col w-full">
                                    <label class="text-sm" for="specialty">Especialidade:</label>
                                    <select
                                        class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="specialty" name="specialty">
                                        <option value="">Selecione</option>
                                        @for(var specialty : specialties)
                                        <option value="${specialty.getId()}" selected="${specialty.getId().equals(selectedSpecialty)}">${specialty.getTitle()}</option>
                                        @endfor
                                    </select>
                                </div>
                                <div class="flex flex-col w-full">
                                    <label class="text-sm" for="procedureType">Marcação:</label>
                                    <select
                                        class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="procedureType" name="procedureType"
                                        hx-get="/queue-management/v2/procedures" hx-target="#selectMedicalProcedure"
                                        hx-include="[name='specialty']" hx-swap="outerHTML"
                                        disabled="${selectedProcedureType == null}">
                                        <option value="">Selecione</option>
                                        @for(var type : ProcedureType.values())
                                        <option value="${type.toString()}" selected="${type.toString().equals(selectedProcedureType)}">${type.getDescription()}</option>
                                        @endfor
                                    </select>
                                </div>
                                @template.queueManagement.queueFragments.medical_procedures_dropdown(procedures = procedures, selectedMedicalProcedure = selectedMedicalProcedure)
                                <div class="flex items-center sm:items-end space-x-2">
                                    <button type="submit"
                                        class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                        <span class="-ml-1 material-symbols-outlined mx-1">search</span>Pesquisar
                                    </button>
                                    <a href="/queue-management/v2/clear"
                                        class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">
                                        <span class="-ml-1 material-symbols-outlined mx-1">close</span>Limpar
                                    </a>
                                </div>
                            </div>
                        </form>
                        @template.queueManagement.queueFragments.queue_datatable(
                            queuePage = queuePage,
                            secHelper = secHelper,
                            selectedUBS = selectedUBS,
                            selectedSpecialty = selectedSpecialty,
                            selectedMedicalProcedure = selectedMedicalProcedure,
                            selectedProcedureType = selectedProcedureType
                        )
                    </div>
                </div>
            </div>

        </div>
    </section>
    <div id="patientAppointment-modal"></div>
    <div id="default-modal" tabindex="-1" aria-hidden="true"
        class="hidden overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-40 justify-center items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full">
        <div class="relative p-4 w-full max-w-2xl max-h-full">
            <div class="relative bg-white rounded-lg shadow">
                <div
                    class="flex items-center justify-between p-4 md:p-5 bg-slate-900 rounded-t border-2 border-slate-600">
                    <h3 class="text-xl font-semibold text-slate-200">
                        Observações
                    </h3>
                    <button type="button"
                        onclick="this.parentElement.parentElement.parentElement.parentElement.classList.add('hidden')"
                        class="text-slate-400 bg-transparent hover:bg-slate-700 hover:text-white rounded-lg text-sm w-8 h-8 ms-auto inline-flex justify-center items-center">
                        <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none"
                            viewBox="0 0 14 14">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                stroke-width="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6" />
                        </svg>
                        <span class="sr-only">Close modal</span>
                    </button>
                </div>
                <div class="p-4 md:p-5 space-y-4">
                    <p class="text-base leading-relaxed text-gray-900" id="modal-observacoes"></p>
                </div>
            </div>
        </div>
    </div>
    <div id="marcacoes-stats"
        class="hidden absolute z-50 w-1/3 p-6 bg-white rounded-lg shadow-xl border border-gray-200 mt-2">
    </div>
    <input type="hidden" id="csrfToken" name="${csrf.parameterName()}" value="${csrf.token()}"/>
</main>
</body>
<script>

    $('#specialty').change(function () {
        if ($(this).val()) {
            $('#procedureType').prop('disabled', false).val(null);
            $('#medicalProcedure').prop('disabled', true).empty().append('<option value="">Selecione</option>');
        } else {
            $('#procedureType').prop('disabled', true).val(null);
        }
    });

    function showModalTab(tabId, clickedBtn) {
        document.getElementById('patient-details').classList.add('hidden');
        document.getElementById('appointment-details').classList.add('hidden');
        const patientHistoryEl = document.getElementById('patientHistory-details');
        if (patientHistoryEl) patientHistoryEl.classList.add('hidden');
        const statusEl = document.getElementById('status-details');
        if (statusEl) statusEl.classList.add('hidden');
        document.getElementById(tabId).classList.remove('hidden');

        if (clickedBtn) {
            clickedBtn.closest('.flex').querySelectorAll('.modal-tab-btn').forEach(function (btn) {
                btn.classList.remove('bg-slate-300');
                btn.classList.add('bg-slate-200');
            });
            clickedBtn.classList.remove('bg-slate-200');
            clickedBtn.classList.add('bg-slate-300');
        }
    }

    function openModal(observacoes) {
        let modal = document.getElementById("default-modal");
        let modalObservacoes = modal.querySelector("#modal-observacoes");
        modalObservacoes.innerText = observacoes;
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
            inputAttributes: {
                maxlength: "255"
            },
            inputValidator: (value) => {
                if (!value) {
                    return "Você precisa digitar um motivo.";
                }
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
                        if (!value) {
                            return "Você precisa digitar a senha.";
                        }
                        return new Promise((resolve) => {
                            const formData = new URLSearchParams();
                            formData.append('password', value);
                            formData.append(
                                document.getElementById('csrfToken').name,
                                document.getElementById('csrfToken').value
                            );
                            fetch('/systemUser-management/validate', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                body: formData
                            })
                                .then(response => {
                                    if (response.ok) {
                                        resolve();
                                    } else {
                                        response.text().then(errorText => resolve(errorText));
                                    }
                                })
                                .catch(error => {
                                    console.error('Erro ao validar senha:', error);
                                    resolve('Erro ao validar a senha. Tente novamente.');
                                });
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
                    inputAttributes: {
                        maxlength: "255"
                    },
                    inputValidator: (value) => {
                        if (!value) {
                            return "Você precisa digitar um motivo!";
                        }
                    }
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
