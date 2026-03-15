@import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO
@import org.springframework.data.domain.Page
@param Page<PatientOpenAppointmentDTO> examesPage
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param String hide = ""
!{String baseUrl = "/queue-management/paginated?type=EXAME" + (selectedUBS != null ? "&ubs=" + selectedUBS : "") + (selectedSpecialty != null ? "&specialty=" + selectedSpecialty : "");}
<div id="exames" class="tab-content p-4 bg-gray-100 ${hide}">
    <div class="overflow-x-auto">
        <table class="min-w-full bg-white divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr class="bg-gray-700 text-white">
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">N.</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Paciente</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Agendamento</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Prioridade</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Idade</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Situação Social</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Sexo</th>
                <th class="px-4 py-2 text-left text-sm font-medium uppercase tracking-wider">Ações</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
            !{int expIdx = 0;}
            @for(var exp : examesPage.getContent())
            <tr>
                <td class="px-4 py-2 text-gray-700">${examesPage.getNumber() == 0 ? expIdx + 1 : (expIdx + 1) + (examesPage.getNumber() * examesPage.getSize())}</td>
                <td class="px-4 py-2 text-gray-700">${exp.patientName()}</td>
                <td class="px-4 py-2 text-gray-700">${exp.formattedRequestDate()}</td>
                <td class="px-4 py-2 text-gray-700">${exp.priority().getDescription()}</td>
                <td class="px-4 py-2 text-gray-700">${exp.getBirthDateWithAge()}</td>
                <td class="px-4 py-2 text-gray-700">${exp.patientSocialSituationRating().getDescription()}</td>
                <td class="px-4 py-2 text-gray-700">${exp.patientGender()}</td>
                <td class="px-4 py-2 text-gray-700">
                    <a hx-get="/queue-management/${exp.appointmentId()}/load${selectedUBS != null ? "?ubs=" + selectedUBS : ""}${selectedSpecialty != null ? (selectedUBS != null ? "&" : "?") + "specialty=" + selectedSpecialty : ""}"
                       hx-target="#patientAppointment-modal"
                       hx-swap="outerHTML"
                       href=""
                       class="flex items-center bg-indigo-500 text-white px-2 py-1 rounded-md hover:bg-indigo-400 focus:outline-none transition-colors duration-300"
                       title="Ver Detalhes">
                        <div class="flex items-center w-full">
                            <span class="material-symbols-outlined px-2">edit_note</span>
                        </div>
                    </a>
                </td>
            </tr>
            !{expIdx++;}
            @endfor
            </tbody>
        </table>
    </div>
    @if(examesPage.getTotalPages() > 0)
    <div id="examesPaginationPanel"
         class="flex items-center justify-between mt-4 p-4 bg-white border border-gray-300 rounded-lg shadow-md">
        <div class="flex items-center space-x-2">
            <label for="examesPageSize" class="text-sm font-medium text-gray-700">Linhas por página:</label>
            <select id="examesPageSize" name="examesPageSize"
                    hx-get="${baseUrl}&page=${examesPage.getNumber()}"
                    hx-target="#exames"
                    hx-swap="outerHTML"
                    class="border-gray-300 rounded-md shadow-md focus:ring-gray-500 focus:border-gray-500 p-2">
                <option value="10" selected="${examesPage.getSize() == 10}">10</option>
                <option value="15" selected="${examesPage.getSize() == 15}">15</option>
                <option value="30" selected="${examesPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center space-x-4">
            <button id="examesPrevPage"
                    disabled="${!examesPage.hasPrevious()}"
                    hx-get="${baseUrl}&page=${examesPage.getNumber() - 1}"
                    hx-target="#exames"
                    hx-include="[name='examesPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="px-4 py-2 bg-gray-700 text-white font-semibold rounded-md shadow-md hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed focus:ring-2 focus:ring-gray-400">
                Anterior
            </button>
            <span class="text-sm text-gray-700">
                Página <span id="examesCurrentPage">${examesPage.getNumber() + 1}</span> de
                <span>${examesPage.getTotalPages()}</span>
            </span>
            <button id="examesNextPage"
                    disabled="${!examesPage.hasNext()}"
                    hx-get="${baseUrl}&page=${examesPage.getNumber() + 1}"
                    hx-target="#exames"
                    hx-include="[name='examesPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="px-4 py-2 bg-gray-700 text-white font-semibold rounded-md shadow-md hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed focus:ring-2 focus:ring-gray-400">
                Próximo
            </button>
        </div>
    </div>
    @endif
</div>
