package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO;
import br.com.tecsus.sigaubs.dtos.MedicalSlotBatchCommandDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.repositories.BasicHealthUnitRepository;
import br.com.tecsus.sigaubs.repositories.MedicalProcedureRepository;
import br.com.tecsus.sigaubs.repositories.MedicalSlotRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.utils.MedicalSlotLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final BasicHealthUnitRepository basicHealthUnitRepository;
    private final MedicalProcedureRepository medicalProcedureRepository;

    @Autowired
    public MedicalSlotService(MedicalSlotRepository medicalSlotRepository,
            BasicHealthUnitRepository basicHealthUnitRepository,
            MedicalProcedureRepository medicalProcedureRepository) {
        this.medicalSlotRepository = medicalSlotRepository;
        this.basicHealthUnitRepository = basicHealthUnitRepository;
        this.medicalProcedureRepository = medicalProcedureRepository;
    }

    MedicalSlotService(MedicalSlotRepository medicalSlotRepository) {
        this(medicalSlotRepository, null, null);
    }

    @Transactional
    public ResultadoOperacao<Void> registerAvailableMedicalSlotsBatch(
            MedicalSlotBatchCommandDTO batch,
            SystemUserDetails loggedUser) {
        if (batch == null
                || batch.getAvailableMedicalSlots() == null
                || batch.getAvailableMedicalSlots().isEmpty()) {
            return ResultadoOperacao.falha("Informe ao menos uma vaga.");
        }
        if (batch.getAvailableMedicalSlots().size()
                > MedicalSlotLimits.MAXIMUM_BATCH_SIZE) {
            return ResultadoOperacao.falha(
                    "O lote não pode exceder "
                            + MedicalSlotLimits.MAXIMUM_BATCH_SIZE
                            + " vagas.");
        }

        Long referenceUbsId = batch.getAvailableMedicalSlots().getFirst().getBasicHealthUnit().getId();
        if (batch.getAvailableMedicalSlots().stream().anyMatch(command ->
                !Objects.equals(referenceUbsId, command.getBasicHealthUnit().getId()))) {
            return ResultadoOperacao.falha("Cadastre as vagas para uma UBS de cada vez.");
        }

        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(referenceUbsId).orElse(null);
        if (basicHealthUnit == null) {
            return ResultadoOperacao.falha("UBS não encontrada.");
        }

        List<MedicalSlot> slots = new java.util.ArrayList<>();
        for (var command : batch.getAvailableMedicalSlots()) {
            MedicalProcedure procedure = medicalProcedureRepository
                    .findById(command.getMedicalProcedure().getId())
                    .orElse(null);
            if (procedure == null
                    || command.getTotalSlots() == null
                    || command.getTotalSlots() < 1
                    || command.getReferenceMonth() == null) {
                return ResultadoOperacao.falha("Vaga ou procedimento inválido.");
            }
            MedicalSlot slot = new MedicalSlot();
            slot.setReferenceMonth(command.getReferenceMonth());
            slot.setTotalSlots(command.getTotalSlots());
            slot.setCurrentSlots(command.getTotalSlots());
            slot.setBasicHealthUnit(basicHealthUnit);
            slot.setMedicalProcedure(procedure);
            slot.setCreationUser(loggedUser.getName());
            slot.setCreationDate(LocalDateTime.now());
            slots.add(slot);
        }
        medicalSlotRepository.saveAll(slots);
        return ResultadoOperacao.sucessoSemValor();
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

    @Transactional(readOnly = true)
    public MedicalSlot findById(Long id) {
        return medicalSlotRepository.findById(id).orElse(null);
    }

    @Transactional
    public ResultadoOperacao<MedicalSlot> addSlot(MedicalSlot medicalSlot) {
        if (medicalSlot == null || medicalSlot.getId() == null
                || medicalSlotRepository.incrementIfBelowTotal(medicalSlot.getId()) != 1) {
            return ResultadoOperacao.falha("O limite máximo de slots já foi atingido.");
        }
        return medicalSlotRepository.findById(medicalSlot.getId())
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Vaga não encontrada."));
    }

    @Transactional
    public ResultadoOperacao<MedicalSlot> removeSlot(MedicalSlot medicalSlot) {
        if (medicalSlot == null || medicalSlot.getId() == null
                || medicalSlotRepository.decrementIfAvailable(medicalSlot.getId()) != 1) {
            return ResultadoOperacao.falha("Não há mais slots disponíveis.");
        }
        return medicalSlotRepository.findById(medicalSlot.getId())
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Vaga não encontrada."));
    }
}
