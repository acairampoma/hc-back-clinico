package com.formacionbdi.microservicios.app.orden.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para actualizar el estado de una orden
 * Implementado como record para inmutabilidad
 */
public record ActualizarEstadoOrdenDTO(
    @NotBlank(message = "El nuevo estado es obligatorio")
    String nuevoEstado,
    
    @NotNull(message = "El ID del médico es obligatorio")
    Long medicoId,
    
    String observacion
) {
    /**
     * Constructor estático para facilitar la creación
     */
    public static ActualizarEstadoOrdenDTO crear(String nuevoEstado, Long medicoId, String observacion) {
        return new ActualizarEstadoOrdenDTO(nuevoEstado, medicoId, observacion);
    }
}
