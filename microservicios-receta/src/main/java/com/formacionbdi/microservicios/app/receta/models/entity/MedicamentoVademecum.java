package com.formacionbdi.microservicios.app.receta.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 💊 Entity para Vademécum de Medicamentos - Data Maestra
 */
@Entity
@Table(name = "medicamentos_vademecum")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicamentoVademecum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_medicamento", unique = true, nullable = false, length = 20)
    private String codigoMedicamento;

    @Column(name = "generic_name", nullable = false, length = 200)
    private String genericName;

    @Column(name = "brand_names", columnDefinition = "jsonb")
    private String brandNames; // JSON array

    @Column(name = "concentracion", length = 100)
    private String concentracion;

    @Column(name = "forma_farmaceutica", length = 50)
    private String formaFarmaceutica;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion;

    @Column(name = "requiere_receta")
    private Boolean requiereReceta;

    @Column(name = "controlado")
    private Boolean controlado;

    @Column(name = "disponible", length = 1)
    private String disponible;

    @Column(name = "activo", length = 1)
    private String activo;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}