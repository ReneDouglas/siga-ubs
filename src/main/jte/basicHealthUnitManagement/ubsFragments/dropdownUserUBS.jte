@import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO
@import java.util.List
@param List<UBSsystemUserDTO> users
@for(var user : users)
<div class="w-full p-2 text-sm text-slate-700 hover:bg-slate-100 cursor-pointer transition duration-200">
    <span>${user.name()}</span>
    <input type="hidden" value="${user.id()}">
</div>
@endfor
