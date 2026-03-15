@import br.com.tecsus.sigaubs.entities.Contemplation
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@param Contemplation contemplated
<div class="p-4">
    <h3 class="text-sm font-semibold text-slate-800 mb-4">Histórico de Status</h3>

    <div class="max-h-[400px] overflow-y-auto overflow-x-auto rounded-lg">
        <table class="min-w-full bg-white">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
            <tr class="bg-slate-200">
                <th class="text-sm rounded-l-lg py-2 px-4">N.</th>
                <th class="text-sm pl-4">Status</th>
                <th class="text-sm pl-4">Data</th>
                <th class="text-sm rounded-r-lg py-2 px-4">Usuário</th>
            </tr>
            </thead>
            <tbody>
            !{int stIdx = 0;}
            @for(var st : contemplated.getAppointment().getAppointmentStatusHistory())
            <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                <td class="border-b text-sm py-2 px-4">${stIdx + 1}</td>
                <td class="border-b text-sm pl-4">${st.getStatus().getDescription()}</td>
                <td class="border-b text-sm pl-4">${TemplateUtils.formatDateTime(st.getCreationDate())}</td>
                <td class="border-b text-sm py-2 px-4">${st.getCreationUser()}</td>
            </tr>
            !{stIdx++;}
            @endfor
            </tbody>
        </table>
    </div>
</div>
