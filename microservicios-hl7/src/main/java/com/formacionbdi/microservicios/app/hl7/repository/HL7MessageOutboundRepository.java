// =====================================================
// 📤 REPOSITORY 2: HL7 MENSAJES OUTBOUND
// =====================================================
package com.formacionbdi.microservicios.app.hl7.repository;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7MessageOutbound;
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
 * 📤 REPOSITORY OUTBOUND: Mensajes HL7 que ENVIAMOS
 * 🎯 Optimizado para: Demonio sender + Cola de envío
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot + PostgreSQL
 */
@Repository
public interface HL7MessageOutboundRepository extends JpaRepository<HL7MessageOutbound, Long> {

    // ===== QUERIES PARA EL DEMONIO SENDER =====

    /**
     * 🚀 CORE DEL DEMONIO: Cola de envío prioritaria
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.estado = '01' " +
            "AND h.intentosEnvio < h.maxIntentos " +
            "AND h.requiereIntervencion = 'N' " +
            "ORDER BY " +
            "CASE h.prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "h.fechaGeneracion ASC")
    List<HL7MessageOutbound> findMensajesPendientesEnvio();

    /**
     * 🔄 MENSAJES PARA REINTENTO
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.estado = '03' " +
            "AND h.intentosEnvio < h.maxIntentos " +
            "AND h.requiereIntervencion = 'N' " +
            "AND h.fechaEnvio < :tiempoReintento " +
            "ORDER BY " +
            "CASE h.prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "h.fechaEnvio ASC")
    List<HL7MessageOutbound> findMensajesParaReintento(@Param("tiempoReintento") LocalDateTime tiempoReintento);

    /**
     * 🎯 MENSAJES POR DESTINO (envío por sistema)
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.sistemaDestino = :sistemaDestino " +
            "AND h.estado = '01' " +
            "ORDER BY " +
            "CASE h.prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "h.fechaGeneracion ASC")
    List<HL7MessageOutbound> findMensajesPorDestino(@Param("sistemaDestino") String sistemaDestino);

    /**
     * 📋 MENSAJES POR PROCESO ORIGEN
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.procesoOrigen = :procesoOrigen " +
            "AND h.fechaGeneracion >= :desde " +
            "ORDER BY h.fechaGeneracion DESC")
    List<HL7MessageOutbound> findMensajesPorProceso(
            @Param("procesoOrigen") String procesoOrigen,
            @Param("desde") LocalDateTime desde
    );

    /**
     * 🔍 BUSCAR POR MENSAJE ID
     */
    Optional<HL7MessageOutbound> findByMensajeId(String mensajeId);

