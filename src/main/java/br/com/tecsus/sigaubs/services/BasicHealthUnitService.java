package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.BasicHealthUnitCommandDTO;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.repositories.BasicHealthUnitRepository;
import br.com.tecsus.sigaubs.repositories.MedicalProcedureRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BasicHealthUnitService {

    private static final Logger log = LoggerFactory.getLogger(BasicHealthUnitService.class);

    private final BasicHealthUnitRepository basicHealthUnitRepository;
    private final MedicalProcedureRepository medicalProcedureRepository;
    private SystemUserService systemUserService;

    public BasicHealthUnitService(BasicHealthUnitRepository basicHealthUnitRepository, MedicalProcedureRepository medicalProcedureRepository) {
        this.basicHealthUnitRepository = basicHealthUnitRepository;
        this.medicalProcedureRepository = medicalProcedureRepository;
    }

    @Autowired
    public void setSystemUserService(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    //@Transactional(readOnly = true)
    public BasicHealthUnit findSystemUserUBS(Long id) {
        return basicHealthUnitRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Nenhuma UBS encontrada para o usuário logado.")
        );
    }

    public Optional<BasicHealthUnit> findSystemUserUBSOptional(Long id) {
        return basicHealthUnitRepository.findById(id);
    }

    public BasicHealthUnit findReferenceById(Long id) {
        return basicHealthUnitRepository.getReferenceById(id);
    }

    /*public List<BasicHealthUnit> findBasicHealthUnitsByCityHallOfLoggedSystemUser() {

        SystemUserDetails systemUserDetails = (SystemUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (systemUserDetails.getCityHallId() != null) {
            return basicHealthUnitRepository.findByCityHallId(systemUserDetails.getCityHallId());
        }
        return basicHealthUnitRepository.findByCityHallId(1L);
    }*/

    @Cacheable(value = "ubs", key = "T(br.com.tecsus.sigaubs.tenancy.TenantContextHolder).getRequiredTenantId()")
    public List<BasicHealthUnit> findAllUBS() {
        return basicHealthUnitRepository.findAll();
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> registerBasicHealthUnit(BasicHealthUnit basicHealthUnit,
            SystemUserDetails loggedUser) {

        basicHealthUnit.setCreationDate(LocalDateTime.now());
        basicHealthUnit.setCreationUser(loggedUser.getLoginUsername());
        basicHealthUnitRepository.save(basicHealthUnit);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> updateBasicHealthUnit(BasicHealthUnit basicHealthUnit,
            SystemUserDetails loggedUser) {

        basicHealthUnit.setUpdateUser(loggedUser.getLoginUsername());
        basicHealthUnit.setUpdateDate(LocalDateTime.now());
        basicHealthUnitRepository.save(basicHealthUnit);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> registerBasicHealthUnit(
            BasicHealthUnitCommandDTO command, SystemUserDetails loggedUser) {
        BasicHealthUnit basicHealthUnit = new BasicHealthUnit();
        basicHealthUnit.setName(command.getName().trim());
        basicHealthUnit.setNeighborhood(command.getNeighborhood().trim());
        basicHealthUnit.setCreationDate(LocalDateTime.now());
        basicHealthUnit.setCreationUser(loggedUser.getLoginUsername());
        basicHealthUnitRepository.save(basicHealthUnit);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> updateBasicHealthUnit(
            BasicHealthUnitCommandDTO command, SystemUserDetails loggedUser) {
        if (command.getId() == null) {
            return ResultadoOperacao.falha("UBS não informada.");
        }
        BasicHealthUnit persisted = basicHealthUnitRepository.findById(command.getId()).orElse(null);
        if (persisted == null) {
            return ResultadoOperacao.falha("UBS não encontrada.");
        }
        persisted.setName(command.getName().trim());
        persisted.setNeighborhood(command.getNeighborhood().trim());
        persisted.setUpdateUser(loggedUser.getLoginUsername());
        persisted.setUpdateDate(LocalDateTime.now());
        basicHealthUnitRepository.save(persisted);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> deleteBasicHealtUnit(Long id, SystemUserDetails loggedUser) {

        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(id).orElse(null);
        if (basicHealthUnit == null) {
            log.error("UBS [id = {}] não encontrada.", id);
            return ResultadoOperacao.falha("Erro ao deletar UBS.");
        }

        List<SystemUser> systemUsers = new ArrayList<>();

        for (SystemUser su :  basicHealthUnit.getSystemUsers()) {
            su.setBasicHealthUnit(null);
            su.setUpdateUser(loggedUser.getLoginUsername());
            su.setUpdateDate(LocalDateTime.now());
            systemUsers.add(su);
        }
        systemUserService.updateBasicHealthUnitSystemUsers(systemUsers);
        basicHealthUnitRepository.delete(basicHealthUnit);
        return ResultadoOperacao.sucessoSemValor();
    }

    //@Transactional(readOnly = true)
    public List<UBSsystemUserDTO> findUBSsystemUsersByUBSid(Long id) {
        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(id).orElse(null);

        if (basicHealthUnit == null) {
            return List.of();
        }
        if (basicHealthUnit.getSystemUsers() == null || basicHealthUnit.getSystemUsers().isEmpty()) {
            return List.of();
        }

        return basicHealthUnit
                .getSystemUsers()
                .stream()
                .map(user -> new UBSsystemUserDTO(
                        user.getId(),
                        user.getName(),
                        user.getFirstRoleTitle(),
                        user.getActive() ? "Sim" : "Não"))
                .toList();
    }



    @Transactional
    public ResultadoOperacao<Void> unlinkBasicHealthUnitSystemUser(Long id, SystemUserDetails loggedUser) {
        SystemUser systemUser = systemUserService.findManageableSystemUserById(id, loggedUser);
        if (systemUser == null) {
            return ResultadoOperacao.falha("Usuário não encontrado.");
        }
        systemUser.setBasicHealthUnit(null);
        systemUser.setUpdateUser(loggedUser.getLoginUsername());
        systemUser.setUpdateDate(LocalDateTime.now());
        systemUserService.updateBasicHealthUnitSystemUsers(List.of(systemUser));
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public ResultadoOperacao<Void> attachSystemUserToUBS(
            Long idSystemUser, Long idUBS, SystemUserDetails loggedUser) {

        SystemUser systemUser = systemUserService.findManageableSystemUserById(idSystemUser, loggedUser);
        if (systemUser == null) {
            return ResultadoOperacao.falha("Usuário não encontrado.");
        }
        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(idUBS).orElse(null);
        if (basicHealthUnit == null) {
            return ResultadoOperacao.falha("UBS não encontrada.");
        }
        systemUser.setBasicHealthUnit(basicHealthUnit);
        systemUser.setUpdateUser(loggedUser.getLoginUsername());
        systemUser.setUpdateDate(LocalDateTime.now());
        systemUserService.updateBasicHealthUnitSystemUsers(List.of(systemUser));
        return ResultadoOperacao.sucessoSemValor();
    }

    //@Transactional(readOnly = true)
    public MedicalProcedure fetchMedicalProcedure(Long medicalProcedureId) {
        return medicalProcedureRepository.findFetchedMedicalProcedure(medicalProcedureId);
    }

    public MedicalSlot getFetchedAssociations(MedicalSlot availableMedicalSlot) {

        MedicalProcedure mp = medicalProcedureRepository
                .findFetchedMedicalProcedure(availableMedicalSlot.getMedicalProcedure().getId());
        availableMedicalSlot.setMedicalProcedure(mp);

        return availableMedicalSlot;
    }

    //@Transactional(readOnly = true)
    /*public BasicHealthUnit findSystemUserUBS(SystemUserDetails loggedUser) {
        return basicHealthUnitRepository.findById(loggedUser.getBasicHealthUnitId()).orElseThrow(
                () -> new RuntimeException("Nenhuma UBS encontrada para o usuário logado.")
        );
    }*/
}
