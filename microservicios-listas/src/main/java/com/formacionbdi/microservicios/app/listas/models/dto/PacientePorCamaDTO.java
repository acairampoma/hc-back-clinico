package com.formacionbdi.microservicios.app.listas.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO que mapea completamente la estructura JSON de vista_pacientes_por_cama
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
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PatientData {

        @JsonProperty("personal_info")
        private PersonalInfo personalInfo;

        @JsonProperty("medical_info")
        private MedicalInfo medicalInfo;
    }

    /**
     * DTO para información personal del paciente
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
}