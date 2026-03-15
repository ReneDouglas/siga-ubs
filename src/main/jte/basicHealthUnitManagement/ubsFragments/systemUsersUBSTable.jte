@import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import java.util.List
@param List<UBSsystemUserDTO> ubsUsers
@param CsrfHelper csrf
@param String attach_message = null
<div id="systemUsersUBStable">
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
        @for(var ubsUser : ubsUsers)
        <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
            <td class="border-b text-sm py-2 px-4">${ubsUser.name()}</td>
            <td class="border-b text-sm pl-4">${ubsUser.role()}</td>
            <td class="border-b text-sm pl-4">${ubsUser.active()}</td>
            <td class="border-b text-sm py-2 px-4">
                <form hx-post="/basicHealthUnit-management/systemUser/delete"
                      hx-target="#systemUsersUBStable"
                      hx-include="[name='basicHealthUnit']">
                    <input type="hidden" name="id" value="${ubsUser.id()}"/>
                    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                    <div class="relative group">
                        <button type="submit" class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                            <span class="material-symbols-outlined hover:text-red-500">link_off</span>
                        </button>
                        <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                            Desvincular
                        </div>
                    </div>
                </form>
            </td>
        </tr>
        @endfor
        </tbody>
    </table>
    </div>
    @if(attach_message != null)
    <p class="text-sm text-slate-600 mt-2">${attach_message}</p>
    @endif
</div>
