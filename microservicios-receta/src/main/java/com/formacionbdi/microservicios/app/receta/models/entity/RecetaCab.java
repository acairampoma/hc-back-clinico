package com.formacionbdi.microservicios.app.receta.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * 📋 Entity para Receta Cabecera
 */
@Entity
@Table(name = "receta_cab")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class RecetaCab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_receta", unique = true, nullable = false, length = 20)
    private String numeroReceta;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Column(name = "tipo_origen", nullable = false, length = 3)
    private String tipoOrigen; // ACT=Acto Médico, HOS=Hospitalización

    @Column(name = "origen_id", nullable = false)
    private Long origenId;

    @Column(name = "fecha_receta")
    private LocalDateTime fechaReceta;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "diagnostico_principal", length = 10)
    private String diagnosticoPrincipal; // CIE-10

    @Column(name = "indicaciones_generales", columnDefinition = "TEXT")
    private String indicacionesGenerales;

    @Column(name = "estado", length = 2)
    private String estado; // 01=Activa, 02=Despachada, 03=Vencida, 04=Anulada

    @Column(name = "firmada", length = 1)
    private String firmada; // S/N

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Type(type = "jsonb")
    @Column(name = "firma_digital", columnDefinition = "jsonb")
    private JsonNode firmaDigital;

    @Column(name = "activo", length = 1)
    private String activo;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // ===== RELACIÓN ONE-TO-MANY CON DETALLE =====
    @OneToMany(mappedBy = "recetaCab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecetaDet> medicamentos;

    // ===== MÉTODOS HELPER =====

    /**
     * Verifica si la receta está firmada
     */
    public boolean estaFirmada() {
        return "S".equals(firmada);
    }

    /**
     * Verifica si es una receta activa
     */
    public boolean esActiva() {
        return "01".equals(estado);
    }

    /**
     * Verifica si puede ser modificada (hasta 24h antes de vencimiento)
     */
    public boolean puedeSerModificada() {
        if (fechaVencimiento == null) return false;

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = fechaVencimiento.atStartOfDay().minusHours(24);

        return ahora.isBefore(limite) && esActiva();
    }

    /**
     * Verifica si es de hospitalización
     */
    public boolean esDeHospitalizacion() {
        return "HOS".equals(tipoOrigen);
    }

    /**
     * Verifica si es de acto médico
     */
    public boolean esDeActoMedico() {
        return "ACT".equals(tipoOrigen);
    }
}