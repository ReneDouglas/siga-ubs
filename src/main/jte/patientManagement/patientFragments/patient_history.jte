@import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO
@import org.springframework.data.domain.Page
@param Page<PatientAppointmentsHistoryDTO> patientHistoryPage
<div id="patientHistoryDatatable">
    <div class="overflow-auto">
        <table class="table-auto w-full text-base">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
                <tr class="bg-slate-200">
                    <th class="text-sm rounded-l-lg py-2 px-4">Nº</th>
                    <th class="text-sm pl-4 text-nowrap">Data da Requisição</th>
                    <th class="text-sm pl-4 text-nowrap">Data da Contemplação</th>
                    <th class="text-sm pl-4">Status</th>
                    <th class="text-sm pl-4 text-nowrap">Contemplado por</th>
                    <th class="text-sm pl-4">Prioridade</th>
                    <th class="text-sm pl-4">Marcação</th>
                    <th class="text-sm pl-4">Procedimento</th>
                    <th class="text-sm pl-4 text-nowrap">Especialidade</th>
                    <th class="text-sm rounded-r-lg py-2">Ações</th>
                </tr>
            </thead>
            <tbody>
                @if(!patientHistoryPage.hasContent())
                <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                    <td class="border-b text-sm py-2 px-4 text-nowrap">Sem registro</td>
                </tr>
                @endif

                !{int histIdx = 0;}
                @for(var patientHistory : patientHistoryPage)
                !{int rowNum = patientHistoryPage.getNumber() == 0 ? histIdx + 1 : (histIdx + 1) + (patientHistoryPage.getNumber() * patientHistoryPage.getSize());}
                <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                    <td class="border-b text-sm py-2 px-4">${rowNum}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.formattedRequestDate()}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.formattedContemplationDate()}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.appointmentStatus() != null ? patientHistory.appointmentStatus().getDescription() : ""}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.nullableContemplatedBy()}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.priorityRegistered() != null ? patientHistory.priorityRegistered().getDescription() : ""}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.procedureType() != null ? patientHistory.procedureType().getDescription() : ""}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.medicalProcedure()}</td>
                    <td class="border-b text-sm py-2 px-4">${patientHistory.specialty()}</td>
                    <td class="border-b text-sm py-2">
                        <div class="inline-flex py-2">
                            <div class="relative group cursor-pointer">
                                <button type="button" onclick="openModal('${patientHistory.observations() != null ? patientHistory.observations().replace("'", "\\'") : ""}')"
                                        class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition transition-300">
                                    <span class="material-symbols-outlined hover:text-blue-500 transition-all duration-300">pending_actions</span>
                                </button>
                                <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                                    Observações
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
                !{histIdx++;}
                @endfor
            </tbody>
        </table>
    </div>

    @if(patientHistoryPage.hasContent())
    <div class="flex flex-1 mt-4 justify-between items-center bg-white" id="paginationPanelHistory">
        <div>
            <label class="text-sm text-slate-500" for="pageSizeHistory">Linhas por página:</label>
            <select id="pageSizeHistory" name="pageSizeHistory"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500"
                    hx-get="/patient-list/history?page=${patientHistoryPage.getNumber()}&pagination=true"
                    hx-target="#patientHistoryDatatable"
                    hx-swap="outerHTML">
                <option value="10" selected="${patientHistoryPage.getSize() == 10}">10</option>
                <option value="15" selected="${patientHistoryPage.getSize() == 15}">15</option>
                <option value="30" selected="${patientHistoryPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center">
            <div class="flex items-center">
                <button id="prevPageHistory" class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200"
                        disabled="${!patientHistoryPage.hasPrevious()}"
                        hx-get="/patient-list/history?page=${patientHistoryPage.getNumber() - 1}&pagination=true"
                        hx-target="#patientHistoryDatatable"
                        hx-include="[name='pageSizeHistory']"
                        hx-swap="outerHTML"
                        hx-trigger="click">
                    <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
                </button>
                <span class="mx-2 text-sm text-slate-500">Página <span id="currentPageHistory">${patientHistoryPage.getNumber() + 1}</span> de <span>${patientHistoryPage.getTotalPages()}</span></span>
                <button id="nextPageHistory"
                        class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200"
                        disabled="${!patientHistoryPage.hasNext()}"
                        hx-get="/patient-list/history?page=${patientHistoryPage.getNumber() + 1}&pagination=true"
                        hx-target="#patientHistoryDatatable"
                        hx-include="[name='pageSizeHistory']"
                        hx-swap="outerHTML"
                        hx-trigger="click">
                    <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
                </button>
            </div>
        </div>
    </div>
    @endif
</div>
