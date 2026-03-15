@import br.com.tecsus.sigaubs.entities.Contemplation
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import org.springframework.data.domain.Page
@param Page<Contemplation> consultasPage
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param String selectedMonth = null
@param String selectedStatus = null
@param String hide = ""
!{String paginationBase = "/contemplation-management/paginated?type=CONSULTA"
    + (selectedUBS != null ? "&ubs=" + selectedUBS : "")
    + (selectedSpecialty != null ? "&specialty=" + selectedSpecialty : "")
    + (selectedMonth != null && !selectedMonth.isEmpty() ? "&month=" + selectedMonth : "")
    + (selectedStatus != null && !selectedStatus.isEmpty() ? "&status=" + selectedStatus : "");}
<div id="consultas" class="tab-content ${hide}">
    <div class="overflow-x-auto">
    <table class="min-w-full bg-white">
        <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
        <tr class="bg-slate-200">
            <th class="text-sm rounded-l-lg py-2 px-4">N.</th>
            <th class="text-sm pl-4">Paciente</th>
            <th class="text-sm pl-4">CPF</th>
            @if(selectedSpecialty == null)
            <th class="text-sm pl-4">Esp.</th>
            @endif
            <th class="text-sm pl-4">Prioridade</th>
            <th class="text-sm pl-4">Contemplado por</th>
            <th class="text-sm pl-4">Contemplação</th>
            <th class="text-sm pl-4">Status</th>
            <th class="text-sm pl-4">UBS</th>
            <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
        </tr>
        </thead>
        <tbody>
        !{int cpIdx = 0;}
        @for(var cp : consultasPage)
        !{int rowNum = consultasPage.getNumber() == 0 ? cpIdx + 1 : (cpIdx + 1) + (consultasPage.getNumber() * consultasPage.getSize());}
        <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
            <td class="border-b text-sm py-2 px-4">${rowNum}</td>
            <td class="border-b text-sm pl-4">${cp.getAppointment().getPatient().getName()}</td>
            <td class="border-b text-sm pl-4">${cp.getAppointment().getPatient().getCpf()}</td>
            @if(selectedSpecialty == null)
            <td class="border-b text-sm pl-4">${cp.getAppointment().getMedicalProcedure().getSpecialty().getTitle()}</td>
            @endif
            <td class="border-b text-sm pl-4">${cp.getAppointment().getPriority().getDescription()}</td>
            <td class="border-b text-sm pl-4">${cp.getContemplatedBy().getDescription()}</td>
            <td class="border-b text-sm pl-4">${TemplateUtils.formatDateTime(cp.getContemplationDate())}</td>
            <td class="border-b text-sm pl-4">${cp.getAppointment().getStatus().getDescription()}</td>
            <td class="border-b text-sm pl-4">${cp.getMedicalSlot().getBasicHealthUnit().getName()}</td>
            <td class="border-b text-sm py-2 px-4">
                <div class="relative group">
                    <a hx-get="/contemplation-management/${cp.getId()}/load?ubs=${selectedUBS}&specialty=${selectedSpecialty}&month=${selectedMonth}"
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
        </tr>
        !{cpIdx++;}
        @endfor
        </tbody>
    </table>
    </div>
    @if(consultasPage.getTotalPages() > 0)
    <div id="consultasPaginationPanel" class="flex flex-1 mt-4 justify-between items-center bg-white">
        <div>
            <label class="text-sm text-slate-500">Linhas por página:</label>
            <select id="consultasPageSize" name="consultasPageSize"
                    hx-get="${paginationBase}&page=${consultasPage.getNumber()}"
                    hx-target="#consultas"
                    hx-swap="outerHTML"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500">
                <option value="10" selected="${consultasPage.getSize() == 10}">10</option>
                <option value="15" selected="${consultasPage.getSize() == 15}">15</option>
                <option value="30" selected="${consultasPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center">
            <button id="consultasPrevPage"
                    disabled="${!consultasPage.hasPrevious()}"
                    hx-get="${paginationBase}&page=${consultasPage.getNumber() - 1}"
                    hx-target="#consultas"
                    hx-include="[name='consultasPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
            </button>
            <span class="mx-2 text-sm text-slate-500">
                Página <span id="consultasCurrentPage">${consultasPage.getNumber() + 1}</span> de
                <span>${consultasPage.getTotalPages()}</span>
            </span>
            <button id="consultasNextPage"
                    disabled="${!consultasPage.hasNext()}"
                    hx-get="${paginationBase}&page=${consultasPage.getNumber() + 1}"
                    hx-target="#consultas"
                    hx-include="[name='consultasPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
            </button>
        </div>
    </div>
    @endif
</div>
