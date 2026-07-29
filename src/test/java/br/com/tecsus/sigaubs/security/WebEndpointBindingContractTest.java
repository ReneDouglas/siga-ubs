package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.controllers.AdminSessionController;
import br.com.tecsus.sigaubs.controllers.AdminSmsUserController;
import br.com.tecsus.sigaubs.controllers.AdminUserManagementController;
import br.com.tecsus.sigaubs.controllers.AppointmentController;
import br.com.tecsus.sigaubs.controllers.BasicHealthUnitController;
import br.com.tecsus.sigaubs.controllers.ContemplationController;
import br.com.tecsus.sigaubs.controllers.MedicalSlotController;
import br.com.tecsus.sigaubs.controllers.PatientController;
import br.com.tecsus.sigaubs.controllers.QueueController;
import br.com.tecsus.sigaubs.controllers.ScheduleController;
import br.com.tecsus.sigaubs.controllers.SessionController;
import br.com.tecsus.sigaubs.controllers.SessionManagementController;
import br.com.tecsus.sigaubs.controllers.SpecialtyController;
import br.com.tecsus.sigaubs.controllers.TenantManagementController;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebEndpointBindingContractTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            AdminSessionController.class,
            AdminSmsUserController.class,
            AdminUserManagementController.class,
            AppointmentController.class,
            BasicHealthUnitController.class,
            ContemplationController.class,
            MedicalSlotController.class,
            PatientController.class,
            QueueController.class,
            ScheduleController.class,
            SessionController.class,
            SessionManagementController.class,
            SpecialtyController.class,
            TenantManagementController.class);

    @Test
    void endpointsMapeadosNaoFazemBindingDiretoDeEntidadesJpa() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMapped(method)) {
                    continue;
                }
                for (Class<?> parameterType : method.getParameterTypes()) {
                    if (parameterType.isAnnotationPresent(Entity.class)
                            || parameterType.getPackageName().equals("br.com.tecsus.sigaubs.entities")) {
                        violations.add(controller.getSimpleName() + "#" + method.getName()
                                + " recebe " + parameterType.getSimpleName());
                    }
                }
            }
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void endpointQueDisparaJobExisteSomenteNoProfileDev() {
        Profile profile = ScheduleController.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");
        assertThat(ScheduleController.class.getDeclaredMethods())
                .filteredOn(this::isMapped)
                .allMatch(method -> method.isAnnotationPresent(PostMapping.class));
    }

    private boolean isMapped(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
