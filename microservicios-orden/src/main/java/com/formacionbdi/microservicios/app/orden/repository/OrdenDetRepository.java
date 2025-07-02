package com.formacionbdi.microservicios.app.orden.repository;

import com.formacionbdi.microservicios.app.orden.models.entity.OrdenDet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🔬 Repository Órdenes DET - Java 17 Completo
 * Puerto: 8006
 *
 * ✅ Características Java 17:
 * - Text blocks para queries complejas
 * - JOIN inteligente: orden_det + examenes
 * - Queries optimizadas con INNER JOIN
 * - Métodos atómicos para operaciones CRUD
 * - Estadísticas avanzadas
 */
@Repository
public interface OrdenDetRepository extends JpaRepository<OrdenDet, Long> {

    // =====================================================
    // 🔍 CONSULTAS BÁSICAS CON TEXT BLOCKS (JAVA 17)
    // =====================================================

    /**
     * ⭐ JOIN inteligente: OrdenDet + Examen completo
     * Retorna Object[] que se mapea a OrdenExamenDTO
     */
    @Query("""
        SELECT od, e 
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.ordenCab.id = :ordenId 
        AND od.activo = 'S' 
        AND e.activo = 'S'
        ORDER BY od.ordenItem ASC, od.id ASC
        """)
    List<Object[]> findExamenesConInfoByOrdenId(@Param("ordenId") Long ordenId);

    /**
     * Buscar por orden específica (sin JOIN, más rápido)
     */
    @Query("""
        SELECT od 
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.activo = 'S'
        ORDER BY od.ordenItem ASC, od.id ASC
        """)
    List<OrdenDet> findByOrdenIdActivos(@Param("ordenId") Long ordenId);

    /**
     * ✅ MÉTODO FALTANTE: Contar exámenes activos por orden
     */
    @Query("""
        SELECT COUNT(od) 
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.activo = :activo
        """)
    int countExamenesActivos(@Param("ordenId") Long ordenId, @Param("activo") String activo);

    /**
     * Método estándar de Spring Data JPA (alternativo)
     */
    int countByOrdenCabIdAndActivo(Long ordenCabId, String activo);

    // =====================================================
    // 🔬 JOINS AVANZADOS CON FILTROS
    // =====================================================

    /**
     * JOIN con filtro por estado de detalle
     */
    @Query("""
        SELECT od, e 
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.ordenCab.id = :ordenId 
        AND od.estadoDetalle = :estado
        AND od.activo = 'S' 
        AND e.activo = 'S'
        ORDER BY od.ordenItem ASC
        """)
    List<Object[]> findExamenesConInfoByOrdenIdAndEstado(@Param("ordenId") Long ordenId,
                                                         @Param("estado") String estado);

    /**
     * JOIN con filtro por categoría
     */
    @Query("""
        SELECT od, e 
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.ordenCab.id = :ordenId 
        AND e.categoria = :categoria
        AND od.activo = 'S' 
        AND e.activo = 'S'
        ORDER BY od.ordenItem ASC
        """)
    List<Object[]> findExamenesConInfoByOrdenIdAndCategoria(@Param("ordenId") Long ordenId,
                                                            @Param("categoria") String categoria);

    /**
     * Buscar por múltiples estados
     */
    @Query("""
        SELECT od, e 
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.ordenCab.id = :ordenId 
        AND od.estadoDetalle IN :estados
        AND od.activo = 'S' 
        AND e.activo = 'S'
        ORDER BY od.ordenItem ASC
        """)
    List<Object[]> findExamenesConInfoByOrdenIdAndEstados(@Param("ordenId") Long ordenId,
                                                          @Param("estados") List<String> estados);

    // =====================================================
    // ⚡ CONSULTAS DE PRIORIDAD Y URGENCIA
    // =====================================================

    /**
     * Exámenes pendientes (estado 01)
     */
    @Query("""
        SELECT od, e 
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.estadoDetalle = '01' 
        AND od.activo = 'S' 
        AND e.activo = 'S'
        ORDER BY od.creadoEn ASC
        """)
    List<Object[]> findExamenesPendientesConInfo();

    /**
     * Exámenes urgentes (emergencia + urgente)
     */
    @Query("""
        SELECT od, e, oc
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        INNER JOIN OrdenCab oc ON od.ordenCab.id = oc.id
        WHERE oc.prioridad IN ('E', 'U')
        AND od.estadoDetalle = '01' 
        AND od.activo = 'S' 
        AND e.activo = 'S'
        AND oc.activo = 'S'
        ORDER BY oc.prioridad DESC, od.creadoEn ASC
        """)
    List<Object[]> findExamenesUrgentesConInfo();

