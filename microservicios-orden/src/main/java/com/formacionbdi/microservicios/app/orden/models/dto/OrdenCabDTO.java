package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO Record para crear nueva orden médica - Java 17
 * POST /ordenes
 */
public record OrdenCabDTO(

        @NotNull(message = "El paciente es obligatorio")
        @JsonProperty("pacienteId")
        Long pacienteId,

        @NotNull(message = "El médico es obligatorio")
        @JsonProperty("medicoId")
        Long medicoId,

        @NotNull(message = "El tipo de origen es obligatorio")
        @Size(max = 3, message = "Tipo origen máximo 3 caracteres")
        @JsonProperty("tipoOrigen")
        String tipoOrigen,

        @NotNull(message = "El origen ID es obligatorio")
        @JsonProperty("origenId")
        Long origenId,

        @NotNull(message = "El tipo de orden es obligatorio")
        @Size(max = 20, message = "Tipo orden máximo 20 caracteres")
        @JsonProperty("tipoOrden")
        String tipoOrden,

        @NotNull(message = "La justificación clínica es obligatoria")
        @NotEmpty(message = "La justificación clínica no puede estar vacía")
        @JsonProperty("justificacionClinica")
        String justificacionClinica,

        @NotNull(message = "Los exámenes son obligatorios")
        @NotEmpty(message = "Debe incluir al menos un examen")
        @Valid
        @JsonProperty("examenes")
        List<OrdenDetDTO> examenes,

        @Size(max = 10, message = "Diagnóstico no puede exceder 10 caracteres")
        @JsonProperty("diagnosticoPrincipal")
        String diagnosticoPrincipal,

        @Size(max = 1, message = "Prioridad debe ser E, U o N")
        @JsonProperty("prioridad")
        String prioridad,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("fechaProgramada")
        LocalDate fechaProgramada
) {

    /**
     * Compact constructor con validaciones personalizadas
     */
    public OrdenCabDTO {
        // Validar tipo de origen
        if (tipoOrigen != null && !esTipoOrigenValido(tipoOrigen)) {
            throw new IllegalArgumentException("Tipo de origen debe ser HOS, AMB o EMR");
        }

        // Validar tipo de orden
        if (tipoOrden != null && !esTipoOrdenValido(tipoOrden)) {
            throw new IllegalArgumentException("Tipo de orden debe ser LAB, IMG, PROC o FUNC");
        }

        // Validar prioridad
        if (prioridad != null && !esPrioridadValida(prioridad)) {
            throw new IllegalArgumentException("Prioridad debe ser E, U o N");
        }

        // Validar cantidad de exámenes
        if (examenes != null && examenes.size() > 10) {
            throw new IllegalArgumentException("Máximo 10 exámenes por orden");
        }

        // Validar fecha programada
        if (fechaProgramada != null && fechaProgramada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha programada no puede ser anterior a hoy");
        }
    }

    /**
     * Métodos helper para validaciones
     */
    private static boolean esTipoOrigenValido(String tipo) {
        return switch (tipo) {
            case "HOS", "AMB", "EMR" -> true;
            default -> false;
        };
    }

    private static boolean esTipoOrdenValido(String tipo) {
        return switch (tipo) {
            case "LAB", "IMG", "PROC", "FUNC" -> true;
            default -> false;
        };
    }

    private static boolean esPrioridadValida(String prioridad) {
        return switch (prioridad) {
            case "E", "U", "N" -> true;
            default -> false;
        };
    }

    /**
     * Factory method para crear con valores por defecto
     */
    public static OrdenCabDTO crear(Long pacienteId, Long medicoId, String tipoOrigen,
                                    Long origenId, String tipoOrden, String justificacion,
                                    List<OrdenDetDTO> examenes) {
        return new OrdenCabDTO(
                pacienteId, medicoId, tipoOrigen, origenId, tipoOrden,
                justificacion, examenes, null, "N", null
        );
    }

    /**
     * Métodos de utilidad
     */
    public boolean esEmergencia() {
        return "E".equals(prioridad);
    }

    public boolean esUrgente() {
        return "U".equals(prioridad);
    }

    public boolean esNormal() {
        return "N".equals(prioridad) || prioridad == null;
    }

    public String getPrioridadDescripcion() {
        return switch (prioridad != null ? prioridad : "N") {
            case "E" -> "Emergencia";
            case "U" -> "Urgente";
            case "N" -> "Normal";
            default -> "Normal";
        };
    }

    public String getTipoOrigenDescripcion() {
        return switch (tipoOrigen) {
            case "HOS" -> "Hospitalización";
            case "AMB" -> "Ambulatorio";
            case "EMR" -> "Emergencia";
            default -> "Desconocido";
        };
    }

    public int getTotalExamenes() {
        return examenes != null ? examenes.size() : 0;
    }

    public boolean requiereFirmaAutomatica() {
        return esEmergencia() || getTotalExamenes() <= 3;
    }
}