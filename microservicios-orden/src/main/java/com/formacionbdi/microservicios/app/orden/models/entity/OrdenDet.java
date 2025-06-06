package com.formacionbdi.microservicios.app.orden.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import com.fasterxml.jackson.databind.JsonNode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 📋 Entidad Órdenes Detalle - ESTRUCTURA LIMPIA
 * Solo FK a examenes, sin duplicar datos
 * Puerto: 8006, Tabla: ordenes_det
 */
@Entity
@Table(name = "ordenes_det")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class OrdenDet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @JsonBackReference
    @NotNull
    private OrdenCab ordenCab;

    // ✅ SOLO FK A TABLA EXAMENES - JOIN en Repository
    @Column(name = "examen_id", nullable = false)
    @NotNull
    private Long examenId;

    // ❌ ELIMINAR ESTOS CAMPOS (están en tabla examenes):
    // - codigo_examen
    // - nombre_examen
    // - categoria
    // - preparacion_especial
    // - indicaciones

    // ✅ MANTENER CAMPOS ESPECÍFICOS DEL DETALLE:

    // RESULTADOS (JSONB para flexibilidad)
    @Type(type = "jsonb")
    @Column(name = "resultado", columnDefinition = "jsonb")
    private JsonNode resultado;

    // ORDEN Y ESTADO DEL DETALLE
    @Column(name = "orden_item")
    @Builder.Default
    private Integer ordenItem = 1;

    @Column(name = "estado_detalle", length = 2)
    @Size(max = 2)
    @Builder.Default
    private String estadoDetalle = "01"; // 01=Pendiente, 02=En proceso, 03=Completado, 04=Cancelado

    // ✅ AUDITORÍA
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

    @Column(name = "cantidad")
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "des_indicacion", length = 50)
    @Size(max = 50)
    private String desIndicacion;

    @Column(name = "des_consideraciones", length = 50)
    @Size(max = 50)
    private String desConsideraciones;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (creadoEn == null) {
            creadoEn = now;
        }
        if (actualizadoEn == null) {
            actualizadoEn = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // ✅ MÉTODOS DE UTILIDAD
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

    public String getEstadoDetalleDescripcion() {
        switch (estadoDetalle) {
            case "01": return "Pendiente";
            case "02": return "En Proceso";
            case "03": return "Completado";
            case "04": return "Cancelado";
            default: return "Desconocido";
        }
    }

    public boolean tieneResultados() {
        return resultado != null && !resultado.isNull();
    }
}