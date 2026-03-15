@import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@param AvailableMedicalSlotsFormDTO availableMedicalSlotsForm
@param CsrfHelper csrf
<form id="availableMedicalSlotsFormTable"
      action="/medicalSlot-management/slots/create"
      method="post"
      class="mt-4">
    <div class="overflow-x-auto">
    <table class="min-w-full bg-white">
        <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
        <tr class="bg-slate-200">
            <th class="text-sm rounded-l-lg py-2 px-4">Mês Ref.</th>
            <th class="text-sm pl-4">Vagas</th>
            <th class="text-sm pl-4">Especialidade</th>
            <th class="text-sm pl-4">Marcação</th>
            <th class="text-sm pl-4">Procedimento</th>
            <th class="text-sm pl-4">UBS</th>
            <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
        </tr>
        </thead>
        <tbody>
        !{int rowIdx = 0;}
        @for(var row : availableMedicalSlotsForm.getAvailableMedicalSlots())
        <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
            <td class="border-b text-sm py-2 px-4">${TemplateUtils.formatYearMonth(row.getReferenceMonth())}</td>
            <td class="border-b text-sm pl-4 text-center">${row.getTotalSlots()}</td>
            <td class="border-b text-sm pl-4">${row.getMedicalProcedure().getSpecialty().getTitle()}</td>
            <td class="border-b text-sm pl-4">${row.getMedicalProcedure().getProcedureType().getDescription()}</td>
            <td class="border-b text-sm pl-4">${row.getMedicalProcedure().getDescription()}</td>
            <td class="border-b text-sm pl-4">${row.getBasicHealthUnit().getName()}</td>
            <td style="display:none;">
                <input type="hidden" name="availableMedicalSlots[${rowIdx}].referenceMonth" value="${row.getReferenceMonth().toString()}">
            </td>
            <td style="display:none;">
                <input type="hidden" name="availableMedicalSlots[${rowIdx}].totalSlots" value="${row.getTotalSlots()}">
            </td>
            <td style="display:none;">
                <input type="hidden" name="availableMedicalSlots[${rowIdx}].medicalProcedure.id" value="${row.getMedicalProcedure().getId()}">
            </td>
            <td style="display:none;">
                <input type="hidden" name="availableMedicalSlots[${rowIdx}].basicHealthUnit.id" value="${row.getBasicHealthUnit().getId()}">
            </td>
            <td class="border-b text-sm py-2 px-4">
                <div class="relative group">
                    <a hx-get="/medicalSlot-management/slots/${rowIdx}/remove"
                       hx-target="#availableMedicalSlotsFormTable"
                       hx-swap="outerHTML"
                       href=""
                       class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                        <span class="material-symbols-outlined hover:text-red-500">delete</span>
                    </a>
                    <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                        Remover
                    </div>
                </div>
            </td>
        </tr>
        !{rowIdx++;}
        @endfor
        </tbody>
    </table>
    </div>
    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
</form>
