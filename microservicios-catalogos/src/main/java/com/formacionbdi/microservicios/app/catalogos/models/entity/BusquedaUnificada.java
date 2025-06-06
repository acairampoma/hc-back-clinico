package com.formacionbdi.microservicios.app.catalogos.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Type;

/**
 * 🔍 Entidad que mapea directamente el JSON de la vista PostgreSQL
 * vista_busqueda_unificada - Búsquedas de examenes, medicamentos, catálogos
 * Puerto: 8009 - Microservicio Catálogos
 */
@Entity
@Table(name = "vista_busqueda_unificada")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaUnificada {

    @Id
    @Column(name = "tabla_origen")
    private String tablaOrigen;

    @Column(name = "codigo_busqueda")
    @JsonProperty("codigo_busqueda")
    private String codigoBusqueda;

    @Column(name = "descripcion_principal")
    @JsonProperty("descripcion_principal")
    private String descripcionPrincipal;

    @Column(name = "categoria_principal")
    @JsonProperty("categoria_principal")
    private String categoriaPrincipal;

    @Column(name = "estado")
    private String estado;

    @Column(name = "datos_json", columnDefinition = "json")
    @Type(type = "org.hibernate.type.TextType")
    @JsonProperty("datos_json")
    private String datosJson;

    // Constructor para mapear directamente el JSON
    public BusquedaUnificada(String tablaOrigen, String codigoBusqueda, String descripcionPrincipal, String datosJson) {
        this.tablaOrigen = tablaOrigen;
        this.codigoBusqueda = codigoBusqueda;
        this.descripcionPrincipal = descripcionPrincipal;
        this.datosJson = datosJson;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    /**
     * Verificar si es un examen
     */
    public boolean esExamen() {
        return "examenes".equals(this.tablaOrigen);
    }

    /**
     * Verificar si es un medicamento
     */
    public boolean esMedicamento() {
        return "medicamentos".equals(this.tablaOrigen);
    }

    /**
     * Verificar si es un catálogo administrativo
     */
    public boolean esCatalogo() {
        return this.tablaOrigen != null && this.tablaOrigen.startsWith("catalogo_");
    }

    /**
     * Verificar si está activo
     */
    public boolean estaActivo() {
        return "ACTIVO".equals(this.estado) || "DISPONIBLE".equals(this.estado);
    }

    /**
     * Obtener tipo de búsqueda
     */
    public String getTipoBusqueda() {
        if (esExamen()) return "EXAMEN";
        if (esMedicamento()) return "MEDICAMENTO";
        if (esCatalogo()) return "CATALOGO";
        return "DESCONOCIDO";
    }
}