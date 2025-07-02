package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO Record para respuesta completa de orden médica - Java 17
 * GET /ordenes/{id} - Response completo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdenCompletaDTO(

        @JsonProperty("id")
        Long id,

        @JsonProperty("numeroOrden")
        String numeroOrden,

        @JsonProperty("pacienteId")
        Long pacienteId,

        @JsonProperty("medicoId")
        Long medicoId,

        @JsonProperty("tipoOrigen")
        String tipoOrigen,

        @JsonProperty("tipoOrigenDescripcion")
        String tipoOrigenDescripcion,

        @JsonProperty("origenId")
        Long origenId,

        @JsonProperty("tipoOrden")
        String tipoOrden,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonProperty("fechaOrden")
        LocalDateTime fechaOrden,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("fechaProgramada")
        LocalDate fechaProgramada,

        @JsonProperty("diagnosticoPrincipal")
        String diagnosticoPrincipal,

        @JsonProperty("justificacionClinica")
        String justificacionClinica,

        @JsonProperty("prioridad")
        String prioridad,

        @JsonProperty("prioridadDescripcion")
        String prioridadDescripcion,

        @JsonProperty("estado")
        String estado,

        @JsonProperty("estadoDescripcion")
        String estadoDescripcion,

        @JsonProperty("firmada")
        String firmada,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonProperty("fechaFirma")
        LocalDateTime fechaFirma,

        @JsonProperty("firmaDigital")
        String firmaDigital,

        @JsonProperty("examenes")
        List<OrdenExamenDTO> examenes,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonProperty("creadoEn")
        LocalDateTime creadoEn,

        @JsonProperty("creadoPor")
        Long creadoPor,

        @JsonProperty("totalExamenes")
        Integer totalExamenes
) {

    /**
     * Compact constructor con valores por defecto
     */
    public OrdenCompletaDTO {
        // Total de exámenes por defecto
        if (totalExamenes == null) {
            totalExamenes = examenes != null ? examenes.size() : 0;
        }

        // Firmada por defecto
        if (firmada == null) {
            firmada = "N";
        }
    }

    /**
     * Factory method básico
     */
    public static OrdenCompletaDTO crear(Long id, String numeroOrden, Long pacienteId, Long medicoId,
                                         String estado, String estadoDescripcion, List<OrdenExamenDTO> examenes) {
        return new OrdenCompletaDTO(
                id, numeroOrden, pacienteId, medicoId, null, null, null, null,
                LocalDateTime.now(), null, null, null, null, null, estado, estadoDescripcion,
                "N", null, null, examenes, LocalDateTime.now(), medicoId, null
        );
    }

    /**
     * Métodos de estado
     */
    public boolean estaActiva() {
        return estado != null && !estado.equals("05"); // No cancelada
    }

    public boolean estaSolicitada() {
        return "01".equals(estado);
    }

    public boolean estaProgramada() {
        return "02".equals(estado);
    }

    public boolean estaEnProceso() {
        return "03".equals(estado);
    }

    public boolean estaCompletada() {
        return "04".equals(estado);
    }

    public boolean estaCancelada() {
        return "05".equals(estado);
    }

    /**
     * Métodos de prioridad
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

    public int getPrioridadNumerica() {
        return switch (prioridad != null ? prioridad : "N") {
            case "E" -> 3;
            case "U" -> 2;
            case "N" -> 1;
            default -> 1;
        };
    }

    /**
     * Métodos de firma
     */
    public boolean estaFirmada() {
        return "S".equals(firmada);
    }

    public boolean requiereFirma() {
        return switch (estado != null ? estado : "01") {
            case "01", "02" -> true;  // Solicitada o Programada
            case "03", "04", "05" -> false; // Estados avanzados
            default -> true;
        };
    }

    /**
     * Métodos de exámenes
     */
    public boolean tieneExamenes() {
        return examenes != null && !examenes.isEmpty();
    }

    public int getTotalExamenesSeguro() {
        return totalExamenes != null ? totalExamenes : 0;
    }

    public long getExamenesPendientes() {
        if (examenes == null) return 0;
        return examenes.stream()
                .filter(examen -> "01".equals(examen.estadoDetalle()))
                .count();
    }

    public long getExamenesCompletados() {
        if (examenes == null) return 0;
        return examenes.stream()
                .filter(examen -> "03".equals(examen.estadoDetalle()))
                .count();
    }

    /**
     * Métodos de fechas
     */
    public boolean esProgramadaParaHoy() {
        return fechaProgramada != null && fechaProgramada.equals(LocalDate.now());
    }

    public boolean esProgramadaParaFuturo() {
        return fechaProgramada != null && fechaProgramada.isAfter(LocalDate.now());
    }

    public boolean esProgramadaVencida() {
        return fechaProgramada != null && fechaProgramada.isBefore(LocalDate.now());
    }

    /**
     * Métodos de origen
     */
    public boolean esDeHospitalizacion() {
        return "HOS".equals(tipoOrigen);
    }

    public boolean esAmbulatorio() {
        return "AMB".equals(tipoOrigen);
    }

    public boolean esDeEmergencia() {
        return "EMR".equals(tipoOrigen);
    }

    /**
     * Métodos de modificación
     */
    public boolean puedeSerModificada() {
        return switch (estado != null ? estado : "01") {
            case "01", "02" -> true;  // Solicitada o Programada
            case "03", "04", "05" -> false; // En proceso, Completada, Cancelada
            default -> false;
        };
    }

    public boolean puedeSerCancelada() {
        return switch (estado != null ? estado : "01") {
            case "01", "02", "03" -> true; // Hasta En proceso
            case "04", "05" -> false; // Completada o ya Cancelada
            default -> false;
        };
    }

    /**
     * Resumen para logging o display
     */
    public String getResumenCorto() {
        return String.format("Orden %s - %s - %d exámenes",
                numeroOrden, estadoDescripcion, getTotalExamenesSeguro());
    }

    public String getResumenCompleto() {
        return String.format(
                "Orden %s | Paciente: %d | Estado: %s | Prioridad: %s | Exámenes: %d | Firmada: %s",
                numeroOrden, pacienteId, estadoDescripcion, prioridadDescripcion,
                getTotalExamenesSeguro(), estaFirmada() ? "Sí" : "No"
        );
    }

    /**
     * Validación completa
     */
    public boolean esValida() {
        return id != null &&
                numeroOrden != null &&
                pacienteId != null &&
                medicoId != null &&
                estado != null &&
                tieneExamenes();
    }

    /**
     * Porcentaje de progreso (0-100)
     */
    public int getPorcentajeProgreso() {
        if (!tieneExamenes()) return 0;

        return switch (estado != null ? estado : "01") {
            case "01" -> 10;  // Solicitada
            case "02" -> 25;  // Programada
            case "03" -> 75;  // En proceso
            case "04" -> 100; // Completada
            case "05" -> 0;   // Cancelada
            default -> 0;
        };
    }

    /**
     * Copia con nuevo estado (para actualizaciones inmutables)
     */
    public OrdenCompletaDTO conEstado(String nuevoEstado, String nuevaDescripcion) {
        return new OrdenCompletaDTO(
                id, numeroOrden, pacienteId, medicoId, tipoOrigen, tipoOrigenDescripcion,
                origenId, tipoOrden, fechaOrden, fechaProgramada, diagnosticoPrincipal,
                justificacionClinica, prioridad, prioridadDescripcion, nuevoEstado, nuevaDescripcion,
                firmada, fechaFirma, firmaDigital, examenes, creadoEn, creadoPor, totalExamenes
        );
    }

    /**
     * Copia firmada
     */
    public OrdenCompletaDTO firmada(LocalDateTime fechaFirma, String firmaDigital) {
        return new OrdenCompletaDTO(
                id, numeroOrden, pacienteId, medicoId, tipoOrigen, tipoOrigenDescripcion,
                origenId, tipoOrden, fechaOrden, fechaProgramada, diagnosticoPrincipal,
                justificacionClinica, prioridad, prioridadDescripcion, estado, estadoDescripcion,
                "S", fechaFirma, firmaDigital, examenes, creadoEn, creadoPor, totalExamenes
        );
    }
}