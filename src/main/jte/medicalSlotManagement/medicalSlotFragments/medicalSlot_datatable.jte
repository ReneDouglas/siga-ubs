@import br.com.tecsus.sigaubs.entities.MedicalSlot
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import org.springframework.data.domain.Page
@param Page<MedicalSlot> medicalSlotsPage
<div id="medicalSlotsDatatable">
    <div class="overflow-x-auto">
    <table class="min-w-full bg-white">
        <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
        <tr class="bg-slate-200">
            <th class="text-sm rounded-l-lg py-2 px-4">N.</th>
            <th class="text-sm pl-4">UBS</th>
            <th class="text-sm pl-4">Mês Ref.</th>
            <th class="text-sm pl-4">Vagas</th>
            <th class="text-sm pl-4">Especialidade</th>
            <th class="text-sm pl-4">Marcação</th>
            <th class="text-sm pl-4">Procedimento</th>
            <th class="text-sm pl-4">Data do Cadastro</th>
            <th class="text-sm rounded-r-lg py-2 px-4">Usuário</th>
        </tr>
        </thead>
        <tbody>
        !{int msIdx = 0;}
        @for(var ms : medicalSlotsPage)
        !{int rowNum = medicalSlotsPage.getNumber() == 0 ? msIdx + 1 : (msIdx + 1) + (medicalSlotsPage.getNumber() * medicalSlotsPage.getSize());}
        <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
            <td class="border-b text-sm py-2 px-4">${rowNum}</td>
            <td class="border-b text-sm pl-4">${ms.getBasicHealthUnit().getName()}</td>
            <td class="border-b text-sm pl-4">${TemplateUtils.formatYearMonth(ms.getReferenceMonth())}</td>
            <td class="border-b text-sm pl-4 text-center">${ms.getTotalSlots()}</td>
            <td class="border-b text-sm pl-4">${ms.getMedicalProcedure().getSpecialty().getTitle()}</td>
            <td class="border-b text-sm pl-4">${ms.getMedicalProcedure().getProcedureType().getDescription()}</td>
            <td class="border-b text-sm pl-4">${ms.getMedicalProcedure().getDescription()}</td>
            <td class="border-b text-sm pl-4">${TemplateUtils.formatDateTime(ms.getCreationDate())}</td>
            <td class="border-b text-sm py-2 px-4">${ms.getCreationUser()}</td>
        </tr>
        !{msIdx++;}
        @endfor
        </tbody>
    </table>
    </div>
    @if(medicalSlotsPage.getTotalPages() > 0)
    <div id="paginationPanel" class="flex flex-1 mt-4 justify-between items-center bg-white">
        <div>
            <label class="text-sm text-slate-500">Linhas por página:</label>
            <select id="pageSize" name="pageSize"
                    hx-get="/medicalSlot-management/paginated?page=${medicalSlotsPage.getNumber()}"
                    hx-target="#medicalSlotsDatatable"
                    hx-swap="outerHTML"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500">
                <option value="10" selected="${medicalSlotsPage.getSize() == 10}">10</option>
                <option value="15" selected="${medicalSlotsPage.getSize() == 15}">15</option>
                <option value="30" selected="${medicalSlotsPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center">
            <button id="prevPage"
                    disabled="${!medicalSlotsPage.hasPrevious()}"
                    hx-get="/medicalSlot-management/paginated?page=${medicalSlotsPage.getNumber() - 1}"
                    hx-target="#medicalSlotsDatatable"
                    hx-include="[name='pageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
            </button>
            <span class="mx-2 text-sm text-slate-500">
                Página <span id="currentPage">${medicalSlotsPage.getNumber() + 1}</span> de
                <span>${medicalSlotsPage.getTotalPages()}</span>
            </span>
            <button id="nextPage"
                    disabled="${!medicalSlotsPage.hasNext()}"
                    hx-get="/medicalSlot-management/paginated?page=${medicalSlotsPage.getNumber() + 1}"
                    hx-target="#medicalSlotsDatatable"
                    hx-include="[name='pageSize']"
                    hx-trigger="click"
                    hx-swap="outerHTML"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
            </button>
        </div>
    </div>
    @endif
</div>
