@import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO
@import br.com.tecsus.sigaubs.entities.SystemUser
@import br.com.tecsus.sigaubs.entities.Tenant
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@param Tenant tenant
@param SystemUser systemUser
@param SmsUserSearchDTO searchUser = null
@param Page<SystemUser> smsUsersPage
@param String selectedSort = "creationDate"
@param String selectedDirection = "DESC"
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Usuários SMS", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res, csrf = csrf, adminArea = true)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res, adminArea = true)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <div class="w-full bg-white rounded-lg mt-4 p-4">
                <div class="grid grid-flow-row grid-cols-1 sm:grid-cols-3 gap-2 gap-x-4">
                    <div class="flex flex-col">
                        <span class="font-semibold text-sm text-slate-700">Tenant</span>
                        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${tenant.getName()}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="font-semibold text-sm text-slate-700">Slug</span>
                        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${tenant.getSlug()}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="font-semibold text-sm text-slate-700">Status</span>
                        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${tenant.getStatus() != null ? tenant.getStatus().getDescription() : ""}</span>
                    </div>
                </div>
            </div>

            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panelSmsForm" class="peer hidden" checked>
                <label for="panelSmsForm" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">person_add</span>${systemUser.getId() == null ? "Cadastrar Usuário SMS" : "Atualizar Usuário SMS"}
                    </div>
                </label>
                <div class="flex justify-center peer-checked:max-h-screen max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          action="${systemUser.getId() == null ? "/admin/tenant-management/" + tenant.getId() + "/sms-users/create" : "/admin/tenant-management/" + tenant.getId() + "/sms-users/update"}"
                          method="post">

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="username">Login:</label>
                            @if(systemUser.getId() == null)
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="username" name="username" value="${systemUser.getUsername() != null ? systemUser.getUsername() : ""}" required>
                            @else
                            <span id="username" class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${systemUser.getUsername()}</span>
                            @endif
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="email">E-mail:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="email" id="email" name="email" value="${systemUser.getEmail() != null ? systemUser.getEmail() : ""}" required>
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="password">Senha:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="password" id="password" name="password" required="${systemUser.getId() == null}">
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="confirmPassword">Confirmação:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="password" id="confirmPassword" name="confirmPassword" required="${systemUser.getId() == null}">
                        </div>

                        <div class="flex flex-col w-full sm:col-span-2">
                            <label class="text-sm" for="name">Nome:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="name" name="name" value="${systemUser.getName() != null ? systemUser.getName() : ""}" required>
                        </div>

                        @if(systemUser.getId() != null)
                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="active">Ativo:</label>
                            <select id="active" name="active"
                                    class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200">
                                <option value="true" selected="${systemUser.getActive() != null && systemUser.getActive()}">Sim</option>
                                <option value="false" selected="${systemUser.getActive() != null && !systemUser.getActive()}">Não</option>
                            </select>
                        </div>
                        @endif

                        <input type="hidden" id="id" name="id" value="${systemUser.getId() != null ? systemUser.getId().toString() : ""}"/>
                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

                        <div class="col-span-1 sm:col-span-2 lg:col-span-4 text-center mt-2">
                            <button type="submit" class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Salvar</button>
                            @if(systemUser.getId() != null)
                            <a href="/admin/tenant-management/${tenant.getId()}/sms-users" class="text-sm inline-block rounded-lg bg-slate-100 py-2.5 px-8 text-slate-700 cursor-pointer hover:bg-slate-200 transition duration-300 ml-2">Novo</a>
                            @endif
                        </div>
                    </form>
                </div>
            </div>

            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panelSmsTable" class="peer hidden" checked>
                <label for="panelSmsTable" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">manage_accounts</span> Usuários SMS
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form id="smsUserSearchForm" action="/admin/tenant-management/${tenant.getId()}/sms-users" method="get"
                          class="grid grid-flow-row grid-cols-1 sm:grid-cols-4 md:grid-cols-12 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          hx-get="/admin/tenant-management/${tenant.getId()}/sms-users?pagination=true"
                          hx-target="#smsUsersDatatable"
                          hx-swap="outerHTML">
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-3">
                            <label class="text-sm" for="usernameSearch">Login:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="usernameSearch" name="username" value="${searchUser != null && searchUser.getUsername() != null ? searchUser.getUsername() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-5">
                            <label class="text-sm" for="nameSearch">Nome:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="nameSearch" name="name" value="${searchUser != null && searchUser.getName() != null ? searchUser.getName() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-2">
                            <label class="text-sm" for="activeSearch">Ativo:</label>
                            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                    id="activeSearch" name="active">
                                <option value="">Todos</option>
                                <option value="true" selected="${searchUser != null && searchUser.getActive() != null && searchUser.getActive()}">Sim</option>
                                <option value="false" selected="${searchUser != null && searchUser.getActive() != null && !searchUser.getActive()}">Não</option>
                            </select>
                        </div>
                        <div class="inline-flex col-span-1 sm:col-span-4 md:col-span-2 justify-center sm:justify-start items-end gap-x-2 mt-2">
                            <button type="submit" class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                <span class="-ml-1 material-symbols-outlined mx-1">search</span>Buscar
                            </button>
                            <a href="/admin/tenant-management/${tenant.getId()}/sms-users" class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">
                                <span class="-ml-1 material-symbols-outlined mx-1">close</span>Limpar
                            </a>
                        </div>
                    </form>
                    <div class="pb-4 px-4 rounded-lg">
                        @template.tenantManagement.smsUserFragments.sms_user_datatable(tenant = tenant, smsUsersPage = smsUsersPage, searchUser = searchUser, selectedSort = selectedSort, selectedDirection = selectedDirection, csrf = csrf)
                    </div>
                </div>
            </div>

        </div>
    </section>
    @template.fragments.footer()
</main>
</body>
</html>
