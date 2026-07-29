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
@template.fragments.sidebar(secHelper = secHelper, res = res, csrf = csrf)
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
                                 data-statistics-panel>
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
                                    data-action="show-tab" data-tab-target="consultas">
                                Consultas
                            </button>
                            <button class="tab-btn w-1/3 py-2 text-center bg-gray-200 hover:bg-gray-300 focus:outline-none"
                                    data-action="show-tab" data-tab-target="exames">
                                Exames
                            </button>
                            <button class="tab-btn w-1/3 py-2 text-center bg-gray-200 hover:bg-gray-300 focus:outline-none"
                                    data-action="show-tab" data-tab-target="cirurgias">
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
                                data-action="close-modal" data-modal-id="default-modal"
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
</html>
