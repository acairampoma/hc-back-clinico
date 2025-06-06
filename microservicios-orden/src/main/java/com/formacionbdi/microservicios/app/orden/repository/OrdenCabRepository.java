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
 */
@Repository
public interface OrdenCabRepository extends JpaRepository<OrdenCab, Long> {

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
}