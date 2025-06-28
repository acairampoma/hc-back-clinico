package com.formacionbdi.microservicios.app.listas.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Entidad que mapea la vista vista_pacientes_por_cama
 * Representa la información de pacientes asignados a camas
 * REFACTORIZADA: Incluye campos necesarios para notas médicas
 */
@Entity
@Table(name = "vista_pacientes_por_cama")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacientePorCama {

    /**
     * Código de la cama (bed_number) - Clave primaria
     */
    @Id
    @Column(name = "bed_number")
    @JsonProperty("bed_number")
    private String bedNumber;

    /**
     * Datos completos del paciente en formato JSON
     * Si la cama está vacía, este campo será null
     * INCLUYE: hospitalizacion_id, numero_cuenta, paciente_id, etc.
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
        return this.patientData != null && !this.patientData.trim().isEmpty() && !"null".equals(this.patientData.trim());
    }

    /**
     * Verifica si la cama está disponible
     */
    public boolean isAvailable() {
        return !isOccupied();
    }

    /**
     * Obtiene descripción del estado de la cama
     */
    public String getEstadoDescripcion() {
        return isOccupied() ? "OCUPADA" : "DISPONIBLE";
    }
}