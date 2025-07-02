package com.formacionbdi.microservicios.app.orden.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.Type;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * 🏥 ENTIDAD ORDEN CABECERA - JAVA 17 ENHANCED
 *
 * Features Java 17:
 * - Switch expressions para validaciones
 * - Text blocks para queries
 * - Pattern matching simulado
 * - Records para datos inmutables (inner classes)
 *
 * Patrón CAB+DET para órdenes médicas
 * Puerto: 8006 | Tabla: ordenes_cab
 *
 * @author Microservicio Órdenes
 * @version 2.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Entity
@Table(name = "ordenes_cab", indexes = {
        @Index(name = "idx_orden_numero", columnList = "numero_orden", unique = true),
        @Index(name = "idx_orden_paciente", columnList = "paciente_id"),
        @Index(name = "idx_orden_medico", columnList = "medico_id"),
        @Index(name = "idx_orden_estado", columnList = "estado"),
        @Index(name = "idx_orden_fecha", columnList = "fecha_orden"),
        @Index(name = "idx_orden_origen", columnList = "tipo_origen, origen_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_orden", unique = true, nullable = false, length = 25)
    @NotNull
    @Size(max = 25)
    private String numeroOrden;

    // =====================================================
    // 👤 INFORMACIÓN DEL PACIENTE Y MÉDICO
    // =====================================================

    @Column(name = "paciente_id", nullable = false)
    @NotNull
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    @NotNull
    private Long medicoId;

    // =====================================================
    // 🏥 ORIGEN DINÁMICO (patrón exitoso de recetas)
    // =====================================================

    @Column(name = "tipo_origen", nullable = false, length = 3)
    @NotNull
    @Size(max = 3)
    private String tipoOrigen; // HOS, AMB, EMR

    @Column(name = "origen_id", nullable = false)
    @NotNull
    private Long origenId;

    // =====================================================
    // 📋 CLASIFICACIÓN Y FECHAS
    // =====================================================

    @Column(name = "tipo_orden", nullable = false, length = 20)
    @NotNull
    @Size(max = 20)
    private String tipoOrden; // LAB, IMG, PROC, FUNC

    @Column(name = "fecha_orden", nullable = false)
    @Builder.Default
    private LocalDateTime fechaOrden = LocalDateTime.now();

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    // =====================================================
    // 🩺 INFORMACIÓN MÉDICA
    // =====================================================

    @Column(name = "diagnostico_principal", length = 10)
    @Size(max = 10)
    private String diagnosticoPrincipal; // CIE-10

    @Column(name = "justificacion_clinica", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String justificacionClinica;

    // =====================================================
    // ⚡ PRIORIDAD Y ESTADO
    // =====================================================

    @Column(name = "prioridad", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String prioridad = "N"; // E=Emergencia, U=Urgente, N=Normal

    @Column(name = "estado", length = 2)
    @Size(max = 2)
    @Builder.Default
    private String estado = "01"; // 01=Solicitada, 02=Programada, 03=En Proceso, 04=Completada, 05=Cancelada

    // =====================================================
    // 🖊️ FIRMA DIGITAL (patrón recetas)
    // =====================================================

    @Column(name = "firmada", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String firmada = "N";

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Type(com.vladmihalcea.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "firma_digital", columnDefinition = "jsonb")
    private JsonNode firmaDigital;

    // =====================================================
    // 🔗 RELACIÓN CON DETALLE (CAB+DET pattern)
    // =====================================================

    @OneToMany(mappedBy = "ordenCab", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<OrdenDet> examenes = new ArrayList<>();

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
        if (fechaOrden == null) {
            fechaOrden = now;
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
     * 🔥 SWITCH EXPRESSION JAVA 17: Verifica si puede ser modificada
     */
    public boolean puedeSerModificada() {
        return switch (this.estado) {
            case "01", "02" -> true;  // Solicitada o Programada
            case "03", "04", "05" -> false;  // En Proceso, Completada o Cancelada
            default -> false;
        };
    }

    /**
     * 🔥 PATTERN MATCHING SIMULADO: Verificaciones de estado
     */
    public boolean esActiva() {
        return "S".equals(activo);
    }

    public boolean estaFirmada() {
        return "S".equals(firmada);
    }

    public boolean esEmergencia() {
        return "E".equals(prioridad);
    }

    public boolean esUrgente() {
        return "U".equals(prioridad);
    }

    public boolean esNormal() {
        return "N".equals(prioridad);
    }

    /**
     * 🔥 SWITCH EXPRESSION: Descripción del estado
     */
    public String getEstadoDescripcion() {
        return switch (estado) {
            case "01" -> "Solicitada";
            case "02" -> "Programada";
            case "03" -> "En Proceso";
            case "04" -> "Completada";
            case "05" -> "Cancelada";
            default -> "Desconocido";
        };
    }

    /**
     * 🔥 SWITCH EXPRESSION: Descripción de prioridad
     */
    public String getPrioridadDescripcion() {
        return switch (prioridad) {
            case "E" -> "Emergencia";
            case "U" -> "Urgente";
            case "N" -> "Normal";
            default -> "Normal";
        };
    }

    /**
     * 🔥 SWITCH EXPRESSION: Descripción del origen
     */
    public String getTipoOrigenDescripcion() {
        return switch (tipoOrigen) {
            case "HOS" -> "Hospitalización";
            case "AMB" -> "Ambulatorio";
            case "EMR" -> "Emergencia";
            case "CON" -> "Consultorio";
            default -> "Desconocido";
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene prioridad numérica para ordenamiento
     */
    public int getPrioridadNumerica() {
        return switch (prioridad) {
            case "E" -> 3; // Emergencia = máxima prioridad
            case "U" -> 2; // Urgente
            case "N" -> 1; // Normal
            default -> 0;
        };
    }

    /**
     * 🔥 PATTERN MATCHING: Valida transición de estado
     */
    public boolean puedeTransicionarA(String nuevoEstado) {
        return switch (this.estado) {
            case "01" -> switch (nuevoEstado) { // Solicitada
                case "02", "03", "05" -> true; // → Programada, En Proceso, Cancelada
                default -> false;
            };
            case "02" -> switch (nuevoEstado) { // Programada
                case "03", "04", "05" -> true; // → En Proceso, Completada, Cancelada
                default -> false;
            };
            case "03" -> switch (nuevoEstado) { // En Proceso
                case "04", "05" -> true; // → Completada, Cancelada
                default -> false;
            };
            case "04", "05" -> false; // Estados finales
            default -> false;
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene resumen para logging
     */
    public String getResumenParaLog() {
        return """
               OrdenCab {
                 id: %d,
                 numero: %s,
                 paciente: %d,
                 medico: %d,
                 estado: %s,
                 examenes: %d
               }
               """.formatted(
                id,
                numeroOrden,
                pacienteId,
                medicoId,
                getEstadoDescripcion(),
                examenes != null ? examenes.size() : 0
        );
    }

    /**
     * 🔥 RECORD JAVA 17: Para datos inmutables de resumen
     */
    public record ResumenOrden(
            Long id,
            String numeroOrden,
            String estado,
            String estadoDescripcion,
            String prioridad,
            String prioridadDescripcion,
            int totalExamenes,
            LocalDateTime fechaOrden
    ) {
        /**
         * Factory method funcional
         */
        public static ResumenOrden from(OrdenCab orden) {
            return new ResumenOrden(
                    orden.getId(),
                    orden.getNumeroOrden(),
                    orden.getEstado(),
                    orden.getEstadoDescripcion(),
                    orden.getPrioridad(),
                    orden.getPrioridadDescripcion(),
                    orden.getExamenes() != null ? orden.getExamenes().size() : 0,
                    orden.getFechaOrden()
            );
        }
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene resumen inmutable
     */
    public ResumenOrden getResumen() {
        return ResumenOrden.from(this);
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Verifica si tiene exámenes pendientes
     */
    public boolean tieneExamenesPendientes() {
        return Optional.ofNullable(examenes)
                .map(list -> list.stream()
                        .anyMatch(examen -> "01".equals(examen.getEstadoDetalle())))
                .orElse(false);
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Cuenta exámenes por estado
     */
    public long contarExamenesPorEstado(String estado) {
        return Optional.ofNullable(examenes)
                .map(list -> list.stream()
                        .filter(examen -> estado.equals(examen.getEstadoDetalle()))
                        .count())
                .orElse(0L);
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Valida reglas de negocio
     */
    public boolean cumpleReglasDeNegocio() {
        // Usar pattern matching simulado
        return switch (tipoOrden) {
            case "LAB" -> examenes.size() <= 10; // Máximo 10 exámenes de laboratorio
            case "IMG" -> examenes.size() <= 5;  // Máximo 5 estudios de imagen
            case "PROC" -> examenes.size() <= 3; // Máximo 3 procedimientos
            default -> true;
        };
    }
}