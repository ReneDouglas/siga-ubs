package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.config.CacheNames;
import br.com.tecsus.sigaubs.dtos.ProcedureDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.SpecialtyDTO;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.MedicalProcedureRepository;
import br.com.tecsus.sigaubs.repositories.SpecialtyRepository;
import br.com.tecsus.sigaubs.utils.SpecialtyLimits;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final MedicalProcedureRepository medicalProcedureRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository, MedicalProcedureRepository medicalProcedureRepository) {
        this.specialtyRepository = specialtyRepository;
        this.medicalProcedureRepository = medicalProcedureRepository;
    }

    @Cacheable(value = CacheNames.SPECIALTIES,
            key = "T(br.com.tecsus.sigaubs.tenancy.TenantContextHolder).getRequiredTenantId()")
    public List<Specialty> findSpecialties() {
        return specialtyRepository.findAllByOrderByTitleAsc();
    }

    public SpecialtyDTO findFetchedSpecialty(Long id) {

        Specialty specialty = specialtyRepository.loadByIdWithProcedures(id);
        SpecialtyDTO specialtyDTO = new SpecialtyDTO();

        specialtyDTO.setId(specialty.getId());
        specialtyDTO.setTitle(specialty.getTitle());
        specialtyDTO.setDescription(specialty.getDescription());
        specialtyDTO.setActive(specialty.getActive());

        var proceduresDTO = specialty.getMedicalProcedures().stream().map(medicalProcedure -> {
            ProcedureDTO procedureDTO = new ProcedureDTO();
            procedureDTO.setDescription(medicalProcedure.getDescription());
            procedureDTO.setProcedureType(medicalProcedure.getProcedureType().getDescription());
            return procedureDTO;
        }).toList();
        specialtyDTO.setProcedures(proceduresDTO);

        return specialtyDTO;
    }

    @CacheEvict(value = CacheNames.SPECIALTIES, allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> registerSpecialty(SpecialtyDTO specialtyDTO, SystemUserDetails loggedUser) {

        var validationResult = validateProcedureTypes(specialtyDTO);
        if (validationResult.falhou()) {
            return validationResult;
        }

        Specialty specialty = new Specialty();
        Set<MedicalProcedure> procedures = new HashSet<>();
        specialty.setId(null);
        specialty.setTitle(specialtyDTO.getTitle());
        specialty.setDescription(specialtyDTO.getDescription());
        specialty.setActive(true);
        specialty.setCreationUser(loggedUser.getLoginUsername());
        specialty.setCreationDate(LocalDateTime.now());

        for (ProcedureDTO procedureDTO : specialtyDTO.getProcedures()) {
            MedicalProcedure medicalProcedure = new MedicalProcedure();
            medicalProcedure.setId(null);
            medicalProcedure.setDescription(procedureDTO.getDescription());
            medicalProcedure.setProcedureType(ProcedureType.findByDescription(procedureDTO.getProcedureType()).orElse(null));
            medicalProcedure.setCreationUser(loggedUser.getLoginUsername());
            medicalProcedure.setCreationDate(LocalDateTime.now());
            medicalProcedure.setSpecialty(specialty);
            procedures.add(medicalProcedure);
        }

        specialty.setMedicalProcedures(procedures);
        specialtyRepository.save(specialty);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = CacheNames.SPECIALTIES, allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> updateSpecialty(SpecialtyDTO specialtyDTO, SystemUserDetails loggedUser) {

        var validationResult = validateProcedureTypes(specialtyDTO);
        if (validationResult.falhou()) {
            return validationResult;
        }

        Specialty specialty = specialtyRepository.findById(specialtyDTO.getId()).orElse(null);
        if (specialty == null) {
            return ResultadoOperacao.falha("Especialidade não encontrada.");
        }
        specialty.setTitle(specialtyDTO.getTitle().trim());
        specialty.setDescription(specialtyDTO.getDescription() == null
                ? null
                : specialtyDTO.getDescription().trim());
        specialty.setActive(Boolean.TRUE.equals(specialtyDTO.getActive()));

        specialty = specialtyRepository.save(specialty);

        for (ProcedureDTO procedureDTO : specialtyDTO.getProcedures()) {
            var medicalProcedure = new MedicalProcedure();
            medicalProcedure.setId(null);
            medicalProcedure.setDescription(procedureDTO.getDescription());
            medicalProcedure.setProcedureType(ProcedureType.findByDescription(procedureDTO.getProcedureType()).orElse(null));
            medicalProcedure.setCreationUser(loggedUser.getLoginUsername());
            medicalProcedure.setCreationDate(LocalDateTime.now());
            medicalProcedure.setSpecialty(specialty);
            medicalProcedureRepository.save(medicalProcedure);
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    private ResultadoOperacao<Void> validateProcedureTypes(SpecialtyDTO specialtyDTO) {
        if (specialtyDTO.getProcedures() == null) {
            return ResultadoOperacao.falha("Procedimentos obrigatórios.");
        }
        if (specialtyDTO.getProcedures().size()
                > SpecialtyLimits.MAXIMUM_PROCEDURES) {
            return ResultadoOperacao.falha("Limite de procedimentos excedido.");
        }
        for (ProcedureDTO procedureDTO : specialtyDTO.getProcedures()) {
            if (procedureDTO == null
                    || procedureDTO.getDescription() == null
                    || procedureDTO.getDescription().isBlank()
                    || procedureDTO.getDescription().length()
                            > SpecialtyLimits.MAXIMUM_PROCEDURE_DESCRIPTION_LENGTH
                    || ProcedureType.findByDescription(procedureDTO.getProcedureType()).isEmpty()) {
                return ResultadoOperacao.falha("Erro ao encontrar procedimento.");
            }
        }
        return ResultadoOperacao.sucessoSemValor();
    }
}
