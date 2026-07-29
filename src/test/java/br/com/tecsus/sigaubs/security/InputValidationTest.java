package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.dtos.AppointmentCommandDTO;
import br.com.tecsus.sigaubs.dtos.EntityIdDTO;
import br.com.tecsus.sigaubs.dtos.MedicalSlotBatchCommandDTO;
import br.com.tecsus.sigaubs.dtos.MedicalSlotCommandDTO;
import br.com.tecsus.sigaubs.dtos.PatientCommandDTO;
import br.com.tecsus.sigaubs.dtos.SystemUserCommandDTO;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejeitaDocumentosDataEObservacaoInvalidos() {
        PatientCommandDTO patient = new PatientCommandDTO();
        patient.setName("Paciente");
        patient.setCpf("11111111111");
        patient.setSusNumber("111111111111111");
        patient.setPhoneNumber("000");
        patient.setGender("Feminino");
        patient.setBirthDate(LocalDate.now().plusDays(1));
        patient.setSocialSituationRating(SocialSituationRating.UM_SALARIO_MINIMO);
        patient.setAddressStreet("Rua");

        assertThat(validator.validate(patient))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("cpfValid", "susNumberValid", "phoneNumberValid", "birthDate");

        AppointmentCommandDTO appointment = new AppointmentCommandDTO();
        appointment.setPatient(new EntityIdDTO(1L));
        appointment.setMedicalProcedure(new EntityIdDTO(1L));
        appointment.setPriority(Priorities.ELETIVO);
        appointment.setObservation("</script>\r\n" + "x".repeat(2_000));
        assertThat(validator.validate(appointment))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("observation");
    }

    @Test
    void rejeitaSenhaFracaPapelAusenteELoteAbusivo() {
        SystemUserCommandDTO user = new SystemUserCommandDTO();
        user.setUsername("novo");
        user.setPassword("12345678");
        user.setConfirmPassword("12345678");
        user.setName("Novo");
        user.setEmail("novo@example.com");
        assertThat(validator.validate(user))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("passwordValid", "selectedRoleId");

        MedicalSlotBatchCommandDTO batch = new MedicalSlotBatchCommandDTO();
        ArrayList<MedicalSlotCommandDTO> slots = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            slots.add(new MedicalSlotCommandDTO());
        }
        batch.setAvailableMedicalSlots(slots);
        assertThat(validator.validate(batch))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("availableMedicalSlots");
    }
}
