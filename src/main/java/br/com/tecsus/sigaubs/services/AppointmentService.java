package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.MedicalProceduresTotalDTO;
import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.ProcedureTypeTotalDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.*;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.*;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalProcedureRepository medicalProcedureRepository;
    private final AppointmentStatusHistoryService appointmentStatusHistoryService;

    public AppointmentService(AppointmentRepository appointmentRepository, MedicalProcedureRepository medicalProcedureRepository, AppointmentStatusHistoryService appointmentStatusHistoryService) {
        this.appointmentRepository = appointmentRepository;
        this.medicalProcedureRepository = medicalProcedureRepository;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
    }


    public List<MedicalProcedure> findBySpecialtyIdAndProcedureType(Long specialtyId, ProcedureType procedureType) {
        Specialty specialty = new Specialty();
        specialty.setId(specialtyId);
        return medicalProcedureRepository.findAllBySpecialtyAndProcedureType(specialty, procedureType);
    }

    public List<PatientOpenAppointmentDTO> findPatientOpenAppointments(Long patientId) {
        return appointmentRepository.findPatientOpenAppointments(patientId);
    }

    @Transactional
    public ResultadoOperacao<Void> registerAppointment(Appointment appointment, SystemUserDetails loggedUser) {

        List<PatientOpenAppointmentDTO> patientOpenAppointments = appointmentRepository.findPatientOpenAppointments(appointment.getPatient().getId());
        MedicalProcedure medicalProcedure;

        final Appointment finalAppointment = appointment;
        boolean isDuplicated = patientOpenAppointments
                .stream()
                .anyMatch(patientOpenAppointment -> Objects.equals(patientOpenAppointment.medicalProcedureId(), finalAppointment.getMedicalProcedure().getId()));

        if (isDuplicated) {
            return ResultadoOperacao.falha("Existe, pelo menos, uma consulta marcada para este procedimento em aberto.");
        }


        //Patient patient = patientRepository.getReferenceById(appointment.getPatient().getId());
        //medicalProcedure = medicalProcedureRepository.getReferenceById(appointment.getMedicalProcedure().getId());

        //appointment.setPatient(patient);
        //appointment.setMedicalProcedure(medicalProcedure);
        appointment.setContemplation(null);
        appointment.setStatus(AppointmentStatus.AGUARDANDO_CONTEMPLACAO);
        appointment.setCreationUser(loggedUser.getName());
        appointment.setRequestDate(LocalDateTime.now());

        appointment = appointmentRepository.save(appointment);

        appointmentStatusHistoryService.registerAppointmentStatusHistory(appointment, loggedUser.getName());

        return ResultadoOperacao.sucessoSemValor();

    }

    @Transactional
    public ResultadoOperacao<Void> cancelSolicitation(Long id, SystemUserDetails loggedUser) {

        Appointment appt = appointmentRepository.findById(id).orElse(null);
        if (appt == null) {
            return ResultadoOperacao.falha("Marcação não encontrada.");
        }

        appt.setStatus(AppointmentStatus.DESISTENCIA_PACIENTE);
        appt.setUpdateUser(loggedUser.getName());
        appt.setUpdateDate(LocalDateTime.now());

        appt = appointmentRepository.save(appt);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(appt, loggedUser.getName());

        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional(readOnly = true)
    public Page<PatientOpenAppointmentDTO> findOpenAppointmentsQueuePaginated(ProcedureType type, Long ubsId, Long specialtyId, Pageable pageable) {
        return appointmentRepository.findOpenAppointmentsQueuePaginated(type, ubsId, specialtyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PatientOpenAppointmentDTO> findOpenAppointmentsQueuePaginatedV2(Long ubsId, Long specialtyId, Long medicalProcedureId, Pageable pageable) {
        return appointmentRepository.findOpenAppointmentsQueuePaginatedV2(ubsId, specialtyId, medicalProcedureId, pageable);
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<Appointment> findById(Long id) {
        return appointmentRepository.findById(id)
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Marcação não encontrada."));
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<Appointment> findByIdWithQueueDetails(Long id) {
        return appointmentRepository.findByIdWithQueueDetails(id)
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Marcação não encontrada."));
    }

    @Transactional(readOnly = true)
    public Appointment findReferenceById(Long id) {
        return appointmentRepository.getReferenceById(id);
    }

    public List<ProcedureTypeTotalDTO> findProcedureTypeTotal(Long ubsId, Long specialtyId) {
        return appointmentRepository.totalByProcedureTypeAndUBSAndSpecialty(ubsId, specialtyId);
    }

    public List<MedicalProceduresTotalDTO> findMedicalProceduresTotal(Long ubsId, Long specialtyId) {
        return appointmentRepository.totalByMedicalProceduresAndUBSAndSpecialty(ubsId, specialtyId);
    }

    @Transactional
    public Appointment updateAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }


}
