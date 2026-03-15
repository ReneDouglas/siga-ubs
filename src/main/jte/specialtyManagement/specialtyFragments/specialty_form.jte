@import br.com.tecsus.sigaubs.dtos.ProcedureDTO
@import br.com.tecsus.sigaubs.dtos.SpecialtyDTO
@import br.com.tecsus.sigaubs.enums.ProcedureType
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import java.util.List
@param SpecialtyDTO specialtyDTO
@param List<ProcedureDTO> procedures
@param CsrfHelper csrf
<div>
    <form action="${specialtyDTO.getId() == null ? "/specialty-management/create" : "/specialty-management/update"}"
          method="post"
          class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-8 xl:grid-cols-12 gap-2 sm:gap-x-4 sm:gap-y-2 w-full px-4 py-2">

        <%-- BLOCO: Especialidade / Ativo / Descrição --%>
        <div class="${specialtyDTO.getId() != null ? "col-span-1 sm:col-span-1 md:col-span-3 lg:col-span-2 xl:col-span-2" : "sm:col-span-1 md:col-span-2 lg:col-span-2 xl:col-span-2"}">
            <label class="text-sm" for="title">Especialidade: </label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   placeholder="Nome especialidade" type="text" id="title" name="title"
                   value="${specialtyDTO.getTitle() != null ? specialtyDTO.getTitle() : ""}">
        </div>

        @if(specialtyDTO.getId() != null)
        <div class="col-span-1">
            <label class="text-sm" for="active">Ativo: </label>
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="active" name="active">
                <option value="true" selected="${specialtyDTO.getActive() != null && specialtyDTO.getActive()}">Sim</option>
                <option value="false" selected="${specialtyDTO.getActive() != null && !specialtyDTO.getActive()}">Não</option>
            </select>
        </div>
        @endif

        <div class="${specialtyDTO.getId() != null ? "sm:col-span-2 md:col-span-4 lg:col-span-5 xl:col-span-4" : "sm:col-span-1 md:col-span-2 lg:col-span-6 xl:col-span-5"}">
            <label class="text-sm" for="description">Descrição: </label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   placeholder="Adicione uma descrição" type="text" id="description" name="description"
                   value="${specialtyDTO.getDescription() != null ? specialtyDTO.getDescription() : ""}">
        </div>

        <hr class="mt-2 col-span-full xl:hidden">

        <div class="sm:col-span-1 md:col-span-2 lg:col-span-4 xl:col-span-2">
            <label class="text-sm" for="procedure">Procedimento: </label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   placeholder="Nome procedimento" type="text" id="procedure">
        </div>

        <div class="flex w-full flex-wrap xl:flex-nowrap justify-center md:justify-end sm:justify-start items-end gap-x-4 sm:col-span-1 md:col-span-2 lg:col-span-4 xl:col-span-3 sm:gap-x-4">
            <div class="flex-1 xl:flex-auto">
                <label class="text-sm text-nowrap" for="procedureType">Tipo de procedimento:</label>
                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                        id="procedureType">
                    <option value="">Selecione</option>
                    @for(var type : ProcedureType.values())
                    <option value="${type.getDescription()}">${type.getDescription()}</option>
                    @endfor
                </select>
            </div>

            <button type="button" id="addProcedureBtn"
                    class="text-sm inline-block mt-4 rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                Adicionar
            </button>
        </div>

        <%-- TABELA DE PROCEDIMENTOS --%>
        <div class="overflow-x-auto col-span-full mt-2">
            <table id="procedureTable" class="table-auto w-full text-base border-collapse">
                <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
                <tr class="bg-slate-200">
                    <th class="text-sm rounded-l-lg py-2 px-4 pl-4">Nome</th>
                    <th class="w-full pl-4">Procedimentos</th>
                    <th class="text-sm rounded-r-lg py-2 px-4 w-full">Ações</th>
                </tr>
                </thead>
                <tbody>
                @for(var procedure : procedures)
                <tr>
                    <td class="border-b text-sm py-2 pl-4 text-nowrap">${procedure.getDescription()}</td>
                    <td class="border-b text-sm py-2 pl-4">${procedure.getProcedureType()}</td>
                    <td class="border-b text-sm py-2 pl-4">
                        <a class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer">
                            <span class="material-symbols-outlined hover:text-red-400 transition transition-300">delete</span>
                        </a>
                    </td>
                </tr>
                @endfor
                </tbody>
            </table>
        </div>

        <%-- BOTÕES --%>
        <div class="col-span-full my-2">
            <hr class="col-span-1 ${specialtyDTO.getId() != null ? "hidden" : ""}">
            <input type="hidden" id="proceduresJson" name="proceduresJson"/>
            <input type="hidden" id="id" name="id" value="${specialtyDTO.getId() != null ? specialtyDTO.getId().toString() : ""}"/>
            <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

            <div class="inline-flex w-full justify-center ${specialtyDTO.getId() == null ? "mt-4" : "mt-0"}">
                <button class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer transition duration-300 ${specialtyDTO.getId() != null ? "hover:bg-green-500 active:bg-green-600" : "hover:bg-blue-500 active:bg-blue-400"}">
                    ${specialtyDTO.getId() == null ? "Salvar" : "Editar"}
                </button>
            </div>
        </div>
    </form>
</div>
