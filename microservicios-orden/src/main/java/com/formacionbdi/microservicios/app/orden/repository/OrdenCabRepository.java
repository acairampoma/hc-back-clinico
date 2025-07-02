package com.formacionbdi.microservicios.app.orden.repository;

import com.formacionbdi.microservicios.app.orden.models.entity.OrdenCab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Órdenes CAB - Consultas de cabecera
 * Puerto: 8006
 * 
 * ACTUALIZADO: Métodos para transacciones atómicas con PostgreSQL
 * PATRÓN: Idéntico al microservicio de recetas
 */
@Repository
public interface OrdenCabRepository extends JpaRepository<OrdenCab, Long> {

    // =====================================================
    // FUNCIÓN POSTGRESQL - OPERACIONES ATÓMICAS
    // =====================================================

    /**
     * CREAR ORDEN - OPERACIÓN ATÓMICA COMPLETA
     * Usa la función procesar_orden_http('POST', datos_jsonb)
     * 
     * @param datosOrdenJson Datos de la orden en formato JSON
     * @return Resultado de la operación en formato JSON
     */
    @Query(value = "SELECT procesar_orden_http('POST', CAST(:datosOrden AS jsonb))",
            nativeQuery = true)
    String crearOrdenAtomica(@Param("datosOrden") String datosOrdenJson);

    /**
     * ACTUALIZAR ORDEN - OPERACIONES GRANULARES
     * Usa la función procesar_orden_http('PUT', datos_jsonb)
     * 
     * @param datosOrdenJson Datos de actualización en formato JSON
     * @return Resultado de la operación en formato JSON
     */
    @Query(value = "SELECT procesar_orden_http('PUT', CAST(:datosOrden AS jsonb))",
            nativeQuery = true)
    String actualizarOrdenAtomica(@Param("datosOrden") String datosOrdenJson);

    /**
     * CAMBIAR ESTADO - OPERACIÓN ESPECÍFICA
     * Usa la función procesar_orden_http('PATCH', datos_jsonb)
     * 
     * @param datosEstadoJson Datos del cambio de estado en formato JSON
     * @return Resultado de la operación en formato JSON
     */
    @Query(value = "SELECT procesar_orden_http('PATCH', CAST(:datosEstado AS jsonb))",
            nativeQuery = true)
    String cambiarEstadoAtomica(@Param("datosEstado") String datosEstadoJson);

    // =====================================================
    // CONSULTAS BÁSICAS
    // =====================================================

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.numeroOrden = :numeroOrden AND oc.activo = 'S'")
    Optional<OrdenCab> findByNumeroOrden(@Param("numeroOrden") String numeroOrden);

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.pacienteId = :pacienteId AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByPacienteId(@Param("pacienteId") Long pacienteId);

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.medicoId = :medicoId AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByMedicoId(@Param("medicoId") Long medicoId);

    // =====================================================
    // CONSULTAS POR ORIGEN (HOS, AMB, EMR)
    // =====================================================

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.tipoOrigen = :tipoOrigen AND oc.origenId = :origenId AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByOrigen(@Param("tipoOrigen") String tipoOrigen, @Param("origenId") Long origenId);

    // =====================================================
    // CONSULTAS POR ESTADO Y PRIORIDAD
    // =====================================================

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.estado = :estado AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByEstado(@Param("estado") String estado);

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.prioridad = :prioridad AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByPrioridad(@Param("prioridad") String prioridad);

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.tipoOrden = :tipoOrden AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByTipoOrden(@Param("tipoOrden") String tipoOrden);

    // =====================================================
    // CONSULTAS POR FECHAS
    // =====================================================