    /**
     * Órdenes con exámenes incompletos
     */
    @Query("""
        SELECT DISTINCT od.ordenCab.id
        FROM OrdenDet od 
        WHERE od.estadoDetalle IN ('01', '02') 
        AND od.activo = 'S'
        """)
    List<Long> findOrdenesConExamenesIncompletos();

    // =====================================================
    // 🔍 BÚSQUEDAS ESPECÍFICAS
    // =====================================================

    /**
     * Buscar por examen específico
     */
    @Query("""
        SELECT od 
        FROM OrdenDet od 
        WHERE od.examenId = :examenId 
        AND od.activo = 'S'
        ORDER BY od.creadoEn DESC
        """)
    List<OrdenDet> findByExamenIdActivos(@Param("examenId") Long examenId);

    /**
     * Buscar examen específico en orden
     */
    @Query("""
        SELECT od 
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.examenId = :examenId 
        AND od.activo = 'S'
        """)
    Optional<OrdenDet> findByOrdenIdAndExamenId(@Param("ordenId") Long ordenId,
                                                @Param("examenId") Long examenId);

    /**
     * ✅ MÉTODO FALTANTE: Verificar si examen ya existe en orden
     */
    @Query("""
        SELECT COUNT(od) > 0 
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.examenId = :examenId 
        AND od.activo = 'S'
        """)
    boolean existeExamenEnOrden(@Param("ordenId") Long ordenId, @Param("examenId") Long examenId);

    // =====================================================
    // 🛠️ OPERACIONES DE MODIFICACIÓN
    // =====================================================

    /**
     * ✅ MÉTODO FALTANTE: Eliminar lógicamente examen de orden
     */
    @Modifying
    @Query("""
        UPDATE OrdenDet od 
        SET od.activo = 'N', 
            od.actualizadoPor = :medicoId, 
            od.actualizadoEn = CURRENT_TIMESTAMP
        WHERE od.id = :examenDetalleId 
        AND od.ordenCab.id = :ordenId 
        AND od.activo = 'S'
        """)
    int eliminarExamenLogico(@Param("ordenId") Long ordenId,
                             @Param("examenDetalleId") Long examenDetalleId,
                             @Param("medicoId") Long medicoId);

    /**
     * Método alternativo para eliminar (mantener compatibilidad)
     */
    @Modifying
    @Query("""
        UPDATE OrdenDet od 
        SET od.activo = 'N', 
            od.actualizadoPor = :medicoId, 
            od.actualizadoEn = CURRENT_TIMESTAMP
        WHERE od.id = :examenDetalleId 
        AND od.ordenCab.id = :ordenId
        """)
    int eliminarExamenDeOrden(@Param("ordenId") Long ordenId,
                              @Param("examenDetalleId") Long examenDetalleId,
                              @Param("medicoId") Long medicoId);

    /**
     * ✅ MÉTODO FALTANTE: Próximo número de item para orden
     */
    @Query("""
        SELECT COALESCE(MAX(od.ordenItem), 0) + 1
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.activo = 'S'
        """)
    Integer getNextOrdenItem(@Param("ordenId") Long ordenId);

    // =====================================================
    // 📊 ESTADÍSTICAS CON JOINS INTELIGENTES
    // =====================================================

    /**
     * ✅ CORREGIDO: Estadísticas por categoría con JOIN
     */
    @Query("""
        SELECT e.categoria, COUNT(od)
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.activo = 'S' 
        AND e.activo = 'S'
        GROUP BY e.categoria
        ORDER BY COUNT(od) DESC
        """)
    List<Object[]> findEstadisticasPorCategoria();

    /**
     * ✅ CORREGIDO: Exámenes más solicitados con JOIN
     */
    @Query("""
        SELECT e.codigo, e.nombre, COUNT(od), e.categoria
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.activo = 'S' 
        AND e.activo = 'S'
        GROUP BY e.id, e.codigo, e.nombre, e.categoria
        ORDER BY COUNT(od) DESC
        """)
    List<Object[]> findExamenesMasSolicitados();

    /**
     * Estadísticas por estado de detalle
     */
    @Query("""
        SELECT od.estadoDetalle, COUNT(od)
        FROM OrdenDet od 
        WHERE od.activo = 'S'
        GROUP BY od.estadoDetalle
        ORDER BY COUNT(od) DESC
        """)
    List<Object[]> findEstadisticasPorEstadoDetalle();

    // =====================================================
    // ⏱️ ANÁLISIS DE TIEMPO Y PERFORMANCE
    // =====================================================

    /**
     * Tiempo promedio de procesamiento por categoría
     */
    @Query(value = """
        SELECT e.categoria, 
               AVG(EXTRACT(EPOCH FROM (od.actualizado_en - od.creado_en))/3600) as horas_promedio
        FROM ordenes_det od 
        INNER JOIN examenes e ON od.examen_id = e.id 
        WHERE od.estado_detalle = '03' 
        AND od.actualizado_en IS NOT NULL
        AND od.activo = 'S'
        AND e.activo = 'S'
        GROUP BY e.categoria
        ORDER BY horas_promedio ASC
        """, nativeQuery = true)
    List<Object[]> findTiempoPromedioByCategoria();

