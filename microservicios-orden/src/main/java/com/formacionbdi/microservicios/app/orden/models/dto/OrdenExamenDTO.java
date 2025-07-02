package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO Record para examen con información completa - Java 17
 * Combina datos de orden_det + examenes (JOIN)
 * Para respuestas de órdenes completas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdenExamenDTO(

        // Datos del detalle (orden_det)
        @JsonProperty("id")
        Long id,

        @JsonProperty("examenId")
        Long examenId,

        @JsonProperty("cantidad")
        Integer cantidad,

        @JsonProperty("desIndicacion")
        String desIndicacion,

        @JsonProperty("desConsideraciones")
        String desConsideraciones,

        @JsonProperty("ordenItem")
        Integer ordenItem,

        @JsonProperty("estadoDetalle")
        String estadoDetalle,

        @JsonProperty("creadoEn")
        LocalDateTime creadoEn,

        // Datos del examen (tabla examenes)
        @JsonProperty("codigoExamen")
        String codigoExamen,

        @JsonProperty("nombreExamen")
        String nombreExamen,

        @JsonProperty("categoria")
        String categoria,

        @JsonProperty("subcategoria")
        String subcategoria,

        @JsonProperty("requiereAyuno")
        String requiereAyuno,

        @JsonProperty("preparacionEspecial")
        String preparacionEspecial,

        @JsonProperty("tipoMuestra")
        String tipoMuestra,

        @JsonProperty("tiempoProcesamiento")
        String tiempoProcesamiento
) {

    /**
     * Compact constructor con valores por defecto
     */
    public OrdenExamenDTO {
        // Cantidad por defecto
        if (cantidad == null) {
            cantidad = 1;
        }

        // Estado por defecto
        if (estadoDetalle == null) {
            estadoDetalle = "01"; // Pendiente
        }
    }

    /**
     * Factory method básico (solo datos esenciales)
     */
    public static OrdenExamenDTO crear(Long id, Long examenId, String nombreExamen,
                                       Integer cantidad, String estadoDetalle) {
        return new OrdenExamenDTO(
                id, examenId, cantidad, null, null, null, estadoDetalle, null,
                null, nombreExamen, null, null, null, null, null, null
        );
    }

    /**
     * Factory method completo (para JOINs)
     */
    public static OrdenExamenDTO completo(Long id, Long examenId, Integer cantidad,
                                          String desIndicacion, String desConsideraciones,
                                          String estadoDetalle, String codigoExamen, String nombreExamen,
                                          String categoria, String requiereAyuno) {
        return new OrdenExamenDTO(
                id, examenId, cantidad, desIndicacion, desConsideraciones, null, estadoDetalle, null,
                codigoExamen, nombreExamen, categoria, null, requiereAyuno, null, null, null
        );
    }

    /**
     * Métodos de estado del detalle
     */
    public boolean estaPendiente() {
        return "01".equals(estadoDetalle);
    }

    public boolean estaEnProceso() {
        return "02".equals(estadoDetalle);
    }

    public boolean estaCompletado() {
        return "03".equals(estadoDetalle);
    }

    public boolean estaCancelado() {
        return "04".equals(estadoDetalle);
    }

    public String getEstadoDetalleDescripcion() {
        return switch (estadoDetalle != null ? estadoDetalle : "01") {
            case "01" -> "Pendiente";
            case "02" -> "En Proceso";
            case "03" -> "Completado";
            case "04" -> "Cancelado";
            default -> "Desconocido";
        };
    }

    /**
     * Métodos de categoría del examen
     */
    public boolean esExamenLaboratorio() {
        return "LAB".equals(categoria);
    }

    public boolean esExamenImagenologia() {
        return "IMG".equals(categoria);
    }

    public boolean esExamenProcedimiento() {
        return "PROC".equals(categoria);
    }

    public boolean esExamenFuncional() {
        return "FUNC".equals(categoria);
    }

    public String getCategoriaDescripcion() {
        return switch (categoria != null ? categoria : "") {
            case "LAB" -> "Laboratorio";
            case "IMG" -> "Imagenología";
            case "PROC" -> "Procedimiento";
            case "FUNC" -> "Prueba Funcional";
            default -> categoria != null ? categoria : "Sin categoría";
        };
    }

    public String getCategoriaCompleta() {
        var descripcion = getCategoriaDescripcion();
        if (subcategoria != null && !subcategoria.trim().isEmpty()) {
            return descripcion + " - " + subcategoria;
        }
        return descripcion;
    }

    /**
     * Métodos de preparación
     */
    public boolean esConAyuno() {
        return "S".equals(requiereAyuno);
    }

    public boolean tienePreparacionEspecial() {
        return preparacionEspecial != null && !preparacionEspecial.trim().isEmpty();
    }

    public boolean tieneIndicaciones() {
        return desIndicacion != null && !desIndicacion.trim().isEmpty();
    }

    public boolean tieneConsideraciones() {
        return desConsideraciones != null && !desConsideraciones.trim().isEmpty();
    }

    /**
     * Métodos de tiempo y urgencia
     */
    public int getTiempoEstimadoHoras() {
        if (tiempoProcesamiento == null) return 24;

        String tiempo = tiempoProcesamiento.toLowerCase();
        if (tiempo.contains("inmediato")) return 0;
        if (tiempo.contains("30 min")) return 1;
        if (tiempo.contains("1 hora")) return 1;
        if (tiempo.contains("2-4 horas")) return 4;
        if (tiempo.contains("6 horas")) return 6;
        if (tiempo.contains("12 horas")) return 12;
        if (tiempo.contains("24 horas")) return 24;
        if (tiempo.contains("48 horas")) return 48;

        return 24;
    }

    public String getNivelUrgencia() {
        int horas = getTiempoEstimadoHoras();
        return switch (horas) {
            case 0 -> "INMEDIATO";
            case 1, 2 -> "URGENTE";
            case 3, 4, 5, 6 -> "PRIORITARIO";
            default -> horas <= 24 ? "NORMAL" : "PROGRAMADO";
        };
    }

    /**
     * Métodos de display y resumen
     */
    public String getNombreCompleto() {
        var nombre = nombreExamen != null ? nombreExamen : "Examen ID: " + examenId;
        var codigo = codigoExamen != null ? " (" + codigoExamen + ")" : "";
        return nombre + codigo;
    }

    public String getResumenCantidad() {
        return cantidad != null && cantidad > 1 ? " x" + cantidad : "";
    }

    public String getResumenCompleto() {
        return getNombreCompleto() + getResumenCantidad() + " - " + getEstadoDetalleDescripcion();
    }

    /**
     * Información para el paciente
     */
    public String getInstruccionesPaciente() {
        var instrucciones = new StringBuilder();

        if (esConAyuno()) {
            instrucciones.append("⚠️ Requiere ayuno. ");
        }

        if (tienePreparacionEspecial()) {
            instrucciones.append("📋 ").append(preparacionEspecial).append(" ");
        }

        if (tieneIndicaciones()) {
            instrucciones.append("💡 ").append(desIndicacion).append(" ");
        }

        if (tieneConsideraciones()) {
            instrucciones.append("⚡ ").append(desConsideraciones);
        }

        return instrucciones.toString().trim();
    }

    /**
     * Validaciones
     */
    public boolean esValido() {
        return examenId != null &&
                nombreExamen != null &&
                cantidad != null && cantidad > 0 &&
                estadoDetalle != null;
    }

    public boolean cantidadEsValida() {
        if (cantidad == null || cantidad < 1) return false;

        return switch (categoria != null ? categoria : "") {
            case "LAB" -> cantidad <= 5;
            case "IMG" -> cantidad <= 2;
            case "PROC" -> cantidad == 1;
            case "FUNC" -> cantidad <= 3;
            default -> cantidad <= 3;
        };
    }

    /**
     * Prioridad calculada (para ordenamiento)
     */
    public int calcularPrioridad() {
        int prioridadEstado = switch (estadoDetalle != null ? estadoDetalle : "01") {
            case "01" -> 3; // Pendiente = alta prioridad
            case "02" -> 2; // En proceso = media
            case "03" -> 1; // Completado = baja
            case "04" -> 0; // Cancelado = sin prioridad
            default -> 0;
        };

        int prioridadTiempo = switch (getNivelUrgencia()) {
            case "INMEDIATO" -> 10;
            case "URGENTE" -> 8;
            case "PRIORITARIO" -> 6;
            case "NORMAL" -> 4;
            case "PROGRAMADO" -> 2;
            default -> 1;
        };

        return prioridadEstado * 10 + prioridadTiempo;
    }

    /**
     * Para logging estructurado
     */
    public String toLogString() {
        return String.format(
                "OrdenExamen{id=%d, examenId=%d, nombre='%s', cantidad=%d, estado='%s'}",
                id, examenId, nombreExamen, cantidad, estadoDetalle
        );
    }
}