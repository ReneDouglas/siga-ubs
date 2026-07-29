package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.BasicHealthUnitRepository;
import br.com.tecsus.sigaubs.repositories.MedicalProcedureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.role;
import static br.com.tecsus.sigaubs.support.TestDataFactory.systemUser;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicHealthUnitServiceTest {

    @Mock
    private BasicHealthUnitRepository basicHealthUnitRepository;

    @Mock
    private MedicalProcedureRepository medicalProcedureRepository;

    @Mock
    private SystemUserService systemUserService;

    private BasicHealthUnitService service;

    @BeforeEach
    void setUp() {
        service = new BasicHealthUnitService(basicHealthUnitRepository, medicalProcedureRepository);
        service.setSystemUserService(systemUserService);
    }

    @Test
    void deveBuscarUbsDoUsuarioOuLancarErro() {
        BasicHealthUnit ubs = ubs(1L, "UBS");
        when(basicHealthUnitRepository.findById(1L)).thenReturn(Optional.of(ubs));

        assertThat(service.findSystemUserUBS(1L)).isEqualTo(ubs);
        assertThatThrownBy(() -> service.findSystemUserUBS(2L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deveCadastrarEAtualizarUbsComAuditoria() throws Exception {
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        BasicHealthUnit ubs = ubs(1L, "UBS");

        service.registerBasicHealthUnit(ubs, loggedUser);
        assertThat(ubs.getCreationUser()).isEqualTo("admin");
        assertThat(ubs.getCreationDate()).isNotNull();
        verify(basicHealthUnitRepository).save(ubs);

        service.updateBasicHealthUnit(ubs, loggedUser);
        assertThat(ubs.getUpdateUser()).isEqualTo("admin");
        assertThat(ubs.getUpdateDate()).isNotNull();
        verify(basicHealthUnitRepository, times(2)).save(ubs);
    }

    @Test
    void deveDeletarUbsDesvinculandoUsuarios() throws Exception {
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        var role = role(1L, Roles.ROLE_USER);
        SystemUser user = systemUser(1L, "user", role);
        BasicHealthUnit ubs = ubs(1L, "UBS");
        ubs.setSystemUsers(List.of(user));
        when(basicHealthUnitRepository.findById(1L)).thenReturn(Optional.of(ubs));

        service.deleteBasicHealtUnit(1L, loggedUser);

        ArgumentCaptor<List<SystemUser>> captor = ArgumentCaptor.forClass(List.class);
        verify(systemUserService).updateBasicHealthUnitSystemUsers(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(su -> {
            assertThat(su.getBasicHealthUnit()).isNull();
            assertThat(su.getUpdateUser()).isEqualTo("admin");
            assertThat(su.getUpdateDate()).isNotNull();
        });
        verify(basicHealthUnitRepository).delete(ubs);
    }

    @Test
    void deveMapearUsuariosDaUbsEDesvincular() throws Exception {
        var role = role(1L, Roles.ROLE_ATENDENTE);
        SystemUser user = systemUser(1L, "user", role);
        BasicHealthUnit ubs = ubs(1L, "UBS");
        ubs.setSystemUsers(List.of(user));
        when(basicHealthUnitRepository.findById(1L)).thenReturn(Optional.of(ubs));
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        when(systemUserService.findManageableSystemUserById(1L, loggedUser)).thenReturn(user);

        assertThat(service.findUBSsystemUsersByUBSid(1L))
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.role()).isEqualTo(Roles.ROLE_ATENDENTE.getDescription());
                });

        service.unlinkBasicHealthUnitSystemUser(1L, loggedUser);
        assertThat(user.getBasicHealthUnit()).isNull();
        verify(systemUserService).updateBasicHealthUnitSystemUsers(anyList());

    }

    @Test
    void deveRetornarListaVaziaQuandoUbsNaoExisteOuSemUsuarios() {
        when(basicHealthUnitRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findUBSsystemUsersByUBSid(99L)).isEmpty();

        when(basicHealthUnitRepository.findById(1L)).thenReturn(Optional.of(ubs(1L, "UBS")));
        assertThat(service.findUBSsystemUsersByUBSid(1L)).isEmpty();
    }
}
