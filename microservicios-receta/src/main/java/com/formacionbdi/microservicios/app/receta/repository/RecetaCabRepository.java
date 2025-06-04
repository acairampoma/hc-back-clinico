package com.formacionbdi.microservicios.app.receta.repository;


import com.formacionbdi.microservicios.app.receta.models.entity.RecetaCab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 📋 Repository para Receta Cabecera - Con consultas optimizadas
 */
@Repository
public interface RecetaCabRepository extends JpaRepository<RecetaCab, Long> {

    // ===== 🔒 VALIDACIONES CRÍTICAS =====

    /**
     * Verifica si ya existe receta del mismo origen en el mismo día
     * REGLA: No se puede crear receta del mismo origen en el mismo día
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RecetaCab r " +
            "WHERE r.tipoOrigen = :tipoOrigen " +
            "AND r.origenId = :origenId " +
            "AND DATE(r.fechaReceta) = CURRENT_DATE " +
            "AND r.activo = 'S'")
    boolean existeRecetaMismoOrigenHoy(@Param("tipoOrigen") String tipoOrigen,
                                       @Param("origenId") Long origenId);

    /**
     * Cuenta recetas activas por paciente (para validaciones)
     */
    @Query("SELECT COUNT(r) FROM RecetaCab r " +
            "WHERE r.pacienteId = :pacienteId " +
            "AND r.estado = '01' " +
            "AND r.activo = 'S'")
    Long contarRecetasActivasPorPaciente(@Param("pacienteId") Long pacienteId);

    // ===== 📖 CONSULTAS PRINCIPALES =====

    /**
     * Busca receta por número
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.numeroReceta = :numeroReceta " +
            "AND r.activo = 'S'")
    Optional<RecetaCab> findByNumeroReceta(@Param("numeroReceta") String numeroReceta);

    /**
     * Obtiene recetas por tipo de origen y origen ID
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.tipoOrigen = :tipoOrigen " +
            "AND r.origenId = :origenId " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaReceta DESC")
    List<RecetaCab> findByTipoOrigenAndOrigenId(@Param("tipoOrigen") String tipoOrigen,
                                                @Param("origenId") Long origenId);

    /**
     * Obtiene todas las recetas de un paciente
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.pacienteId = :pacienteId " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaReceta DESC")
    List<RecetaCab> findByPacienteId(@Param("pacienteId") Long pacienteId);

    /**
     * Obtiene recetas creadas por un médico
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.medicoId = :medicoId " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaReceta DESC")
    List<RecetaCab> findByMedicoId(@Param("medicoId") Long medicoId);

    // ===== 🎯 CONSULTAS CON FILTROS =====

    /**
     * Búsqueda con filtros múltiples
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE (:estado IS NULL OR r.estado = :estado) " +
            "AND (:tipoOrigen IS NULL OR r.tipoOrigen = :tipoOrigen) " +
            "AND (:origenId IS NULL OR r.origenId = :origenId) " +
            "AND (:pacienteId IS NULL OR r.pacienteId = :pacienteId) " +
            "AND (:medicoId IS NULL OR r.medicoId = :medicoId) " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaReceta DESC")
    List<RecetaCab> findWithFilters(@Param("estado") String estado,
                                    @Param("tipoOrigen") String tipoOrigen,
                                    @Param("origenId") Long origenId,
                                    @Param("pacienteId") Long pacienteId,
                                    @Param("medicoId") Long medicoId);

    /**
     * Recetas por médico en rango de fechas
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.medicoId = :medicoId " +
            "AND r.fechaReceta BETWEEN :fechaInicio AND :fechaFin " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaReceta DESC")
    List<RecetaCab> findByMedicoIdAndFechasBetween(@Param("medicoId") Long medicoId,
                                                   @Param("fechaInicio") LocalDateTime fechaInicio,
                                                   @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Recetas vencidas
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.fechaVencimiento < CURRENT_DATE " +
            "AND r.estado IN ('01', '02') " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaVencimiento ASC")
    List<RecetaCab> findRecetasVencidas();

    /**
     * Recetas pendientes de firma
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.firmada = 'N' " +
            "AND r.estado = '01' " +
            "AND r.activo = 'S' " +
            "ORDER BY r.fechaReceta DESC")
    List<RecetaCab> findRecetasPendientesFirma();

    // ===== ⏰ VALIDACIONES DE TIEMPO =====

    /**
     * Encuentra recetas que pueden ser modificadas (dentro de 24h antes de vencimiento)
     */
    @Query("SELECT r FROM RecetaCab r " +
            "WHERE r.id = :recetaId " +
            "AND r.fechaVencimiento >= CURRENT_DATE + 1 " +
            "AND r.estado = '01' " +
            "AND r.activo = 'S'")
    Optional<RecetaCab> findRecetaModificable(@Param("recetaId") Long recetaId);

    // ===== 📊 ESTADÍSTICAS =====

    /**
     * Cuenta recetas por estado
     */
    @Query("SELECT r.estado, COUNT(r) FROM RecetaCab r " +
            "WHERE r.activo = 'S' " +
            "GROUP BY r.estado")
    List<Object[]> contarRecetasPorEstado();

    /**
     * Estadísticas por médico
     */
    @Query("SELECT COUNT(r), " +
            "SUM(CASE WHEN r.estado = '01' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.estado = '02' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.firmada = 'S' THEN 1 ELSE 0 END) " +
            "FROM RecetaCab r " +
            "WHERE r.medicoId = :medicoId " +
            "AND r.activo = 'S'")
    Object[] obtenerEstadisticasMedico(@Param("medicoId") Long medicoId);

    /**
     * Verifica si ya existe receta del mismo origen en el mismo día con medicamentos duplicados
     * REGLA: No se puede crear receta del mismo origen en el mismo día con medicamentos duplicados
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RecetaCab r " +
            "JOIN RecetaDet rd ON r.id = rd.recetaId " +
            "WHERE r.tipoOrigen = :tipoOrigen " +
            "AND r.origenId = :origenId " +
            "AND DATE(r.fechaReceta) = CURRENT_DATE " +
            "AND r.estado = '01' " +
            "AND rd.medicamentoId IN :medicamentoIds " +
            "AND r.activo = 'S' " +
            "AND rd.activo = 'S'")
    boolean existeRecetaConMedicamentosDuplicadosHoy(@Param("tipoOrigen") String tipoOrigen,
                                                     @Param("origenId") Long origenId,
                                                     @Param("medicamentoIds") List<Long> medicamentoIds);

}
