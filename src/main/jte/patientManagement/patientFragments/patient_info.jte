@import br.com.tecsus.sigaubs.entities.Patient
@import br.com.tecsus.sigaubs.security.CsrfHelper
@param Patient patient
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<div>
    @if(error != null && !error)
    <div class="border border-emerald-300 bg-emerald-100 text-emerald-700 rounded-lg px-4 py-3 mx-4 mt-4 text-sm flex items-center justify-between">
        <span>${message}</span>
        <button type="button" class="ml-4 font-bold leading-none" onclick="this.parentElement.remove()">×</button>
    </div>
    @endif
    @if(error != null && error)
    <div class="border border-red-300 bg-red-100 text-red-700 rounded-lg px-4 py-3 mx-4 mt-4 text-sm flex items-center justify-between">
        <span>${message}</span>
        <button type="button" class="ml-4 font-bold leading-none" onclick="this.parentElement.remove()">×</button>
    </div>
    @endif

    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 gap-x-4 py-4 px-4 w-full"
          hx-post="/patient-management/edit"
          hx-target="this"
          hx-swap="outerHTML">

        <div class="flex flex-col w-full col-span-2">
            <label class="text-sm text-slate-500">Nome completo:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getName()}</span>
            <input type="hidden" name="name" value="${patient.getName()}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Cartão SUS:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getSusNumber()}</span>
            <input type="hidden" name="susNumber" value="${patient.getSusNumber()}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">CPF:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getCpf()}</span>
            <input type="hidden" name="cpf" value="${patient.getCpf()}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Sexo:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getGender()}</span>
            <input type="hidden" name="gender" value="${patient.getGender()}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Data de Nascimento:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.formattedBirthDate()}</span>
            <input type="hidden" name="birthDate" value="${patient.getBirthDate() != null ? patient.getBirthDate().toString() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Situação Social:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getSocialSituationRating() != null ? patient.getSocialSituationRating().getDescription() : ""}</span>
            <input type="hidden" name="socialSituationRating" value="${patient.getSocialSituationRating() != null ? patient.getSocialSituationRating().name() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Telefone:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getPhoneNumber()}</span>
            <input type="hidden" name="phoneNumber" value="${patient.getPhoneNumber() != null ? patient.getPhoneNumber() : ""}"/>
        </div>
        <div class="flex flex-col w-full col-span-2">
            <label class="text-sm text-slate-500">Logradouro:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getAddressStreet()}</span>
            <input type="hidden" name="addressStreet" value="${patient.getAddressStreet() != null ? patient.getAddressStreet() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Número:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getAddressNumber()}</span>
            <input type="hidden" name="addressNumber" value="${patient.getAddressNumber() != null ? patient.getAddressNumber() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Complemento:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getAddressComplement()}</span>
            <input type="hidden" name="addressComplement" value="${patient.getAddressComplement() != null ? patient.getAddressComplement() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">Referência:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getAddressReference()}</span>
            <input type="hidden" name="addressReference" value="${patient.getAddressReference() != null ? patient.getAddressReference() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">ACS Responsável:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getAcsName()}</span>
            <input type="hidden" name="acsName" value="${patient.getAcsName() != null ? patient.getAcsName() : ""}"/>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm text-slate-500">UBS:</label>
            <span class="text-sm py-2 pl-1 border-b border-slate-200 mt-1">${patient.getBasicHealthUnit() != null ? patient.getBasicHealthUnit().getName() : ""}</span>
            <input type="hidden" name="basicHealthUnit.id" value="${patient.getBasicHealthUnit() != null ? patient.getBasicHealthUnit().getId().toString() : ""}"/>
        </div>

        <input type="hidden" id="id" name="id" value="${patient.getId() != null ? patient.getId().toString() : ""}"/>
        <input type="hidden" id="ubsid" name="basicHealthUnit.id" value="${patient.getBasicHealthUnit() != null ? patient.getBasicHealthUnit().getId().toString() : ""}"/>
        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

        <div class="flex items-center justify-center sm:justify-end sm:items-end space-x-4 mt-2 sm:mt-0 col-span-2 lg:col-span-4 w-full">
            <a href="/patient-management"
               class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                Novo Paciente
            </a>
            <button type="submit"
                    class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                Editar
            </button>
        </div>
    </form>
</div>
