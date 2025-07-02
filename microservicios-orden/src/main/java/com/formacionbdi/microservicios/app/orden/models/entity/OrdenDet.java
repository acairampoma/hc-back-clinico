package com.formacionbdi.microservicios.app.orden.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Optional;

import org.hibernate.annotations.Type;

/**
 * 🔬 ENTIDAD ORDEN DETALLE - JAVA 17 ENHANCED
 *
 * Features Java 17:
 * - Switch expressions para estados
 * - Pattern matching para validaciones
 * - Records para datos inmutables
 * - Text blocks para queries
 *
 * ESTRUCTURA LIMPIA: Solo FK a examenes, sin duplicar datos
 * El JOIN en Repository trae la info completa del examen
 *
 * Puerto: 8006 | Tabla: ordenes_det
 *
 * @author Microservicio Órdenes
 * @version 2.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Entity
@Table(name = "ordenes_det", indexes = {
        @Index(name = "idx_det_orden", columnList = "orden_id"),
        @Index(name = "idx_det_examen", columnList = "examen_id"),
        @Index(name = "idx_det_estado", columnList = "estado_detalle"),
        @Index(name = "idx_det_orden_item", columnList = "orden_id, orden_item")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // 🔗 RELACIÓN CON CABECERA (CAB+DET pattern)
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @JsonBackReference
    @NotNull
    private OrdenCab ordenCab;

    // =====================================================
    // 🔬 REFERENCIA A EXAMEN (SOLO FK - JOIN en Repository)
    // =====================================================

    @Column(name = "examen_id", nullable = false)
    @NotNull
    private Long examenId;

    // ❌ ELIMINADOS: campos duplicados de tabla examenes
    // - codigo_examen (está en tabla examenes)
    // - nombre_examen (está en tabla examenes)
    // - categoria (está en tabla examenes)
    // - preparacion_especial (está en tabla examenes)

    // =====================================================
    // 📊 CAMPOS ESPECÍFICOS DEL DETALLE
    // =====================================================

    @Column(name = "cantidad")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "des_indicacion", length = 200)
    @Size(max = 200, message = "Indicación no puede exceder 200 caracteres")
    private String desIndicacion;

    @Column(name = "des_consideraciones", length = 200)
    @Size(max = 200, message = "Consideraciones no puede exceder 200 caracteres")
    private String desConsideraciones;

    // =====================================================
    // 📋 RESULTADOS Y ORDEN
    // =====================================================

    @Type(com.vladmihalcea.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "resultado", columnDefinition = "jsonb")
    private JsonNode resultado;

    @Column(name = "orden_item")
    @Builder.Default
    private Integer ordenItem = 1;

    @Column(name = "estado_detalle", length = 2)
    @Size(max = 2)
    @Builder.Default
    private String estadoDetalle = "01"; // 01=Pendiente, 02=En proceso, 03=Completado, 04=Cancelado

    // =====================================================
    // 📊 AUDITORÍA (4 campos estándar)
    // =====================================================

    @Column(name = "activo", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String activo = "S";

    @Column(name = "creado_por", nullable = false)
    @NotNull
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // =====================================================
    // 🔄 LIFECYCLE HOOKS
    // =====================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (creadoEn == null) {
            creadoEn = now;
        }
        if (actualizadoEn == null) {
            actualizadoEn = now;
        }
        if (actualizadoPor == null) {
            actualizadoPor = creadoPor;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // =====================================================
    // 🔥 MÉTODOS DE NEGOCIO CON JAVA 17
    // =====================================================

    /**
     * 🔥 PATTERN MATCHING: Verificaciones de estado
     */
    public boolean esActivo() {
        return "S".equals(activo);
    }

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

    /**
     * 🔥 SWITCH EXPRESSION: Descripción del estado
     */
    public String getEstadoDetalleDescripcion() {
        return switch (estadoDetalle) {
            case "01" -> "Pendiente";
            case "02" -> "En Proceso";
            case "03" -> "Completado";
            case "04" -> "Cancelado";
            default -> "Desconocido";
        };
    }

    /**
     * 🔥 PATTERN MATCHING: Verificaciones de resultados
     */
    public boolean tieneResultados() {
        return resultado != null && !resultado.isNull() && !resultado.isEmpty();
    }

    public boolean tieneIndicaciones() {
        return desIndicacion != null && !desIndicacion.trim().isEmpty();
    }

    public boolean tieneConsideraciones() {
        return desConsideraciones != null && !desConsideraciones.trim().isEmpty();
    }

    /**
     * 🔥 SWITCH EXPRESSION: Valida transición de estado
     */
    public boolean puedeTransicionarA(String nuevoEstado) {
        return switch (this.estadoDetalle) {
            case "01" -> switch (nuevoEstado) { // Pendiente
                case "02", "04" -> true; // → En Proceso, Cancelado
                default -> false;
            };
            case "02" -> switch (nuevoEstado) { // En Proceso
                case "03", "04" -> true; // → Completado, Cancelado
                default -> false;
            };
            case "03", "04" -> false; // Estados finales
            default -> false;
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Verifica si puede ser modificado
     */
    public boolean puedeSerModificado() {
        return switch (estadoDetalle) {
            case "01" -> true;  // Pendiente - puede modificarse
            case "02" -> true;  // En Proceso - puede modificarse con restricciones
            case "03", "04" -> false; // Completado/Cancelado - no puede modificarse
            default -> false;
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Valida cantidad según tipo de examen
     */
    public boolean cantidadEsValida(String tipoExamen) {
        if (cantidad == null || cantidad < 1) {
            return false;
        }

        return switch (tipoExamen) {
            case "LAB" -> cantidad <= 5;   // Laboratorio: máximo 5 repeticiones
            case "IMG" -> cantidad <= 2;   // Imagen: máximo 2 repeticiones
            case "PROC" -> cantidad == 1;  // Procedimiento: solo 1
            default -> cantidad <= 3;      // Otros: máximo 3
        };
    }

    /**
     * 🔥 RECORD JAVA 17: Para datos inmutables de resumen
     */
    public record ResumenExamen(
            Long id,
            Long examenId,
            Integer cantidad,
            String estado,
            String estadoDescripcion,
            boolean tieneResultados,
            LocalDateTime creadoEn
    ) {
        /**
         * Factory method funcional
         */
        public static ResumenExamen from(OrdenDet detalle) {
            return new ResumenExamen(
                    detalle.getId(),
                    detalle.getExamenId(),
                    detalle.getCantidad(),
                    detalle.getEstadoDetalle(),
                    detalle.getEstadoDetalleDescripcion(),
                    detalle.tieneResultados(),
                    detalle.getCreadoEn()
            );
        }
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene resumen inmutable
     */
    public ResumenExamen getResumen() {
        return ResumenExamen.from(this);
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para logging estructurado
     */
    public String getResumenParaLog() {
        return """
               OrdenDet {
                 id: %d,
                 examenId: %d,
                 cantidad: %d,
                 estado: %s,
                 orden: %s
               }
               """.formatted(
                id,
                examenId,
                cantidad,
                getEstadoDetalleDescripcion(),
                ordenCab != null ? ordenCab.getNumeroOrden() : "SIN_ORDEN"
        );
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Calcula prioridad basada en orden y estado
     */
    public int calcularPrioridad() {
        int prioridadEstado = switch (estadoDetalle) {
            case "01" -> 3; // Pendiente = alta prioridad
            case "02" -> 2; // En proceso = media prioridad
            case "03" -> 1; // Completado = baja prioridad
            case "04" -> 0; // Cancelado = sin prioridad
            default -> 0;
        };

        // Factor de orden (menor número = mayor prioridad)
        int prioridadOrden = ordenItem != null ? (11 - Math.min(ordenItem, 10)) : 1;

        return prioridadEstado * 10 + prioridadOrden;
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Valida reglas de negocio específicas
     */
    public boolean cumpleReglasDeNegocio() {
        // Validaciones con pattern matching simulado
        return switch (estadoDetalle) {
            case "01" -> cantidad != null && cantidad > 0; // Pendiente: debe tener cantidad válida
            case "02" -> cantidad != null && cantidad > 0; // En proceso: debe tener cantidad válida
            case "03" -> tieneResultados(); // Completado: debe tener resultados
            case "04" -> true; // Cancelado: sin restricciones adicionales
            default -> false;
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene descripción completa para UI
     */
    public String getDescripcionCompleta() {
        var builder = new StringBuilder();

        builder.append("Examen ID: ").append(examenId);

        if (cantidad != null && cantidad > 1) {
            builder.append(" (x").append(cantidad).append(")");
        }

        if (tieneIndicaciones()) {
            builder.append(" - ").append(desIndicacion);
        }

        if (tieneConsideraciones()) {
            builder.append(" | ").append(desConsideraciones);
        }

        return builder.toString();
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Verifica si requiere atención urgente
     */
    public boolean requiereAtencionUrgente() {
        // Lógica basada en el estado de la orden padre y tiempo transcurrido
        if (ordenCab == null) return false;

        boolean ordenUrgente = ordenCab.esEmergencia() || ordenCab.esUrgente();
        boolean tiempoExcedido = creadoEn != null &&
                creadoEn.isBefore(LocalDateTime.now().minusHours(24));

        return ordenUrgente || (estaPendiente() && tiempoExcedido);
    }
}