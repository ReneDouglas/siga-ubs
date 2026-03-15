@import br.com.tecsus.sigaubs.entities.Patient
@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.enums.SocialSituationRating
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import java.util.List
@param Patient patient
@param CsrfHelper csrf
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param Boolean error = null
@param String message = null
@param List<BasicHealthUnit> basicHealthUnits = null
@param BasicHealthUnit systemUserUBS = null
<div>
    @if(message != null)
    <div class="border rounded-lg px-4 py-3 mx-4 mt-4 text-sm flex items-center justify-between ${error != null && error ? "bg-red-100 text-red-700 border-red-300" : "bg-emerald-100 text-emerald-700 border-emerald-300"}">
        <span>${message}</span>
        <button type="button" class="ml-4 font-bold leading-none" onclick="this.parentElement.remove()">×</button>
    </div>
    @endif

    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 gap-x-4 py-4 px-4 w-full"
          hx-post="${patient.getId() == null ? "/patient-management/create" : "/patient-management/update"}"
          hx-target="this"
          hx-swap="outerHTML"
          hx-indicator=".animate-pulse">

        <div class="flex flex-col w-full col-span-2">
            <label class="text-sm" for="name">Nome completo:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="name" name="name" value="${patient.getName() != null ? patient.getName() : ""}" autocomplete="off" required>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="susNumber">Cartão SUS:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="susNumber" name="susNumber" value="${patient.getSusNumber() != null ? patient.getSusNumber() : ""}" autocomplete="off" required>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="cpf">CPF:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="cpf" name="cpf" value="${patient.getCpf() != null ? patient.getCpf() : ""}" autocomplete="off" required>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="gender">Sexo:</label>
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="gender" name="gender" required>
                <option value="" selected="${patient.getId() == null}">Selecione</option>
                <option value="Feminino" selected="${"Feminino".equals(patient.getGender())}">Feminino</option>
                <option value="Masculino" selected="${"Masculino".equals(patient.getGender())}">Masculino</option>
            </select>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="birthDate">Data de Nascimento:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="date" id="birthDate" name="birthDate" value="${patient.getBirthDate() != null ? patient.getBirthDate().toString() : ""}" required>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="socialSituationRating">Situação Social:</label>
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="socialSituationRating" name="socialSituationRating" required>
                <option value="" selected="${patient.getId() == null}">Selecione</option>
                @for(var situation : SocialSituationRating.values())
                <option value="${situation.name()}" selected="${situation.equals(patient.getSocialSituationRating())}">${situation.getDescription()}</option>
                @endfor
            </select>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="phoneNumber">Telefone:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="phoneNumber" name="phoneNumber" value="${patient.getPhoneNumber() != null ? patient.getPhoneNumber() : ""}" autocomplete="off" maxlength="15" required>
        </div>
        <div class="flex flex-col w-full col-span-2">
            <label class="text-sm" for="addressStreet">Logradouro: (rua, lote, comunidade ou sítio para zona rural):</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="addressStreet" name="addressStreet" value="${patient.getAddressStreet() != null ? patient.getAddressStreet() : ""}" autocomplete="off" required>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="addressNumber">Número:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="addressNumber" name="addressNumber" value="${patient.getAddressNumber() != null ? patient.getAddressNumber() : ""}" autocomplete="off">
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="addressComplement">Complemento:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="addressComplement" name="addressComplement" value="${patient.getAddressComplement() != null ? patient.getAddressComplement() : ""}" autocomplete="off">
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="addressReference">Referência:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="addressReference" name="addressReference" value="${patient.getAddressReference() != null ? patient.getAddressReference() : ""}" autocomplete="off">
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="acsName">ACS Responsável:</label>
            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                   type="text" id="acsName" name="acsName" value="${patient.getAcsName() != null ? patient.getAcsName() : ""}">
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="basicHealthUnit">UBS:</label>

            @if(secHelper.hasAnyRole("ADMIN", "SMS"))
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="ubs" name="basicHealthUnit.id" required>
                <option value="">Selecione</option>
                @if(basicHealthUnits != null)
                @for(var ubs : basicHealthUnits)
                <option value="${ubs.getId()}" selected="${patient.getBasicHealthUnit() != null && ubs.getId().equals(patient.getBasicHealthUnit().getId())}">${ubs.getName()}</option>
                @endfor
                @endif
            </select>
            @else
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="basicHealthUnit" name="basicHealthUnit.id" disabled>
                @if(patient.getId() == null && systemUserUBS != null)
                <option value="${systemUserUBS.getId()}">${systemUserUBS.getName()}</option>
                @elseif(patient.getId() != null && patient.getBasicHealthUnit() != null)
                <option value="${patient.getBasicHealthUnit().getId()}" selected>${patient.getBasicHealthUnit().getName()}</option>
                @endif
            </select>
            @endif
        </div>

        <input type="hidden" id="id" name="id" value="${patient.getId() != null ? patient.getId().toString() : ""}"/>
        @if(patient.getId() != null)
        <input type="hidden" id="ubsid" name="basicHealthUnit.id" value="${patient.getBasicHealthUnit() != null ? patient.getBasicHealthUnit().getId().toString() : ""}"/>
        @endif
        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

        <div class="flex items-center justify-center sm:justify-end sm:items-end space-x-4 mt-2 sm:mt-0 col-span-2 lg:col-span-4 w-full">
            <button type="submit"
                    class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Enviar</button>
            <a href="/patient-management/cancel"
               class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">Cancelar</a>
        </div>
    </form>
</div>
