package com.formacionbdi.microservicios.app.catalogos.repository;

import com.formacionbdi.microservicios.app.catalogos.models.entity.BusquedaUnificada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🔍 Repository para consultar la vista vista_busqueda_unificada
 * Puerto: 8009 - Microservicio Catálogos
 */
@Repository
public interface CatalogosRepository extends JpaRepository<BusquedaUnificada, String> {

    // ===== BÚSQUEDAS GENERALES =====

    /**
     * Búsqueda general por término
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE (LOWER(codigo_busqueda) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "   OR LOWER(descripcion_principal) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
            "  AND estado IN ('ACTIVO', 'DISPONIBLE') " +
            "ORDER BY descripcion_principal",
            nativeQuery = true)
    List<Object[]> buscarPorTermino(@Param("termino") String termino);

    /**
     * Búsqueda por código exacto
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE UPPER(codigo_busqueda) = UPPER(:codigo) " +
            "  AND estado IN ('ACTIVO', 'DISPONIBLE') " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Object[]> buscarPorCodigoExacto(@Param("codigo") String codigo);

    // ===== BÚSQUEDAS POR TIPO =====

    /**
     * Búsqueda solo en exámenes
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE tabla_origen = 'examenes' " +
            "  AND (LOWER(codigo_busqueda) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "   OR LOWER(descripcion_principal) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
            "  AND estado = 'ACTIVO' " +
            "ORDER BY descripcion_principal",
            nativeQuery = true)
    List<Object[]> buscarExamenes(@Param("termino") String termino);

    /**
     * Búsqueda solo en medicamentos
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE tabla_origen = 'medicamentos' " +
            "  AND (LOWER(codigo_busqueda) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "   OR LOWER(descripcion_principal) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
            "  AND estado IN ('DISPONIBLE', 'ACTIVO') " +
            "ORDER BY descripcion_principal",
            nativeQuery = true)
    List<Object[]> buscarMedicamentos(@Param("termino") String termino);

    /**
     * Búsqueda solo en catálogos administrativos
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE tabla_origen LIKE 'catalogo_%' " +
            "  AND (LOWER(codigo_busqueda) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "   OR LOWER(descripcion_principal) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
            "  AND estado = 'ACTIVO' " +
            "ORDER BY categoria_principal, descripcion_principal",
            nativeQuery = true)
    List<Object[]> buscarCatalogos(@Param("termino") String termino);

    // ===== BÚSQUEDAS POR CATEGORÍA =====

    /**
     * Búsqueda por categoría específica
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE LOWER(categoria_principal) = LOWER(:categoria) " +
            "  AND estado IN ('ACTIVO', 'DISPONIBLE') " +
            "ORDER BY descripcion_principal",
            nativeQuery = true)
    List<Object[]> buscarPorCategoria(@Param("categoria") String categoria);

    /**
     * Búsqueda en tabla específica de catálogo
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE tabla_origen = CONCAT('catalogo_', :tablaCodigo) " +
            "  AND estado = 'ACTIVO' " +
            "ORDER BY descripcion_principal",
            nativeQuery = true)
    List<Object[]> buscarEnTablaEspecifica(@Param("tablaCodigo") String tablaCodigo);

    // ===== LISTADOS COMPLETOS =====

    /**
     * Obtener todos los exámenes activos
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE tabla_origen = 'examenes' " +
            "  AND estado = 'ACTIVO' " +
            "ORDER BY categoria_principal, descripcion_principal",
            nativeQuery = true)
    List<Object[]> obtenerTodosLosExamenes();

    /**
     * Obtener todos los medicamentos disponibles
     */
    @Query(value = "SELECT tabla_origen, codigo_busqueda, descripcion_principal, categoria_principal, estado, " +
            "CAST(datos_json AS TEXT) as datos_json " +
            "FROM vista_busqueda_unificada " +
            "WHERE tabla_origen = 'medicamentos' " +
            "  AND estado IN ('DISPONIBLE', 'ACTIVO') " +
            "ORDER BY categoria_principal, descripcion_principal",
            nativeQuery = true)
    List<Object[]> obtenerTodosLosMedicamentos();

    /**
     * Obtener todas las categorías disponibles
     */
    @Query(value = "SELECT DISTINCT categoria_principal " +
            "FROM vista_busqueda_unificada " +
            "WHERE estado IN ('ACTIVO', 'DISPONIBLE') " +
            "ORDER BY categoria_principal",
            nativeQuery = true)
    List<String> obtenerTodasLasCategorias();

    /**
     * Obtener todos los tipos de tabla disponibles
     */
    @Query(value = "SELECT DISTINCT tabla_origen " +
            "FROM vista_busqueda_unificada " +
            "WHERE estado IN ('ACTIVO', 'DISPONIBLE') " +
            "ORDER BY tabla_origen",
            nativeQuery = true)
    List<String> obtenerTiposDeTabla();
}