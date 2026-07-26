package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.PatientRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final BasicHealthUnitService basicHealthUnitService;

    public PatientService(PatientRepository patientRepository, BasicHealthUnitService basicHealthUnitService) {
        this.patientRepository = patientRepository;
        this.basicHealthUnitService = basicHealthUnitService;
    }

    @Transactional
    public ResultadoOperacao<Patient> registerPatient(Patient patient, SystemUserDetails loggedUser) {

        var basicHealthUnitResult = resolvePatientBasicHealthUnit(patient, loggedUser);
        if (basicHealthUnitResult.falhou()) {
            return ResultadoOperacao.falha(basicHealthUnitResult.mensagem());
        }

        patient.setBasicHealthUnit(basicHealthUnitResult.valor());
        patient.setCreationUser(loggedUser.getName());
        patient.setCreationDate(LocalDateTime.now());

        return ResultadoOperacao.sucesso(patientRepository.save(patient));
    }

    @Transactional
    public ResultadoOperacao<Patient> updatePatient(Patient patient, SystemUserDetails loggedUser) {
        var basicHealthUnitResult = resolvePatientBasicHealthUnit(patient, loggedUser);
        if (basicHealthUnitResult.falhou()) {
            return ResultadoOperacao.falha(basicHealthUnitResult.mensagem());
        }

        patient.setBasicHealthUnit(basicHealthUnitResult.valor());
        patient.setUpdateUser(loggedUser.getName());
        patient.setUpdateDate(LocalDateTime.now());
        return ResultadoOperacao.sucesso(patientRepository.save(patient));
    }

    public List<Patient> searchNativePatients(String terms, Long id) {
       return patientRepository.searchNativePatientsContainingByUBS(terms, id);
    }

    public List<Patient> searchNativePatients(String terms, SystemUserDetails loggedUser) {
       return patientRepository.searchNativePatientsContainingByUBS(terms, getScopedBasicHealthUnitId(loggedUser));
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<Patient> findByIdAndUBS(Long idPatient, Long idUBS) {

        if (idUBS == null) {
            return patientRepository.findById(idPatient)
                    .map(ResultadoOperacao::sucesso)
                    .orElseGet(() -> ResultadoOperacao.falha("Paciente não encontrado."));
        }

        BasicHealthUnit ubs = basicHealthUnitService.findReferenceById(idUBS);
        Patient patient = patientRepository.findByIdAndBasicHealthUnit(idPatient, ubs);
        if (patient == null) {
            return ResultadoOperacao.falha("Paciente não encontrado.");
        }
        return ResultadoOperacao.sucesso(patient);
    }

    @Transactional(readOnly = true)
    public Page<Patient> findPatientsPage(Patient patient, PageRequest pageRequest, SystemUserDetails loggedUser) {

        if (!canAccessAllBasicHealthUnits(loggedUser)) {
            BasicHealthUnit ubs = new BasicHealthUnit();
            ubs.setId(requireLoggedUserBasicHealthUnitId(loggedUser));
            patient.setBasicHealthUnit(ubs);
        } else if (patient.getBasicHealthUnit() != null && patient.getBasicHealthUnit().getId() == null) {
            patient.setBasicHealthUnit(null);
        }
        return patientRepository.findPatientsPaginated(patient, pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<PatientAppointmentsHistoryDTO> findPatientAppointmentsHistoryPage(Long patientId, PageRequest pageRequest, SystemUserDetails loggedUser) {

        Patient patient = new Patient();
        if (!canAccessAllBasicHealthUnits(loggedUser)) {
            BasicHealthUnit ubs = new BasicHealthUnit();
            ubs.setId(requireLoggedUserBasicHealthUnitId(loggedUser));
            patient.setBasicHealthUnit(ubs);
        }
        patient.setId(patientId);
        return patientRepository.findPatientAppointmentsHistoryPaginated(patient, pageRequest);
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<Patient> findPatientToEdit(Long id) {
        return patientRepository.findById(id)
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> {
            log.error("Paciente [id = {}] não encontrado.", id);
            return ResultadoOperacao.falha("Paciente não encontrado. Contate o TI.");
        });
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<Patient> findPatientToEdit(Long id, SystemUserDetails loggedUser) {
        if (canAccessAllBasicHealthUnits(loggedUser)) {
            return findPatientToEdit(id);
        }

        var patientResult = findByIdAndUBS(id, requireLoggedUserBasicHealthUnitId(loggedUser));
        if (patientResult.falhou()) {
            log.error("Paciente [id = {}] não encontrado para a UBS do usuário logado.", id);
            return ResultadoOperacao.falha("Paciente não encontrado. Contate o TI.");
        }
        return patientResult;
    }

    private ResultadoOperacao<BasicHealthUnit> resolvePatientBasicHealthUnit(Patient patient,
            SystemUserDetails loggedUser) {
        if (canAccessAllBasicHealthUnits(loggedUser)) {
            Long id = patient.getBasicHealthUnit() != null ? patient.getBasicHealthUnit().getId() : null;
            if (id == null) {
                return ResultadoOperacao.falha("UBS obrigatória para cadastrar ou atualizar paciente.");
            }
            return basicHealthUnitService.findSystemUserUBSOptional(id)
                    .map(ResultadoOperacao::sucesso)
                    .orElseGet(() -> ResultadoOperacao.falha("Nenhuma UBS encontrada para o usuário logado."));
        }

        Long loggedUserBasicHealthUnitId = loggedUser.getBasicHealthUnitId();
        if (loggedUserBasicHealthUnitId == null) {
            return ResultadoOperacao.falha("Usuário sem UBS vinculada.");
        }
        return basicHealthUnitService.findSystemUserUBSOptional(loggedUserBasicHealthUnitId)
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Nenhuma UBS encontrada para o usuário logado."));
    }

    private Long getScopedBasicHealthUnitId(SystemUserDetails loggedUser) {
        return canAccessAllBasicHealthUnits(loggedUser) ? null : requireLoggedUserBasicHealthUnitId(loggedUser);
    }

    private Long requireLoggedUserBasicHealthUnitId(SystemUserDetails loggedUser) {
        if (loggedUser.getBasicHealthUnitId() == null) {
            throw new IllegalArgumentException("Usuário sem UBS vinculada.");
        }
        return loggedUser.getBasicHealthUnitId();
    }

    private boolean canAccessAllBasicHealthUnits(SystemUserDetails loggedUser) {
        return loggedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Roles.ROLE_ADMIN.toString())
                        || a.getAuthority().equals(Roles.ROLE_SMS.toString()));
    }

}
