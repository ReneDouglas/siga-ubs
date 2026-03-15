@param String unlink_message = null
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
        <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
            <td class="border-b text-sm py-2 px-4" colspan="4">Sem registro</td>
        </tr>
        </tbody>
    </table>
    </div>
    @if(unlink_message != null)
    <p class="text-sm text-slate-600 mt-2">${unlink_message}</p>
    @endif
</div>
