package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class RepositoryPerformanceContractTest {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime());

    private final TenantRepository tenantRepository;
    private final SpecialtyRepository specialtyRepository;
    private final MedicalProcedureRepository medicalProcedureRepository;
    private final BasicHealthUnitRepository basicHealthUnitRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalSlotRepository medicalSlotRepository;
    private final ContemplationRepository contemplationRepository;
    private final EntityManager entityManager;
    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    RepositoryPerformanceContractTest(TenantRepository tenantRepository,
            SpecialtyRepository specialtyRepository,
            MedicalProcedureRepository medicalProcedureRepository,
            BasicHealthUnitRepository basicHealthUnitRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            MedicalSlotRepository medicalSlotRepository,
            ContemplationRepository contemplationRepository,
            EntityManager entityManager,
            EntityManagerFactory entityManagerFactory) {
        this.tenantRepository = tenantRepository;
        this.specialtyRepository = specialtyRepository;
        this.medicalProcedureRepository = medicalProcedureRepository;
        this.basicHealthUnitRepository = basicHealthUnitRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalSlotRepository = medicalSlotRepository;
        this.contemplationRepository = contemplationRepository;
        this.entityManager = entityManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void filaV2DevePreservarOrdenacaoENaoExecutarConsultaPorLinha() {
        Tenant tenant = tenant("perf-fila");

        withTenant(tenant, () -> {
            BasicHealthUnit ubs = ubs("UBS Fila");
            Specialty specialty = specialty("Cardiologia");
            MedicalProcedure procedure = procedure("Consulta", ProcedureType.CONSULTA, specialty);

            Appointment antigoEletivo = appointment(
                    patient("Paciente Antigo", ubs, LocalDate.of(1990, 1, 1),
                            SocialSituationRating.MAIS_DE_QUATRO_SALARIOS_MINIMOS),
                    procedure,
                    Priorities.ELETIVO,
                    LocalDateTime.now().minusMonths(5));
            Appointment urgente = appointment(
                    patient("Paciente Urgente", ubs, LocalDate.of(1995, 1, 1),
                            SocialSituationRating.MAIS_DE_QUATRO_SALARIOS_MINIMOS),
                    procedure,
                    Priorities.URGENCIA,
                    LocalDateTime.now().minusDays(1));
            Appointment eletivo = appointment(
                    patient("Paciente Eletivo", ubs, LocalDate.of(2000, 1, 1),
                            SocialSituationRating.MAIS_DE_QUATRO_SALARIOS_MINIMOS),
                    procedure,
                    Priorities.ELETIVO,
                    LocalDateTime.now().minusHours(1));

            clearPersistenceContextAndStatistics();

            var page = appointmentRepository.findOpenAppointmentsQueuePaginatedV2(
                    ubs.getId(), specialty.getId(), null, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(PatientOpenAppointmentDTO::appointmentId)
                    .containsExactly(antigoEletivo.getId(), urgente.getId(), eletivo.getId());
            assertThat(preparedStatementCount()).isLessThanOrEqualTo(3);
            return null;
        });
    }

    @Test
    void contemplacoesDevemCarregarAssociacoesUsadasNaTelaSemNMaisUm() {
        Tenant tenant = tenant("perf-contemplacao");

        withTenant(tenant, () -> {
            BasicHealthUnit ubs = ubs("UBS Contemplacao");
            Specialty specialty = specialty("Oftalmologia");
            MedicalProcedure procedure = procedure("Consulta", ProcedureType.CONSULTA, specialty);
            MedicalSlot slot = slot(ubs, procedure);

            contemplation(appointment(patient("Maria", ubs, LocalDate.of(1980, 1, 1),
                            SocialSituationRating.UM_SALARIO_MINIMO), procedure, Priorities.ELETIVO,
                            LocalDateTime.now().minusDays(3)),
                    slot);
            contemplation(appointment(patient("Ana", ubs, LocalDate.of(1985, 1, 1),
                            SocialSituationRating.MEIO_SALARIO_MINIMO), procedure, Priorities.ELETIVO,
                            LocalDateTime.now().minusDays(2)),
                    slot);

            clearPersistenceContextAndStatistics();

            var page = contemplationRepository.findConsultationsByUBSAndSpecialtyPaginated(
                    ProcedureType.CONSULTA,
                    ubs.getId(),
                    specialty.getId(),
                    YearMonth.now(),
                    null,
                    PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);
            page.getContent().forEach(contemplation -> {
                assertThat(contemplation.getAppointment().getPatient().getName()).isNotBlank();
                assertThat(contemplation.getAppointment().getPatient().getCpf()).isNotBlank();
                assertThat(contemplation.getAppointment().getMedicalProcedure().getSpecialty().getTitle())
                        .isEqualTo("Oftalmologia");
                assertThat(contemplation.getMedicalSlot().getBasicHealthUnit().getName())
                        .isEqualTo("UBS Contemplacao");
            });
            assertThat(preparedStatementCount()).isLessThanOrEqualTo(3);
            return null;
        });
    }

    @Test
    void listagemDePacientesDeveCarregarUbsUsadaNaTabelaSemNMaisUm() {
        Tenant tenant = tenant("perf-paciente");

        withTenant(tenant, () -> {
            BasicHealthUnit primeiraUbs = ubs("UBS A");
            BasicHealthUnit segundaUbs = ubs("UBS B");
            BasicHealthUnit terceiraUbs = ubs("UBS C");

            patient("Ana", primeiraUbs, LocalDate.of(1990, 1, 1), SocialSituationRating.UM_SALARIO_MINIMO);
            patient("Bruna", segundaUbs, LocalDate.of(1991, 1, 1), SocialSituationRating.MEIO_SALARIO_MINIMO);
            patient("Carla", terceiraUbs, LocalDate.of(1992, 1, 1), SocialSituationRating.UM_QUARTO_DE_SALARIO_MINIMO);

            Patient filter = new Patient();
            BasicHealthUnit allUbs = new BasicHealthUnit();
            allUbs.setId(null);
            filter.setBasicHealthUnit(allUbs);

            clearPersistenceContextAndStatistics();

            var page = patientRepository.findPatientsPaginated(filter, PageRequest.of(0, 10));

            assertThat(page.getContent()).extracting(Patient::getName)
                    .contains("Ana", "Bruna", "Carla");
            page.getContent().forEach(patient ->
                    assertThat(patient.getBasicHealthUnit().getName()).startsWith("UBS "));
            assertThat(preparedStatementCount()).isLessThanOrEqualTo(3);
            return null;
        });
    }

    private Tenant tenant(String prefix) {
        String slug = prefix + "-" + SEQUENCE.incrementAndGet();
        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName("Tenant " + slug);
        tenant.setDomain(slug + ".sigaubs.com.br");
        tenant.setStatus("ACTIVE");
        tenant.setCreationDate(LocalDateTime.now());
        tenant.setCreationUser("test");
        return tenantRepository.saveAndFlush(tenant);
    }

    private BasicHealthUnit ubs(String name) {
        BasicHealthUnit ubs = new BasicHealthUnit();
        ubs.setName(name);
        ubs.setNeighborhood("Centro");
        ubs.setCreationDate(LocalDateTime.now());
        ubs.setCreationUser("test");
        return basicHealthUnitRepository.saveAndFlush(ubs);
    }

    private Specialty specialty(String title) {
        Specialty specialty = new Specialty();
        specialty.setTitle(title);
        specialty.setDescription(title);
        specialty.setActive(true);
        specialty.setCreationDate(LocalDateTime.now());
        specialty.setCreationUser("test");
        return specialtyRepository.saveAndFlush(specialty);
    }

    private MedicalProcedure procedure(String description, ProcedureType type, Specialty specialty) {
        MedicalProcedure procedure = new MedicalProcedure();
        procedure.setDescription(description);
        procedure.setProcedureType(type);
        procedure.setSpecialty(specialty);
        procedure.setCreationDate(LocalDateTime.now());
        procedure.setCreationUser("test");
        return medicalProcedureRepository.saveAndFlush(procedure);
    }

    private Patient patient(String name, BasicHealthUnit ubs, LocalDate birthDate,
            SocialSituationRating socialSituationRating) {
        long id = SEQUENCE.incrementAndGet();
        Patient patient = new Patient();
        patient.setName(name);
        patient.setSusNumber("sus-" + id);
        patient.setCpf("cpf-" + id);
        patient.setGender("Feminino");
        patient.setBirthDate(birthDate);
        patient.setSocialSituationRating(socialSituationRating);
        patient.setPhoneNumber("81999999999");
        patient.setAddressStreet("Rua Teste");
        patient.setAddressNumber("1");
        patient.setAcsName("ACS");
        patient.setBasicHealthUnit(ubs);
        patient.setCreationDate(LocalDateTime.now());
        patient.setCreationUser("test");
        return patientRepository.saveAndFlush(patient);
    }

    private Appointment appointment(Patient patient, MedicalProcedure procedure, Priorities priority,
            LocalDateTime requestDate) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setMedicalProcedure(procedure);
        appointment.setPriority(priority);
        appointment.setStatus(AppointmentStatus.AGUARDANDO_CONTEMPLACAO);
        appointment.setRequestDate(requestDate);
        appointment.setCreationUser("test");
        return appointmentRepository.saveAndFlush(appointment);
    }

    private MedicalSlot slot(BasicHealthUnit ubs, MedicalProcedure procedure) {
        MedicalSlot slot = new MedicalSlot();
        slot.setBasicHealthUnit(ubs);
        slot.setMedicalProcedure(procedure);
        slot.setReferenceMonth(YearMonth.now());
        slot.setTotalSlots(10);
        slot.setCurrentSlots(8);
        slot.setCreationDate(LocalDateTime.now());
        slot.setCreationUser("test");
        return medicalSlotRepository.saveAndFlush(slot);
    }

    private Contemplation contemplation(Appointment appointment, MedicalSlot slot) {
        Contemplation contemplation = new Contemplation();
        contemplation.setMedicalSlot(slot);
        contemplation.setContemplatedBy(Priorities.ADMINISTRATIVO);
        contemplation.setContemplationDate(LocalDateTime.now());
        contemplation.setCreationDate(LocalDateTime.now());
        contemplation.setCreationUser("test");
        contemplation = contemplationRepository.saveAndFlush(contemplation);

        appointment.setContemplation(contemplation);
        appointment.setStatus(AppointmentStatus.PRESENCA_CONFIRMADA);
        appointmentRepository.saveAndFlush(appointment);
        return contemplation;
    }

    private <T> T withTenant(Tenant tenant, Supplier<T> action) {
        TenantContextHolder.setTenant(tenant.getId(), tenant.getSlug());
        try {
            return action.get();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void clearPersistenceContextAndStatistics() {
        entityManager.flush();
        entityManager.clear();
        statistics().clear();
    }

    private long preparedStatementCount() {
        return statistics().getPrepareStatementCount();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
