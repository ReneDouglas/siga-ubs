package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
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

    @Cacheable("ubs")
    public List<BasicHealthUnit> findAllUBS() {
        return basicHealthUnitRepository.findAll();
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public void registerBasicHealthUnit(BasicHealthUnit basicHealthUnit, SystemUserDetails loggedUser) throws Exception{

        basicHealthUnit.setCreationDate(LocalDateTime.now());
        basicHealthUnit.setCreationUser(loggedUser.getUsername());
        basicHealthUnitRepository.save(basicHealthUnit);
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public void updateBasicHealthUnit(BasicHealthUnit basicHealthUnit, SystemUserDetails loggedUser) throws Exception{

        basicHealthUnit.setUpdateUser(loggedUser.getUsername());
        basicHealthUnit.setUpdateDate(LocalDateTime.now());
        basicHealthUnitRepository.save(basicHealthUnit);
    }

    @CacheEvict(value = "ubs", allEntries = true)
    @Transactional
    public void deleteBasicHealtUnit(Long id, SystemUserDetails loggedUser) throws Exception{

        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(id).orElseThrow(() -> {
            log.error("UBS [id = {}] não encontrada.", id);
            return new Exception("Erro ao deletar UBS.");
        });

        List<SystemUser> systemUsers = new ArrayList<>();

        for (SystemUser su :  basicHealthUnit.getSystemUsers()) {
            su.setBasicHealthUnit(null);
            su.setUpdateUser(loggedUser.getUsername());
            su.setUpdateDate(LocalDateTime.now());
            systemUsers.add(su);
        }
        systemUserService.updateBasicHealthUnitSystemUsers(systemUsers);
        basicHealthUnitRepository.delete(basicHealthUnit);
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
    public void unlinkBasicHealthUnitSystemUser(Long id, SystemUserDetails loggedUser) {
        SystemUser systemUser = systemUserService.findSystemUserById(id);
        systemUser.setBasicHealthUnit(null);
        systemUser.setUpdateUser(loggedUser.getUsername());
        systemUser.setUpdateDate(LocalDateTime.now());
        systemUserService.updateBasicHealthUnitSystemUsers(List.of(systemUser));
    }

    @Transactional
    public void attachSystemUserToUBS(Long idSystemUser, Long idUBS) throws Exception{

        SystemUser systemUser = systemUserService.findSystemUserById(idSystemUser);
        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(idUBS).orElse(null);
        systemUser.setBasicHealthUnit(basicHealthUnit);
        systemUserService.updateBasicHealthUnitSystemUsers(List.of(systemUser));
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