    /**
     * Exámenes con mayor tiempo de procesamiento
     */
    @Query(value = """
        SELECT e.codigo, e.nombre, 
               AVG(EXTRACT(EPOCH FROM (od.actualizado_en - od.creado_en))/3600) as horas_promedio,
               COUNT(od) as total_procesados
        FROM ordenes_det od 
        INNER JOIN examenes e ON od.examen_id = e.id 
        WHERE od.estado_detalle = '03' 
        AND od.actualizado_en IS NOT NULL
        AND od.activo = 'S'
        AND e.activo = 'S'
        GROUP BY e.id, e.codigo, e.nombre
        HAVING COUNT(od) >= 5
        ORDER BY horas_promedio DESC
        """, nativeQuery = true)
    List<Object[]> findExamenesConMayorTiempoProcesamiento();

    // =====================================================
    // 🔄 CONSULTAS DE ESTADO Y WORKFLOW
    // =====================================================

    /**
     * Exámenes por rango de fechas
     */
    @Query("""
        SELECT od, e 
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        WHERE od.creadoEn BETWEEN :fechaInicio AND :fechaFin
        AND od.activo = 'S' 
        AND e.activo = 'S'
        ORDER BY od.creadoEn DESC
        """)
    List<Object[]> findExamenesEnRangoFechas(@Param("fechaInicio") java.time.LocalDateTime fechaInicio,
                                             @Param("fechaFin") java.time.LocalDateTime fechaFin);

    /**
     * Exámenes pendientes más antiguos
     */
    @Query("""
        SELECT od, e, oc
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        INNER JOIN OrdenCab oc ON od.ordenCab.id = oc.id
        WHERE od.estadoDetalle = '01' 
        AND od.activo = 'S' 
        AND e.activo = 'S'
        AND oc.activo = 'S'
        AND od.creadoEn < :fechaLimite
        ORDER BY od.creadoEn ASC
        """)
    List<Object[]> findExamenesPendientesAntiguos(@Param("fechaLimite") java.time.LocalDateTime fechaLimite);

    // =====================================================
    // 🏥 CONSULTAS POR ORIGEN Y CONTEXTO
    // =====================================================

    /**
     * Exámenes por tipo de origen
     */
    @Query("""
        SELECT od, e, oc
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        INNER JOIN OrdenCab oc ON od.ordenCab.id = oc.id
        WHERE oc.tipoOrigen = :tipoOrigen
        AND od.activo = 'S' 
        AND e.activo = 'S'
        AND oc.activo = 'S'
        ORDER BY od.creadoEn DESC
        """)
    List<Object[]> findExamenesPorTipoOrigen(@Param("tipoOrigen") String tipoOrigen);

    /**
     * Exámenes de emergencia prioritarios
     */
    @Query("""
        SELECT od, e, oc
        FROM OrdenDet od 
        INNER JOIN Examen e ON od.examenId = e.id 
        INNER JOIN OrdenCab oc ON od.ordenCab.id = oc.id
        WHERE oc.tipoOrigen = 'EMR'
        AND oc.prioridad = 'E'
        AND od.estadoDetalle IN ('01', '02')
        AND od.activo = 'S' 
        AND e.activo = 'S'
        AND oc.activo = 'S'
        ORDER BY od.creadoEn ASC
        """)
    List<Object[]> findExamenesEmergenciaPrioritarios();

    // =====================================================
    // 🧪 MÉTODOS HELPER Y UTILIDADES
    // =====================================================

    /**
     * Verificar si orden tiene exámenes pendientes
     */
    @Query("""
        SELECT COUNT(od) > 0
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.estadoDetalle IN ('01', '02')
        AND od.activo = 'S'
        """)
    boolean tieneExamenesPendientes(@Param("ordenId") Long ordenId);

    /**
     * Obtener último examen agregado a una orden
     */
    @Query("""
        SELECT od
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.activo = 'S'
        ORDER BY od.creadoEn DESC, od.id DESC
        """)
    Optional<OrdenDet> findUltimoExamenDeOrden(@Param("ordenId") Long ordenId);

    /**
     * Contar exámenes por estado en una orden
     */
    @Query("""
        SELECT od.estadoDetalle, COUNT(od)
        FROM OrdenDet od 
        WHERE od.ordenCab.id = :ordenId 
        AND od.activo = 'S'
        GROUP BY od.estadoDetalle
        """)
    List<Object[]> countExamenesPorEstadoEnOrden(@Param("ordenId") Long ordenId);
}