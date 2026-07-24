package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TenantJpaIsolationTest {

    private final TenantRepository tenantRepository;
    private final BasicHealthUnitRepository basicHealthUnitRepository;
    private final PatientRepository patientRepository;

    @Autowired
    TenantJpaIsolationTest(TenantRepository tenantRepository,
            BasicHealthUnitRepository basicHealthUnitRepository,
            PatientRepository patientRepository) {
        this.tenantRepository = tenantRepository;
        this.basicHealthUnitRepository = basicHealthUnitRepository;
        this.patientRepository = patientRepository;
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveIsolarUBSEPacientesPorTenant() {
        Tenant afogados = createTenant("afogados", "Afogados");
        Tenant caruaru = createTenant("caruaru", "Caruaru");

        Long pacienteAfogadosId = withTenant(afogados, () -> {
            BasicHealthUnit ubs = basicHealthUnitRepository.saveAndFlush(ubs("UBS Afogados"));
            return patientRepository.saveAndFlush(patient("Paciente Afogados", ubs)).getId();
        });

        Long pacienteCaruaruId = withTenant(caruaru, () -> {
            BasicHealthUnit ubs = basicHealthUnitRepository.saveAndFlush(ubs("UBS Caruaru"));
            return patientRepository.saveAndFlush(patient("Paciente Caruaru", ubs)).getId();
        });

        withTenant(afogados, () -> {
            assertThat(basicHealthUnitRepository.findAll()).extracting(BasicHealthUnit::getName)
                    .containsExactly("UBS Afogados");
            assertThat(patientRepository.findAll()).extracting(Patient::getName)
                    .containsExactly("Paciente Afogados");
            assertThat(patientRepository.findById(pacienteAfogadosId)).isPresent();
            assertThat(patientRepository.findById(pacienteCaruaruId)).isEmpty();
            return null;
        });

        withTenant(caruaru, () -> {
            assertThat(basicHealthUnitRepository.findAll()).extracting(BasicHealthUnit::getName)
                    .containsExactly("UBS Caruaru");
            assertThat(patientRepository.findAll()).extracting(Patient::getName)
                    .containsExactly("Paciente Caruaru");
            assertThat(patientRepository.findById(pacienteAfogadosId)).isEmpty();
            assertThat(patientRepository.findById(pacienteCaruaruId)).isPresent();
            return null;
        });
    }

    @Test
    void deveExigirContextoDeTenantParaInserirEntidadeTenantScoped() {
        assertThatThrownBy(() -> patientRepository.saveAndFlush(patient("Paciente Sem Tenant", null)))
                .hasMessageContaining("Contexto de tenant ausente");
    }

    private Tenant createTenant(String slug, String name) {
        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName(name);
        tenant.setDomain(slug + ".sigaubs.com.br");
        tenant.setStatus("ACTIVE");
        tenant.setCreationDate(LocalDateTime.now());
        tenant.setCreationUser("test");
        return tenantRepository.saveAndFlush(tenant);
    }

    private BasicHealthUnit ubs(String name) {
        BasicHealthUnit basicHealthUnit = new BasicHealthUnit();
        basicHealthUnit.setName(name);
        basicHealthUnit.setNeighborhood("Centro");
        basicHealthUnit.setCreationDate(LocalDateTime.now());
        basicHealthUnit.setCreationUser("test");
        return basicHealthUnit;
    }

    private Patient patient(String name, BasicHealthUnit basicHealthUnit) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setSusNumber("111111111111111");
        patient.setCpf("11111111111");
        patient.setGender("Feminino");
        patient.setBirthDate(LocalDate.of(1990, 1, 1));
        patient.setSocialSituationRating(SocialSituationRating.UM_SALARIO_MINIMO);
        patient.setPhoneNumber("81999999999");
        patient.setAddressStreet("Rua Teste");
        patient.setAddressNumber("1");
        patient.setBasicHealthUnit(basicHealthUnit);
        patient.setCreationDate(LocalDateTime.now());
        patient.setCreationUser("test");
        return patient;
    }

    private <T> T withTenant(Tenant tenant, Supplier<T> action) {
        TenantContextHolder.setTenant(tenant.getId(), tenant.getSlug());
        try {
            return action.get();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
