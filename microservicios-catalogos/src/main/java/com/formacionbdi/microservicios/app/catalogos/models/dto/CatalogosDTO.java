package com.formacionbdi.microservicios.app.catalogos.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 🔍 DTO que mapea el JSON completo de búsqueda unificada
 * Puerto: 8009 - Microservicio Catálogos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatalogosDTO {

    @JsonProperty("tabla_origen")
    private String tablaOrigen;

    @JsonProperty("codigo_busqueda")
    private String codigoBusqueda;

    @JsonProperty("descripcion_principal")
    private String descripcionPrincipal;

    @JsonProperty("categoria_principal")
    private String categoriaPrincipal;

    private String estado;

    @JsonProperty("tipo_busqueda")
    private String tipoBusqueda; // EXAMEN, MEDICAMENTO, CATALOGO

    // DATOS ESPECÍFICOS SEGÚN EL TIPO
    private ExamenDTO examen;
    private MedicamentoDTO medicamento;
    private CatalogoDTO catalogo;

    // ===== DTOs ANIDADOS =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExamenDTO {
        private Long id;
        private String codigo;
        private String nombre;
        private String descripcion;
        private String categoria;
        private String subcategoria;

        @JsonProperty("precio_base")
        private Double precioBase;

        @JsonProperty("requiere_ayuno")
        private String requiereAyuno;

        @JsonProperty("tiempo_procesamiento")
        private String tiempoProcesamiento;

        @JsonProperty("tipo_muestra")
        private String tipoMuestra;

        @JsonProperty("preparacion_especial")
        private String preparacionEspecial;

        private String activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MedicamentoDTO {
        private Long id;
        private String codigo;

        @JsonProperty("nombre_generico")
        private String nombreGenerico;

        @JsonProperty("nombres_comerciales")
        private List<String> nombresComerciales;

        private String concentracion;

        @JsonProperty("forma_farmaceutica")
        private String formaFarmaceutica;

        private String categoria;

        @JsonProperty("via_administracion")
        private String viaAdministracion;

        @JsonProperty("requiere_receta")
        private Boolean requiereReceta;

        private Boolean controlado;
        private String disponible;
        private String activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CatalogoDTO {
        private Long id;

        @JsonProperty("tabla_codigo")
        private String tablaCodigo;

        @JsonProperty("tabla_nombre")
        private String tablaNombre;

        private String codigo;
        private String nombre;

        @JsonProperty("descripcion_corta")
        private String descripcionCorta;

        @JsonProperty("descripcion_larga")
        private String descripcionLarga;

        @JsonProperty("valor_adicional")
        private String valorAdicional;

        @JsonProperty("padre_id")
        private Long padreId;

        private String activo;
    }

    // ===== MÉTODOS HELPER =====

    /**
     * Crear DTO para examen
     */
    public static CatalogosDTO forExamen(String codigo, String descripcion, ExamenDTO examen) {
        return CatalogosDTO.builder()
                .tablaOrigen("examenes")
                .codigoBusqueda(codigo)
                .descripcionPrincipal(descripcion)
                .categoriaPrincipal(examen.getCategoria())
                .estado("ACTIVO")
                .tipoBusqueda("EXAMEN")
                .examen(examen)
                .build();
    }

    /**
     * Crear DTO para medicamento
     */
    public static CatalogosDTO forMedicamento(String codigo, String descripcion, MedicamentoDTO medicamento) {
        return CatalogosDTO.builder()
                .tablaOrigen("medicamentos")
                .codigoBusqueda(codigo)
                .descripcionPrincipal(descripcion)
                .categoriaPrincipal(medicamento.getCategoria())
                .estado("DISPONIBLE")
                .tipoBusqueda("MEDICAMENTO")
                .medicamento(medicamento)
                .build();
    }

    /**
     * Crear DTO para catálogo
     */
    public static CatalogosDTO forCatalogo(String tablaCodigo, String codigo, String descripcion, CatalogoDTO catalogo) {
        return CatalogosDTO.builder()
                .tablaOrigen("catalogo_" + tablaCodigo)
                .codigoBusqueda(codigo)
                .descripcionPrincipal(descripcion)
                .categoriaPrincipal(catalogo.getTablaNombre())
                .estado("ACTIVO")
                .tipoBusqueda("CATALOGO")
                .catalogo(catalogo)
                .build();
    }
}