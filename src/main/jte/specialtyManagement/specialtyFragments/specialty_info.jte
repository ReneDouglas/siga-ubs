@import br.com.tecsus.sigaubs.entities.Specialty
@import java.util.List
@param List<Specialty> specialties
<div>
    <div class="overflow-auto">
        <table class="table-auto w-full text-base" id="systemUserDatatable">
            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
            <tr class="bg-slate-200">
                <th class="text-sm rounded-l-lg py-2 px-4">N°</th>
                <th class="text-sm pl-4">Nome</th>
                <th class="text-sm pl-4">Descrição</th>
                <th class="text-sm pl-4">Ativo</th>
                <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
            </tr>
            </thead>
            <tbody>
            !{int idx = 0;}
            @for(var specialty : specialties)
            <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                <td class="border-b text-sm py-4 px-4">${idx + 1}</td>
                <td class="border-b text-sm pl-4">${specialty.getTitle()}</td>
                <td class="border-b text-sm pl-4 py-2">${specialty.getDescription()}</td>
                <td class="border-b text-sm pl-4">
                    <div class="inline-block px-4 rounded-xl ${specialty.getActive() ? "bg-green-400" : "bg-red-400"}">
                        ${specialty.getActive() ? "Sim" : "Não"}
                    </div>
                </td>
                <td class="border-b text-sm px-4">
                    <div class="inline-flex">
                        <div class="relative group">
                            <a href="/specialty-management/specialty?id=${specialty.getId()}"
                               class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition transition-300">
                                <span class="material-symbols-outlined hover:text-blue-500">edit</span>
                            </a>
                            <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                                Editar
                            </div>
                        </div>
                        <div class="relative group cursor-pointer">
                            <a class="inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer">
                                <span class="material-symbols-outlined hover:text-red-400 transition transition-300">delete</span>
                            </a>
                            <div class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1">
                                Remover
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
            !{idx++;}
            @endfor
            </tbody>
        </table>
    </div>
</div>
