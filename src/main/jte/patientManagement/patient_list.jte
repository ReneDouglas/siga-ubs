@import br.com.tecsus.sigaubs.entities.Patient
@import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO
@import br.com.tecsus.sigaubs.enums.SocialSituationRating
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import org.springframework.data.domain.Page
@param Page<Patient> patientsPage
@param Page<PatientAppointmentsHistoryDTO> patientHistoryPage
@param Patient patient
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Lista de Pacientes", res = res)
<body class="flex w-full h-dvh justify-between items-center bg-slate-100">
@template.fragments.sidebar(secHelper = secHelper, res = res)

<main class="flex flex-col h-full w-full overflow-auto">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 — Busca --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">person_search</span> Buscar Paciente
                    </div>
                </label>

                <div class="peer-checked:max-h-screen max-h-0 overflow-hidden peer-checked:overflow-visible overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          id="searchPatientForm" action="/patient-list" method="get">

                        <div class="text-sm flex flex-col w-full col-span-2 sm:col-span-3 lg:col-span-3 xl:col-span-3 relative">
                            <label for="patientSearch">Nome:</label>
                            <input class="rounded-lg border border-stroke py-2 pl-4 w-full mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200 focus:shadow-lg"
                                   placeholder="Nome do Paciente" type="search" id="patientSearch"
                                   name="name" value="${patient.getName() != null ? patient.getName() : ""}"
                                   hx-get="/patient-list/search"
                                   hx-trigger="input changed delay:500ms, patientSearch"
                                   hx-vals="js:{'autocomplete':true}"
                                   hx-target="#dropdown"
                                   hx-swap="outerHTML">
                            <div class="absolute top-full w-full z-50">
                                <div id="dropdown"
                                     class="bg-white border border-gray-300 rounded shadow-lg opacity-0 invisible transition-opacity duration-300 peer-checked:opacity-100 peer-checked:visible">
                                </div>
                            </div>
                        </div>

                        <div class="flex flex-col text-sm w-full col-span-2 sm:col-span-1">
                            <label for="cpf">CPF:</label>
                            <input class="rounded-lg border border-stroke py-2 pl-4 w-full mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200 focus:shadow-lg"
                                   placeholder="CPF do Paciente" type="text" id="cpf"
                                   name="cpf" value="${patient.getCpf() != null ? patient.getCpf() : ""}">
                        </div>

                        <div class="flex flex-col text-sm w-full col-span-2 sm:col-span-2 lg:col-span-1">
                            <label for="susNumber">Cartão SUS:</label>
                            <input class="rounded-lg border border-stroke py-2 pl-4 w-full mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200 focus:shadow-lg"
                                   placeholder="Digite o Cartão SUS" type="text" id="susNumber"
                                   name="susNumber" value="${patient.getSusNumber() != null ? patient.getSusNumber() : ""}">
                        </div>
                        <div class="flex flex-col w-full text-sm col-span-2 sm:col-span-2 lg:col-span-1">
                            <label for="acsName">ACS:</label>
                            <input class="rounded-lg border border-stroke py-2 pl-4 w-full mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200 focus:shadow-lg"
                                   placeholder="ACS Responsável" type="text" id="acsName"
                                   name="acsName" value="${patient.getAcsName() != null ? patient.getAcsName() : ""}">
                        </div>

                        <div class="flex flex-col text-sm w-full col-span-2 sm:col-span-4 lg:col-span-2 xl:col-span-2">
                            <label for="addressStreet">Logradouro:</label>
                            <input class="rounded-lg border border-stroke py-2 pl-4 w-full mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200 focus:shadow-lg"
                                   placeholder="Digite o Endereço" type="text" id="addressStreet"
                                   name="addressStreet" value="${patient.getAddressStreet() != null ? patient.getAddressStreet() : ""}">
                        </div>

                        <div class="flex flex-col text-sm w-full col-span-2 sm:col-span-2 lg:col-span-2 xl:col-span-1">
                            <label for="socialSituationRating">Situação Social:</label>
                            <select class="rounded-lg border border-stroke text-sm py-2 pl-4 w-full mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 text-gray-700 transition duration-200"
                                    id="socialSituationRating" name="socialSituationRating">
                                <option value="" selected="${patient.getSocialSituationRating() == null}">Selecione</option>
                                @for(var situation : SocialSituationRating.values())
                                <option value="${situation.name()}" selected="${situation.equals(patient.getSocialSituationRating())}">${situation.getDescription()}</option>
                                @endfor
                            </select>
                        </div>

                        <div class="flex items-center justify-center sm:justify-end sm:items-end space-x-4 mt-2 sm:mt-0 col-span-2 sm:col-span-2 lg:col-span-1 w-full">
                            <button class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition transition-300"
                                    type="submit">
                                <span class="-ml-1 material-symbols-outlined mx-1">search</span>Buscar
                            </button>
                            <button class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300"
                                    type="submit" formaction="/patient-list/clear">
                                <span class="-ml-1 material-symbols-outlined mx-1">close</span>Limpar
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <%-- Panel 2 — Datatable --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel2" class="peer hidden" checked>
                <label for="panel2"
                       class="block bg-slate-900 text-slate-200 tracking-wide py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-2">patient_list</span>Pacientes Cadastrados
                    </div>
                </label>
                <div class="peer-checked:max-h-[2000px] max-h-0 overflow-hidden transition-max-height duration-500 ease-in-out">
                    <div class="p-4 rounded-lg">
                        @template.patientManagement.patientFragments.patient_datatable(patientsPage = patientsPage, secHelper = secHelper)
                    </div>
                </div>
            </div>

            <%-- Panel 3 — Histórico --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel3" class="peer hidden" checked>
                <label for="panel3"
                       class="block bg-slate-900 text-slate-200 tracking-wide py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-2">demography</span>Histórico do Paciente
                    </div>
                </label>
                <div class="peer-checked:max-h-screen max-h-0 overflow-hidden transition-max-height duration-500 ease-in-out">
                    <div class="p-4 rounded-lg">
                        @template.patientManagement.patientFragments.patient_history(patientHistoryPage = patientHistoryPage)

                        <%-- Modal de Observações --%>
                        <div id="default-modal" tabindex="-1" aria-hidden="true"
                             class="hidden overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-50 justify-center items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full">
                            <div class="relative p-4 w-full max-w-2xl max-h-full">
                                <div class="relative bg-white rounded-lg shadow dark:bg-gray-700">
                                    <div class="flex items-center justify-between p-4 md:p-5 border-b rounded-t dark:border-gray-600">
                                        <h3 class="text-xl font-semibold text-gray-900 dark:text-white">Observações</h3>
                                        <button type="button" onclick="closeModal()"
                                                class="text-gray-400 bg-transparent hover:bg-gray-200 hover:text-gray-900 rounded-lg text-sm w-8 h-8 ms-auto inline-flex justify-center items-center dark:hover:bg-gray-600 dark:hover:text-white">
                                            <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6"/>
                                            </svg>
                                        </button>
                                    </div>
                                    <div class="p-4 md:p-5 space-y-4">
                                        <p class="text-base leading-relaxed text-gray-900 dark:text-gray-100" id="modal-observacoes"></p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</main>
</body>

<script src="${res.url("/js/cpfmask.js")}"></script>
<script>
    $(document).click(function (event) {
        var target = $(event.target);
        if (!target.closest('#dropdown').length && !target.closest('#patientSearch').length) {
            $('#dropdown').hide();
        }
    });

    function autocompleteInputSearch(patientName) {
        document.getElementById('patientSearch').value = patientName;
        document.getElementById('dropdown').classList.add('hidden');
    }

    function openModal(observacoes) {
        let modal = document.getElementById("default-modal");
        let modalObservacoes = modal.querySelector("#modal-observacoes")
        modalObservacoes.innerText = observacoes;
        modal.classList.remove("hidden");
        modal.classList.add("flex");
        modal.classList.add("opacity-100");
    }

    function closeModal() {
        let modal = document.getElementById("default-modal");
        modal.classList.add("opacity-0");
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }
</script>
</html>