    /**
     * 👤 MENSAJES POR PACIENTE
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.pacienteId = :pacienteId " +
            "OR h.numeroDocumento = :numeroDocumento " +
            "ORDER BY h.fechaGeneracion DESC")
    List<HL7MessageOutbound> findMensajesPorPaciente(
            @Param("pacienteId") Long pacienteId,
            @Param("numeroDocumento") String numeroDocumento
    );

    // ===== ACTUALIZACIONES PARA EL DEMONIO =====

    /**
     * ✅ MARCAR COMO ENVIADO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7MessageOutbound h " +
            "SET h.estado = '02', " +
            "h.fechaEnvio = :fechaEnvio, " +
            "h.intentosEnvio = h.intentosEnvio + 1 " +
            "WHERE h.id = :id")
    int marcarComoEnviado(@Param("id") Long id, @Param("fechaEnvio") LocalDateTime fechaEnvio);

    /**
     * ❌ MARCAR ERROR DE ENVÍO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7MessageOutbound h " +
            "SET h.estado = '03', " +
            "h.ultimoError = :error, " +
            "h.intentosEnvio = h.intentosEnvio + 1, " +
            "h.requiereIntervencion = CASE " +
            "WHEN h.intentosEnvio + 1 >= h.maxIntentos THEN 'S' " +
            "ELSE 'N' END " +
            "WHERE h.id = :id")
    int marcarErrorEnvio(@Param("id") Long id, @Param("error") String error);

    /**
     * 🎯 CONFIRMAR ACK RECIBIDO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7MessageOutbound h " +
            "SET h.estado = '04', " +
            "h.ackRecibido = 'S', " +
            "h.ackCodigo = :ackCodigo, " +
            "h.ackMensaje = :ackMensaje, " +
            "h.fechaAck = :fechaAck " +
            "WHERE h.id = :id")
    int confirmarAckRecibido(
            @Param("id") Long id,
            @Param("ackCodigo") String ackCodigo,
            @Param("ackMensaje") String ackMensaje,
            @Param("fechaAck") LocalDateTime fechaAck
    );

    /**
     * 🔄 RESETEAR PARA REINTENTO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7MessageOutbound h " +
            "SET h.estado = '01', " +
            "h.ultimoError = NULL " +
            "WHERE h.id = :id " +
            "AND h.intentosEnvio < h.maxIntentos")
    int resetearParaReintento(@Param("id") Long id);

    // ===== CONSULTAS ESTADÍSTICAS =====

    /**
     * 📊 ESTADÍSTICAS POR DESTINO
     */
    @Query("SELECT h.sistemaDestino as destino, " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN h.estado = '02' THEN 1 END) as enviados, " +
            "COUNT(CASE WHEN h.estado = '04' THEN 1 END) as confirmados, " +
            "COUNT(CASE WHEN h.estado = '03' THEN 1 END) as errores, " +
            "COUNT(CASE WHEN h.requiereIntervencion = 'S' THEN 1 END) as requierenIntervencion " +
            "FROM HL7MessageOutbound h " +
            "WHERE h.fechaGeneracion >= :desde " +
            "GROUP BY h.sistemaDestino " +
            "ORDER BY h.sistemaDestino")
    List<Object[]> getEstadisticasPorDestino(@Param("desde") LocalDateTime desde);

    /**
     * 🚨 COLA DE PENDIENTES (alerta demonio)
     */
    @Query("SELECT COUNT(h) FROM HL7MessageOutbound h " +
            "WHERE h.estado = '01' " +
            "AND h.fechaGeneracion < :tiempoLimite")
    long countMensajesPendientesAntiguos(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    /**
     * 🔥 MENSAJES QUE REQUIEREN INTERVENCIÓN
     */
    @Query("SELECT COUNT(h) FROM HL7MessageOutbound h " +
            "WHERE h.requiereIntervencion = 'S' " +
            "AND h.estado = '03'")
    long countMensajesRequierenIntervencion();

    /**
     * 📈 ESTADÍSTICAS POR TIPO DE MENSAJE
     */
    @Query("SELECT h.tipoMensaje, COUNT(h) " +
            "FROM HL7MessageOutbound h " +
            "WHERE h.fechaGeneracion >= :desde " +
            "GROUP BY h.tipoMensaje " +
            "ORDER BY COUNT(h) DESC")
    List<Object[]> getEstadisticasPorTipo(@Param("desde") LocalDateTime desde);

    // ===== QUERIES NATIVAS OPTIMIZADAS =====

    /**
     * ⚡ OBTENER COLA DE ENVÍO (query súper optimizada)
     */
    @Query(value = "SELECT id, mensaje_id, tipo_mensaje, sistema_destino, " +
            "ip_destino, puerto_destino, mensaje_hl7, prioridad " +
            "FROM hl7_mensajes_outbound " +
            "WHERE estado = '01' " +
            "AND intentos_envio < max_intentos " +
            "AND requiere_intervencion = 'N' " +
            "ORDER BY " +
            "CASE prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "fecha_generacion ASC " +
            "LIMIT :limite " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Object[]> getColaEnvioOptimizada(@Param("limite") int limite);

    /**
     * 📈 THROUGHPUT POR HORA
     */
    @Query(value = "SELECT DATE_TRUNC('hour', fecha_envio) as hora, " +
            "COUNT(*) as mensajesEnviados " +
            "FROM hl7_mensajes_outbound " +
            "WHERE fecha_envio >= :desde " +
            "AND estado IN ('02', '04') " +
            "GROUP BY DATE_TRUNC('hour', fecha_envio) " +
            "ORDER BY hora DESC",
            nativeQuery = true)
    List<Object[]> getThroughputPorHora(@Param("desde") LocalDateTime desde);

    /**
     * 🧹 LIMPIAR MENSAJES CONFIRMADOS ANTIGUOS
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM hl7_mensajes_outbound " +
            "WHERE estado = '04' " +
            "AND fecha_ack < :fechaLimite",
            nativeQuery = true)
    int limpiarMensajesConfirmadosAntiguos(@Param("fechaLimite") LocalDateTime fechaLimite);

    // ===== QUERIES ESPECÍFICAS PARA EL DEMONIO SENDER =====

    /**
     * 🎯 OBTENER LOTE PARA ENVÍO (optimizada para demonio sender)
     */
    @Query(value = "SELECT * FROM hl7_mensajes_outbound " +
            "WHERE estado = '01' " +
            "AND intentos_envio < max_intentos " +
            "AND requiere_intervencion = 'N' " +
            "ORDER BY " +
            "CASE prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "fecha_generacion ASC " +
            "LIMIT :limite " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Object[]> obtenerLoteParaEnvio(@Param("limite") int limite);

    /**
     * ⏰ MENSAJES PARA REINTENTO BASADO EN TIEMPO
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.estado = '03' " +
            "AND h.intentosEnvio < h.maxIntentos " +
            "AND h.requiereIntervencion = 'N' " +
            "AND h.fechaEnvio <= :tiempoReintento " +
            "ORDER BY h.prioridad ASC, h.intentosEnvio ASC")
    List<HL7MessageOutbound> findMensajesParaReintentoTiempo(@Param("tiempoReintento") LocalDateTime tiempoReintento);

    /**
     * 🔍 VALIDAR MENSAJE DUPLICADO OUTBOUND
     */
    @Query("SELECT COUNT(h) FROM HL7MessageOutbound h WHERE h.mensajeId = :mensajeId")
    long existeMensajeIdOutbound(@Param("mensajeId") String mensajeId);

    /**
     * 📊 RESUMEN DE ESTADO DE COLA
     */
    @Query("SELECT h.estado, COUNT(h) " +
            "FROM HL7MessageOutbound h " +
            "GROUP BY h.estado " +
            "ORDER BY h.estado")
    List<Object[]> getResumenEstadoCola();

    /**
     * 🎯 MENSAJES POR RANGO DE FECHAS
     */
    @Query("SELECT h FROM HL7MessageOutbound h " +
            "WHERE h.fechaGeneracion BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY h.fechaGeneracion DESC")
    List<HL7MessageOutbound> findMensajesPorRangoFechas(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}