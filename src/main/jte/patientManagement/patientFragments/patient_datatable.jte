@import br.com.tecsus.sigaubs.entities.Patient
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import org.springframework.data.domain.Page
@param Page<Patient> patientsPage
@param SecurityHelper secHelper
<div id="patientsDatatable">
    <div class="overflow-auto">
        <table class="table-auto w-full text-base" id="systemUserDatatable">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
                <tr class="bg-slate-200">
                    <th class="text-sm rounded-l-lg py-2 px-4">N°</th>
                    <th class="text-sm pl-4">Nome</th>
                    <th class="text-sm pl-4">Contato</th>
                    <th class="text-sm pl-4">CPF</th>
                    @if(secHelper.hasAnyRole("ADMIN", "SMS"))
                    <th class="text-sm pl-4 text-nowrap">UBS</th>
                    @endif
                    <th class="text-sm pl-4 text-nowrap">Data de Nascimento</th>
                    <th class="text-sm pl-4">ACS</th>
                    <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
                </tr>
            </thead>
            <tbody>
                @if(!patientsPage.hasContent())
                <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                    <td class="border-b text-sm py-2 px-4 text-nowrap">Sem registro</td>
                </tr>
                @endif

                !{int patientIdx = 0;}
                @for(var patient : patientsPage)
                !{int rowNum = patientsPage.getNumber() == 0 ? patientIdx + 1 : (patientIdx + 1) + (patientsPage.getNumber() * patientsPage.getSize());}
                <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                    <td class="border-b text-sm px-4 text-nowrap">${rowNum}</td>
                    <td class="border-b text-sm px-4 text-nowrap">${patient.getName()}</td>
                    <td class="border-b text-sm px-4">${patient.getPhoneNumber()}</td>
                    <td class="border-b text-sm px-4 text-nowrap">${patient.getCpf()}</td>
                    @if(secHelper.hasAnyRole("ADMIN", "SMS"))
                    <td class="border-b text-sm px-4 text-nowrap">${patient.getBasicHealthUnit() != null ? patient.getBasicHealthUnit().getName() : ""}</td>
                    @endif
                    <td class="border-b text-sm px-4 border-b text-sm pl-4 py-4">${patient.getBirthDateWithAge()}</td>
                    <td class="border-b text-sm px-4">${patient.getAcsName()}</td>
                    <td class="border-b text-sm">
                        <div class="inline-flex">
                            <div class="relative group">
                                <a href="/patient-list/edit?id=${patient.getId()}"
                                   class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition transition-300">
                                    <span class="material-symbols-outlined hover:text-blue-500">edit</span>
                                </a>
                                <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                                    Editar
                                </div>
                            </div>
                            <div class="relative group cursor-pointer">
                                <a class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer"
                                   hx-get="/patient-list/history?id=${patient.getId()}&pagination=false"
                                   hx-target="#patientHistoryDatatable" hx-trigger="click">
                                    <span class="material-symbols-outlined hover:text-red-400">history</span>
                                </a>
                                <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                                    Histórico
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
                !{patientIdx++;}
                @endfor
            </tbody>
        </table>
    </div>

    @if(patientsPage.hasContent())
    <div class="flex flex-1 mt-4 justify-between items-center bg-white" id="paginationPanel">
        <div>
            <label class="text-sm text-slate-500" for="pageSize">Linhas por página:</label>
            <select id="pageSize" name="pageSize"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500"
                    hx-get="/patient-list?page=${patientsPage.getNumber()}&pagination=true"
                    hx-target="#patientsDatatable"
                    hx-swap="outerHTML">
                <option value="10" selected="${patientsPage.getSize() == 10}">10</option>
                <option value="15" selected="${patientsPage.getSize() == 15}">15</option>
                <option value="30" selected="${patientsPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center" id="pagination">
            <div class="flex items-center">
                <button id="prevPage"
                        class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200"
                        disabled="${!patientsPage.hasPrevious()}"
                        hx-get="/patient-list?page=${patientsPage.getNumber() - 1}&pagination=true"
                        hx-target="#patientsDatatable"
                        hx-include="[name='pageSize']"
                        hx-swap="outerHTML"
                        hx-trigger="click">
                    <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
                </button>
                <span class="mx-2 text-sm text-slate-500">Página <span id="currentPage">${patientsPage.getNumber() + 1}</span> de <span>${patientsPage.getTotalPages()}</span></span>
                <button id="nextPage"
                        class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200"
                        disabled="${!patientsPage.hasNext()}"
                        hx-get="/patient-list?page=${patientsPage.getNumber() + 1}&pagination=true"
                        hx-target="#patientsDatatable"
                        hx-include="[name='pageSize']"
                        hx-swap="outerHTML"
                        hx-trigger="click">
                    <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
                </button>
            </div>
        </div>
    </div>
    @endif
</div>
