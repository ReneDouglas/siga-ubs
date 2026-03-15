@import br.com.tecsus.sigaubs.entities.Patient
@import java.util.List
@param List<Patient> patients
<div id="dropdown"
     class="absolute bg-white border border-gray-300 w-full mt-1 rounded shadow-lg z-10 ${patients.isEmpty() ? "hidden" : ""}">
    <ul>
        @for(var patient : patients)
        <li onclick="autocompleteInputSearch('${patient.getName().replace("'", "\\'")}')"
            class="w-full p-2 hover:bg-gray-200 cursor-pointer">
            <span class="text-slate-600 font-medium">${patient.getName()}</span>
        </li>
        @endfor
    </ul>
</div>
