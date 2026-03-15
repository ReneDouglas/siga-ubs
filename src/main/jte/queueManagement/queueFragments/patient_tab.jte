@import br.com.tecsus.sigaubs.entities.Patient
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@param Patient patient
<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 mt-4 p-2">
    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Nome:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getName()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Cartão SUS:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getSusNumber()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">CPF:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getCpf()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Sexo:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getGender()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Data de Nascimento:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getBirthDateWithAge()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Situação Social:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getSocialSituationRating().getDescription()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Telefone:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getPhoneNumber() != null ? patient.getPhoneNumber() : ""}</span>
    </p>

    <p class="flex flex-col sm:col-span-2">
        <span class="font-semibold text-sm text-slate-700">Logradouro:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getAddressStreet() + ", " + (patient.getAddressNumber() != null ? patient.getAddressNumber() : "") + ", " + (patient.getAddressComplement() != null ? patient.getAddressComplement() : "")}</span>
    </p>

    <p class="flex flex-col sm:col-span-2 md:col-span-1">
        <span class="font-semibold text-sm text-slate-700">Ponto de Ref.:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getAddressReference() != null ? patient.getAddressReference() : ""}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">UBS:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getBasicHealthUnit().getName()}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">ACS Responsável:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${patient.getAcsName() != null ? patient.getAcsName() : ""}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Cadastrado em:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${TemplateUtils.formatDateTime(patient.getCreationDate())}</span>
    </p>

    <p class="flex flex-col">
        <span class="font-semibold text-sm text-slate-700">Última atualização:</span>
        <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${TemplateUtils.formatDateTime(patient.getUpdateDate())}</span>
    </p>

    <p class="col-span-full text-right">
        <button class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
            Acessar Paciente
        </button>
    </p>
</div>
