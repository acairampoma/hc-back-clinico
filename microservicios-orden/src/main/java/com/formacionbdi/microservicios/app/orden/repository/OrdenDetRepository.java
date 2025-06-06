package com.formacionbdi.microservicios.app.orden.repository;

import com.formacionbdi.microservicios.app.orden.models.entity.OrdenDet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITORY ÓRDENES DETALLE - JPQL COMO RECETAS
 * Sin native queries problemáticas
 */
@Repository
public interface OrdenDetRepository extends JpaRepository<OrdenDet, Long> {

    // =====================================================
    // JPQL COMO RECETAS - SIN PROBLEMAS DE CAST
    // =====================================================

    /**
     * JPQL CON JOIN - IGUAL QUE RECETAS
     * Obtiene exámenes con información adicional
     */
    @Query("SELECT od, e FROM OrdenDet od " +
            "LEFT JOIN Examen e ON od.examenId = e.id " +
            "WHERE od.ordenCab.id = :ordenId " +
            "AND od.activo = 'S' " +
            "ORDER BY od.ordenItem ASC")
    List<Object[]> findExamenesConDetalleByOrdenId(@Param("ordenId") Long ordenId);

    /**
     * Buscar por código de examen - JOIN con tabla Examen
     */
    @Query("SELECT od FROM OrdenDet od " +
            "JOIN Examen e ON od.examenId = e.id " +
            "WHERE e.codigo = :codigoExamen " +
            "AND od.activo = 'S' " +
            "ORDER BY od.creadoEn DESC")
    List<OrdenDet> findByCodigoExamen(@Param("codigoExamen") String codigoExamen);

    /**
     * Examenes por orden (sin JOIN para operaciones simples)
     */
    @Query("SELECT od FROM OrdenDet od " +
            "WHERE od.ordenCab.id = :ordenId " +
            "AND od.activo = 'S' " +
            "ORDER BY od.ordenItem")
    List<OrdenDet> findByOrdenIdOrderByOrden(@Param("ordenId") Long ordenId);

    /**
     * Buscar exámenes por categoría - JOIN con tabla Examen
     */
    @Query("SELECT od FROM OrdenDet od " +
            "JOIN Examen e ON od.examenId = e.id " +
            "WHERE e.categoria = :categoria " +
            "AND od.activo = 'S' " +
            "ORDER BY od.ordenItem")
    List<OrdenDet> findByCategoria(@Param("categoria") String categoria);

    /**
     * Buscar por estado detalle - JPQL
     */
    @Query("SELECT od FROM OrdenDet od " +
            "WHERE od.estadoDetalle = :estado " +
            "AND od.activo = 'S'")
    List<OrdenDet> findByEstadoDetalle(@Param("estado") String estado);

    /**
     * Estadísticas de exámenes más solicitados - JOIN con tabla Examen
     */
    @Query("SELECT e.codigo, e.descripcion, COUNT(od.id) as total " +
            "FROM OrdenDet od " +
            "JOIN Examen e ON od.examenId = e.id " +
            "WHERE od.activo = 'S' " +
            "GROUP BY e.codigo, e.descripcion " +
            "ORDER BY total DESC")
    List<Object[]> findExamenesMasSolicitados();

    /**
     * Estadísticas por categoría - JOIN con tabla Examen
     */
    @Query("SELECT e.categoria, COUNT(od.id) as total " +
            "FROM OrdenDet od " +
            "JOIN Examen e ON od.examenId = e.id " +
            "WHERE od.activo = 'S' " +
            "GROUP BY e.categoria " +
            "ORDER BY total DESC")
    List<Object[]> findEstadisticasPorCategoria();

    /**
     * Buscar exámenes con resultados - JPQL
     */
    @Query("SELECT od FROM OrdenDet od " +
            "WHERE od.resultado IS NOT NULL " +
            "AND od.activo = 'S' " +
            "ORDER BY od.creadoEn DESC")
    List<OrdenDet> findExamenesConResultados();

    /**
     * Buscar exámenes pendientes - JPQL
     */
    @Query("SELECT od FROM OrdenDet od " +
            "WHERE od.estadoDetalle = '01' " +
            "AND od.activo = 'S' " +
            "ORDER BY od.creadoEn ASC")
    List<OrdenDet> findExamenesPendientes();

    /**
     * Contar exámenes por orden - JPQL
     */
    @Query("SELECT COUNT(od) FROM OrdenDet od " +
            "WHERE od.ordenCab.id = :ordenId " +
            "AND od.activo = 'S'")
    Long countExamenesByOrdenId(@Param("ordenId") Long ordenId);

    /**
     * Verificar si existe examen en orden - JPQL
     */
    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrdenDet od " +
            "WHERE od.ordenCab.id = :ordenId " +
            "AND od.examenId = :examenId " +
            "AND od.activo = 'S'")
    boolean existsByOrdenIdAndExamenId(@Param("ordenId") Long ordenId,
                                       @Param("examenId") Long examenId);

    /**
     * Contar exámenes activos por orden - JPQL
     */
    @Query("SELECT COUNT(od) FROM OrdenDet od " +
            "WHERE od.ordenCab.id = :ordenId " +
            "AND od.activo = :activo")
    Integer countByOrdenIdAndActivo(@Param("ordenId") Long ordenId, @Param("activo") String activo);

    /**
     * Obtiene exámenes de una orden con información completa
     * JOIN con tabla examenes para nombreExamen y categoria
     * Campos nuevos: cantidad, des_indicacion, des_consideraciones
     */
    @Query("SELECT od, e FROM OrdenDet od " +
            "JOIN Examen e ON od.examenId = e.id " +
            "WHERE od.ordenCab.id = :ordenId " +
            "AND od.activo = 'S' " +
            "AND e.activo = 'S' " +
            "ORDER BY od.ordenItem ASC")
    List<Object[]> findExamenesConInfoByOrdenId(@Param("ordenId") Long ordenId);

}