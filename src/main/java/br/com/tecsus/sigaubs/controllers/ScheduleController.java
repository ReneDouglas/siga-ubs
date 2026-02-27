package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.jobs.ContemplationScheduleV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduleController {

    private final ContemplationScheduleV2 contemplationSchedule;

    @Autowired
    public ScheduleController(ContemplationScheduleV2 contemplationSchedule) {
        this.contemplationSchedule = contemplationSchedule;
    }

    @GetMapping("/contemplation-management/test/schedule")
    public ResponseEntity<String> start() {
        contemplationSchedule.processContemplationTask();
        return ResponseEntity.ok("executado");
    }
}
