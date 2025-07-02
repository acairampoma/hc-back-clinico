package com.formacionbdi.microservicios.app.receta.repository;

import com.formacionbdi.microservicios.app.receta.models.entity.RecetaCab;
import com.formacionbdi.microservicios.app.receta.models.dto.RecetaBasicaInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY HÍBRIDO para Receta Cabecera - JAVA 17
 * Queries directos para GET (performance)
 * Función PostgreSQL para CRUD (atomicidad)
 * Records para responses
 * Switch expressions en métodos
 */
@Repository
public interface RecetaCabRepository extends JpaRepository<RecetaCab, Long> {

    // =====================================================
    // FUNCIÓN POSTGRESQL - OPERACIONES ATÓMICAS
    // =====================================================

    /**
     * CREAR RECETA - OPERACIÓN ATÓMICA COMPLETA
     * Usa la función procesar_receta_http('POST', datos_jsonb)
     */
    @Query(value = "SELECT procesar_receta_http('POST', CAST(:datosReceta AS jsonb))",
            nativeQuery = true)
    String crearRecetaAtomica(@Param("datosReceta") String datosRecetaJson);

    /**
     * ACTUALIZAR RECETA - OPERACIONES GRANULARES
     * Usa la función procesar_receta_http('PUT', datos_jsonb)
     */
    @Query(value = "SELECT procesar_receta_http('PUT', CAST(:datosReceta AS jsonb))",
            nativeQuery = true)
    String actualizarRecetaAtomica(@Param("datosReceta") String datosRecetaJson);

    /**
     * CAMBIAR ESTADO - OPERACIÓN ESPECÍFICA
     * Usa la función procesar_receta_http('PATCH', datos_jsonb)
     */
    @Query(value = "SELECT procesar_receta_http('PATCH', CAST(:datosEstado AS jsonb))",
            nativeQuery = true)
    String cambiarEstadoAtomica(@Param("datosEstado") String datosEstadoJson);

    // =====================================================
    // VALIDACIONES CRÍTICAS (Para Service Layer)
    // =====================================================

    /**
     * VALIDAR: No receta duplicada mismo día
     * REGLA: No se puede crear receta del mismo origen en el mismo día
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END 
        FROM RecetaCab r 
        WHERE r.tipoOrigen = :tipoOrigen 
        AND r.origenId = :origenId 
        AND DATE(r.fechaReceta) = CURRENT_DATE 
        AND r.activo = 'S'
        """)
    boolean existeRecetaMismoOrigenHoy(@Param("tipoOrigen") String tipoOrigen,
                                       @Param("origenId") Long origenId);

    /**
     * VALIDAR: Recetas activas por paciente
     */
    @Query("""
        SELECT COUNT(r) FROM RecetaCab r 
        WHERE r.pacienteId = :pacienteId 
        AND r.estado = '01' 
        AND r.activo = 'S'
        """)
    Long contarRecetasActivasPorPaciente(@Param("pacienteId") Long pacienteId);

