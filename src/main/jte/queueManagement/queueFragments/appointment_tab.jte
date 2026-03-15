@import br.com.tecsus.sigaubs.entities.Appointment
@import br.com.tecsus.sigaubs.entities.Contemplation
@import br.com.tecsus.sigaubs.enums.ProcedureType
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@param Appointment appointment
@param Boolean isContemplated
@param Contemplation contemplated = null
@param Integer availableSlots = 0
@param Long medicalSlotId = null
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param Long selectedMedicalProcedure = null
@param String selectedProcedureType = null
@param Long selectedMonth = null
<div class="grid gap-6 mt-4 p-2">
    <%-- Seção de Detalhes do Agendamento --%>
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <p class="flex flex-col">
            <span class="font-semibold text-sm text-slate-700">Data do agendamento:</span>
            <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${TemplateUtils.formatDateTime(appointment.getRequestDate())}</span>
        </p>

        <p class="flex flex-col">
            <span class="font-semibold text-sm text-slate-700">Marcação:</span>
            <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getMedicalProcedure().getProcedureType().getDescription()}</span>
        </p>

        @if(!ProcedureType.CONSULTA.equals(appointment.getMedicalProcedure().getProcedureType()))
        <p class="flex flex-col">
            <span class="font-semibold text-sm text-slate-700">Procedimento:</span>
            <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getMedicalProcedure().getDescription()}</span>
        </p>
        @endif

        <p class="flex flex-col">
            <span class="font-semibold text-sm text-slate-700">Prioridade:</span>
            <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getPriority().getDescription()}</span>
        </p>

        <p class="flex flex-col col-span-full">
            <span class="font-semibold text-sm text-slate-700">Observações:</span>
            <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getObservation() != null ? appointment.getObservation() : ""}</span>
        </p>
    </div>

    <%-- Seção de Contemplação --%>
    <div class="grid gap-4">
        <h3 class="text-sm font-semibold text-slate-200 bg-slate-900 p-2 rounded-lg border-2 border-slate-600">
            Contemplação</h3>

        @if(isContemplated)
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center">
            <p class="flex flex-col">
                <span class="font-semibold text-sm text-slate-700">Motivo de Contemplação:</span>
                <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getContemplation().getContemplatedBy().getDescription()}</span>
            </p>
            <p class="flex flex-col">
                <span class="font-semibold text-sm text-slate-700">Status:</span>
                <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getStatus().getDescription()}</span>
            </p>
            <p class="flex flex-col">
                <span class="font-semibold text-sm text-slate-700">Observações:</span>
                <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${appointment.getContemplation().getObservation() != null ? appointment.getContemplation().getObservation() : ""}</span>
            </p>
            <div class="col-span-full flex justify-end gap-4">
                <form id="confirmForm" action="/contemplation-management/confirm" method="post">
                    <input type="hidden" name="ubs" value="${selectedUBS}">
                    <input type="hidden" name="specialty" value="${selectedSpecialty}">
                    <input type="hidden" name="month" value="${selectedMonth}">
                    <input type="hidden" name="contemplationId" value="${contemplated != null ? contemplated.getId().toString() : ""}">
                    <button type="button"
                        class="text-sm inline-block rounded-lg bg-green-600 py-2.5 px-6 text-white cursor-pointer hover:bg-green-500 active:bg-green-400 transition duration-300"
                        onclick="handleConfirmation()">
                        Confirmar Presença
                    </button>
                </form>
                <form id="cancelForm" action="/contemplation-management/cancel" method="post">
                    <input type="hidden" name="ubs" value="${selectedUBS}">
                    <input type="hidden" name="specialty" value="${selectedSpecialty}">
                    <input type="hidden" name="month" value="${selectedMonth}">
                    <input type="hidden" name="contemplationId" value="${contemplated != null ? contemplated.getId().toString() : ""}">
                    <input type="hidden" id="reason" name="reason" value="">
                    <button type="button"
                        class="text-sm inline-block rounded-lg bg-red-600 py-2.5 px-6 text-white cursor-pointer hover:bg-red-500 active:bg-red-400 transition duration-300"
                        onclick="handleCancel()">
                        Cancelar
                    </button>
                </form>
            </div>
        </div>
        @else
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center">
            <p class="flex flex-col">
                <span class="font-semibold text-sm text-slate-700">Vagas disponíveis:</span>
                <span class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg">${availableSlots}</span>
            </p>
            <form id="contemplationForm" action="/queue-management/v2/contemplate" method="post" class="justify-self-end">
                <input type="hidden" name="ubs" value="${selectedUBS}">
                <input type="hidden" name="specialty" value="${selectedSpecialty}">
                <input type="hidden" name="medicalProcedure" value="${selectedMedicalProcedure}">
                <input type="hidden" name="procedureType" value="${selectedProcedureType != null ? selectedProcedureType : ""}">
                <input type="hidden" name="appointmentId" value="${appointment.getId()}">
                <input type="hidden" name="medicalSlotId" value="${medicalSlotId}">
                <input type="hidden" id="reason" name="reason" value="">
                <button type="button"
                    class="text-sm inline-block rounded-lg py-2.5 px-6 text-white cursor-pointer transition duration-300 ${availableSlots == 0 ? "bg-gray-600 opacity-50" : "bg-green-600 hover:bg-green-500 active:bg-green-400"}"
                    onclick="handleContemplation()" disabled="${availableSlots == 0}">
                    Contemplar Paciente
                </button>
            </form>
        </div>
        @endif
    </div>
</div>
