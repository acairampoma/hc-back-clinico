package com.formacionbdi.microservicios.app.listas.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO que mapea completamente la estructura JSON de vista_pacientes_por_cama
 * REFACTORIZADO: Incluye todos los campos necesarios para notas médicas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PacientePorCamaDTO {

    @JsonProperty("bed_number")
    private String bedNumber;

    @JsonProperty("patient_data")
    private PatientData patientData;

    /**
     * DTO para la información completa del paciente
     * AGREGADOS: campos necesarios para notas médicas
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PatientData {

        // *** CAMPOS PARA NOTAS MÉDICAS ***
        @JsonProperty("hospitalizacion_id")
        private Long hospitalizacionId;

        @JsonProperty("numero_cuenta")
        private String numeroCuenta;

        @JsonProperty("paciente_id")
        private Long pacienteId;

        @JsonProperty("medico_tratante_id")
        private Long medicoTratanteId;

        @JsonProperty("especialidad_id")
        private Long especialidadId;

        @JsonProperty("fecha_ingreso")
        private LocalDateTime fechaIngreso;

        // *** DATOS EXISTENTES ***
        @JsonProperty("personal_info")
        private PersonalInfo personalInfo;

        @JsonProperty("medical_info")
        private MedicalInfo medicalInfo;
    }

    /**
     * DTO para información personal del paciente
     * AGREGADOS: fullname, medical_record
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PersonalInfo {

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        // *** CAMPOS NUEVOS ***
        @JsonProperty("fullname")
        private String fullname;

        @JsonProperty("medical_record")
        private String medicalRecord;

        @JsonProperty("age")
        private Integer age;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("dni")
        private String dni;

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("emergency_contact")
        private String emergencyContact;
    }

    /**
     * DTO para información médica del paciente
     * AGREGADO: blood_type
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MedicalInfo {

        @JsonProperty("primary_diagnosis")
        private String primaryDiagnosis;

        @JsonProperty("primary_diagnosis_code")
        private String primaryDiagnosisCode;

        @JsonProperty("secondary_diagnosis")
        private String secondaryDiagnosis;

        @JsonProperty("admission_date")
        private LocalDate admissionDate;

        @JsonProperty("medical_record")
        private String medicalRecord;

        @JsonProperty("attending_physician")
        private String attendingPhysician;

        @JsonProperty("admission_reason")
        private String admissionReason;

        @JsonProperty("current_medications")
        private List<String> currentMedications;

        @JsonProperty("allergies")
        private String allergies;

        // *** CAMPO NUEVO ***
        @JsonProperty("blood_type")
        private String bloodType;
    }

    /**
     * Verifica si la cama está ocupada
     */
    public boolean isOccupied() {
        return this.patientData != null;
    }

    /**
     * Verifica si la cama está disponible
     */
    public boolean isAvailable() {
        return !isOccupied();
    }

    /**
     * Obtiene el nombre completo del paciente
     */
    public String getPatientFullName() {
        if (patientData != null && patientData.getPersonalInfo() != null) {
            PersonalInfo info = patientData.getPersonalInfo();
            // Priorizar fullname si existe, sino concatenar
            if (info.getFullname() != null && !info.getFullname().trim().isEmpty()) {
                return info.getFullname().trim();
            }
            return (info.getFirstName() != null ? info.getFirstName() : "") + " " +
                    (info.getLastName() != null ? info.getLastName() : "");
        }
        return null;
    }

    /**
     * Obtiene el diagnóstico principal
     */
    public String getPrimaryDiagnosis() {
        if (patientData != null && patientData.getMedicalInfo() != null) {
            return patientData.getMedicalInfo().getPrimaryDiagnosis();
        }
        return null;
    }

    /**
     * Obtiene el ID de hospitalización para notas médicas
     */
    public Long getHospitalizacionId() {
        if (patientData != null) {
            return patientData.getHospitalizacionId();
        }
        return null;
    }

    /**
     * Obtiene el número de cuenta para notas médicas
     */
    public String getNumeroCuenta() {
        if (patientData != null) {
            return patientData.getNumeroCuenta();
        }
        return null;
    }

    /**
     * Obtiene el ID del paciente para notas médicas
     */
    public Long getPacienteId() {
        if (patientData != null) {
            return patientData.getPacienteId();
        }
        return null;
    }

    /**
     * Obtiene el ID del médico tratante para notas médicas
     */
    public Long getMedicoTratanteId() {
        if (patientData != null) {
            return patientData.getMedicoTratanteId();
        }
        return null;
    }
}