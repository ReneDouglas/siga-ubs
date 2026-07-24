package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.jobs.ContemplationScheduleV2;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScheduleControllerTest {

    @Test
    void deveExecutarRotinaManual() {
        ContemplationScheduleV2 schedule = mock(ContemplationScheduleV2.class);
        ScheduleController controller = new ScheduleController(schedule);

        var response = controller.start();

        assertThat(response.getBody()).isEqualTo("executado");
        verify(schedule).processContemplationTask();
    }
}
