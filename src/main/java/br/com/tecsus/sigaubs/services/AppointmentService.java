package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.MedicalProceduresTotalDTO;
import br.com.tecsus.sigaubs.dtos.AppointmentCommandDTO;
import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.ProcedureTypeTotalDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.*;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.*;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.security.AuthorizationScopeService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PatientService patientService;
    private final AuthorizationScopeService authorizationScopeService;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
            MedicalProcedureRepository medicalProcedureRepository,
            AppointmentStatusHistoryService appointmentStatusHistoryService,
            PatientService patientService,
            AuthorizationScopeService authorizationScopeService) {
        this.appointmentRepository = appointmentRepository;
        this.medicalProcedureRepository = medicalProcedureRepository;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
        this.patientService = patientService;
        this.authorizationScopeService = authorizationScopeService;
    }

    AppointmentService(AppointmentRepository appointmentRepository,
            MedicalProcedureRepository medicalProcedureRepository,
            AppointmentStatusHistoryService appointmentStatusHistoryService) {
        this(appointmentRepository, medicalProcedureRepository, appointmentStatusHistoryService, null,
                new AuthorizationScopeService());
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
    public ResultadoOperacao<Void> registerAppointment(
            AppointmentCommandDTO command, SystemUserDetails loggedUser) {
        if (patientService == null) {
            return ResultadoOperacao.falha("Serviço de paciente indisponível.");
        }
        var patientResult = patientService.findPatientToEdit(command.getPatient().getId(), loggedUser);
        if (patientResult.falhou()) {
            return ResultadoOperacao.falha("Paciente não encontrado no escopo autorizado.");
        }
        Patient patient = patientResult.valor();

        MedicalProcedure medicalProcedure = medicalProcedureRepository.findById(command.getMedicalProcedure().getId())
                .orElse(null);
        if (medicalProcedure == null) {
            return ResultadoOperacao.falha("Procedimento não encontrado.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setMedicalProcedure(medicalProcedure);
        appointment.setPriority(command.getPriority());
        appointment.setObservation(command.getObservation() == null ? null : command.getObservation().trim());
        return persistNewAppointment(appointment, loggedUser);
    }

    @Transactional
    public ResultadoOperacao<Void> registerAppointment(Appointment appointment, SystemUserDetails loggedUser) {
        if (appointment == null
                || appointment.getPatient() == null
                || appointment.getPatient().getId() == null
                || appointment.getMedicalProcedure() == null
                || appointment.getMedicalProcedure().getId() == null) {
            return ResultadoOperacao.falha("Dados da marcação incompletos.");
        }
        if (patientService != null) {
            var patientResult = patientService.findPatientToEdit(appointment.getPatient().getId(), loggedUser);
            if (patientResult.falhou()) {
                return ResultadoOperacao.falha("Paciente não encontrado no escopo autorizado.");
            }
            appointment.setPatient(patientResult.valor());
            var procedure = medicalProcedureRepository.findById(appointment.getMedicalProcedure().getId()).orElse(null);
            if (procedure == null) {
                return ResultadoOperacao.falha("Procedimento não encontrado.");
            }
            appointment.setMedicalProcedure(procedure);
        }
        return persistNewAppointment(appointment, loggedUser);
    }

    private ResultadoOperacao<Void> persistNewAppointment(
            Appointment appointment, SystemUserDetails loggedUser) {

        List<PatientOpenAppointmentDTO> patientOpenAppointments = appointmentRepository.findPatientOpenAppointments(appointment.getPatient().getId());

        final Appointment finalAppointment = appointment;
        boolean isDuplicated = patientOpenAppointments
                .stream()
                .anyMatch(patientOpenAppointment -> Objects.equals(patientOpenAppointment.medicalProcedureId(), finalAppointment.getMedicalProcedure().getId()));

        if (isDuplicated) {
            return ResultadoOperacao.falha("Existe, pelo menos, uma consulta marcada para este procedimento em aberto.");
        }


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

        Appointment appt = appointmentRepository.findByIdWithQueueDetails(id).orElse(null);
        if (appt == null) {
            return ResultadoOperacao.falha("Marcação não encontrada.");
        }
        authorizationScopeService.requireBasicHealthUnit(
                loggedUser,
                appt.getPatient().getBasicHealthUnit() != null
                        ? appt.getPatient().getBasicHealthUnit().getId()
                        : null);
        if (appt.getStatus() != AppointmentStatus.AGUARDANDO_CONTEMPLACAO
                || appt.getContemplation() != null) {
            return ResultadoOperacao.falha("A marcação não pode ser cancelada no estado atual.");
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
    public ResultadoOperacao<Appointment> findByIdWithQueueDetails(Long id, SystemUserDetails loggedUser) {
        var result = findByIdWithQueueDetails(id);
        if (result.falhou()) {
            return result;
        }
        Appointment appointment = result.valor();
        authorizationScopeService.requireBasicHealthUnit(
                loggedUser,
                appointment.getPatient().getBasicHealthUnit() != null
                        ? appointment.getPatient().getBasicHealthUnit().getId()
                        : null);
        return result;
    }

    @Transactional(readOnly = true)
    public Appointment findReferenceById(Long id) {
        return appointmentRepository.getReferenceById(id);
    }

    @Transactional
    public Appointment findForUpdateWithQueueDetails(Long id) {
        return appointmentRepository.findByIdForUpdateWithQueueDetails(id).orElse(null);
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
