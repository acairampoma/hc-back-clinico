package com.formacionbdi.microservicios.app.hl7.repository;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7Message;
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
 * 📡 REPOSITORY INBOUND: Mensajes HL7 que RECIBIMOS
 * 🎯 Optimizado para: Demonio procesador + Consultas rápidas
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot + PostgreSQL
 */
@Repository
public interface HL7MessageRepository extends JpaRepository<HL7Message, Long> {

    // ===== QUERIES PARA EL DEMONIO PROCESADOR =====

    /**
     * 🔄 CORE DEL DEMONIO: Buscar mensajes pendientes de procesar
     * Estado '01' = Recibido pero no procesado
     */
    @Query("SELECT h FROM HL7Message h " +
            "WHERE h.estado = '01' " +
            "AND h.esValido = 'S' " +
            "ORDER BY " +
            "CASE h.prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "h.fechaRecepcion ASC")
    List<HL7Message> findMensajesPendientesProcesamiento();

    /**
     * 🚨 MENSAJES DE EMERGENCIA: Prioridad E y U primero
     */
    @Query("SELECT h FROM HL7Message h " +
            "WHERE h.estado = '01' " +
            "AND h.prioridad IN ('E', 'U') " +
            "AND h.esValido = 'S' " +
            "ORDER BY " +
            "CASE h.prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "h.fechaRecepcion ASC")
    List<HL7Message> findMensajesUrgentes();

    /**
     * 🔍 BUSCAR POR CONTROL ID (evitar duplicados)
     */
    Optional<HL7Message> findByMensajeId(String mensajeId);

    /**
     * 👤 BUSCAR MENSAJES POR PACIENTE
     */
    @Query("SELECT h FROM HL7Message h " +
            "WHERE h.pacienteId = :pacienteId " +
            "OR h.numeroDocumento = :numeroDocumento " +
            "ORDER BY h.fechaRecepcion DESC")
    List<HL7Message> findMensajesPorPaciente(
            @Param("pacienteId") Long pacienteId,
            @Param("numeroDocumento") String numeroDocumento
    );

    /**
     * 🏥 BUSCAR POR EQUIPO ORIGEN (monitoreo equipos)
     */
    @Query("SELECT h FROM HL7Message h " +
            "WHERE h.equipoOrigen = :equipoOrigen " +
            "AND h.fechaRecepcion >= :desde " +
            "ORDER BY h.fechaRecepcion DESC")
    List<HL7Message> findMensajesPorEquipo(
            @Param("equipoOrigen") String equipoOrigen,
            @Param("desde") LocalDateTime desde
    );

    /**
     * 🔥 MENSAJES SIN PROCESAR (alerta demonio)
     */
    @Query("SELECT COUNT(h) FROM HL7Message h " +
            "WHERE h.estado = '01' " +
            "AND h.fechaRecepcion < :tiempoLimite")
    long countMensajesSinProcesar(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    /**
     * 🩺 MENSAJES POR TIPO (monitoreo)
     */
    @Query("SELECT h.tipoMensaje, COUNT(h) " +
            "FROM HL7Message h " +
            "WHERE h.fechaRecepcion >= :desde " +
            "GROUP BY h.tipoMensaje " +
            "ORDER BY COUNT(h) DESC")
    List<Object[]> getEstadisticasPorTipo(@Param("desde") LocalDateTime desde);

    // ===== ACTUALIZACIONES PARA EL DEMONIO =====

    /**
     * ✅ MARCAR COMO PROCESADO
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7Message h " +
            "SET h.estado = '02', " +
            "h.fechaProcesamiento = :fechaProcesamiento, " +
            "h.tablaDestino = :tablaDestino, " +
            "h.registroDestinoId = :registroDestinoId, " +
            "h.procesadoPor = :procesadoPor " +
            "WHERE h.id = :id")
    int marcarComoProcesado(
            @Param("id") Long id,
            @Param("fechaProcesamiento") LocalDateTime fechaProcesamiento,
            @Param("tablaDestino") String tablaDestino,
            @Param("registroDestinoId") Long registroDestinoId,
            @Param("procesadoPor") String procesadoPor
    );

    /**
     * ❌ MARCAR COMO ERROR
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7Message h " +
            "SET h.estado = '03', " +
            "h.ultimoError = :error, " +
            "h.intentosProcesamiento = h.intentosProcesamiento + 1, " +
            "h.requiereIntervencion = CASE " +
            "WHEN h.intentosProcesamiento >= 3 THEN 'S' " +
            "ELSE 'N' END " +
            "WHERE h.id = :id")
    int marcarComoError(@Param("id") Long id, @Param("error") String error);

    /**
     * 📤 ENVIAR ACK/NACK
     */
    @Modifying
    @Transactional
    @Query("UPDATE HL7Message h " +
            "SET h.ackEnviado = 'S', " +
            "h.ackCodigo = :ackCodigo, " +
            "h.ackMensaje = :ackMensaje, " +
            "h.fechaAck = :fechaAck " +
            "WHERE h.id = :id")
    int registrarAck(
            @Param("id") Long id,
            @Param("ackCodigo") String ackCodigo,
            @Param("ackMensaje") String ackMensaje,
            @Param("fechaAck") LocalDateTime fechaAck
    );

    // ===== CONSULTAS ESTADÍSTICAS =====

    /**
     * 📊 ESTADÍSTICAS POR EQUIPO (últimas 24 horas)
     */
    @Query("SELECT h.equipoOrigen as equipo, " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN h.estado = '02' THEN 1 END) as procesados, " +
            "COUNT(CASE WHEN h.estado = '03' THEN 1 END) as errores, " +
            "MAX(h.fechaRecepcion) as ultimoMensaje " +
            "FROM HL7Message h " +
            "WHERE h.fechaRecepcion >= :desde " +
            "GROUP BY h.equipoOrigen " +
            "ORDER BY h.equipoOrigen")
    List<Object[]> getEstadisticasPorEquipo(@Param("desde") LocalDateTime desde);

    // ===== QUERIES NATIVAS PARA PERFORMANCE =====

    /**
     * ⚡ QUERY NATIVA: Buscar mensajes para procesar (súper rápido)
     */
    @Query(value = "SELECT id, mensaje_id, tipo_mensaje, equipo_origen, mensaje_raw, " +
            "mensaje_parseado, paciente_id, numero_documento, numero_cuenta " +
            "FROM hl7_mensajes " +
            "WHERE estado = '01' " +
            "AND es_valido = 'S' " +
            "AND fecha_recepcion >= NOW() - INTERVAL '1 hour' " +
            "ORDER BY " +
            "CASE prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "fecha_recepcion ASC " +
            "LIMIT :limite",
            nativeQuery = true)
    List<Object[]> findMensajesPendientesNativo(@Param("limite") int limite);

    /**
     * 🧹 LIMPIAR MENSAJES ANTIGUOS (mantenimiento)
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM hl7_mensajes " +
            "WHERE estado = '02' " +
            "AND fecha_procesamiento < :fechaLimite",
            nativeQuery = true)
    int limpiarMensajesAntiguos(@Param("fechaLimite") LocalDateTime fechaLimite);

    // ===== QUERIES ESPECÍFICAS PARA EL DEMONIO 24/7 =====

    /**
     * 🎯 OBTENER LOTE PARA PROCESAMIENTO (optimizada para demonio)
     */
    @Query(value = "SELECT * FROM hl7_mensajes " +
            "WHERE estado = '01' " +
            "AND es_valido = 'S' " +
            "ORDER BY " +
            "CASE prioridad " +
            "WHEN 'E' THEN 1 " +
            "WHEN 'U' THEN 2 " +
            "ELSE 3 END, " +
            "fecha_recepcion ASC " +
            "LIMIT :limite " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Object[]> obtenerLoteParaProcesamiento(@Param("limite") int limite);

    /**
     * 🔍 VALIDAR MENSAJE DUPLICADO RÁPIDO
     */
    @Query("SELECT COUNT(h) FROM HL7Message h WHERE h.mensajeId = :mensajeId")
    long existeMensajeId(@Param("mensajeId") String mensajeId);

    /**
     * ⏰ MENSAJES RECIENTES POR EQUIPO (para heartbeat)
     */
    @Query("SELECT h FROM HL7Message h " +
            "WHERE h.equipoOrigen = :equipoOrigen " +
            "AND h.fechaRecepcion >= :desde " +
            "ORDER BY h.fechaRecepcion DESC")
    List<HL7Message> findMensajesRecientesPorEquipo(
            @Param("equipoOrigen") String equipoOrigen,
            @Param("desde") LocalDateTime desde
    );
}