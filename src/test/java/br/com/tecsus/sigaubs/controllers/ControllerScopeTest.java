package br.com.tecsus.sigaubs.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerScopeTest {

    @Test
    void controllersNaoDevemUsarSessionScope() {
        List<Class<?>> controllers = List.of(
                AppointmentController.class,
                BasicHealthUnitController.class,
                ContemplationController.class,
                MedicalSlotController.class,
                PatientController.class,
                QueueController.class,
                ScheduleController.class,
                SessionController.class,
                SpecialtyController.class);

        assertThat(controllers)
                .noneMatch(controller -> controller.isAnnotationPresent(SessionScope.class));
    }
}
