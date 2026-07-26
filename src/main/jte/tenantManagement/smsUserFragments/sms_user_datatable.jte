@import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO
@import br.com.tecsus.sigaubs.entities.SystemUser
@import br.com.tecsus.sigaubs.entities.Tenant
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import org.springframework.data.domain.Page
@param Tenant tenant
@param Page<SystemUser> smsUsersPage
@param SmsUserSearchDTO searchUser = null
@param String selectedSort = "creationDate"
@param String selectedDirection = "DESC"
@param CsrfHelper csrf
<div id="smsUsersDatatable">
    <div class="overflow-x-auto">
        <table class="min-w-full bg-white">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
            <tr class="bg-slate-200">
                <th class="text-sm rounded-l-lg py-2 px-4">
                    <button type="button"
                            hx-get="/admin/tenant-management/${tenant.getId()}/sms-users?pagination=true&page=0&pageSize=${smsUsersPage.getSize()}&sort=username&direction=${"username".equals(selectedSort) && "ASC".equals(selectedDirection) ? "DESC" : "ASC"}"
                            hx-target="#smsUsersDatatable" hx-include="#smsUserSearchForm" hx-swap="outerHTML"
                            class="inline-flex items-center gap-1">Login<span class="material-symbols-outlined text-sm">swap_vert</span></button>
                </th>
                <th class="text-sm pl-4">
                    <button type="button"
                            hx-get="/admin/tenant-management/${tenant.getId()}/sms-users?pagination=true&page=0&pageSize=${smsUsersPage.getSize()}&sort=name&direction=${"name".equals(selectedSort) && "ASC".equals(selectedDirection) ? "DESC" : "ASC"}"
                            hx-target="#smsUsersDatatable" hx-include="#smsUserSearchForm" hx-swap="outerHTML"
                            class="inline-flex items-center gap-1">Nome<span class="material-symbols-outlined text-sm">swap_vert</span></button>
                </th>
                <th class="text-sm pl-4">E-mail</th>
                <th class="text-sm pl-4">Ativo</th>
                <th class="text-sm pl-4">Criado em</th>
                <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
            </tr>
            </thead>
            <tbody>
            @for(var user : smsUsersPage.getContent())
            <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                <td class="border-b text-sm py-2 px-4 text-nowrap">${user.getUsername()}</td>
                <td class="border-b text-sm py-2 pl-4">${user.getName()}</td>
                <td class="border-b text-sm py-2 pl-4">${user.getEmail()}</td>
                <td class="border-b text-sm py-2 pl-4">
                    <span class="inline-block px-3 py-1 rounded-xl text-xs ${user.getActive() != null && user.getActive() ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}">
                        ${user.getActive() != null && user.getActive() ? "Sim" : "Não"}
                    </span>
                </td>
                <td class="border-b text-sm py-2 pl-4 text-nowrap">${TemplateUtils.formatDateTime(user.getCreationDate())}</td>
                <td class="border-b text-sm py-2 px-4">
                    <div class="inline-flex items-center gap-1">
                        <a href="/admin/tenant-management/${tenant.getId()}/sms-users?editUserId=${user.getId()}" class="relative group inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                            <span class="material-symbols-outlined hover:text-blue-500 transition duration-300">edit</span>
                            <span class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1 whitespace-nowrap">Editar</span>
                        </a>
                        @if(user.getActive() != null && user.getActive())
                        <form action="/admin/tenant-management/${tenant.getId()}/sms-users/deactivate" method="post" class="inline-flex">
                            <input type="hidden" name="id" value="${user.getId()}"/>
                            <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                            <button type="submit" class="relative group inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                                <span class="material-symbols-outlined hover:text-red-500 transition duration-300">block</span>
                                <span class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1 whitespace-nowrap">Desativar</span>
                            </button>
                        </form>
                        @else
                        <form action="/admin/tenant-management/${tenant.getId()}/sms-users/activate" method="post" class="inline-flex">
                            <input type="hidden" name="id" value="${user.getId()}"/>
                            <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                            <button type="submit" class="relative group inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                                <span class="material-symbols-outlined hover:text-green-600 transition duration-300">restart_alt</span>
                                <span class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1 whitespace-nowrap">Ativar</span>
                            </button>
                        </form>
                        @endif
                    </div>
                </td>
            </tr>
            @endfor
            </tbody>
        </table>
    </div>

    @if(smsUsersPage.hasContent())
    <div class="flex flex-1 mt-4 justify-between items-center bg-white" id="paginationPanel">
        <div>
            <label class="text-sm text-slate-500" for="pageSize">Linhas por página:</label>
            <select id="pageSize" name="pageSize"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500"
                    hx-get="/admin/tenant-management/${tenant.getId()}/sms-users?pagination=true&page=${smsUsersPage.getNumber()}&sort=${selectedSort}&direction=${selectedDirection}"
                    hx-target="#smsUsersDatatable"
                    hx-include="#smsUserSearchForm"
                    hx-swap="outerHTML">
                <option value="10" selected="${smsUsersPage.getSize() == 10}">10</option>
                <option value="15" selected="${smsUsersPage.getSize() == 15}">15</option>
                <option value="30" selected="${smsUsersPage.getSize() == 30}">30</option>
            </select>
        </div>
        <div class="flex items-center" id="pagination">
            <button id="prevPage"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed"
                    disabled="${!smsUsersPage.hasPrevious()}"
                    hx-get="/admin/tenant-management/${tenant.getId()}/sms-users?pagination=true&page=${smsUsersPage.getNumber() - 1}&sort=${selectedSort}&direction=${selectedDirection}"
                    hx-target="#smsUsersDatatable"
                    hx-include="#smsUserSearchForm, [name='pageSize']"
                    hx-swap="outerHTML"
                    hx-trigger="click">
                <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
            </button>
            <span class="mx-2 text-sm text-slate-500">Página <span id="currentPage">${smsUsersPage.getNumber() + 1}</span> de <span>${smsUsersPage.getTotalPages()}</span></span>
            <button id="nextPage"
                    class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed"
                    disabled="${!smsUsersPage.hasNext()}"
                    hx-get="/admin/tenant-management/${tenant.getId()}/sms-users?pagination=true&page=${smsUsersPage.getNumber() + 1}&sort=${selectedSort}&direction=${selectedDirection}"
                    hx-target="#smsUsersDatatable"
                    hx-include="#smsUserSearchForm, [name='pageSize']"
                    hx-swap="outerHTML"
                    hx-trigger="click">
                <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
            </button>
        </div>
    </div>
    @endif
</div>