    /**
     * VALIDAR: Medicamentos duplicados en mismo día
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END 
        FROM RecetaCab r JOIN RecetaDet rd ON r.id = rd.recetaId 
        WHERE r.tipoOrigen = :tipoOrigen 
        AND r.origenId = :origenId 
        AND DATE(r.fechaReceta) = CURRENT_DATE 
        AND r.estado = '01' 
        AND rd.medicamentoId IN :medicamentoIds 
        AND r.activo = 'S' 
        AND rd.activo = 'S'
        """)
    boolean existeRecetaConMedicamentosDuplicadosHoy(@Param("tipoOrigen") String tipoOrigen,
                                                     @Param("origenId") Long origenId,
                                                     @Param("medicamentoIds") List<Long> medicamentoIds);

    /**
     * VALIDAR: Receta puede modificarse
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM RecetaCab r 
        WHERE r.id = :recetaId 
        AND r.fechaVencimiento > CURRENT_DATE + 1 
        AND r.estado = '01' 
        AND r.activo = 'S'
        """)
    boolean puedeModificarseReceta(@Param("recetaId") Long recetaId);

    // =====================================================
    // CONSULTAS DIRECTAS - PERFORMANCE OPTIMIZADA
    // =====================================================

    /**
     * BUSCAR: Por número de receta
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.numeroReceta = :numeroReceta 
        AND r.activo = 'S'
        """)
    Optional<RecetaCab> findByNumeroReceta(@Param("numeroReceta") String numeroReceta);

    /**
     * BUSCAR: Por tipo de origen y origen ID
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.tipoOrigen = :tipoOrigen 
        AND r.origenId = :origenId 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findByTipoOrigenAndOrigenId(@Param("tipoOrigen") String tipoOrigen,
                                                @Param("origenId") Long origenId);

    /**
     * BUSCAR: Todas las recetas de un paciente
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.pacienteId = :pacienteId 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findByPacienteId(@Param("pacienteId") Long pacienteId);

    /**
     * BUSCAR: Recetas creadas por un médico
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.medicoId = :medicoId 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findByMedicoId(@Param("medicoId") Long medicoId);

    // =====================================================
    // CONSULTAS CON FILTROS AVANZADOS
    // =====================================================

    /**
     * BUSCAR: Con filtros múltiples
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE (:estado IS NULL OR r.estado = :estado) 
        AND (:tipoOrigen IS NULL OR r.tipoOrigen = :tipoOrigen) 
        AND (:origenId IS NULL OR r.origenId = :origenId) 
        AND (:pacienteId IS NULL OR r.pacienteId = :pacienteId) 
        AND (:medicoId IS NULL OR r.medicoId = :medicoId) 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findWithFilters(@Param("estado") String estado,
                                    @Param("tipoOrigen") String tipoOrigen,
                                    @Param("origenId") Long origenId,
                                    @Param("pacienteId") Long pacienteId,
                                    @Param("medicoId") Long medicoId);

    /**
     * BUSCAR: Por médico en rango de fechas
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.medicoId = :medicoId 
        AND r.fechaReceta BETWEEN :fechaInicio AND :fechaFin 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findByMedicoIdAndFechasBetween(@Param("medicoId") Long medicoId,
                                                   @Param("fechaInicio") LocalDateTime fechaInicio,
                                                   @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * BUSCAR: Recetas vencidas
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.fechaVencimiento < CURRENT_DATE 
        AND r.estado IN ('01', '02') 
        AND r.activo = 'S' 
        ORDER BY r.fechaVencimiento ASC
        """)
    List<RecetaCab> findRecetasVencidas();

    /**
     * BUSCAR: Recetas pendientes de firma
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.firmada = 'N' 
        AND r.estado = '01' 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findRecetasPendientesFirma();

    // =====================================================
    // CONSULTAS OPTIMIZADAS PARA UI (Con Records)
    // =====================================================

    /**
     * LISTAR: Info básica para listas (Performance optimizada)
     */
    @Query("""
        SELECT new com.formacionbdi.microservicios.app.receta.models.dto.RecetaBasicaInfo(
            r.id, 
            r.numeroReceta, 
            CONCAT('Paciente ID ', r.pacienteId),
            CONCAT('Médico ID ', r.medicoId),
            r.estado,
            CASE r.estado 
                WHEN '01' THEN 'Activa'
                WHEN '02' THEN 'Despachada'
                WHEN '03' THEN 'Vencida'
                WHEN '04' THEN 'Anulada'
                ELSE 'Desconocido'
            END,
            r.fechaReceta,
            CASE WHEN r.firmada = 'S' THEN true ELSE false END,
            COALESCE((SELECT COUNT(rd) FROM RecetaDet rd WHERE rd.recetaId = r.id AND rd.activo = 'S'), 0)
        )
        FROM RecetaCab r 
        WHERE r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaBasicaInfo> findRecetasBasicaInfo();

    /**
     * LISTAR: Info básica por paciente
     */
    @Query("""
        SELECT new com.formacionbdi.microservicios.app.receta.models.dto.RecetaBasicaInfo(
            r.id, 
            r.numeroReceta, 
            CONCAT('Paciente ID ', r.pacienteId),
            CONCAT('Médico ID ', r.medicoId),
            r.estado,
            CASE r.estado 
                WHEN '01' THEN 'Activa'
                WHEN '02' THEN 'Despachada'
                WHEN '03' THEN 'Vencida'
                WHEN '04' THEN 'Anulada'
                ELSE 'Desconocido'
            END,
            r.fechaReceta,
            CASE WHEN r.firmada = 'S' THEN true ELSE false END,
            COALESCE((SELECT COUNT(rd) FROM RecetaDet rd WHERE rd.recetaId = r.id AND rd.activo = 'S'), 0)
        )
        FROM RecetaCab r 
        WHERE r.pacienteId = :pacienteId 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaBasicaInfo> findRecetasBasicaInfoByPaciente(@Param("pacienteId") Long pacienteId);

    /**
     * LISTAR: Info básica por médico
     */
    @Query("""
        SELECT new com.formacionbdi.microservicios.app.receta.models.dto.RecetaBasicaInfo(
            r.id, 
            r.numeroReceta, 
            CONCAT('Paciente ID ', r.pacienteId),
            CONCAT('Médico ID ', r.medicoId),
            r.estado,
            CASE r.estado 
                WHEN '01' THEN 'Activa'
                WHEN '02' THEN 'Despachada'
                WHEN '03' THEN 'Vencida'
                WHEN '04' THEN 'Anulada'
                ELSE 'Desconocido'
            END,
            r.fechaReceta,
            CASE WHEN r.firmada = 'S' THEN true ELSE false END,
            COALESCE((SELECT COUNT(rd) FROM RecetaDet rd WHERE rd.recetaId = r.id AND rd.activo = 'S'), 0)
        )
        FROM RecetaCab r 
        WHERE r.medicoId = :medicoId 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaBasicaInfo> findRecetasBasicaInfoByMedico(@Param("medicoId") Long medicoId);

    // =====================================================
    // ESTADÍSTICAS Y MÉTRICAS
    // =====================================================

    /**
     * CONTAR: Recetas por estado
     */
    @Query("""
        SELECT r.estado, COUNT(r) 
        FROM RecetaCab r 
        WHERE r.activo = 'S' 
        GROUP BY r.estado
        """)
    List<Object[]> contarRecetasPorEstado();

    /**
     * ESTADÍSTICAS: Por médico (formato optimizado)
     */
    @Query("""
        SELECT COUNT(r), 
        SUM(CASE WHEN r.estado = '01' THEN 1 ELSE 0 END), 
        SUM(CASE WHEN r.estado = '02' THEN 1 ELSE 0 END), 
        SUM(CASE WHEN r.estado = '03' THEN 1 ELSE 0 END), 
        SUM(CASE WHEN r.estado = '04' THEN 1 ELSE 0 END), 
        SUM(CASE WHEN r.firmada = 'S' THEN 1 ELSE 0 END)
        FROM RecetaCab r 
        WHERE r.medicoId = :medicoId 
        AND r.activo = 'S'
        """)
    Object[] obtenerEstadisticasMedico(@Param("medicoId") Long medicoId);

    /**
     * CONTAR: Recetas del día actual
     */
    @Query("""
        SELECT COUNT(r) 
        FROM RecetaCab r 
        WHERE DATE(r.fechaReceta) = CURRENT_DATE 
        AND r.activo = 'S'
        """)
    Long contarRecetasHoy();

    /**
     * CONTAR: Recetas pendientes de firma
     */
    @Query("""
        SELECT COUNT(r) 
        FROM RecetaCab r 
        WHERE r.firmada = 'N' 
        AND r.estado = '01' 
        AND r.activo = 'S'
        """)
    Long contarRecetasPendientesFirma();

    // =====================================================
    // OPERACIONES DE MANTENIMIENTO
    // =====================================================

    /**
     * MANTENIMIENTO: Marcar recetas vencidas (Job automático)
     */
    @Modifying
    @Query("""
        UPDATE RecetaCab r 
        SET r.estado = '03', 
            r.actualizadoEn = CURRENT_TIMESTAMP 
        WHERE r.fechaVencimiento < CURRENT_DATE 
        AND r.estado = '01' 
        AND r.activo = 'S'
        """)
    int marcarRecetasVencidas();

    /**
     * LIMPIEZA: Soft delete de recetas muy antiguas (Job automático)
     */
    @Modifying
    @Query("""
        UPDATE RecetaCab r 
        SET r.activo = 'N', 
            r.actualizadoEn = CURRENT_TIMESTAMP 
        WHERE r.fechaReceta < :fechaLimite 
        AND r.estado IN ('03', '04') 
        AND r.activo = 'S'
        """)
    int archivarRecetasAntiguas(@Param("fechaLimite") LocalDateTime fechaLimite);

    // =====================================================
    // CONSULTAS ESPECÍFICAS PARA REPORTES
    // =====================================================

    /**
     * REPORTE: Recetas por rango de fechas
     */
    @Query("""
        SELECT r FROM RecetaCab r 
        WHERE r.fechaReceta BETWEEN :fechaInicio AND :fechaFin 
        AND r.activo = 'S' 
        ORDER BY r.fechaReceta DESC
        """)
    List<RecetaCab> findRecetasPorRangoFechas(@Param("fechaInicio") LocalDateTime fechaInicio,
                                              @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * REPORTE: Recetas por tipo de origen en rango
     */
    @Query("""
        SELECT r.tipoOrigen, COUNT(r) 
        FROM RecetaCab r 
        WHERE r.fechaReceta BETWEEN :fechaInicio AND :fechaFin 
        AND r.activo = 'S' 
        GROUP BY r.tipoOrigen 
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> reporteRecetasPorTipoOrigen(@Param("fechaInicio") LocalDateTime fechaInicio,
                                               @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * DASHBOARD: Métricas principales
     */
    @Query("""
        SELECT 
        COUNT(r) as total,
        SUM(CASE WHEN r.estado = '01' THEN 1 ELSE 0 END) as activas,
        SUM(CASE WHEN r.estado = '02' THEN 1 ELSE 0 END) as despachadas,
        SUM(CASE WHEN r.firmada = 'N' AND r.estado = '01' THEN 1 ELSE 0 END) as pendientesFirma,
        SUM(CASE WHEN DATE(r.fechaReceta) = CURRENT_DATE THEN 1 ELSE 0 END) as hoy
        FROM RecetaCab r 
        WHERE r.activo = 'S'
        """)
    Object[] obtenerMetricasDashboard();
}