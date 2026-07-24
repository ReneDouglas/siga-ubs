package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO;
import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.Impl.AppointmentRepositoryCustomImpl;
import br.com.tecsus.sigaubs.repositories.Impl.ContemplationRepositoryCustomImpl;
import br.com.tecsus.sigaubs.repositories.Impl.MedicalSlotRepositoryCustomImpl;
import br.com.tecsus.sigaubs.repositories.Impl.PatientRepositoryCustomImpl;
import br.com.tecsus.sigaubs.repositories.Impl.SystemUserRepositoryImpl;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import br.com.tecsus.sigaubs.utils.ValidationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static br.com.tecsus.sigaubs.repositories.RepositoryMockSupport.typedQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomRepositoriesTest {

    @Test
    void devePaginarFilaDeMarcacoesV1EV2() {
        EntityManager em = mock(EntityManager.class);
        JpaContext jpaContext = mock(JpaContext.class);
        when(jpaContext.getEntityManagerByManagedType(Appointment.class)).thenReturn(em);
        PatientOpenAppointmentDTO dto = TestDataFactory.openAppointment(10L,
                br.com.tecsus.sigaubs.enums.Priorities.ELETIVO,
                LocalDateTime.now(), LocalDate.of(1980, 1, 1),
                br.com.tecsus.sigaubs.enums.SocialSituationRating.UM_SALARIO_MINIMO, "Feminino");

        TypedQuery<Long> idsQuery = typedQuery(List.of(10L));
        TypedQuery<Long> countQuery = typedQuery(List.of(), 2L);
        TypedQuery<PatientOpenAppointmentDTO> queueQuery = typedQuery(List.of(dto));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(idsQuery, countQuery);
        when(em.createQuery(anyString(), eq(PatientOpenAppointmentDTO.class))).thenReturn(queueQuery);

        var repository = new AppointmentRepositoryCustomImpl(jpaContext);
        var page = repository.findOpenAppointmentsQueuePaginated(ProcedureType.CONSULTA, 1L, 2L,
                PageRequest.of(0, 1));

        assertThat(page.getContent()).containsExactly(dto);
        assertThat(page.getTotalElements()).isEqualTo(2);

        TypedQuery<Long> idsQueryV2 = typedQuery(List.of(10L));
        TypedQuery<Long> countQueryV2 = typedQuery(List.of(), 3L);
        TypedQuery<PatientOpenAppointmentDTO> queueQueryV2 = typedQuery(List.of(dto));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(idsQueryV2, countQueryV2);
        when(em.createQuery(anyString(), eq(PatientOpenAppointmentDTO.class))).thenReturn(queueQueryV2);

        page = repository.findOpenAppointmentsQueuePaginatedV2(1L, 2L, 3L, PageRequest.of(0, 1));

        assertThat(page.getContent()).containsExactly(dto);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void devePaginarPacientesEHistorico() {
        EntityManager em = mock(EntityManager.class);
        JpaContext jpaContext = mock(JpaContext.class);
        when(jpaContext.getEntityManagerByManagedType(Patient.class)).thenReturn(em);
        BasicHealthUnit ubs = TestDataFactory.ubs(1L, "UBS");
        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        patient.setPhoneNumber("81999999999");
        patient.setAddressStreet("Rua");
        patient.setAcsName("ACS");

        TypedQuery<Long> patientIds = typedQuery(List.of(10L));
        TypedQuery<Long> count = typedQuery(List.of(), 2L);
        TypedQuery<Patient> patientsQuery = typedQuery(List.of(patient));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(patientIds, count);
        when(em.createQuery(anyString(), eq(Patient.class))).thenReturn(patientsQuery);

        var repository = new PatientRepositoryCustomImpl(jpaContext);
        repository.setValidationUtils(new ValidationUtils());
        var patients = repository.findPatientsPaginated(patient, PageRequest.of(0, 1));

        assertThat(patients.getContent()).containsExactly(patient);
        assertThat(patients.getTotalElements()).isEqualTo(2);

        PatientAppointmentsHistoryDTO history = new PatientAppointmentsHistoryDTO(LocalDateTime.now(), null, null,
                br.com.tecsus.sigaubs.enums.Priorities.ELETIVO,
                AppointmentStatus.AGUARDANDO_CONTEMPLACAO, ProcedureType.CONSULTA, "Consulta",
                "Cardiologia", "Sem observações.", 3L, 20L, null, 10L);
        TypedQuery<Long> historyIds = typedQuery(List.of(20L));
        TypedQuery<PatientAppointmentsHistoryDTO> historyQuery = typedQuery(List.of(history));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(historyIds);
        when(em.createQuery(anyString(), eq(PatientAppointmentsHistoryDTO.class))).thenReturn(historyQuery);

        var historyPage = repository.findPatientAppointmentsHistoryPaginated(patient, PageRequest.of(0, 10));

        assertThat(historyPage.getContent()).containsExactly(history);
        assertThat(historyPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void devePaginarContemplacoesComFiltrosDinamicos() {
        EntityManager em = mock(EntityManager.class);
        JpaContext jpaContext = mock(JpaContext.class);
        when(jpaContext.getEntityManagerByManagedType(Contemplation.class)).thenReturn(em);
        Contemplation contemplation = TestDataFactory.contemplation(10L,
                TestDataFactory.appointment(11L, TestDataFactory.patient(12L, "Maria", TestDataFactory.ubs(1L, "UBS")),
                        TestDataFactory.procedure(2L, "Consulta", ProcedureType.CONSULTA,
                                TestDataFactory.specialty(3L, "Cardiologia"))),
                TestDataFactory.slot(4L, TestDataFactory.ubs(1L, "UBS"),
                        TestDataFactory.procedure(2L, "Consulta", ProcedureType.CONSULTA,
                                TestDataFactory.specialty(3L, "Cardiologia")), 10, 5));

        TypedQuery<Long> ids = typedQuery(List.of(10L));
        TypedQuery<Long> count = typedQuery(List.of(), 2L);
        TypedQuery<Contemplation> content = typedQuery(List.of(contemplation));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(ids, count);
        when(em.createQuery(anyString(), eq(Contemplation.class))).thenReturn(content);

        var repository = new ContemplationRepositoryCustomImpl(jpaContext);
        var page = repository.findConsultationsByUBSAndSpecialtyPaginated(ProcedureType.CONSULTA,
                1L, 2L, YearMonth.of(2026, 7), AppointmentStatus.PRESENCA_CONFIRMADA,
                PageRequest.of(0, 1));

        assertThat(page.getContent()).containsExactly(contemplation);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void devePaginarVagasMedicas() {
        EntityManager em = mock(EntityManager.class);
        JpaContext jpaContext = mock(JpaContext.class);
        when(jpaContext.getEntityManagerByManagedType(MedicalSlot.class)).thenReturn(em);
        BasicHealthUnit ubs = TestDataFactory.ubs(1L, "UBS");
        MedicalProcedure procedure = TestDataFactory.procedure(2L, "Consulta", ProcedureType.CONSULTA,
                TestDataFactory.specialty(3L, "Cardiologia"));
        MedicalSlot slot = TestDataFactory.slot(4L, ubs, procedure, 10, 5);

        TypedQuery<Long> ids = typedQuery(List.of(4L));
        TypedQuery<Long> count = typedQuery(List.of(), 1L);
        TypedQuery<MedicalSlot> content = typedQuery(List.of(slot));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(ids, count);
        when(em.createQuery(anyString(), eq(MedicalSlot.class))).thenReturn(content);

        var repository = new MedicalSlotRepositoryCustomImpl(jpaContext);
        var page = repository.findMedicalSlotsPaginated(new MedicalSlot(), PageRequest.of(0, 1));

        assertThat(page.getContent()).containsExactly(slot);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deveBuscarUsuariosParaVinculoEPaginarUsuariosDoCriador() {
        EntityManager em = mock(EntityManager.class);
        JpaContext jpaContext = mock(JpaContext.class);
        when(jpaContext.getEntityManagerByManagedType(SystemUser.class)).thenReturn(em);
        UBSsystemUserDTO userDto = new UBSsystemUserDTO(10L, "Maria", "null", "null");
        TypedQuery<UBSsystemUserDTO> searchQuery = typedQuery(List.of(userDto));
        when(em.createQuery(anyString(), eq(UBSsystemUserDTO.class))).thenReturn(searchQuery);

        var repository = new SystemUserRepositoryImpl(jpaContext);
        repository.setValidationUtils(new ValidationUtils());

        assertThat(repository.findSystemUsersNameByNameContains("Maria")).containsExactly(userDto);

        SystemRole role = TestDataFactory.role(2L, Roles.ROLE_ATENDENTE);
        SystemUser user = TestDataFactory.systemUser(10L, "maria", role);
        user.setCreationUser("sms");
        user.setName("Maria");
        user.setBasicHealthUnit(TestDataFactory.ubs(1L, "UBS"));
        user.setSelectedRoleId(2L);
        user.setActive(true);

        TypedQuery<Long> ids = typedQuery(List.of(10L));
        Query count = RepositoryMockSupport.nativeSingleResult(2L);
        TypedQuery<SystemUser> usersQuery = typedQuery(List.of(user));
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(ids);
        when(em.createQuery(anyString())).thenReturn(count);
        when(em.createQuery(anyString(), eq(SystemUser.class))).thenReturn(usersQuery);

        var page = repository.findSystemUsersPaginated(user, PageRequest.of(0, 1));

        assertThat(page.getContent()).containsExactly(user);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
