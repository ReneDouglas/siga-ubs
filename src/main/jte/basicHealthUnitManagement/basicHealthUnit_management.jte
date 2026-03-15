@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import java.util.List
@param BasicHealthUnit basicHealthUnit
@param List<BasicHealthUnit> basicHealthUnits
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Gestão de UBS", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 - Cadastrar UBS --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel2" class="peer hidden" checked>
                <label for="panel2"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">apartment</span>Cadastrar Unidade Básica de Saúde
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 gap-x-4 py-4 px-4 w-full"
                          action="${basicHealthUnit.getId() == null ? "/basicHealthUnit-management/create" : "/basicHealthUnit-management/update"}"
                          method="post">
                        <div class="flex flex-col w-full col-span-2">
                            <label class="text-sm" for="name">Nome da Unidade:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="name" name="name"
                                   value="${basicHealthUnit.getName() != null ? basicHealthUnit.getName() : ""}" required>
                        </div>
                        <div class="flex flex-col w-full col-span-2">
                            <label class="text-sm" for="neighborhood">Bairro:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="neighborhood" name="neighborhood"
                                   value="${basicHealthUnit.getNeighborhood() != null ? basicHealthUnit.getNeighborhood() : ""}" required>
                        </div>
                        <input type="hidden" id="id" name="id" value="${basicHealthUnit.getId() != null ? basicHealthUnit.getId().toString() : ""}"/>
                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                        <div class="col-span-2 lg:col-span-4 text-center mt-2">
                            <button type="submit"
                                    class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Enviar</button>
                        </div>
                    </form>
                </div>
            </div>

            <%-- Panel 2 - Atualizar UBS --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel3" class="peer hidden" checked>
                <label for="panel3"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">edit</span>Atualizar Unidade Básica de Saúde
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div class="py-4 px-4">
                        <div>
                            <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 gap-x-4 w-full items-end"
                                  action="/basicHealthUnit-management" method="post">
                                <div class="flex flex-col w-full col-span-2">
                                    <label class="text-sm" for="basicHealthUnit">Unidades Básicas de Saúde:</label>
                                    <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                            id="basicHealthUnit" name="basicHealthUnit"
                                            hx-get="/basicHealthUnit-management/systemUsers"
                                            hx-target="#systemUsersUBStable">
                                        <option value="">Selecione</option>
                                        @for(var ubs : basicHealthUnits)
                                        <option value="${ubs.getId()}">${ubs.getName()}</option>
                                        @endfor
                                    </select>
                                </div>
                                <div class="flex items-end space-x-2">
                                    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                                    <button type="submit"
                                            class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Editar</button>
                                    <button type="submit" formaction="/basicHealthUnit-management/delete"
                                            class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">Deletar</button>
                                </div>
                            </form>
                        </div>
                        <div class="mt-4">
                            <form id="attachSystemUserForm"
                                  hx-post="/basicHealthUnit-management/systemUser/add"
                                  hx-target="#systemUsersUBStable"
                                  hx-include="[name='basicHealthUnit']"
                                  class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 gap-x-4 w-full items-end">

                                <div class="flex flex-col w-full col-span-2 relative">
                                    <label class="text-sm" for="systemUserSearch">Buscar usuário:</label>
                                    <input type="search" id="systemUserSearch" name="systemUserSearch"
                                           class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                           placeholder="Buscar..."
                                           hx-get="/basicHealthUnit-management/systemUser/search"
                                           hx-trigger="input changed delay:500ms, systemUserSearch"
                                           hx-target="#dropdown"/>
                                    <div class="absolute top-full w-full z-50">
                                        <div id="dropdown"
                                             class="bg-white border border-gray-300 rounded shadow-lg hidden"></div>
                                    </div>
                                    <input type="text" id="idSystemUser" name="idSystemUser" value=""
                                           style="display: none"/>
                                    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                                </div>
                                <div class="flex items-end">
                                    <button id="attachButton"
                                            class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Adicionar</button>
                                </div>
                            </form>
                        </div>
                        <div id="systemUsersUBStable" class="mt-4">
                            <div class="overflow-x-auto">
                            <table class="min-w-full bg-white">
                                <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
                                <tr class="bg-slate-200">
                                    <th class="text-sm rounded-l-lg py-2 px-4">Nome</th>
                                    <th class="text-sm pl-4">Perfil</th>
                                    <th class="text-sm pl-4">Ativo</th>
                                    <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                                    <td class="border-b text-sm py-2 px-4" colspan="4">Sem registro</td>
                                </tr>
                                </tbody>
                            </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </section>
</main>
</body>
<script>
    $(document).ready(function () {
        $('#dropdown').on('click', 'div', function () {
            var selectedText = $(this).find('span').text();
            var selectedId = $(this).find('input').val();
            $('#systemUserSearch').val(selectedText);
            $('#idSystemUser').val(selectedId);
            $('#dropdown').hide();
        });

        $(document).click(function (event) {
            var target = $(event.target);
            if (!target.closest('#dropdown').length && !target.closest('#systemUserSearch').length) {
                $('#dropdown').hide();
            }
        });

        $('#systemUserSearch').on('input', function () {
            $('#dropdown').show();
        });
    });
</script>
</html>
