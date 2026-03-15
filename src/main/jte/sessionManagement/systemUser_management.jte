@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.entities.SystemRole
@import br.com.tecsus.sigaubs.entities.SystemUser
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@import java.util.List
@param SystemUser systemUser
@param SystemUser searchUser = null
@param Page<SystemUser> systemUsersPage
@param List<SystemRole> rolesList
@param List<BasicHealthUnit> basicHealthUnits
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Gestão de Usuários", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 - Cadastrar Usuário --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">person_add</span>Cadastrar Usuário
                    </div>
                </label>
                <div class="flex justify-center peer-checked:max-h-screen max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          action="${systemUser.getId() == null ? "/systemUser-management/create" : "/systemUser-management/update"}"
                          method="post">

                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label class="text-sm" for="username">Nome de Usuário:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="username" name="username"
                                   value="${systemUser.getUsername() != null ? systemUser.getUsername() : ""}"
                                   disabled="${systemUser.getUsername() != null}"
                                   placeholder="Crie o seu nome de usuário" required>
                        </div>
                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label class="text-sm" for="email">E-mail:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   placeholder="Digite seu E-mail" type="email" id="email" name="email"
                                   value="${systemUser.getEmail() != null ? systemUser.getEmail() : ""}" required>
                        </div>
                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label class="text-sm" for="password">Senha de Acesso:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   placeholder="Digite uma Senha" type="password" id="password" name="password" required>
                        </div>
                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label class="text-sm" for="confirmPassword">Confirmação de Senha:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   placeholder="Confirme a sua Senha" type="password" id="confirmPassword" name="confirmPassword" required>
                        </div>

                        <hr class="col-span-2 lg:col-span-4 mt-4 mb-2">

                        <div class="flex flex-col w-full col-span-2 sm:col-span-2">
                            <label class="text-sm" for="name">Nome Completo:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   placeholder="Digite o Nome Completo do Usuário" type="text" id="name" name="name"
                                   value="${systemUser.getName() != null ? systemUser.getName() : ""}" required>
                        </div>

                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label class="text-sm text-nowrap overflow-hidden overflow-ellipsis" for="basicHealthUnit">Selecione a Ubs:</label>
                            <select id="basicHealthUnit" name="basicHealthUnit"
                                    class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200">
                                <option value="" disabled selected="${systemUser.getBasicHealthUnit() == null}">Selecione UBS</option>
                                @for(var ubs : basicHealthUnits)
                                <option value="${ubs.getId()}"
                                        selected="${systemUser.getBasicHealthUnit() != null && systemUser.getBasicHealthUnit().getId().equals(ubs.getId())}">
                                    ${ubs.getName()}
                                </option>
                                @endfor
                            </select>
                        </div>

                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label class="text-sm text-nowrap overflow-hidden overflow-ellipsis" for="selectedRoleId">Selecione o Perfil:</label>
                            <select id="selectedRoleId" name="selectedRoleId"
                                    class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200">
                                <option value="" disabled hidden="${!systemUser.getRoles().isEmpty()}">Perfil Usuário</option>
                                @for(var role : rolesList)
                                <option value="${role.getId()}"
                                        selected="${systemUser.getRoles().isEmpty() ? role.getId().equals(2L) : role.getId().equals(systemUser.getFirstRole())}">
                                    ${role.getTitle()}
                                </option>
                                @endfor
                            </select>
                        </div>

                        @if(systemUser.getId() != null)
                        <div class="flex flex-col w-full col-span-2 sm:col-span-1">
                            <label for="active">Ativo:</label>
                            <select id="active" name="active"
                                    class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200">
                                <option value="true" selected="${systemUser.getActive() != null && systemUser.getActive()}">Sim</option>
                                <option value="false" selected="${systemUser.getActive() != null && !systemUser.getActive()}">Não</option>
                            </select>
                        </div>
                        @endif

                        <input type="hidden" id="id" name="id" value="${systemUser.getId() != null ? systemUser.getId().toString() : ""}"/>
                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

                        <div class="col-span-2 lg:col-span-4 text-center mt-2">
                            <button type="submit" class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition transition-300">Enviar</button>
                        </div>
                    </form>
                </div>
            </div>

            <%-- Panel 2 - Buscar Usuários --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel2" class="peer hidden" checked>
                <label for="panel2" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">person_search</span> Buscar Usuários
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div id="search-panel">
                        <%-- Search form --%>
                        <form id="searchForm" action="/systemUser-management" method="get"
                              class="grid grid-flow-row grid-cols-1 sm:grid-cols-4 md:grid-cols-12 lg:grid-cols-12 xl:grid-cols-12 2xl:grid-cols-12 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full">

                            <div class="flex flex-col w-full col-span-2 sm:col-span-2 md:col-span-8 lg:col-span-6 2xl:col-span-3">
                                <label class="text-sm" for="nameSearch">Nome:</label>
                                <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                       placeholder="Nome Pessoal" type="text" id="nameSearch" name="name"
                                       value="${searchUser != null && searchUser.getName() != null ? searchUser.getName() : ""}">
                            </div>

                            <div class="flex flex-col w-full col-span-2 sm:col-span-1 md:col-span-4 lg:col-span-4 xl:col-span-3 2xl:col-span-2">
                                <label class="text-sm" for="usernameSearch">Login:</label>
                                <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                       type="text" id="usernameSearch" name="username"
                                       value="${searchUser != null && searchUser.getUsername() != null ? searchUser.getUsername() : ""}"
                                       placeholder="Nome Usuário:">
                            </div>

                            <div class="flex flex-col w-full col-span-2 sm:col-span-1 md:col-span-4 lg:col-span-2 xl:col-span-3 2xl:col-span-2">
                                <label class="text-sm text-nowrap overflow-hidden overflow-ellipsis" for="basicHealthUnitSearch">UBS:</label>
                                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="basicHealthUnitSearch" name="basicHealthUnit">
                                    <option value="">Selecione</option>
                                    @for(var ubs : basicHealthUnits)
                                    <option value="${ubs.getId()}">${ubs.getName()}</option>
                                    @endfor
                                </select>
                            </div>

                            <div class="flex flex-col w-full col-span-2 sm:col-span-1 md:col-span-4 lg:col-span-2 xl:col-span-3 2xl:col-span-1">
                                <label class="text-sm text-nowrap overflow-hidden overflow-ellipsis" for="selectedRoleIdSearch">Perfil:</label>
                                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="selectedRoleIdSearch" name="selectedRoleId">
                                    <option value="">Selecione</option>
                                    @for(var role : rolesList)
                                    <option value="${role.getId()}">${role.getTitle()}</option>
                                    @endfor
                                </select>
                            </div>

                            <div class="flex flex-col w-full col-span-2 sm:col-span-1 md:col-span-4 lg:col-span-2 xl:col-span-3 2xl:col-span-1">
                                <label class="text-sm" for="activeSearch">Ativo:</label>
                                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="activeSearch" name="active">
                                    <option value="">Selecione</option>
                                    <option value="true">Sim</option>
                                    <option value="false">Não</option>
                                </select>
                            </div>

                            <div class="inline-flex col-span-2 justify-center lg:justify-start sm:justify-start md:justify-center items-end gap-x-2 mt-2 sm:col-span-2 md:col-span-12 lg:col-span-4 xl:col-span-3 2xl:col-span-3">
                                <button type="submit"
                                        class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition transition-300">
                                    <span class="-ml-1 material-symbols-outlined mx-1">search</span>Buscar
                                </button>
                                <button type="button" id="clearButton"
                                        class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">
                                    <span class="-ml-1 material-symbols-outlined mx-1">close</span>Limpar
                                </button>
                            </div>
                            <hr class="col-span-2 sm:col-span-4 md:col-span-12 lg:col-span-12 xl:col-span-12 2xl:col-span-12 mt-2">
                        </form>
                    </div>
                    <div class="mt-2">
                        <div class="pb-4 px-4 rounded-lg" id="datatable-panel">
                            @template.sessionManagement.sessionFragments.systemUser_datatable(systemUsersPage = systemUsersPage, csrf = csrf)
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </section>
</main>
</body>
<script src="${res.url("/js/sessionManagement.js")}"></script>
</html>
