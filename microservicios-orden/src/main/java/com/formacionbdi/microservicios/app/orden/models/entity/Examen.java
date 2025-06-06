package com.formacionbdi.microservicios.app.orden.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 🔬 ENTIDAD EXAMEN - Para tabla examenes
 * ✅ Para que funcione el JOIN en JPQL
 */
@Entity
@Table(name = "examenes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", length = 20)
    private String codigo;

    @Column(name = "nombre", length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "subcategoria", length = 50)
    private String subcategoria;

    @Column(name = "requiere_ayuno", length = 1)
    private String requiereAyuno; // S/N

    @Column(name = "preparacion_especial", columnDefinition = "TEXT")
    private String preparacionEspecial;

    @Column(name = "tipo_muestra", length = 50)
    private String tipoMuestra;

    @Column(name = "tiempo_procesamiento", length = 50)
    private String tiempoProcesamiento;

    @Column(name = "valores_referencia", columnDefinition = "TEXT")
    private String valoresReferencia;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "activo", length = 1)
    private String activo;

    // Auditoría
    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // =====================================================
    // 🔧 MÉTODOS DE NEGOCIO
    // =====================================================

    /**
     * Verificar si está activo
     */
    public boolean esActivo() {
        return "S".equals(this.activo);
    }

    /**
     * Verificar si requiere ayuno
     */
    public boolean requiereAyuno() {
        return "S".equals(this.requiereAyuno);
    }

    /**
     * Obtener categoría completa
     */
    public String getCategoriaCompleta() {
        if (this.subcategoria != null && !this.subcategoria.trim().isEmpty()) {
            return this.categoria + " - " + this.subcategoria;
        }
        return this.categoria;
    }

    /**
     * Verificar si es examen de laboratorio
     */
    public boolean esExamenLaboratorio() {
        return "LAB".equals(this.categoria);
    }

    /**
     * Verificar si es examen de imagenología
     */
    public boolean esExamenImagenologia() {
        return "IMG".equals(this.categoria);
    }

    @PrePersist
    protected void onCreate() {
        if (this.activo == null) {
            this.activo = "S"; // Activo por defecto
        }
        if (this.requiereAyuno == null) {
            this.requiereAyuno = "N"; // No requiere ayuno por defecto
        }
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}