package com.formacionbdi.microservicios.app.notas.repository;

import com.formacionbdi.microservicios.app.notas.models.entity.HospitalizacionNota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gestión de notas de hospitalización
 */
@Repository
public interface HospitalizacionNotaRepository extends JpaRepository<HospitalizacionNota, Long> {

    // ===== 🔒 VALIDACIONES CRÍTICAS =====

    /**
     * Verifica si un médico puede crear una nueva nota para una hospitalización
     * Regla de negocio: Solo puede haber UNA nota por médico por hospitalización en estado BORRADOR
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN false ELSE true END " +
            "FROM hospitalizacion_notas " +
            "WHERE hospitalizacion_id = :hospitalizacionId " +
            "AND creado_por = :medicoId " +
            "AND estado = '06'",
            nativeQuery = true)
    boolean puedeCrearNota(@Param("hospitalizacionId") Long hospitalizacionId,
                           @Param("medicoId") Long medicoId);

    /**
     * Cuenta notas en borrador por médico y hospitalización
     */
    @Query("SELECT COUNT(n) FROM HospitalizacionNota n " +
            "WHERE n.hospitalizacionId = :hospitalizacionId " +
            "AND n.creadoPor = :medicoId " +
            "AND n.estado = '01'")
    Long contarNotasBorradorPorMedicoYHospitalizacion(@Param("hospitalizacionId") Long hospitalizacionId,
                                                      @Param("medicoId") Long medicoId);

    // ===== 📖 CONSULTAS PRINCIPALES =====

    /**
     * Obtiene todas las notas de una hospitalización ordenadas por fecha
     */
    @Query("SELECT n FROM HospitalizacionNota n " +
            "WHERE n.hospitalizacionId = :hospitalizacionId " +
            "ORDER BY n.fechaNota DESC, n.creadoEn DESC")
    List<HospitalizacionNota> findByHospitalizacionIdOrderByFecha(@Param("hospitalizacionId") Long hospitalizacionId);

    /**
     * Obtiene notas finalizadas de una hospitalización
     */
    @Query("SELECT n FROM HospitalizacionNota n " +
            "WHERE n.hospitalizacionId = :hospitalizacionId " +
            "AND n.estado = '02' " +
            "ORDER BY n.fechaNota DESC")
    List<HospitalizacionNota> findNotasFinalizadasPorHospitalizacion(@Param("hospitalizacionId") Long hospitalizacionId);

    /**
     * Obtiene notas en borrador de una hospitalización
     */
    @Query("SELECT n FROM HospitalizacionNota n " +
            "WHERE n.hospitalizacionId = :hospitalizacionId " +
            "AND n.estado = '01' " +
            "ORDER BY n.creadoEn DESC")
    List<HospitalizacionNota> findNotasBorradorPorHospitalizacion(@Param("hospitalizacionId") Long hospitalizacionId);

    // ===== 🎯 CONSULTAS ESPECÍFICAS =====

    /**
     * Busca notas por número de cuenta
     */
    @Query("SELECT n FROM HospitalizacionNota n " +
            "WHERE n.numeroCuenta = :numeroCuenta " +
            "ORDER BY n.fechaNota DESC")
    List<HospitalizacionNota> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    /**
     * Busca notas por tipo específico
     */
    @Query("SELECT n FROM HospitalizacionNota n " +
            "WHERE n.hospitalizacionId = :hospitalizacionId " +
            "AND n.tipoNota = :tipoNota " +
            "ORDER BY n.fechaNota DESC")
    List<HospitalizacionNota> findByHospitalizacionYTipo(@Param("hospitalizacionId") Long hospitalizacionId,
                                                         @Param("tipoNota") String tipoNota);

    /**
     * Busca notas por médico en un rango de fechas
     */
    @Query("SELECT n FROM HospitalizacionNota n " +
            "WHERE n.creadoPor = :medicoId " +
            "AND n.fechaNota BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY n.fechaNota DESC")
    List<HospitalizacionNota> findByMedicoYRangoFechas(@Param("medicoId") Long medicoId,
                                                       @Param("fechaInicio") LocalDateTime fechaInicio,
                                                       @Param("fechaFin") LocalDateTime fechaFin);

    // ===== 🧹 AUTO-LIMPIEZA DE AUDIO =====

    /**
     * Encuentra notas con audio que deben ser limpiadas
     * (Notas finalizadas con audio de más de X días)
     */
    @Query(value = "SELECT * FROM hospitalizacion_notas " +
            "WHERE estado = '02' " +
            "AND audio_data IS NOT NULL " +
            "AND audio_data->>'tiene_audio' = 'true' " +
            "AND (audio_data->>'audio_eliminado')::boolean = false " +
            "AND fecha_nota < :fechaLimite",
            nativeQuery = true)
    List<HospitalizacionNota> findNotasParaLimpiezaAudio(@Param("fechaLimite") LocalDateTime fechaLimite);

    // ===== 📊 ESTADÍSTICAS =====

    /**
     * Cuenta notas por estado en una hospitalización
     */
    @Query("SELECT n.estado, COUNT(n) FROM HospitalizacionNota n " +
            "WHERE n.hospitalizacionId = :hospitalizacionId " +
            "GROUP BY n.estado")
    List<Object[]> contarNotasPorEstado(@Param("hospitalizacionId") Long hospitalizacionId);

    /**
     * Cuenta notas con firma digital
     */
    @Query(value = "SELECT COUNT(*) FROM hospitalizacion_notas " +
            "WHERE hospitalizacion_id = :hospitalizacionId " +
            "AND firma_digital IS NOT NULL " +
            "AND firma_digital->>'tiene_firma' = 'true'",
            nativeQuery = true)
    Long contarNotasConFirma(@Param("hospitalizacionId") Long hospitalizacionId);
}