@import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import org.springframework.data.domain.Page
@param Page<PatientOpenAppointmentDTO> queuePage
@param SecurityHelper secHelper
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param Long selectedMedicalProcedure = null
@param String selectedProcedureType = null
<div id="queue-datatable" class="tab-content mt-4">
    <div class="overflow-x-auto">
        <table class="min-w-full bg-white">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
                <tr class="bg-slate-200">
                    <th class="text-sm rounded-l-lg py-2 px-4">N.</th>
                    <th class="text-sm pl-4">Paciente</th>
                    <th class="text-sm pl-4">Agendamento</th>
                    <th class="text-sm pl-4">Prioridade</th>
                    <th class="text-sm pl-4">Idade</th>
                    <th class="text-sm pl-4">Situação Social</th>
                    <th class="text-sm pl-4">Sexo</th>
                    <th class="text-sm pl-4">UBS</th>
                    <th class="text-sm pl-4">Marcação</th>
                    @if(secHelper.hasAnyRole("ADMIN", "SMS"))
                    <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
                    @endif
                </tr>
            </thead>
            <tbody>
                !{int queueIdx = 0;}
                @for(var queue : queuePage)
                !{int rowNum = queuePage.getNumber() == 0 ? queueIdx + 1 : (queueIdx + 1) + (queuePage.getNumber() * queuePage.getSize());}
                <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                    <td class="border-b text-sm py-2 px-4">${rowNum}</td>
                    <td class="border-b text-sm pl-4">${queue.patientName()}</td>
                    <td class="border-b text-sm pl-4">${TemplateUtils.formatDateTime(queue.requestDate())}</td>
                    <td class="border-b text-sm pl-4">${queue.priority().getDescription()}</td>
                    <td class="border-b text-sm pl-4">${queue.getBirthDateWithAge()}</td>
                    <td class="border-b text-sm pl-4">${queue.patientSocialSituationRating().getDescription()}</td>
                    <td class="border-b text-sm pl-4">${queue.patientGender()}</td>
                    <td class="border-b text-sm pl-4">${queue.ubsName()}</td>
                    <td class="border-b text-sm pl-4">${"Consulta".equals(queue.procedureType().getDescription()) ? "Consulta [" + queue.specialty() + "]" : queue.medicalProcedureDescription()}</td>
                    @if(secHelper.hasAnyRole("ADMIN", "SMS"))
                    <td class="border-b text-sm py-2 px-4">
                        <div class="relative group">
                            <a hx-get="/queue-management/v2/${queue.appointmentId()}/load?ubs=${selectedUBS}&specialty=${selectedSpecialty}&medicalProcedure=${selectedMedicalProcedure}&procedureType=${selectedProcedureType}"
                               hx-target="#patientAppointment-modal"
                               hx-swap="outerHTML"
                               href=""
                               class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                                <span class="material-symbols-outlined hover:text-blue-500">edit_note</span>
                            </a>
                            <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1 whitespace-nowrap">
                                Ver Detalhes
                            </div>
                        </div>
                    </td>
                    @endif
                </tr>
                !{queueIdx++;}
                @endfor
            </tbody>
        </table>
    </div>
    @if(queuePage.getTotalPages() > 0)
    <div id="paginationPanel" class="flex flex-1 mt-4 justify-between items-center bg-white">
        <div>
            <label class="text-sm text-slate-500">Linhas por página:</label>
            <select id="size" name="size"
                    hx-get="/queue-management/v2/paginated?page=${queuePage.getNumber()}&ubs=${selectedUBS}&specialty=${selectedSpecialty}&medicalProcedure=${selectedMedicalProcedure}&procedureType=${selectedProcedureType}"
                    hx-target="#queue-datatable"
                    hx-indicator=".animate-pulse"
                    hx-swap="outerHTML"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500">
                <option value="10" selected="${queuePage.getSize() == 10}">10</option>
                <option value="15" selected="${queuePage.getSize() == 15}">15</option>
                <option value="30" selected="${queuePage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center">
            <span class="text-sm text-slate-500 mr-4">Há ${queuePage.getTotalElements()} paciente(s) nesta fila de espera</span>
        </div>
        <div class="flex items-center">
            <button id="prevPage"
                    disabled="${!queuePage.hasPrevious()}"
                    hx-get="/queue-management/v2/paginated?page=${queuePage.getNumber() - 1}&ubs=${selectedUBS}&specialty=${selectedSpecialty}&medicalProcedure=${selectedMedicalProcedure}&procedureType=${selectedProcedureType}"
                    hx-target="#queue-datatable"
                    hx-include="[name='size']"
                    hx-trigger="click"
                    hx-indicator=".animate-pulse"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
            </button>
            <span class="mx-2 text-sm text-slate-500">
                Página <span id="currentPage">${queuePage.getNumber() + 1}</span> de
                <span>${queuePage.getTotalPages()}</span>
            </span>
            <button id="nextPage"
                    disabled="${!queuePage.hasNext()}"
                    hx-get="/queue-management/v2/paginated?page=${queuePage.getNumber() + 1}&ubs=${selectedUBS}&specialty=${selectedSpecialty}&medicalProcedure=${selectedMedicalProcedure}&procedureType=${selectedProcedureType}"
                    hx-target="#queue-datatable"
                    hx-include="[name='size']"
                    hx-trigger="click"
                    hx-indicator=".animate-pulse"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
            </button>
        </div>
    </div>
    @endif
</div>