    @Query("SELECT oc FROM OrdenCab oc WHERE DATE(oc.fechaOrden) = :fecha AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByFechaOrden(@Param("fecha") LocalDate fecha);

    @Query("SELECT oc FROM OrdenCab oc WHERE oc.fechaProgramada = :fecha AND oc.activo = 'S' ORDER BY oc.fechaOrden DESC")
    List<OrdenCab> findByFechaProgramada(@Param("fecha") LocalDate fecha);

    // =====================================================
    // VALIDACIONES DE NEGOCIO
    // =====================================================

    @Query("SELECT COUNT(oc) > 0 FROM OrdenCab oc WHERE oc.tipoOrigen = :tipoOrigen AND oc.origenId = :origenId " +
            "AND oc.tipoOrden = :tipoOrden AND DATE(oc.fechaOrden) = CURRENT_DATE AND oc.activo = 'S'")
    boolean existeOrdenMismoTipoHoy(@Param("tipoOrigen") String tipoOrigen,
                                    @Param("origenId") Long origenId,
                                    @Param("tipoOrden") String tipoOrden);

    // =====================================================
    // ESTADÍSTICAS
    // =====================================================

    @Query("SELECT oc.estado, COUNT(oc) FROM OrdenCab oc WHERE oc.activo = 'S' GROUP BY oc.estado")
    List<Object[]> findEstadisticasPorEstado();

    @Query("SELECT oc.tipoOrden, COUNT(oc) FROM OrdenCab oc WHERE oc.activo = 'S' GROUP BY oc.tipoOrden")
    List<Object[]> findEstadisticasPorTipo();

    @Query("SELECT oc.prioridad, COUNT(oc) FROM OrdenCab oc WHERE oc.activo = 'S' GROUP BY oc.prioridad")
    List<Object[]> findEstadisticasPorPrioridad();

    /**
     * Estadísticas por estado
     */
    @Query("SELECT oc.estado, COUNT(oc), oc.estado FROM OrdenCab oc WHERE oc.activo = :activo GROUP BY oc.estado")
    List<Object[]> obtenerEstadisticasPorEstado(@Param("activo") String activo);

    /**
     * Estadísticas por tipo
     */
    @Query("SELECT oc.tipoOrden, COUNT(oc), oc.tipoOrden FROM OrdenCab oc WHERE oc.activo = :activo GROUP BY oc.tipoOrden")
    List<Object[]> obtenerEstadisticasPorTipo(@Param("activo") String activo);

    /**
     * Estadísticas por prioridad
     */
    @Query("SELECT oc.prioridad, COUNT(oc), oc.prioridad FROM OrdenCab oc WHERE oc.activo = :activo GROUP BY oc.prioridad")
    List<Object[]> obtenerEstadisticasPorPrioridad(@Param("activo") String activo);

    // =====================================================
    // CONSULTAS BÁSICAS MEJORADAS (JAVA 17)
    // =====================================================

    /**
     * Obtener todas las órdenes activas
     */
    List<OrdenCab> findByActivo(String activo);

    /**
     * Obtener órdenes por paciente (activas)
     */
    List<OrdenCab> findByPacienteIdAndActivo(Long pacienteId, String activo);

    /**
     * Obtener órdenes por médico (activas)
     */
    List<OrdenCab> findByMedicoIdAndActivo(Long medicoId, String activo);

    /**
     * Obtener órdenes por origen (activas)
     */
    List<OrdenCab> findByTipoOrigenAndOrigenIdAndActivo(String tipoOrigen, Long origenId, String activo);

    /**
     * Obtener órdenes por estado (activas)
     */
    List<OrdenCab> findByEstadoAndActivo(String estado, String activo);

    /**
     * Obtener órdenes por prioridad (activas)
     */
    List<OrdenCab> findByPrioridadAndActivo(String prioridad, String activo);

    /**
     * Obtener órdenes por tipo (activas)
     */
    List<OrdenCab> findByTipoOrdenAndActivo(String tipoOrden, String activo);

    /**
     * Obtener órdenes por fecha (activas)
     */
    List<OrdenCab> findByFechaOrdenAndActivo(LocalDate fecha, String activo);

    /**
     * Obtener órdenes programadas (activas)
     */
    List<OrdenCab> findByFechaProgramadaAndActivo(LocalDate fecha, String activo);

    /**
     * Verificar si existe orden por número (activa)
     */
    boolean existsByNumeroOrdenAndActivo(String numeroOrden, String activo);

    /**
     * Verificar si existe orden activa para el mismo origen y tipo
     */
    boolean existsByTipoOrigenAndOrigenIdAndTipoOrdenAndEstadoNotAndActivo(
            String tipoOrigen, Long origenId, String tipoOrden, String estado, String activo);
}