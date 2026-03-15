@import br.com.tecsus.sigaubs.entities.Patient
@import java.util.List
@param List<Patient> patients
<div id="dropdown" class="absolute bg-white border border-gray-300 w-full mt-1 rounded shadow-lg z-10 ${patients.isEmpty() ? "hidden" : ""}">
    <ul>
        @for(var patient : patients)
        <li class="w-full p-2 hover:bg-gray-200">
            <a href="/appointment-management/load?id=${patient.getId()}">
                <strong>NOME: </strong><span>${patient.getName().toUpperCase()}</span>
                <strong> - SUS: </strong><span>${patient.getSusNumber()}</span>
                <strong> - CPF: </strong><span>${patient.getCpf()}</span>
            </a>
        </li>
        @endfor
    </ul>
</div>
