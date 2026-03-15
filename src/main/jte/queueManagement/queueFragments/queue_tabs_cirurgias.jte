@import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO
@import org.springframework.data.domain.Page
@param Page<PatientOpenAppointmentDTO> cirurgiasPage
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param String hide = ""
!{String baseUrl = "/queue-management/paginated?type=CIRURGIA" + (selectedUBS != null ? "&ubs=" + selectedUBS : "") + (selectedSpecialty != null ? "&specialty=" + selectedSpecialty : "");}
<div id="cirurgias" class="tab-content p-4 bg-gray-100 ${hide}">
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
            !{int crpIdx = 0;}
            @for(var crp : cirurgiasPage.getContent())
            <tr>
                <td class="px-4 py-2 text-gray-700">${cirurgiasPage.getNumber() == 0 ? crpIdx + 1 : (crpIdx + 1) + (cirurgiasPage.getNumber() * cirurgiasPage.getSize())}</td>
                <td class="px-4 py-2 text-gray-700">${crp.patientName()}</td>
                <td class="px-4 py-2 text-gray-700">${crp.formattedRequestDate()}</td>
                <td class="px-4 py-2 text-gray-700">${crp.priority().getDescription()}</td>
                <td class="px-4 py-2 text-gray-700">${crp.getBirthDateWithAge()}</td>
                <td class="px-4 py-2 text-gray-700">${crp.patientSocialSituationRating().getDescription()}</td>
                <td class="px-4 py-2 text-gray-700">${crp.patientGender()}</td>
                <td class="px-4 py-2 text-gray-700">
                    <a hx-get="/queue-management/${crp.appointmentId()}/load${selectedUBS != null ? "?ubs=" + selectedUBS : ""}${selectedSpecialty != null ? (selectedUBS != null ? "&" : "?") + "specialty=" + selectedSpecialty : ""}"
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
            !{crpIdx++;}
            @endfor
            </tbody>
        </table>
    </div>
    @if(cirurgiasPage.getTotalPages() > 0)
    <div id="cirurgiasPaginationPanel"
         class="flex items-center justify-between mt-4 p-4 bg-white border border-gray-300 rounded-lg shadow-md">
        <div class="flex items-center space-x-2">
            <label for="cirurgiasPageSize" class="text-sm font-medium text-gray-700">Linhas por página:</label>
            <select id="cirurgiasPageSize" name="cirurgiasPageSize"
                    hx-get="${baseUrl}&page=${cirurgiasPage.getNumber()}"
                    hx-target="#cirurgias"
                    hx-swap="outerHTML"
                    class="border-gray-300 rounded-md shadow-md focus:ring-gray-500 focus:border-gray-500 p-2">
                <option value="10" selected="${cirurgiasPage.getSize() == 10}">10</option>
                <option value="15" selected="${cirurgiasPage.getSize() == 15}">15</option>
                <option value="30" selected="${cirurgiasPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center space-x-4">
            <button id="cirurgiasPrevPage"
                    disabled="${!cirurgiasPage.hasPrevious()}"
                    hx-get="${baseUrl}&page=${cirurgiasPage.getNumber() - 1}"
                    hx-target="#cirurgias"
                    hx-include="[name='cirurgiasPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="px-4 py-2 bg-gray-700 text-white font-semibold rounded-md shadow-md hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed focus:ring-2 focus:ring-gray-400">
                Anterior
            </button>
            <span class="text-sm text-gray-700">
                Página <span id="cirurgiasCurrentPage">${cirurgiasPage.getNumber() + 1}</span> de
                <span>${cirurgiasPage.getTotalPages()}</span>
            </span>
            <button id="cirurgiasNextPage"
                    disabled="${!cirurgiasPage.hasNext()}"
                    hx-get="${baseUrl}&page=${cirurgiasPage.getNumber() + 1}"
                    hx-target="#cirurgias"
                    hx-include="[name='cirurgiasPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="px-4 py-2 bg-gray-700 text-white font-semibold rounded-md shadow-md hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed focus:ring-2 focus:ring-gray-400">
                Próximo
            </button>
        </div>
    </div>
    @endif
</div>
