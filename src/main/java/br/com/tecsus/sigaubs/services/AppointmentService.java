package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.MedicalProceduresTotalDTO;
import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.ProcedureTypeTotalDTO;
import br.com.tecsus.sigaubs.entities.*;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.*;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.exceptions.AppointmentRegistrationFailureException;
import br.com.tecsus.sigaubs.services.exceptions.CancelAppointmentException;
import br.com.tecsus.sigaubs.services.exceptions.DuplicateAppointmentRegistrationException;
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
    public void registerAppointment(Appointment appointment, SystemUserDetails loggedUser) throws AppointmentRegistrationFailureException, DuplicateAppointmentRegistrationException {

        List<PatientOpenAppointmentDTO> patientOpenAppointments = appointmentRepository.findPatientOpenAppointments(appointment.getPatient().getId());
        MedicalProcedure medicalProcedure;

        final Appointment finalAppointment = appointment;
        boolean isDuplicated = patientOpenAppointments
                .stream()
                .anyMatch(patientOpenAppointment -> Objects.equals(patientOpenAppointment.medicalProcedureId(), finalAppointment.getMedicalProcedure().getId()));

        if (isDuplicated) {
            throw new DuplicateAppointmentRegistrationException("Existe, pelo menos, uma consulta marcada para este procedimento em aberto.");
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


    }

    @Transactional
    public void cancelSolicitation(Long id, SystemUserDetails loggedUser) throws CancelAppointmentException {

        Appointment appt = appointmentRepository.getReferenceById(id);

        appt.setStatus(AppointmentStatus.DESISTENCIA_PACIENTE);
        appt.setUpdateUser(loggedUser.getName());
        appt.setUpdateDate(LocalDateTime.now());

        appt = appointmentRepository.save(appt);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(appt, loggedUser.getName());

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
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Marcação não encontrada."));
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
