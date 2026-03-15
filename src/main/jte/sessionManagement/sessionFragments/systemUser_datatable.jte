@import br.com.tecsus.sigaubs.entities.SystemUser
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import org.springframework.data.domain.Page
@param Page<SystemUser> systemUsersPage
@param CsrfHelper csrf
<div>
    <div class="overflow-auto">
        <table class="table-auto w-full text-base" id="systemUserDatatable">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
            <tr class="bg-slate-200">
                <th class="text-sm rounded-l-lg py-2 px-4">Login</th>
                <th class="text-sm pl-4">Nome</th>
                <th class="text-sm pl-4">E-mail</th>
                <th class="text-sm pl-4">Perfil</th>
                <th class="text-sm pl-4">UBS</th>
                <th class="text-sm pl-4">Ativo</th>
                <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
            </tr>
            </thead>
            <tbody>
            @for(var user : systemUsersPage.getContent())
            <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                <td class="border-b text-sm py-2 pl-4">${user.getUsername()}</td>
                <td class="border-b text-sm py-4 pl-4 text-nowrap">${user.getName()}</td>
                <td class="border-b text-sm pl-4">${user.getEmail()}</td>
                <td class="border-b text-sm pl-4">
                    <label>
                        <select class="text-xs rounded-lg border border-stroke py-1 pl-2 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200">
                            @for(var role : user.getRoles())
                            <option>${role.getTitle()}</option>
                            @endfor
                        </select>
                    </label>
                </td>
                <td class="border-b text-sm pl-4 text-nowrap">
                    ${user.getBasicHealthUnit() != null ? user.getBasicHealthUnit().getName() : "Desvinculado"}
                </td>
                <td class="border-b text-sm py-2 pl-4">
                    <div class="inline-block px-4 rounded-xl ${user.getActive() != null && user.getActive() ? "bg-green-400" : "bg-red-400"}">
                        ${user.getActive() != null && user.getActive() ? "Sim" : "Não"}
                    </div>
                </td>
                <td class="border-b text-sm pl-4">
                    <div class="inline-flex">
                        <form action="/systemUser-management" method="post">
                            <input type="hidden" name="id" value="${user.getId()}"/>
                            <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                            <button class="inline-flex p-2 rounded-lg hover:bg-slate-100 transition transition-300"
                                    type="submit">
                                <span class="material-symbols-outlined hover:text-blue-400 transition transition-300">edit</span>
                            </button>
                        </form>
                        <form action="/systemUser-management/delete" method="post">
                            <input type="hidden" name="id" value="${user.getId()}"/>
                            <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                            <button class="inline-flex p-2 rounded-lg hover:bg-slate-100 cursor-pointer transition transition-300" type="submit">
                                <span class="material-symbols-outlined hover:text-red-400 transition transition-300">delete</span>
                            </button>
                        </form>
                    </div>
                </td>
            </tr>
            @endfor
            </tbody>
        </table>
    </div>

    <div class="flex flex-1 mt-4 justify-between items-center bg-white" id="paginationPanel">
        <div>
            <label class="text-sm text-slate-500" for="pageSize">Linhas por página:</label>
            <select id="pageSize" name="pageSize"
                    class="rounded-lg text-sm text-slate-500 border border-stroke py-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500">
                <option value="5" selected="${systemUsersPage.getSize() == 5}">5</option>
                <option value="10" selected="${systemUsersPage.getSize() == 10}">10</option>
                <option value="15" selected="${systemUsersPage.getSize() == 15}">15</option>
                <option value="30" selected="${systemUsersPage.getSize() == 30}">30</option>
            </select>
        </div>

        <div class="flex items-center" id="pagination">
            <button class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200"
                    id="prevPage" disabled="${!systemUsersPage.hasPrevious()}" onclick="goToPreviousPage(event)">
                <span class="material-symbols-outlined">keyboard_double_arrow_left</span>
            </button>

            <span class="mx-2 text-sm text-slate-500">Página <span id="currentPage">${systemUsersPage.getNumber() + 1}</span> de
                <span>${systemUsersPage.getTotalPages()}</span></span>

            <button class="inline-flex p-2 rounded-lg bg-slate-100 hover:bg-slate-200"
                    id="nextPage" disabled="${!systemUsersPage.hasNext()}" onclick="goToNextPage(event)">
                <span class="material-symbols-outlined">keyboard_double_arrow_right</span>
            </button>
        </div>
    </div>
</div>
