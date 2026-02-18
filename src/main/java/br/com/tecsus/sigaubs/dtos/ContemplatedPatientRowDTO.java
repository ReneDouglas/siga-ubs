package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;

public record ContemplatedPatientRowDTO(
        String patientName,
        String specialty,
        ProcedureType procedureType,
        String medicalProcedureDescription,
        AppointmentStatus appointmentStatus) {}
