package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO Record para actualizar orden médica - Java 17
 * PUT /ordenes/{id}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActualizarOrdenDTO(

        @Size(max = 10, message = "Diagnóstico no puede exceder 10 caracteres")
        @JsonProperty("diagnosticoPrincipal")
        String diagnosticoPrincipal,

        @JsonProperty("justificacionClinica")
        String justificacionClinica,

        @JsonProperty("fechaProgramada")
        LocalDate fechaProgramada,

        @Size(max = 1, message = "Prioridad debe ser E, U o N")
        @JsonProperty("prioridad")
        String prioridad,

        @JsonProperty("observaciones")
        String observaciones,

        // Operaciones con exámenes
        @Valid
        @JsonProperty("agregarExamenes")
        List<NuevoExamenDTO> agregarExamenes,

        @Valid
        @JsonProperty("modificarExamenes")
        List<ModificarExamenDTO> modificarExamenes,

        @JsonProperty("eliminarExamenes")
        List<Long> eliminarExamenes,

        @JsonProperty("medicoId")
        Long medicoId
) {

    /**
     * Record anidado para nuevos exámenes
     */
    public record NuevoExamenDTO(
            @JsonProperty("examenId")
            Long examenId,

            @JsonProperty("cantidad")
            Integer cantidad,

            @JsonProperty("desIndicacion")
            String desIndicacion,

            @JsonProperty("desConsideraciones")
            String desConsideraciones
    ) {
        public NuevoExamenDTO {
            if (cantidad == null) {
                cantidad = 1;
            }
        }

        public static NuevoExamenDTO crear(Long examenId, Integer cantidad) {
            return new NuevoExamenDTO(examenId, cantidad, null, null);
        }

        public boolean esValido() {
            return examenId != null && cantidad != null && cantidad > 0;
        }
    }

    /**
     * Record anidado para modificar exámenes existentes
     */
    public record ModificarExamenDTO(
            @JsonProperty("id")
            Long id,

            @JsonProperty("cantidad")
            Integer cantidad,

            @JsonProperty("desIndicacion")
            String desIndicacion,

            @JsonProperty("desConsideraciones")
            String desConsideraciones
    ) {
        public boolean esValido() {
            return id != null;
        }

        public boolean tieneCambios() {
            return cantidad != null || desIndicacion != null || desConsideraciones != null;
        }
    }

    /**
     * Compact constructor con validaciones
     */
    public ActualizarOrdenDTO {
        // Validar prioridad
        if (prioridad != null && !esPrioridadValida(prioridad)) {
            throw new IllegalArgumentException("Prioridad debe ser E, U o N");
        }

        // Validar fecha programada
        if (fechaProgramada != null && fechaProgramada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha programada no puede ser anterior a hoy");
        }

        // Validar que nuevos exámenes sean válidos
        if (agregarExamenes != null) {
            for (var examen : agregarExamenes) {
                if (!examen.esValido()) {
                    throw new IllegalArgumentException("Examen inválido: " + examen.examenId());
                }
            }
        }

        // Validar que modificaciones tengan cambios
        if (modificarExamenes != null) {
            for (var examen : modificarExamenes) {
                if (!examen.esValido()) {
                    throw new IllegalArgumentException("ID de examen requerido para modificación");
                }
                if (!examen.tieneCambios()) {
                    throw new IllegalArgumentException("No hay cambios especificados para examen: " + examen.id());
                }
            }
        }
    }

    /**
     * Factory method para actualización simple (solo info básica)
     */
    public static ActualizarOrdenDTO simple(String diagnostico, String justificacion, String prioridad) {
        return new ActualizarOrdenDTO(
                diagnostico, justificacion, null, prioridad, null,
                null, null, null, null
        );
    }

    /**
     * Factory method para cambio de fecha
     */
    public static ActualizarOrdenDTO reprogramar(LocalDate nuevaFecha) {
        return new ActualizarOrdenDTO(
                null, null, nuevaFecha, null, null,
                null, null, null, null
        );
    }

    /**
     * Factory method para cambio de prioridad
     */
    public static ActualizarOrdenDTO cambiarPrioridad(String nuevaPrioridad) {
        return new ActualizarOrdenDTO(
                null, null, null, nuevaPrioridad, null,
                null, null, null, null
        );
    }

    /**
     * Factory method para operaciones con exámenes
     */
    public static ActualizarOrdenDTO operacionesExamenes(List<NuevoExamenDTO> agregar,
                                                         List<ModificarExamenDTO> modificar,
                                                         List<Long> eliminar) {
        return new ActualizarOrdenDTO(
                null, null, null, null, null,
                agregar, modificar, eliminar, null
        );
    }

    /**
     * Métodos de verificación
     */
    public boolean tieneOperacionesExamenes() {
        return (agregarExamenes != null && !agregarExamenes.isEmpty()) ||
                (modificarExamenes != null && !modificarExamenes.isEmpty()) ||
                (eliminarExamenes != null && !eliminarExamenes.isEmpty());
    }

    public boolean soloActualizaInfo() {
        return !tieneOperacionesExamenes() &&
                (diagnosticoPrincipal != null ||
                        justificacionClinica != null ||
                        fechaProgramada != null ||
                        prioridad != null ||
                        observaciones != null);
    }

    public boolean esVacio() {
        return diagnosticoPrincipal == null &&
                justificacionClinica == null &&
                fechaProgramada == null &&
                prioridad == null &&
                observaciones == null &&
                !tieneOperacionesExamenes();
    }

    /**
     * Contadores
     */
    public int getTotalExamenesAgregar() {
        return agregarExamenes != null ? agregarExamenes.size() : 0;
    }

    public int getTotalExamenesModificar() {
        return modificarExamenes != null ? modificarExamenes.size() : 0;
    }

    public int getTotalExamenesEliminar() {
        return eliminarExamenes != null ? eliminarExamenes.size() : 0;
    }

    public int getTotalOperacionesExamenes() {
        return getTotalExamenesAgregar() + getTotalExamenesModificar() + getTotalExamenesEliminar();
    }

    /**
     * Métodos de prioridad
     */
    public boolean cambiaAEmergencia() {
        return "E".equals(prioridad);
    }

    public boolean cambiaAUrgente() {
        return "U".equals(prioridad);
    }

    public boolean cambiaANormal() {
        return "N".equals(prioridad);
    }

    public String getPrioridadDescripcion() {
        return switch (prioridad != null ? prioridad : "") {
            case "E" -> "Emergencia";
            case "U" -> "Urgente";
            case "N" -> "Normal";
            default -> "Sin cambio";
        };
    }

    /**
     * Métodos de fecha
     */
    public boolean reprogramaParaHoy() {
        return fechaProgramada != null && fechaProgramada.equals(LocalDate.now());
    }

    public boolean reprogramaParaFuturo() {
        return fechaProgramada != null && fechaProgramada.isAfter(LocalDate.now());
    }

    /**
     * Validaciones
     */
    private static boolean esPrioridadValida(String prioridad) {
        return switch (prioridad) {
            case "E", "U", "N" -> true;
            default -> false;
        };
    }

    public boolean esValido() {
        return !esVacio() &&
                (prioridad == null || esPrioridadValida(prioridad)) &&
                (fechaProgramada == null || !fechaProgramada.isBefore(LocalDate.now()));
    }

    /**
     * Resumen para logging
     */
    public String getResumenCambios() {
        var cambios = new java.util.ArrayList<String>();

        if (diagnosticoPrincipal != null) cambios.add("diagnóstico");
        if (justificacionClinica != null) cambios.add("justificación");
        if (fechaProgramada != null) cambios.add("fecha programada");
        if (prioridad != null) cambios.add("prioridad");
        if (observaciones != null) cambios.add("observaciones");

        if (tieneOperacionesExamenes()) {
            cambios.add(String.format("exámenes (+ %d, ~ %d, - %d)",
                    getTotalExamenesAgregar(), getTotalExamenesModificar(), getTotalExamenesEliminar()));
        }

        return cambios.isEmpty() ? "Sin cambios" : String.join(", ", cambios);
    }

    /**
     * Para logging estructurado
     */
    public String toLogString() {
        return String.format(
                "ActualizarOrden{cambios='%s', operacionesExamenes=%d}",
                getResumenCambios(), getTotalOperacionesExamenes()
        );
    }

    /**
     * Copia con médico (para auditoría)
     */
    public ActualizarOrdenDTO conMedico(Long medicoId) {
        return new ActualizarOrdenDTO(
                diagnosticoPrincipal, justificacionClinica, fechaProgramada, prioridad, observaciones,
                agregarExamenes, modificarExamenes, eliminarExamenes, medicoId
        );
    }
}