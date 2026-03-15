@import br.com.tecsus.sigaubs.entities.Appointment
@import br.com.tecsus.sigaubs.entities.Contemplation
@import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO
@import java.util.List
@param Appointment appointment = null
@param Contemplation contemplated = null
@param Boolean isContemplated
@param Integer availableSlots = 0
@param Long medicalSlotId = null
@param List<PatientOpenAppointmentDTO> patientOpenAppointments = null
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param Long selectedMedicalProcedure = null
@param String selectedProcedureType = null
@param Long selectedMonth = null
!{var appt = isContemplated ? contemplated.getAppointment() : appointment;}
<%-- Main modal --%>
<div id="patientAppointment-modal" tabindex="-1" aria-hidden="false"
    class="flex opacity-100 overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-30 justify-center items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full">

    <%-- Backdrop escurecido --%>
    <div class="fixed inset-0 bg-black opacity-50"></div>

    <div class="relative p-4 w-3/4 max-h-full">
        <%-- Modal content --%>
        <div class="relative bg-white rounded-lg shadow-2xl">
            <%-- Modal header --%>
            <div class="flex items-center justify-between p-4 md:p-5 bg-slate-900 rounded-t border-2 border-slate-600">
                <h3 class="text-xl font-semibold text-slate-200">
                    Detalhes do paciente
                </h3>
                <button type="button" onclick="closeModal()"
                    class="text-slate-400 bg-transparent hover:bg-slate-700 hover:text-white rounded-lg text-sm w-8 h-8 ms-auto inline-flex justify-center items-center">
                    <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none"
                        viewBox="0 0 14 14">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6" />
                    </svg>
                    <span class="sr-only">Close modal</span>
                </button>
            </div>
            <%-- Abas --%>
            <div class="flex space-x-1 border-b border-slate-300 mb-4">
                <button
                    class="modal-tab-btn w-1/3 py-2 text-sm text-center bg-slate-300 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                    onclick="showModalTab('patient-details', this)">
                    Informações do paciente
                </button>
                <button
                    class="modal-tab-btn w-1/3 py-2 text-sm text-center bg-slate-200 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                    onclick="showModalTab('appointment-details', this)">
                    Informações do agendamento
                </button>
                @if(isContemplated)
                <button
                    class="modal-tab-btn w-1/3 py-2 text-sm text-center bg-slate-200 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                    onclick="showModalTab('status-details', this)">
                    Historico de Status
                </button>
                @else
                <button
                    class="modal-tab-btn w-1/3 py-2 text-sm text-center bg-slate-200 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                    onclick="showModalTab('patientHistory-details', this)">
                    Historico do paciente
                </button>
                @endif
            </div>
            <div id="contentPanel">
                <div id="patient-details" class="p-1 bg-white">
                    @template.queueManagement.queueFragments.patient_tab(patient = appt.getPatient())
                </div>
                <div id="appointment-details" class="p-4 bg-white hidden">
                    @template.queueManagement.queueFragments.appointment_tab(
                        appointment = appt,
                        isContemplated = isContemplated,
                        contemplated = contemplated,
                        availableSlots = availableSlots,
                        medicalSlotId = medicalSlotId,
                        selectedUBS = selectedUBS,
                        selectedSpecialty = selectedSpecialty,
                        selectedMedicalProcedure = selectedMedicalProcedure,
                        selectedProcedureType = selectedProcedureType,
                        selectedMonth = selectedMonth
                    )
                </div>
                @if(isContemplated)
                <div id="status-details" class="p-4 bg-white hidden">
                    @template.queueManagement.queueFragments.appointmentStatusHistory_tab(contemplated = contemplated)
                </div>
                @else
                <div id="patientHistory-details" class="p-4 bg-white hidden">
                    @template.queueManagement.queueFragments.patientHistory_tab(patientOpenAppointments = patientOpenAppointments != null ? patientOpenAppointments : java.util.List.of())
                </div>
                @endif
            </div>
        </div>
    </div>
</div>
