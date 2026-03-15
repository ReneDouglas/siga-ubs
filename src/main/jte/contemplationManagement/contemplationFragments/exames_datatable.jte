@import br.com.tecsus.sigaubs.entities.Contemplation
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import org.springframework.data.domain.Page
@param Page<Contemplation> examesPage
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param String selectedMonth = null
@param String selectedStatus = null
@param String hide = ""
!{String paginationBase = "/contemplation-management/paginated?type=EXAME"
    + (selectedUBS != null ? "&ubs=" + selectedUBS : "")
    + (selectedSpecialty != null ? "&specialty=" + selectedSpecialty : "")
    + (selectedMonth != null && !selectedMonth.isEmpty() ? "&month=" + selectedMonth : "")
    + (selectedStatus != null && !selectedStatus.isEmpty() ? "&status=" + selectedStatus : "");}
<div id="exames" class="tab-content ${hide}">
    <div class="overflow-x-auto">
    <table class="min-w-full bg-white">
        <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
        <tr class="bg-slate-200">
            <th class="text-sm rounded-l-lg py-2 px-4">N.</th>
            <th class="text-sm pl-4">Paciente</th>
            <th class="text-sm pl-4">CPF</th>
            <th class="text-sm pl-4">Exame</th>
            <th class="text-sm pl-4">Prioridade</th>
            <th class="text-sm pl-4">Contemplado por</th>
            <th class="text-sm pl-4">Contemplação</th>
            <th class="text-sm pl-4">Status</th>
            <th class="text-sm pl-4">UBS</th>
            <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
        </tr>
        </thead>
        <tbody>
        !{int expIdx = 0;}
        @for(var exp : examesPage)
        !{int rowNum = examesPage.getNumber() == 0 ? expIdx + 1 : (expIdx + 1) + (examesPage.getNumber() * examesPage.getSize());}
        <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
            <td class="border-b text-sm py-2 px-4">${rowNum}</td>
            <td class="border-b text-sm pl-4">${exp.getAppointment().getPatient().getName()}</td>
            <td class="border-b text-sm pl-4">${exp.getAppointment().getPatient().getCpf()}</td>
            @if(selectedSpecialty != null)
            <td class="border-b text-sm pl-4">${exp.getAppointment().getMedicalProcedure().getDescription()}</td>
            @else
            <td class="border-b text-sm pl-4">${exp.getAppointment().getMedicalProcedure().getDescription()} [${exp.getAppointment().getMedicalProcedure().getSpecialty().getTitle()}]</td>
            @endif
            <td class="border-b text-sm pl-4">${exp.getAppointment().getPriority().getDescription()}</td>
            <td class="border-b text-sm pl-4">${exp.getContemplatedBy().getDescription()}</td>
            <td class="border-b text-sm pl-4">${TemplateUtils.formatDateTime(exp.getContemplationDate())}</td>
            <td class="border-b text-sm pl-4">${exp.getAppointment().getStatus().getDescription()}</td>
            <td class="border-b text-sm pl-4">${exp.getMedicalSlot().getBasicHealthUnit().getName()}</td>
            <td class="border-b text-sm py-2 px-4">
                <div class="relative group">
                    <a hx-get="/contemplation-management/${exp.getId()}/load?ubs=${selectedUBS}&specialty=${selectedSpecialty}&month=${selectedMonth}"
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
        !{expIdx++;}
        @endfor
        </tbody>
    </table>
    </div>
    @if(examesPage.getTotalPages() > 0)
    <div id="examesPaginationPanel" class="flex flex-1 mt-4 justify-between items-center bg-white">
        <div>
            <label class="text-sm text-slate-500">Linhas por página:</label>
            <select id="examesPageSize" name="examesPageSize"
                    hx-get="${paginationBase}&page=${examesPage.getNumber()}"
                    hx-target="#exames"
                    hx-swap="outerHTML"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500">
                <option value="10" selected="${examesPage.getSize() == 10}">10</option>
                <option value="15" selected="${examesPage.getSize() == 15}">15</option>
                <option value="30" selected="${examesPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center">
            <button id="examesPrevPage"
                    disabled="${!examesPage.hasPrevious()}"
                    hx-get="${paginationBase}&page=${examesPage.getNumber() - 1}"
                    hx-target="#exames"
                    hx-include="[name='examesPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
            </button>
            <span class="mx-2 text-sm text-slate-500">
                Página <span id="examesCurrentPage">${examesPage.getNumber() + 1}</span> de
                <span>${examesPage.getTotalPages()}</span>
            </span>
            <button id="examesNextPage"
                    disabled="${!examesPage.hasNext()}"
                    hx-get="${paginationBase}&page=${examesPage.getNumber() + 1}"
                    hx-target="#exames"
                    hx-include="[name='examesPageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
            </button>
        </div>
    </div>
    @endif
</div>
