package com.formacionbdi.microservicios.app.orden.models.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para mostrar resumen de órdenes en listados
 * Implementado como record para inmutabilidad
 */
public record OrdenResumenDTO(
    Long id,
    String numeroOrden,
    Long pacienteId,
    String nombrePaciente,
    Long medicoId,
    String nombreMedico,
    String tipoOrigen,
    String tipoOrigenDescripcion,
    Long origenId,
    String tipoOrden,
    LocalDate fechaOrden,
    LocalDate fechaProgramada,
    String diagnosticoPrincipal,
    String prioridad,
    String prioridadDescripcion,
    String estado,
    String estadoDescripcion,
    String firmada,
    int cantidadExamenes,
    LocalDateTime creadoEn
) {
    /**
     * Constructor estático para facilitar la creación
     */
    public static OrdenResumenDTO crear(
            Long id,
            String numeroOrden,
            Long pacienteId,
            String nombrePaciente,
            Long medicoId,
            String nombreMedico,
            String tipoOrigen,
            String tipoOrigenDescripcion,
            Long origenId,
            String tipoOrden,
            LocalDate fechaOrden,
            LocalDate fechaProgramada,
            String diagnosticoPrincipal,
            String prioridad,
            String prioridadDescripcion,
            String estado,
            String estadoDescripcion,
            String firmada,
            int cantidadExamenes,
            LocalDateTime creadoEn
    ) {
        return new OrdenResumenDTO(
                id, numeroOrden, pacienteId, nombrePaciente, medicoId, nombreMedico,
                tipoOrigen, tipoOrigenDescripcion, origenId, tipoOrden,
                fechaOrden, fechaProgramada, diagnosticoPrincipal,
                prioridad, prioridadDescripcion, estado, estadoDescripcion,
                firmada, cantidadExamenes, creadoEn
        );
    }
}
