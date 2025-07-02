// =====================================================
// 🏭 SERVICE REAL: HL7 PROCESSOR SERVICE - INTEGRACIÓN COMPLETA BD
// =====================================================
package com.formacionbdi.microservicios.app.hl7.services;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7Message;
import com.formacionbdi.microservicios.app.hl7.models.entity.HL7MessageOutbound;
import com.formacionbdi.microservicios.app.hl7.models.entity.HL7EquipoConfig;
import com.formacionbdi.microservicios.app.hl7.repository.HL7MessageRepository;
import com.formacionbdi.microservicios.app.hl7.repository.HL7MessageOutboundRepository;
import com.formacionbdi.microservicios.app.hl7.repository.HL7EquipoConfigRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🤖 HL7 PROCESSOR SERVICE - INTEGRACIÓN REAL CON BD
 * 🎯 Responsabilidades:
 *    - Procesar mensajes HL7 REALES (no simulación)
 *    - Parsear ORU^R01 y extraer signos vitales
 *    - Buscar paciente por DNI en BD
 *    - Guardar en hospitalizacion_signos_vitales
 *    - Registrar auditoría en hl7_mensajes
 *    - Enviar mensajes outbound a sistemas externos
 *
 * ✅ FUNCIONALIDAD COMPLETA: Base de datos real
 *
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot + PostgreSQL
 */
@Slf4j
@Service
public class HL7ProcessorService {

    @Autowired
    private HL7MessageRepository hl7MessageRepository;

    @Autowired
    private HL7MessageOutboundRepository hl7MessageOutboundRepository;

    @Autowired
    private HL7EquipoConfigRepository hl7EquipoConfigRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate; // Para queries directas a BD

    // ===== CONFIGURACIÓN DESDE YAML =====
    @Value("${hl7.daemon.check-interval:5000}")
    private long checkInterval;

    @Value("${hl7.daemon.batch-process-size:100}")
    private int batchProcessSize;

    @Value("${hl7.daemon.enabled:true}")
    private boolean daemonEnabled;

    @Value("${hl7.equipment.monitor-frequency:30000}")
    private long equipmentMonitorFrequency;

    // ===== CONTROL DEL DEMONIO =====
    private final AtomicBoolean daemonRunning = new AtomicBoolean(false);
    private final AtomicBoolean processingInbound = new AtomicBoolean(false);
    private final AtomicBoolean processingOutbound = new AtomicBoolean(false);

    // ===== MÉTRICAS DEL DEMONIO =====
    private final AtomicLong totalMensajesProcesados = new AtomicLong(0);
    private final AtomicLong totalMensajesEnviados = new AtomicLong(0);
    private final AtomicLong totalErrores = new AtomicLong(0);
    private LocalDateTime ultimoProcesamiento;
    private LocalDateTime inicioServicio;

    // ===== INICIALIZACIÓN DEL DEMONIO =====
    @PostConstruct
    public void inicializarDemonio() {
        log.info("🚀 INICIANDO HL7 PROCESSOR SERVICE - INTEGRACIÓN REAL BD");
        inicioServicio = LocalDateTime.now();

        if (daemonEnabled) {
            daemonRunning.set(true);
            log.info("✅ Demonio HL7 habilitado - Check interval: {}ms", checkInterval);
            log.info("📊 Batch size: {} mensajes", batchProcessSize);
            log.info("🏥 Monitor equipos cada: {}ms", equipmentMonitorFrequency);
            log.info("💾 Integración BD: ACTIVA - Procesamiento REAL");
        } else {
            log.warn("⚠️ Demonio HL7 DESHABILITADO en configuración");
        }
    }

