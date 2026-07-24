package br.com.tecsus.sigaubs.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumsTest {

    @Test
    void deveResolverStatusPorDescricaoEListarStatusDeContemplacao() {
        assertThat(AppointmentStatus.getByDescription("Paciente Contemplado"))
                .isEqualTo(AppointmentStatus.PACIENTE_CONTEMPLADO);
        assertThat(AppointmentStatus.getContemplationValues())
                .containsExactly(
                        AppointmentStatus.PACIENTE_CONTEMPLADO,
                        AppointmentStatus.PRESENCA_CONFIRMADA,
                        AppointmentStatus.CONTEMPLACAO_CANCELADA,
                        AppointmentStatus.ATENDIMENTO_CONCLUIDO);
        assertThatThrownBy(() -> AppointmentStatus.getByDescription("Não existe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveResolverTipoProcedimentoPorDescricao() {
        assertThat(ProcedureType.getProcedureTypeByDescription("Consulta")).isEqualTo(ProcedureType.CONSULTA);
        assertThat(ProcedureType.getMedicalProceduresDescription())
                .containsExactly("Consulta", "Exame", "Cirurgia");
        assertThatThrownBy(() -> ProcedureType.getProcedureTypeByDescription("Outro"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveExporMetadadosDePrioridadesRolesESituacaoSocial() {
        assertThat(Priorities.URGENCIA.getValue()).isEqualTo(2);
        assertThat(Priorities.URGENCIA.getManual()).isTrue();
        assertThat(Priorities.IDADE.getManual()).isFalse();

        assertThat(Roles.ROLE_ADMIN.getPermission()).isFalse();
        assertThat(Roles.ROLE_SMS.getDescription()).contains("Secretaria");

        assertThat(SocialSituationRating.getDescriptionSortedByRating())
                .first()
                .isEqualTo("1/4 salário mínimo (R$ 353,00)");
    }

    @Test
    void deveResolverMensagensDoSistema() {
        assertThat(SystemMessages.getDescription("scc01")).isEqualTo("Cadastro realizado com sucesso.");
        assertThatThrownBy(() -> SystemMessages.getDescription("nao-existe"))
                .isInstanceOf(RuntimeException.class);
    }
}
