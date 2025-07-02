// =====================================================
// ⚙️ REPOSITORY 3: HL7 CONFIGURACIÓN EQUIPOS
// =====================================================
package com.formacionbdi.microservicios.app.hl7.repository;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7EquipoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ⚙️ REPOSITORY CONFIG: Configuración de equipos médicos HL7
 * 🎯 Optimizado para: Validación rápida + Monitoreo
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot + PostgreSQL
 */
@Repository
public interface HL7EquipoConfigRepository extends JpaRepository<HL7EquipoConfig, Long> {

    // ===== QUERIES PARA VALIDACIÓN RÁPIDA =====

    /**
     * 🔍 BUSCAR EQUIPO POR CÓDIGO (validación demonio)
     */
    Optional<HL7EquipoConfig> findByCodigoEquipo(String codigoEquipo);

    /**
     * 🌐 VALIDAR POR IP Y APLICACIÓN (seguridad)
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.ipPermitida = :ip " +
            "AND e.aplicacionNombre = :aplicacion " +
            "AND e.activo = 'S' " +
            "AND e.enMantenimiento = 'N'")
    Optional<HL7EquipoConfig> findByIpYAplicacion(
            @Param("ip") String ip,
            @Param("aplicacion") String aplicacion
    );

    /**
     * 🏥 EQUIPOS ACTIVOS POR SERVICIO
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.servicio = :servicio " +
            "AND e.activo = 'S' " +
            "AND e.enMantenimiento = 'N' " +
            "ORDER BY e.numeroCama ASC")
    List<HL7EquipoConfig> findEquiposActivosPorServicio(@Param("servicio") String servicio);

    /**
     * 📍 EQUIPOS POR UBICACIÓN (piso + ala)
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.piso = :piso " +
            "AND (:ala IS NULL OR e.ala = :ala) " +
            "AND e.activo = 'S' " +
            "ORDER BY e.numeroCama ASC")
    List<HL7EquipoConfig> findEquiposPorUbicacion(
            @Param("piso") Integer piso,
            @Param("ala") String ala
    );

    /**
     * 🔍 BUSCAR POR APLICACIÓN NOMBRE
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.aplicacionNombre = :aplicacionNombre " +
            "AND e.activo = 'S'")
    List<HL7EquipoConfig> findByAplicacionNombre(@Param("aplicacionNombre") String aplicacionNombre);

    /**
     * 🏭 EQUIPOS POR FABRICANTE
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.fabricante = :fabricante " +
            "AND e.activo = 'S' " +
            "ORDER BY e.servicio, e.numeroCama")
    List<HL7EquipoConfig> findEquiposPorFabricante(@Param("fabricante") String fabricante);

    // ===== QUERIES PARA MONITOREO =====

    /**
     * 🚨 EQUIPOS SIN COMUNICACIÓN (alertas)
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND e.enMantenimiento = 'N' " +
            "AND (e.ultimoMensaje IS NULL " +
            "OR e.ultimoMensaje < :tiempoLimite) " +
            "ORDER BY e.servicio, e.numeroCama")
    List<HL7EquipoConfig> findEquiposSinComunicacion(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    /**
     * 📊 EQUIPOS CON ALTA TASA DE ERROR
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND e.totalMensajesEnviados > 10 " +
            "AND (e.totalMensajesError * 1.0 / e.totalMensajesEnviados) > 0.1 " +
            "ORDER BY (e.totalMensajesError * 1.0 / e.totalMensajesEnviados) DESC")
    List<HL7EquipoConfig> findEquiposConAltaTasaError();

    /**
     * 🔧 EQUIPOS EN MANTENIMIENTO
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.enMantenimiento = 'S' " +
            "ORDER BY e.servicio, e.numeroCama")
    List<HL7EquipoConfig> findEquiposEnMantenimiento();

    /**
     * ✅ EQUIPOS ACTIVOS Y OPERATIVOS
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND e.enMantenimiento = 'N' " +
            "ORDER BY e.servicio, e.numeroCama")
    List<HL7EquipoConfig> findEquiposOperativos();

    /**
     * ⏰ EQUIPOS CON ACTIVIDAD RECIENTE
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND e.ultimoMensaje >= :tiempoLimite " +
            "ORDER BY e.ultimoMensaje DESC")
    List<HL7EquipoConfig> findEquiposConActividadReciente(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    // ===== ACTUALIZACIONES PARA EL DEMONIO =====

    /**
     * ✅ REGISTRAR MENSAJE RECIBIDO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7EquipoConfig e " +
            "SET e.ultimoMensaje = :fechaMensaje, " +
            "e.totalMensajesEnviados = e.totalMensajesEnviados + 1, " +
            "e.actualizadoEn = :fechaActualizacion " +
            "WHERE e.id = :id")
    int registrarMensajeRecibido(
            @Param("id") Long id,
            @Param("fechaMensaje") LocalDateTime fechaMensaje,
            @Param("fechaActualizacion") LocalDateTime fechaActualizacion
    );

    /**
     * ❌ REGISTRAR ERROR
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7EquipoConfig e " +
            "SET e.totalMensajesError = e.totalMensajesError + 1, " +
            "e.actualizadoEn = :fechaActualizacion " +
            "WHERE e.id = :id")
    int registrarError(
            @Param("id") Long id,
            @Param("fechaActualizacion") LocalDateTime fechaActualizacion
    );

    /**
     * 🔧 MARCAR EN MANTENIMIENTO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7EquipoConfig e " +
            "SET e.enMantenimiento = :estado, " +
            "e.actualizadoEn = :fechaActualizacion " +
            "WHERE e.id = :id")
    int cambiarEstadoMantenimiento(
            @Param("id") Long id,
            @Param("estado") String estado,
            @Param("fechaActualizacion") LocalDateTime fechaActualizacion
    );

    /**
     * 🔄 RESETEAR CONTADORES
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7EquipoConfig e " +
            "SET e.totalMensajesEnviados = 0, " +
            "e.totalMensajesError = 0, " +
            "e.actualizadoEn = :fechaActualizacion " +
            "WHERE e.id = :id")
    int resetearContadores(
            @Param("id") Long id,
            @Param("fechaActualizacion") LocalDateTime fechaActualizacion
    );

    // ===== CONSULTAS ESTADÍSTICAS =====

    /**
     * 📈 RESUMEN GENERAL DE EQUIPOS
     */
    @Query("SELECT e.servicio, " +
            "COUNT(*) as totalEquipos, " +
            "COUNT(CASE WHEN e.activo = 'S' THEN 1 END) as activos, " +
            "COUNT(CASE WHEN e.enMantenimiento = 'S' THEN 1 END) as enMantenimiento, " +
            "COUNT(CASE WHEN e.ultimoMensaje IS NULL " +
            "OR e.ultimoMensaje < :tiempoLimite THEN 1 END) as sinComunicacion " +
            "FROM HL7EquipoConfig e " +
            "GROUP BY e.servicio " +
            "ORDER BY e.servicio")
    List<Object[]> getResumenPorServicio(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    /**
     * 🏆 TOP EQUIPOS POR MENSAJES
     */
    @Query("SELECT e.codigoEquipo, e.nombreEquipo, e.servicio, " +
            "e.totalMensajesEnviados, e.totalMensajesError, " +
            "e.ultimoMensaje " +
            "FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "ORDER BY e.totalMensajesEnviados DESC")
    List<Object[]> getTopEquiposPorMensajes();

    /**
     * 📊 ESTADÍSTICAS POR FABRICANTE
     */
    @Query("SELECT e.fabricante, " +
            "COUNT(*) as totalEquipos, " +
            "COUNT(CASE WHEN e.activo = 'S' THEN 1 END) as activos, " +
            "SUM(e.totalMensajesEnviados) as totalMensajes, " +
            "SUM(e.totalMensajesError) as totalErrores " +
            "FROM HL7EquipoConfig e " +
            "GROUP BY e.fabricante " +
            "ORDER BY e.fabricante")
    List<Object[]> getEstadisticasPorFabricante();

    /**
     * 🔥 EQUIPOS QUE REQUIEREN ATENCIÓN
     */
    @Query("SELECT COUNT(e) FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND (e.enMantenimiento = 'S' " +
            "OR e.ultimoMensaje IS NULL " +
            "OR e.ultimoMensaje < :tiempoLimite " +
            "OR (e.totalMensajesEnviados > 10 " +
            "AND (e.totalMensajesError * 1.0 / e.totalMensajesEnviados) > 0.1))")
    long countEquiposRequierenAtencion(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    // ===== QUERIES NATIVAS PARA PERFORMANCE =====

    /**
     * ⚡ VALIDACIÓN RÁPIDA DE EQUIPO (query optimizada)
     */
    @Query(value = "SELECT id, codigo_equipo, aplicacion_nombre, tipos_mensaje_permitidos " +
            "FROM hl7_equipos_config " +
            "WHERE ip_permitida = :ip " +
            "AND aplicacion_nombre = :aplicacion " +
            "AND activo = 'S' " +
            "AND en_mantenimiento = 'N' " +
            "LIMIT 1",
            nativeQuery = true)
    Object[] validarEquipoRapido(@Param("ip") String ip, @Param("aplicacion") String aplicacion);

    /**
     * 📊 DASHBOARD DE MONITOREO (query única para dashboard)
     */
    @Query(value = "SELECT " +
            "servicio, " +
            "COUNT(*) as total_equipos, " +
            "COUNT(CASE WHEN activo = 'S' THEN 1 END) as activos, " +
            "COUNT(CASE WHEN en_mantenimiento = 'S' THEN 1 END) as mantenimiento, " +
            "COUNT(CASE WHEN ultimo_mensaje > NOW() - INTERVAL '10 minutes' THEN 1 END) as conectados, " +
            "AVG(total_mensajes_enviados) as promedio_mensajes, " +
            "SUM(total_mensajes_error) as total_errores " +
            "FROM hl7_equipos_config " +
            "WHERE activo = 'S' " +
            "GROUP BY servicio " +
            "ORDER BY servicio",
            nativeQuery = true)
    List<Object[]> getDashboardMonitoreo();

    /**
     * 🚨 ALERTAS CRÍTICAS (query optimizada)
     */
    @Query(value = "SELECT codigo_equipo, nombre_equipo, servicio, ultimo_mensaje, " +
            "total_mensajes_error, total_mensajes_enviados " +
            "FROM hl7_equipos_config " +
            "WHERE activo = 'S' " +
            "AND en_mantenimiento = 'N' " +
            "AND (ultimo_mensaje IS NULL " +
            "OR ultimo_mensaje < NOW() - INTERVAL '30 minutes' " +
            "OR (total_mensajes_enviados > 10 " +
            "AND total_mensajes_error::float / total_mensajes_enviados > 0.2)) " +
            "ORDER BY " +
            "CASE " +
            "WHEN ultimo_mensaje IS NULL THEN 1 " +
            "WHEN ultimo_mensaje < NOW() - INTERVAL '60 minutes' THEN 2 " +
            "ELSE 3 END, " +
            "servicio, numero_cama",
            nativeQuery = true)
    List<Object[]> getAlertasCriticas();

    // ===== QUERIES ESPECÍFICAS PARA EL DEMONIO =====

    /**
     * 🎯 EQUIPOS PARA HEARTBEAT CHECK
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND e.enMantenimiento = 'N' " +
            "AND e.frecuenciaEnvioSegundos IS NOT NULL " +
            "ORDER BY e.servicio, e.numeroCama")
    List<HL7EquipoConfig> findEquiposParaHeartbeat();

    /**
     * ⏰ EQUIPOS QUE DEBERÍAN HABER ENVIADO MENSAJE
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE e.activo = 'S' " +
            "AND e.enMantenimiento = 'N' " +
            "AND e.frecuenciaEnvioSegundos IS NOT NULL " +
            "AND (e.ultimoMensaje IS NULL " +
            "OR e.ultimoMensaje < :tiempoEsperado) " +
            "ORDER BY e.ultimoMensaje ASC NULLS FIRST")
    List<HL7EquipoConfig> findEquiposRetrasados(@Param("tiempoEsperado") LocalDateTime tiempoEsperado);

    /**
     * 🔍 BUSCAR EQUIPOS POR MÚLTIPLES CRITERIOS
     */
    @Query("SELECT e FROM HL7EquipoConfig e " +
            "WHERE (:servicio IS NULL OR e.servicio = :servicio) " +
            "AND (:fabricante IS NULL OR e.fabricante = :fabricante) " +
            "AND (:activo IS NULL OR e.activo = :activo) " +
            "ORDER BY e.servicio, e.numeroCama")
    List<HL7EquipoConfig> findEquiposPorCriterios(
            @Param("servicio") String servicio,
            @Param("fabricante") String fabricante,
            @Param("activo") String activo
    );
}