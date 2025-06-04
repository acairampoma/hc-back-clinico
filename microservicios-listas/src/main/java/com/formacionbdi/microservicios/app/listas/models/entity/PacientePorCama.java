package com.formacionbdi.microservicios.app.listas.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Entidad que mapea la vista vista_pacientes_por_cama
 * Representa la información de pacientes asignados a camas
 */
@Entity
@Table(name = "vista_pacientes_por_cama")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacientePorCama {

    /**
     * Código de la cama (bed_number)
     */
    @Id
    @Column(name = "bed_number")
    @JsonProperty("bed_number")
    private String bedNumber;

    /**
     * Datos completos del paciente en formato JSON
     * Si la cama está vacía, este campo será null
     */
    @Column(name = "patient_data", columnDefinition = "jsonb")
    @JsonProperty("patient_data")
    private String patientData;

    /**
     * Constructor para crear una cama vacía
     */
    public PacientePorCama(String bedNumber) {
        this.bedNumber = bedNumber;
        this.patientData = null;
    }

    /**
     * Verifica si la cama está ocupada
     */
    public boolean isOccupied() {
        return this.patientData != null && !this.patientData.trim().isEmpty();
    }

    /**
     * Verifica si la cama está disponible
     */
    public boolean isAvailable() {
        return !isOccupied();
    }
}