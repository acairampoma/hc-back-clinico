package com.formacionbdi.microservicios.app.orden.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import com.fasterxml.jackson.databind.JsonNode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Entidad Órdenes Cabecera - Patrón CAB+DET
 * Puerto: 8006
 * Tabla: ordenes_cab
 */
@Entity
@Table(name = "ordenes_cab")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)  // 🔥 IGUAL QUE RECETAS
public class OrdenCab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_orden", unique = true, nullable = false, length = 20)
    @NotNull
    @Size(max = 20)
    private String numeroOrden;

    @Column(name = "paciente_id", nullable = false)
    @NotNull
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    @NotNull
    private Long medicoId;

    // ORIGEN DINÁMICO (patrón recetas exitoso)
    @Column(name = "tipo_origen", nullable = false, length = 3)
    @NotNull
    @Size(max = 3)
    private String tipoOrigen; // HOS, AMB, EMR

    @Column(name = "origen_id", nullable = false)
    @NotNull
    private Long origenId;

    // CLASIFICACIÓN
    @Column(name = "tipo_orden", nullable = false, length = 20)
    @NotNull
    @Size(max = 20)
    private String tipoOrden; // LAB, IMG, PROC, FUNC

    @Column(name = "fecha_orden")
    private LocalDateTime fechaOrden;

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    // DIAGNÓSTICO
    @Column(name = "diagnostico_principal", length = 10)
    @Size(max = 10)
    private String diagnosticoPrincipal; // CIE-10

    @Column(name = "justificacion_clinica", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String justificacionClinica;

    // PRIORIDAD Y ESTADO
    @Column(name = "prioridad", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String prioridad = "N"; // E=Emergencia, U=Urgente, N=Normal

    @Column(name = "estado", length = 2)
    @Size(max = 2)
    @Builder.Default
    private String estado = "01"; // 01=Solicitada, 02=Programada, 03=En Proceso, 04=Completada, 05=Cancelada

    // FIRMA (patrón recetas)
    @Column(name = "firmada", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String firmada = "N";

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Type(type = "jsonb")
    @Column(name = "firma_digital", columnDefinition = "jsonb")
    private JsonNode firmaDigital;

    // RELACIÓN CON DETALLE
    @OneToMany(mappedBy = "ordenCab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<OrdenDet> examenes = new ArrayList<>();

    // AUDITORÍA (4 campos estándar)
    @Column(name = "activo", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String activo = "S";

    @Column(name = "creado_por", nullable = false)
    @NotNull
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

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
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // MÉTODOS DE UTILIDAD
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

    public boolean puedeSerModificada() {
        return esActiva() && ("01".equals(estado) || "02".equals(estado));
    }

    public String getEstadoDescripcion() {
        switch (estado) {
            case "01": return "Solicitada";
            case "02": return "Programada";
            case "03": return "En Proceso";
            case "04": return "Completada";
            case "05": return "Cancelada";
            default: return "Desconocido";
        }
    }

    public String getPrioridadDescripcion() {
        switch (prioridad) {
            case "E": return "Emergencia";
            case "U": return "Urgente";
            case "N": return "Normal";
            default: return "Normal";
        }
    }

    public String getTipoOrigenDescripcion() {
        switch (tipoOrigen) {
            case "HOS": return "Hospitalización";
            case "AMB": return "Ambulatorio";
            case "EMR": return "Emergencia";
            default: return "Desconocido";
        }
    }
}