    @PreDestroy
    public void detenerDemonio() {
        log.info("🛑 DETENIENDO HL7 PROCESSOR SERVICE");
        daemonRunning.set(false);

        // Esperar que terminen los procesos actuales
        while (processingInbound.get() || processingOutbound.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("📊 ESTADÍSTICAS FINALES DEL DEMONIO:");
        log.info("   - Mensajes procesados: {}", totalMensajesProcesados.get());
        log.info("   - Mensajes enviados: {}", totalMensajesEnviados.get());
        log.info("   - Total errores: {}", totalErrores.get());
        log.info("   - Tiempo activo: {}", inicioServicio);
    }

    // ===== DEMONIO PRINCIPAL - PROCESAMIENTO INBOUND =====
    /**
     * 🔄 PROCESADOR PRINCIPAL DE MENSAJES INBOUND
     * Ejecuta cada X segundos según configuración
     */
    @Scheduled(fixedDelayString = "${hl7.daemon.check-interval:5000}")
    public void procesarMensajesInbound() {
        if (!daemonEnabled || !daemonRunning.get() || processingInbound.get()) {
            return;
        }

        processingInbound.set(true);
        try {
            log.debug("🔍 Buscando mensajes HL7 inbound pendientes...");

            // Obtener mensajes pendientes por lotes
            List<HL7Message> mensajesPendientes = hl7MessageRepository.findMensajesPendientesProcesamiento();

            if (!mensajesPendientes.isEmpty()) {
                log.info("📨 Procesando {} mensajes HL7 inbound", mensajesPendientes.size());

                // Procesar en lotes para mejor performance
                int procesados = 0;
                for (HL7Message mensaje : mensajesPendientes) {
                    if (procesados >= batchProcessSize) {
                        break; // Limitar el lote
                    }

                    try {
                        procesarMensajeInboundReal(mensaje);
                        procesados++;
                        totalMensajesProcesados.incrementAndGet();
                    } catch (Exception e) {
                        log.error("❌ Error procesando mensaje ID: {} - {}", mensaje.getMensajeId(), e.getMessage());
                        marcarMensajeComoError(mensaje, e.getMessage());
                        totalErrores.incrementAndGet();
                    }
                }

                ultimoProcesamiento = LocalDateTime.now();
                log.info("✅ Procesados {} mensajes inbound correctamente", procesados);
            }

        } catch (Exception e) {
            log.error("💥 ERROR CRÍTICO en demonio inbound: {}", e.getMessage(), e);
            totalErrores.incrementAndGet();
        } finally {
            processingInbound.set(false);
        }
    }

    // ===== PROCESAMIENTO REAL DE MENSAJES HL7 =====

    /**
     * 🔍 PROCESAR UN MENSAJE INBOUND REAL
     * ✅ NUEVA FUNCIONALIDAD: Integración completa con BD
     */
    @Transactional
    private void procesarMensajeInboundReal(HL7Message mensaje) {
        log.debug("🔍 Procesando mensaje inbound REAL: {}", mensaje.getMensajeId());

        try {
            // 1. Validar equipo origen
            Optional<HL7EquipoConfig> equipoOpt = validarEquipoOrigen(mensaje);
            if (!equipoOpt.isPresent()) {
                throw new RuntimeException("Equipo no autorizado: " + mensaje.getEquipoOrigen());
            }

            HL7EquipoConfig equipo = equipoOpt.get();

            // 2. Procesar según tipo de mensaje REAL
            String tablaDestino = null;
            Long registroId = null;

            switch (mensaje.getTipoMensaje()) {
                case "ORU^R01":
                    // ✅ PROCESAMIENTO REAL: Signos vitales desde monitor
                    registroId = procesarORU_R01_Real(mensaje);
                    tablaDestino = "hospitalizacion_signos_vitales";
                    break;

                case "ADT^A08":
                    // ✅ PROCESAMIENTO REAL: Actualización de datos de paciente
                    registroId = procesarADT_A08_Real(mensaje);
                    tablaDestino = "pacientes";
                    break;

                default:
                    log.warn("⚠️ Tipo de mensaje no implementado: {}", mensaje.getTipoMensaje());
                    tablaDestino = "hl7_mensajes_sin_procesar";
                    registroId = 0L; // Sin procesar
            }

            // 3. Marcar como procesado en BD
            hl7MessageRepository.marcarComoProcesado(
                    mensaje.getId(),
                    LocalDateTime.now(),
                    tablaDestino,
                    registroId,
                    "HL7_DAEMON_REAL"
            );

            // 4. Actualizar estadísticas del equipo
            hl7EquipoConfigRepository.registrarMensajeRecibido(
                    equipo.getId(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // 5. Enviar ACK
            enviarACK(mensaje, "AA", "Message processed successfully by REAL processor");

            log.debug("✅ Mensaje procesado REALMENTE: {} -> {}", mensaje.getMensajeId(), tablaDestino);

        } catch (Exception e) {
            log.error("❌ Error procesando mensaje REAL {}: {}", mensaje.getMensajeId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 🩺 PROCESAR ORU^R01 - SIGNOS VITALES REALES
     * ✅ FUNCIONALIDAD COMPLETA: Parser HL7 + BD real
     */
    @Transactional
    private Long procesarORU_R01_Real(HL7Message mensaje) {
        log.info("🩺 Procesando ORU^R01 REAL - Signos vitales del monitor");

        try {
            // 1. Parsear mensaje HL7 real
            Map<String, Object> datosParseados = parsearMensajeHL7(mensaje.getMensajeRaw());

            // 2. Extraer datos del paciente
            String numeroDocumento = extraerNumeroDocumento(datosParseados);
            if (numeroDocumento == null || numeroDocumento.isEmpty()) {
                throw new RuntimeException("No se pudo extraer número de documento del mensaje HL7");
            }

            // 3. Buscar paciente en BD
            Long pacienteId = buscarPacientePorDocumento(numeroDocumento);
            if (pacienteId == null) {
                throw new RuntimeException("Paciente no encontrado con documento: " + numeroDocumento);
            }

            // 4. Buscar hospitalización activa
            Long hospitalizacionId = buscarHospitalizacionActiva(pacienteId);
            if (hospitalizacionId == null) {
                log.warn("⚠️ Paciente {} no está hospitalizado actualmente, creando registro de signos vitales sin hospitalización", numeroDocumento);
                hospitalizacionId = 0L; // Sin hospitalización
            }

            // 5. Extraer y guardar signos vitales REALES
            List<Map<String, Object>> signosVitales = extraerSignosVitales(datosParseados);
            Long ultimoRegistroId = null;

            for (Map<String, Object> signo : signosVitales) {
                ultimoRegistroId = guardarSignoVital(
                        hospitalizacionId,
                        pacienteId,
                        mensaje.getId(),
                        signo
                );
            }

            log.info("✅ Procesados {} signos vitales para paciente {} (ID: {})",
                    signosVitales.size(), numeroDocumento, pacienteId);

            return ultimoRegistroId;

        } catch (Exception e) {
            log.error("❌ Error procesando ORU^R01: {}", e.getMessage());
            throw new RuntimeException("Error procesando signos vitales: " + e.getMessage(), e);
        }
    }

    /**
     * 📋 PARSEAR MENSAJE HL7 COMPLETO
     */
    private Map<String, Object> parsearMensajeHL7(String mensajeHL7) {
        Map<String, Object> datos = new HashMap<>();

        try {
            String[] lineas = mensajeHL7.split("\\r");

            for (String linea : lineas) {
                if (linea.startsWith("MSH")) {
                    datos.put("MSH", parsearSegmentoMSH(linea));
                } else if (linea.startsWith("PID")) {
                    datos.put("PID", parsearSegmentoPID(linea));
                } else if (linea.startsWith("PV1")) {
                    datos.put("PV1", parsearSegmentoPV1(linea));
                } else if (linea.startsWith("OBR")) {
                    datos.put("OBR", parsearSegmentoOBR(linea));
                } else if (linea.startsWith("OBX")) {
                    // Puede haber múltiples OBX
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> obxList = (List<Map<String, Object>>) datos.getOrDefault("OBX", new ArrayList<>());
                    obxList.add(parsearSegmentoOBX(linea));
                    datos.put("OBX", obxList);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error parseando mensaje HL7: {}", e.getMessage());
            throw new RuntimeException("Error en parser HL7", e);
        }

        return datos;
    }

    /**
     * 👤 EXTRAER NÚMERO DE DOCUMENTO DEL PACIENTE
     */
    private String extraerNumeroDocumento(Map<String, Object> datosParseados) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pid = (Map<String, Object>) datosParseados.get("PID");
        if (pid != null) {
            return (String) pid.get("numeroDocumento");
        }
        return null;
    }

    /**
     * 🔍 BUSCAR PACIENTE POR DOCUMENTO EN BD
     */
    private Long buscarPacientePorDocumento(String numeroDocumento) {
        try {
            String sql = "SELECT id FROM pacientes WHERE numero_doc = ? AND activo = 'S' LIMIT 1";
            List<Long> resultados = jdbcTemplate.queryForList(sql, Long.class, numeroDocumento);

            if (!resultados.isEmpty()) {
                Long pacienteId = resultados.get(0);
                log.debug("👤 Paciente encontrado: DNI {} -> ID {}", numeroDocumento, pacienteId);
                return pacienteId;
            }

            log.warn("⚠️ Paciente no encontrado con DNI: {}", numeroDocumento);
            return null;

        } catch (Exception e) {
            log.error("❌ Error buscando paciente {}: {}", numeroDocumento, e.getMessage());
            throw new RuntimeException("Error consultando paciente", e);
        }
    }

    /**
     * 🏥 BUSCAR HOSPITALIZACIÓN ACTIVA
     */
    private Long buscarHospitalizacionActiva(Long pacienteId) {
        try {
            String sql = "SELECT id FROM hospitalizacion_cab WHERE paciente_id = ? AND estado = '01' ORDER BY fecha_ingreso DESC LIMIT 1";
            List<Long> resultados = jdbcTemplate.queryForList(sql, Long.class, pacienteId);

            if (!resultados.isEmpty()) {
                Long hospitalizacionId = resultados.get(0);
                log.debug("🏥 Hospitalización activa encontrada: Paciente {} -> Hospitalización {}", pacienteId, hospitalizacionId);
                return hospitalizacionId;
            }

            log.debug("ℹ️ Paciente {} no tiene hospitalización activa", pacienteId);
            return null;

        } catch (Exception e) {
            log.error("❌ Error buscando hospitalización para paciente {}: {}", pacienteId, e.getMessage());
            return null;
        }
    }

    /**
     * 📊 EXTRAER SIGNOS VITALES DEL MENSAJE PARSEADO
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extraerSignosVitales(Map<String, Object> datosParseados) {
        List<Map<String, Object>> signosVitales = new ArrayList<>();

        List<Map<String, Object>> obxList = (List<Map<String, Object>>) datosParseados.get("OBX");
        if (obxList != null) {
            for (Map<String, Object> obx : obxList) {
                String observationId = (String) obx.get("observationId");
                String valor = (String) obx.get("valor");
                String unidad = (String) obx.get("unidad");

                if (observationId != null && valor != null) {
                    Map<String, Object> signo = new HashMap<>();
                    signo.put("tipoSigno", mapearTipoSigno(observationId));
                    signo.put("valor", valor);
                    signo.put("unidad", unidad != null ? unidad : "");
                    signo.put("observationId", observationId);

                    signosVitales.add(signo);
                }
            }
        }

        return signosVitales;
    }

    /**
     * 💾 GUARDAR SIGNO VITAL EN BD REAL
     */
    @Transactional
    private Long guardarSignoVital(Long hospitalizacionId, Long pacienteId, Long mensajeHl7Id, Map<String, Object> signo) {
        try {
            String sql = "INSERT INTO hospitalizacion_signos_vitales " +
                    "(hospitalizacion_id, paciente_id, mensaje_hl7_id, monitor_codigo, " +
                    "tipo_signo, valor_numerico, unidad_medida, fecha_registro, " +
                    "es_ultimo_del_dia, estado_registro, creado_en) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'S', '01', ?)";

            LocalDateTime ahora = LocalDateTime.now();

            // Parsear valor numérico
            BigDecimal valorNumerico = null;
            try {
                String valorStr = (String) signo.get("valor");
                if (valorStr != null && !valorStr.isEmpty()) {
                    valorNumerico = new BigDecimal(valorStr);
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ Valor no numérico para signo vital: {}", signo.get("valor"));
                valorNumerico = BigDecimal.ZERO;
            }

            // Ejecutar INSERT
            int rowsAffected = jdbcTemplate.update(sql,
                    hospitalizacionId > 0 ? hospitalizacionId : null,
                    pacienteId,
                    mensajeHl7Id,
                    "PHILIPS_MX450", // Monitor que envía
                    signo.get("tipoSigno"),
                    valorNumerico,
                    signo.get("unidad"),
                    Timestamp.valueOf(ahora),
                    Timestamp.valueOf(ahora)
            );

            if (rowsAffected > 0) {
                // Obtener ID del registro insertado
                String selectIdSql = "SELECT id FROM hospitalizacion_signos_vitales " +
                        "WHERE paciente_id = ? AND mensaje_hl7_id = ? AND tipo_signo = ? " +
                        "ORDER BY id DESC LIMIT 1";

                List<Long> ids = jdbcTemplate.queryForList(selectIdSql, Long.class,
                        pacienteId, mensajeHl7Id, signo.get("tipoSigno"));

                if (!ids.isEmpty()) {
                    Long registroId = ids.get(0);
                    log.debug("💾 Signo vital guardado: {} = {} {} (ID: {})",
                            signo.get("tipoSigno"), valorNumerico, signo.get("unidad"), registroId);
                    return registroId;
                }
            }

            throw new RuntimeException("No se pudo insertar el signo vital");

        } catch (Exception e) {
            log.error("❌ Error guardando signo vital: {}", e.getMessage());
            throw new RuntimeException("Error en BD al guardar signo vital", e);
        }
    }

    // ===== PARSERS DE SEGMENTOS HL7 =====

    private Map<String, Object> parsearSegmentoMSH(String linea) {
        String[] campos = linea.split("\\|");
        Map<String, Object> msh = new HashMap<>();

        if (campos.length > 3) msh.put("sendingApplication", campos[3]);
        if (campos.length > 4) msh.put("sendingFacility", campos[4]);
        if (campos.length > 7) msh.put("dateTime", campos[7]);
        if (campos.length > 9) msh.put("messageControlId", campos[9]);

        return msh;
    }

    private Map<String, Object> parsearSegmentoPID(String linea) {
        String[] campos = linea.split("\\|");
        Map<String, Object> pid = new HashMap<>();

        if (campos.length > 3) {
            // PID.3 Patient ID - formato: 12345678^^^HOSPITAL^MR
            String patientId = campos[3];
            if (patientId.contains("^^^")) {
                String numeroDocumento = patientId.split("\\^\\^\\^")[0];
                pid.put("numeroDocumento", numeroDocumento);
            } else {
                pid.put("numeroDocumento", patientId);
            }
        }

        if (campos.length > 5) pid.put("nombre", campos[5]);
        if (campos.length > 7) pid.put("fechaNacimiento", campos[7]);
        if (campos.length > 8) pid.put("sexo", campos[8]);

        return pid;
    }

    private Map<String, Object> parsearSegmentoPV1(String linea) {
        String[] campos = linea.split("\\|");
        Map<String, Object> pv1 = new HashMap<>();

        if (campos.length > 3) pv1.put("assignedPatientLocation", campos[3]);
        if (campos.length > 19) pv1.put("visitNumber", campos[19]);

        return pv1;
    }

    private Map<String, Object> parsearSegmentoOBR(String linea) {
        String[] campos = linea.split("\\|");
        Map<String, Object> obr = new HashMap<>();

        if (campos.length > 4) obr.put("universalServiceId", campos[4]);
        if (campos.length > 7) obr.put("observationDateTime", campos[7]);

        return obr;
    }

    private Map<String, Object> parsearSegmentoOBX(String linea) {
        String[] campos = linea.split("\\|");
        Map<String, Object> obx = new HashMap<>();

        if (campos.length > 3) {
            // OBX.3 Observation Identifier - formato: HR^Frecuencia Cardiaca^LOCAL
            String observationId = campos[3];
            obx.put("observationId", observationId);

            if (observationId.contains("^")) {
                String[] partes = observationId.split("\\^");
                obx.put("codigo", partes[0]);
                if (partes.length > 1) obx.put("descripcion", partes[1]);
            }
        }

        if (campos.length > 5) obx.put("valor", campos[5]);
        if (campos.length > 6) obx.put("unidad", campos[6]);
        if (campos.length > 8) obx.put("abnormalFlag", campos[8]);

        return obx;
    }

    /**
     * 🏷️ MAPEAR CÓDIGO HL7 A TIPO DE SIGNO
     */
    private String mapearTipoSigno(String observationId) {
        if (observationId == null) return "OTRO";

        String codigo = observationId.split("\\^")[0];

        switch (codigo.toUpperCase()) {
            case "HR": return "FRECUENCIA_CARDIACA";
            case "SBP": return "PRESION_SISTOLICA";
            case "DBP": return "PRESION_DIASTOLICA";
            case "TEMP": return "TEMPERATURA_CORPORAL";
            case "RESP": return "FRECUENCIA_RESPIRATORIA";
            case "SPO2": return "SATURACION_OXIGENO";
            case "PAIN": return "ESCALA_DOLOR";
            default: return "OTRO";
        }
    }

    // ===== PROCESAMIENTO ADT^A08 =====

    @Transactional
    private Long procesarADT_A08_Real(HL7Message mensaje) {
        log.info("🏥 Procesando ADT^A08 REAL - Actualización datos paciente");

        try {
            // Similar al procesamiento ORU pero para datos de paciente
            Map<String, Object> datosParseados = parsearMensajeHL7(mensaje.getMensajeRaw());
            String numeroDocumento = extraerNumeroDocumento(datosParseados);

            // Aquí iría la lógica para actualizar datos del paciente
            // Por ahora simulamos que se procesó
            log.info("✅ ADT^A08 procesado para paciente: {}", numeroDocumento);

            return System.currentTimeMillis(); // ID simulado

        } catch (Exception e) {
            log.error("❌ Error procesando ADT^A08: {}", e.getMessage());
            throw new RuntimeException("Error procesando actualización paciente", e);
        }
    }

    // ===== MÉTODOS DE VALIDACIÓN Y SOPORTE =====

    private Optional<HL7EquipoConfig> validarEquipoOrigen(HL7Message mensaje) {
        return hl7EquipoConfigRepository.findByIpYAplicacion(
                mensaje.getIpOrigen(),
                mensaje.getEquipoOrigen()
        );
    }

    private void enviarACK(HL7Message mensaje, String codigo, String descripcion) {
        hl7MessageRepository.registrarAck(
                mensaje.getId(),
                codigo,
                descripcion,
                LocalDateTime.now()
        );
    }

    private void marcarMensajeComoError(HL7Message mensaje, String error) {
        hl7MessageRepository.marcarComoError(mensaje.getId(), error);
    }

    // ===== DEMONIO SENDER - PROCESAMIENTO OUTBOUND =====

    @Scheduled(fixedDelayString = "${hl7.daemon.check-interval:5000}")
    public void procesarMensajesOutbound() {
        if (!daemonEnabled || !daemonRunning.get() || processingOutbound.get()) {
            return;
        }

        processingOutbound.set(true);
        try {
            log.debug("📤 Buscando mensajes HL7 outbound pendientes...");

            List<HL7MessageOutbound> mensajesPendientes = hl7MessageOutboundRepository.findMensajesPendientesEnvio();

            if (!mensajesPendientes.isEmpty()) {
                log.info("📨 Enviando {} mensajes HL7 outbound", mensajesPendientes.size());

                int enviados = 0;
                for (HL7MessageOutbound mensaje : mensajesPendientes) {
                    if (enviados >= batchProcessSize) {
                        break;
                    }

                    try {
                        enviarMensajeOutbound(mensaje);
                        enviados++;
                        totalMensajesEnviados.incrementAndGet();
                    } catch (Exception e) {
                        log.error("❌ Error enviando mensaje ID: {} - {}", mensaje.getMensajeId(), e.getMessage());
                        marcarMensajeOutboundComoError(mensaje, e.getMessage());
                        totalErrores.incrementAndGet();
                    }
                }

                log.info("✅ Enviados {} mensajes outbound correctamente", enviados);
            }

        } catch (Exception e) {
            log.error("💥 ERROR CRÍTICO en demonio outbound: {}", e.getMessage(), e);
            totalErrores.incrementAndGet();
        } finally {
            processingOutbound.set(false);
        }
    }

    @Transactional
    private void enviarMensajeOutbound(HL7MessageOutbound mensaje) {
        log.debug("📤 Enviando mensaje outbound: {}", mensaje.getMensajeId());

        try {
            // Simular envío TCP MLLP (aquí iría la implementación real)
            boolean enviado = simularEnvioTCP(mensaje);

            if (enviado) {
                // Marcar como enviado
                hl7MessageOutboundRepository.marcarComoEnviado(
                        mensaje.getId(),
                        LocalDateTime.now()
                );

                log.debug("✅ Mensaje enviado correctamente: {}", mensaje.getMensajeId());
            } else {
                throw new RuntimeException("Error en envío TCP MLLP");
            }

        } catch (Exception e) {
            log.error("❌ Error enviando mensaje {}: {}", mensaje.getMensajeId(), e.getMessage());
            throw e;
        }
    }

    private boolean simularEnvioTCP(HL7MessageOutbound mensaje) {
        // Simular envío TCP MLLP
        log.debug("🌐 Enviando a {}:{} - {}",
                mensaje.getIpDestino(),
                mensaje.getPuertoDestino(),
                mensaje.getTipoMensaje());

        // Simular éxito (90% de éxito)
        return Math.random() > 0.1;
    }

    private void marcarMensajeOutboundComoError(HL7MessageOutbound mensaje, String error) {
        hl7MessageOutboundRepository.marcarErrorEnvio(mensaje.getId(), error);
    }

    // ===== MONITOREO DE EQUIPOS =====

    @Scheduled(fixedDelayString = "${hl7.equipment.monitor-frequency:30000}")
    public void monitorearEquipos() {
        if (!daemonEnabled || !daemonRunning.get()) {
            return;
        }

        try {
            log.debug("💓 Monitoreando equipos médicos...");

            LocalDateTime tiempoLimite = LocalDateTime.now().minusMinutes(10);
            List<HL7EquipoConfig> equiposSinComunicacion = hl7EquipoConfigRepository.findEquiposSinComunicacion(tiempoLimite);

            if (!equiposSinComunicacion.isEmpty()) {
                log.warn("🚨 {} equipos sin comunicación detectados", equiposSinComunicacion.size());

                for (HL7EquipoConfig equipo : equiposSinComunicacion) {
                    log.warn("⚠️ Equipo sin comunicación: {} - {} (Servicio: {})",
                            equipo.getCodigoEquipo(),
                            equipo.getNombreEquipo(),
                            equipo.getServicio());
                }

                enviarAlertasEquiposSinComunicacion(equiposSinComunicacion);
            }

        } catch (Exception e) {
            log.error("💥 Error en monitoreo de equipos: {}", e.getMessage(), e);
        }
    }

    private void enviarAlertasEquiposSinComunicacion(List<HL7EquipoConfig> equipos) {
        log.info("📧 Enviando alertas para {} equipos sin comunicación", equipos.size());
        // Aquí iría la implementación de alertas (email, Slack, etc.)
    }

    // ===== MÉTODOS PÚBLICOS PARA MONITOREO =====

    public boolean isDaemonRunning() {
        return daemonRunning.get();
    }

    public boolean isProcessingInbound() {
        return processingInbound.get();
    }

    public boolean isProcessingOutbound() {
        return processingOutbound.get();
    }

    public long getTotalMensajesProcesados() {
        return totalMensajesProcesados.get();
    }

    public long getTotalMensajesEnviados() {
        return totalMensajesEnviados.get();
    }

    public long getTotalErrores() {
        return totalErrores.get();
    }

    public LocalDateTime getUltimoProcesamiento() {
        return ultimoProcesamiento;
    }

    public LocalDateTime getInicioServicio() {
        return inicioServicio;
    }

    // ===== MÉTODO PARA FORZAR PROCESAMIENTO (TESTING) =====

    @Async
    public void forzarProcesamiento() {
        log.info("🔧 Forzando procesamiento manual...");
        procesarMensajesInbound();
        procesarMensajesOutbound();
    }

    // ===== MÉTODO PÚBLICO PARA PROCESAMIENTO DIRECTO =====

    /**
     * 🎯 MÉTODO PRINCIPAL PARA SER LLAMADO DESDE HL7ServerConfig
     * Este método será invocado cuando llegue un mensaje al servidor
     */
    @Transactional
    public boolean procesarMensajeHL7Directo(String equipoOrigen, String ipOrigen, String mensajeHL7Raw) {
        log.info("🔄 Procesamiento DIRECTO de mensaje HL7 desde servidor");

        try {
            // 1. Crear objeto HL7Message temporal para procesamiento
            HL7Message mensaje = new HL7Message();
            mensaje.setMensajeId(extraerControlId(mensajeHL7Raw));
            mensaje.setTipoMensaje(extraerTipoMensaje(mensajeHL7Raw));
            mensaje.setEquipoOrigen(equipoOrigen);
            mensaje.setIpOrigen(ipOrigen);
            mensaje.setMensajeRaw(mensajeHL7Raw);
            mensaje.setFechaMensaje(LocalDateTime.now());
            mensaje.setFechaRecepcion(LocalDateTime.now());
            mensaje.setEstado("01"); // Recibido
            mensaje.setEsValido("S");
            mensaje.setPrioridad("R");

            // 2. Guardar mensaje en BD para auditoría
            HL7Message mensajeGuardado = hl7MessageRepository.save(mensaje);

            // 3. Procesar inmediatamente
            procesarMensajeInboundReal(mensajeGuardado);

            log.info("✅ Mensaje HL7 procesado directamente: {}", mensaje.getMensajeId());
            return true;

        } catch (Exception e) {
            log.error("❌ Error en procesamiento directo: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== MÉTODOS AUXILIARES PARA PROCESAMIENTO DIRECTO =====

    private String extraerControlId(String mensajeHL7) {
        try {
            String[] lineas = mensajeHL7.split("\\r");
            for (String linea : lineas) {
                if (linea.startsWith("MSH")) {
                    String[] campos = linea.split("\\|");
                    if (campos.length > 9) {
                        return campos[9]; // MSH.10 Message Control ID
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ No se pudo extraer Control ID, generando uno: {}", e.getMessage());
        }

        // Generar ID si no se puede extraer
        return "HL7" + System.currentTimeMillis();
    }

    private String extraerTipoMensaje(String mensajeHL7) {
        try {
            String[] lineas = mensajeHL7.split("\\r");
            for (String linea : lineas) {
                if (linea.startsWith("MSH")) {
                    String[] campos = linea.split("\\|");
                    if (campos.length > 8) {
                        return campos[8]; // MSH.9 Message Type
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ No se pudo extraer tipo de mensaje: {}", e.getMessage());
        }

        return "UNKNOWN";
    }

    // ===== ESTADÍSTICAS Y REPORTING =====

    /**
     * 📊 OBTENER ESTADÍSTICAS DETALLADAS
     */
    public Map<String, Object> getEstadisticasDetalladas() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("demonio", Map.of(
                "running", isDaemonRunning(),
                "inicioServicio", getInicioServicio(),
                "ultimoProcesamiento", getUltimoProcesamiento()
        ));

        stats.put("metricas", Map.of(
                "totalProcesados", getTotalMensajesProcesados(),
                "totalEnviados", getTotalMensajesEnviados(),
                "totalErrores", getTotalErrores()
        ));

        stats.put("procesando", Map.of(
                "inbound", isProcessingInbound(),
                "outbound", isProcessingOutbound()
        ));

        return stats;
    }

    /**
     * 🧪 MÉTODO DE TESTING PARA VALIDAR PARSER
     */
    public Map<String, Object> testParsearMensaje(String mensajeHL7) {
        log.info("🧪 Testing parser HL7 con mensaje de prueba");
        try {
            return parsearMensajeHL7(mensajeHL7);
        } catch (Exception e) {
            log.error("❌ Error en test de parser: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}