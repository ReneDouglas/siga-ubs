package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.repositories.MedicalSlotRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MedicalSlotService {

    private static final Logger log = LoggerFactory.getLogger(MedicalSlotService.class);

    private final MedicalSlotRepository medicalSlotRepository;

    public MedicalSlotService(MedicalSlotRepository medicalSlotRepository) {
        this.medicalSlotRepository = medicalSlotRepository;
    }

    @Transactional
    public ResultadoOperacao<Void> registerAvailableMedicalSlotsBatch(
            AvailableMedicalSlotsFormDTO availableMedicalSlotsFormDTO,
            SystemUserDetails loggedUser) {

        if (availableMedicalSlotsFormDTO == null
                || availableMedicalSlotsFormDTO.getAvailableMedicalSlots() == null
                || availableMedicalSlotsFormDTO.getAvailableMedicalSlots().isEmpty()) {
            return ResultadoOperacao.falha("Informe ao menos uma vaga.");
        }

        Long referenceUbsId = availableMedicalSlotsFormDTO.getAvailableMedicalSlots().get(0).getBasicHealthUnit()
                .getId();
        boolean isDistinct = availableMedicalSlotsFormDTO.getAvailableMedicalSlots().stream()
                .anyMatch(slotUbs -> !slotUbs.getBasicHealthUnit().getId().equals(referenceUbsId));

        if (isDistinct) {
            return ResultadoOperacao.falha("Cadastre as vagas para uma UBS de cada vez.");
        }

        for (MedicalSlot medicalSlot : availableMedicalSlotsFormDTO.getAvailableMedicalSlots()) {
            medicalSlot.setCurrentSlots(medicalSlot.getTotalSlots());
            medicalSlot.setCreationUser(loggedUser.getName());
            medicalSlot.setCreationDate(LocalDateTime.now());
        }

        medicalSlotRepository.saveAll(availableMedicalSlotsFormDTO.getAvailableMedicalSlots());
        return ResultadoOperacao.sucessoSemValor();

    }

    public Page<MedicalSlot> findMedicalSlotsPaginated(Pageable page) {
        return medicalSlotRepository.findMedicalSlotsPaginated(null, page);
    }

    public MedicalSlot findAvailableSlots(MedicalSlot medicalSlot) {
        return medicalSlotRepository.findByMedicalProcedureAndBasicHealthUnitAndContemplationsIsNull(
                medicalSlot.getMedicalProcedure(), medicalSlot.getBasicHealthUnit());
    }

    public Optional<MedicalSlot> findAvailableSlotsV2(MedicalSlot medicalSlot) {
        return medicalSlotRepository.findAvailableSlotsByMedicalProcedureAndUBS(
                medicalSlot.getMedicalProcedure().getId(), medicalSlot.getBasicHealthUnit().getId());
    }

    public List<MedicalSlot> findAvailableSlotsByReferenceMonth() {
        YearMonth current = YearMonth.now();
        LocalDate startOfMonth = current.atDay(1);
        LocalDate startOfNextMonth = current.plusMonths(1).atDay(1);
        return medicalSlotRepository.findAllAvailableSlotsByReferenceMonth(startOfMonth, startOfNextMonth);
    }

    @Transactional
    public ResultadoOperacao<MedicalSlot> addSlot(MedicalSlot medicalSlot) {

        MedicalSlot ms = medicalSlotRepository.getReferenceById(medicalSlot.getId());

        if (Objects.equals(ms.getCurrentSlots(), ms.getTotalSlots())) {
            return ResultadoOperacao.falha("O limite máximo de slots já foi atingido.");
        }

        ms.setCurrentSlots(ms.getCurrentSlots() + 1);
        return ResultadoOperacao.sucesso(medicalSlotRepository.save(ms));
    }

    @Transactional
    public ResultadoOperacao<MedicalSlot> removeSlot(MedicalSlot medicalSlot) {

        MedicalSlot ms = medicalSlotRepository.getReferenceById(medicalSlot.getId());

        if (ms.getCurrentSlots() == 0) {
            return ResultadoOperacao.falha("Não há mais slots disponíveis.");
        }

        ms.setCurrentSlots(ms.getCurrentSlots() - 1);
        return ResultadoOperacao.sucesso(medicalSlotRepository.save(ms));
    }
}
