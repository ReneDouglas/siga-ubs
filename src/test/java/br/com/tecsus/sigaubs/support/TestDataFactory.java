package br.com.tecsus.sigaubs.support;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Tenant tenant(Long id, String slug) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setSlug(slug);
        tenant.setName("Tenant " + slug);
        tenant.setDomain(slug + ".sigaubs.com.br");
        tenant.setStatus("ACTIVE");
        tenant.setCreationDate(LocalDateTime.now());
        tenant.setCreationUser("test");
        return tenant;
    }

    public static SystemUserDetails userDetails(String username, String name, Long ubsId, Long tenantId,
            String tenantSlug, Roles... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.toString()))
                .toList();
        return new SystemUserDetails(username, "{noop}123456", authorities, name,
                username + "@example.com", true, ubsId, tenantId, tenantSlug);
    }

    public static SystemUserDetails adminDetails(Long tenantId, String tenantSlug) {
        return userDetails("admin", "Administrador", null, tenantId, tenantSlug, Roles.ROLE_ADMIN);
    }

    public static BasicHealthUnit ubs(Long id, String name) {
        BasicHealthUnit ubs = new BasicHealthUnit();
        ubs.setId(id);
        ubs.setName(name);
        ubs.setNeighborhood("Centro");
        return ubs;
    }

    public static Specialty specialty(Long id, String title) {
        Specialty specialty = new Specialty();
        specialty.setId(id);
        specialty.setTitle(title);
        specialty.setDescription(title);
        specialty.setActive(true);
        return specialty;
    }

    public static MedicalProcedure procedure(Long id, String description, ProcedureType type, Specialty specialty) {
        MedicalProcedure procedure = new MedicalProcedure();
        procedure.setId(id);
        procedure.setDescription(description);
        procedure.setProcedureType(type);
        procedure.setSpecialty(specialty);
        return procedure;
    }

    public static Patient patient(Long id, String name, BasicHealthUnit ubs) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName(name);
        patient.setCpf("0000000000" + id);
        patient.setSusNumber("11111111111111" + id);
        patient.setGender("Feminino");
        patient.setBirthDate(LocalDate.of(1980, 1, 1));
        patient.setSocialSituationRating(SocialSituationRating.UM_SALARIO_MINIMO);
        patient.setPhoneNumber("8199999999" + id);
        patient.setAddressStreet("Rua Teste");
        patient.setAddressNumber(String.valueOf(id));
        patient.setBasicHealthUnit(ubs);
        return patient;
    }

    public static Appointment appointment(Long id, Patient patient, MedicalProcedure procedure) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatient(patient);
        appointment.setMedicalProcedure(procedure);
        appointment.setPriority(Priorities.ELETIVO);
        appointment.setStatus(AppointmentStatus.AGUARDANDO_CONTEMPLACAO);
        appointment.setRequestDate(LocalDateTime.now().minusDays(1));
        return appointment;
    }

    public static MedicalSlot slot(Long id, BasicHealthUnit ubs, MedicalProcedure procedure, int totalSlots,
            int currentSlots) {
        MedicalSlot slot = new MedicalSlot();
        slot.setId(id);
        slot.setBasicHealthUnit(ubs);
        slot.setMedicalProcedure(procedure);
        slot.setReferenceMonth(YearMonth.now());
        slot.setTotalSlots(totalSlots);
        slot.setCurrentSlots(currentSlots);
        return slot;
    }

    public static Contemplation contemplation(Long id, Appointment appointment, MedicalSlot slot) {
        Contemplation contemplation = new Contemplation();
        contemplation.setId(id);
        contemplation.setAppointment(appointment);
        contemplation.setMedicalSlot(slot);
        contemplation.setContemplatedBy(Priorities.ADMINISTRATIVO);
        contemplation.setContemplationDate(LocalDateTime.now());
        return contemplation;
    }

    public static SystemRole role(Long id, Roles role) {
        SystemRole systemRole = new SystemRole();
        systemRole.setId(id);
        systemRole.setRole(role.toString());
        systemRole.setTitle(role.getDescription());
        systemRole.setDescription(role.getDescription());
        systemRole.setRoot(!role.getPermission());
        return systemRole;
    }

    public static SystemUser systemUser(Long id, String username, SystemRole role) {
        SystemUser systemUser = new SystemUser();
        systemUser.setId(id);
        systemUser.setUsername(username);
        systemUser.setPassword("{noop}123456");
        systemUser.setConfirmPassword("123456");
        systemUser.setName("Usuário " + username);
        systemUser.setEmail(username + "@example.com");
        systemUser.setActive(true);
        systemUser.setRoles(Set.of(role));
        return systemUser;
    }

    public static SystemAdmin systemAdmin(String username, String password) {
        SystemAdmin admin = new SystemAdmin();
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setName("Admin");
        admin.setEmail(username + "@example.com");
        admin.setActive(true);
        return admin;
    }

    public static PatientOpenAppointmentDTO openAppointment(Long appointmentId, Priorities priority,
            LocalDateTime requestDate, LocalDate birthDate, SocialSituationRating socialRating, String gender) {
        return new PatientOpenAppointmentDTO(
                requestDate,
                priority,
                ProcedureType.CONSULTA,
                10L,
                "Consulta",
                "Cardiologia",
                "UBS",
                "Sem observações.",
                appointmentId,
                appointmentId + 100,
                "Paciente " + appointmentId,
                "00000000000",
                gender,
                birthDate,
                socialRating);
    }
}
