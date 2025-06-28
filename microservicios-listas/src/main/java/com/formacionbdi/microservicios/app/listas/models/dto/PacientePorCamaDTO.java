package com.formacionbdi.microservicios.app.listas.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

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

    @JsonProperty("available")
    private boolean available;

    @JsonProperty("occupied")
    private boolean occupied;

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
}