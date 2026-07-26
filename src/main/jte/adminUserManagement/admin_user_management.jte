@import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO
@import br.com.tecsus.sigaubs.entities.SystemAdmin
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@param SystemAdmin systemAdmin
@param AdminUserSearchDTO searchAdmin = null
@param Page<SystemAdmin> adminsPage
@param String selectedSort = "creationDate"
@param String selectedDirection = "DESC"
@param String currentUsername = ""
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Administradores", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res, csrf = csrf, adminArea = true)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res, adminArea = true)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panelAdminForm" class="peer hidden" checked>
                <label for="panelAdminForm" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">admin_panel_settings</span>${systemAdmin.getId() == null ? "Cadastrar Administrador" : "Atualizar Administrador"}
                    </div>
                </label>
                <div class="flex justify-center peer-checked:max-h-screen max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          action="${systemAdmin.getId() == null ? "/admin/admin-user-management/create" : "/admin/admin-user-management/update"}"
                          method="post">

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="username">Login:</label>
                            @if(systemAdmin.getId() == null)
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="username" name="username" value="${systemAdmin.getUsername() != null ? systemAdmin.getUsername() : ""}" required>
                            @else
                            <span id="username" class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${systemAdmin.getUsername()}</span>
                            @endif
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="email">E-mail:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="email" id="email" name="email" value="${systemAdmin.getEmail() != null ? systemAdmin.getEmail() : ""}" required>
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="password">Senha:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="password" id="password" name="password" required="${systemAdmin.getId() == null}">
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="confirmPassword">Confirmação:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="password" id="confirmPassword" name="confirmPassword" required="${systemAdmin.getId() == null}">
                        </div>

                        <div class="flex flex-col w-full sm:col-span-2">
                            <label class="text-sm" for="name">Nome:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="name" name="name" value="${systemAdmin.getName() != null ? systemAdmin.getName() : ""}" required>
                        </div>

                        @if(systemAdmin.getId() != null)
                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="active">Ativo:</label>
                            <select id="active" name="active"
                                    class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200">
                                <option value="true" selected="${systemAdmin.getActive() != null && systemAdmin.getActive()}">Sim</option>
                                <option value="false" selected="${systemAdmin.getActive() != null && !systemAdmin.getActive()}">Não</option>
                            </select>
                        </div>
                        @endif

                        <input type="hidden" id="id" name="id" value="${systemAdmin.getId() != null ? systemAdmin.getId().toString() : ""}"/>
                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

                        <div class="col-span-1 sm:col-span-2 lg:col-span-4 text-center mt-2">
                            <button type="submit" class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Salvar</button>
                            @if(systemAdmin.getId() != null)
                            <a href="/admin/admin-user-management" class="text-sm inline-block rounded-lg bg-slate-100 py-2.5 px-8 text-slate-700 cursor-pointer hover:bg-slate-200 transition duration-300 ml-2">Novo</a>
                            @endif
                        </div>
                    </form>
                </div>
            </div>

            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panelAdminTable" class="peer hidden" checked>
                <label for="panelAdminTable" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">manage_accounts</span> Administradores
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form id="adminUserSearchForm" action="/admin/admin-user-management" method="get"
                          class="grid grid-flow-row grid-cols-1 sm:grid-cols-4 md:grid-cols-12 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          hx-get="/admin/admin-user-management?pagination=true"
                          hx-target="#adminUsersDatatable"
                          hx-swap="outerHTML">
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-3">
                            <label class="text-sm" for="usernameSearch">Login:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="usernameSearch" name="username" value="${searchAdmin != null && searchAdmin.getUsername() != null ? searchAdmin.getUsername() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-5">
                            <label class="text-sm" for="nameSearch">Nome:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="nameSearch" name="name" value="${searchAdmin != null && searchAdmin.getName() != null ? searchAdmin.getName() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-2">
                            <label class="text-sm" for="activeSearch">Ativo:</label>
                            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                    id="activeSearch" name="active">
                                <option value="">Todos</option>
                                <option value="true" selected="${searchAdmin != null && searchAdmin.getActive() != null && searchAdmin.getActive()}">Sim</option>
                                <option value="false" selected="${searchAdmin != null && searchAdmin.getActive() != null && !searchAdmin.getActive()}">Não</option>
                            </select>
                        </div>
                        <div class="inline-flex col-span-1 sm:col-span-4 md:col-span-2 justify-center sm:justify-start items-end gap-x-2 mt-2">
                            <button type="submit" class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                <span class="-ml-1 material-symbols-outlined mx-1">search</span>Buscar
                            </button>
                            <a href="/admin/admin-user-management" class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">
                                <span class="-ml-1 material-symbols-outlined mx-1">close</span>Limpar
                            </a>
                        </div>
                    </form>
                    <div class="pb-4 px-4 rounded-lg">
                        @template.adminUserManagement.adminUserFragments.admin_user_datatable(adminsPage = adminsPage, searchAdmin = searchAdmin, selectedSort = selectedSort, selectedDirection = selectedDirection, currentUsername = currentUsername, csrf = csrf)
                    </div>
                </div>
            </div>

        </div>
    </section>
    @template.fragments.footer()
</main>
</body>
</html>
