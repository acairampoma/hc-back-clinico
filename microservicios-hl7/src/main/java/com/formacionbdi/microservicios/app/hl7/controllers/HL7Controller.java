// =====================================================
// 🌐 CONTROLLER: HL7 CONTROLLER - API REST MONITOREO
// =====================================================
package com.formacionbdi.microservicios.app.hl7.controllers;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7Message;
import com.formacionbdi.microservicios.app.hl7.models.entity.HL7MessageOutbound;
import com.formacionbdi.microservicios.app.hl7.models.entity.HL7EquipoConfig;
import com.formacionbdi.microservicios.app.hl7.repository.HL7MessageRepository;
import com.formacionbdi.microservicios.app.hl7.repository.HL7MessageOutboundRepository;
import com.formacionbdi.microservicios.app.hl7.repository.HL7EquipoConfigRepository;
import com.formacionbdi.microservicios.app.hl7.services.HL7ProcessorService;
import com.formacionbdi.microservicios.app.hl7.services.HL7GeneratorService;
import com.formacionbdi.microservicios.app.hl7.config.HL7ServerConfig;
import com.formacionbdi.microservicios.app.hl7.config.HL7Properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 🌐 HL7 CONTROLLER - API REST PARA MONITOREO Y GESTIÓN
 * 🎯 Responsabilidades:
 *    - API REST para monitoreo del sistema HL7
 *    - Consulta de mensajes y estadísticas
 *    - Control del demonio (start/stop/status)
 *    - Generación de mensajes de prueba
 *
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot REST
 */
@Slf4j
@RestController
@RequestMapping("/")  // ✅ VUELTO AL ORIGINAL
@CrossOrigin(origins = "*")
public class HL7Controller {

    @Autowired
    private HL7MessageRepository hl7MessageRepository;

    @Autowired
    private HL7MessageOutboundRepository hl7MessageOutboundRepository;

    @Autowired
    private HL7EquipoConfigRepository hl7EquipoConfigRepository;

    @Autowired
    private HL7ProcessorService hl7ProcessorService;

    @Autowired
    private HL7GeneratorService hl7GeneratorService;

    @Autowired
    private HL7ServerConfig hl7ServerConfig;

    @Autowired
    private HL7Properties hl7Properties;

    // ===== ENDPOINTS DE ESTADO Y MONITOREO =====

    /**
     * 💓 HEALTH CHECK - Estado general del sistema HL7
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("🔍 Health check solicitado");

        Map<String, Object> health = new HashMap<>();
        boolean isHealthy = true;

        try {
            // Estado del servidor TCP
            Map<String, Object> servidor = new HashMap<>();
            servidor.put("running", hl7ServerConfig.isServerRunning());
            servidor.put("puerto", hl7ServerConfig.getServerPort());
            servidor.put("conexionesActivas", hl7ServerConfig.getConexionesActivas());
            servidor.put("totalConexiones", hl7ServerConfig.getTotalConexiones());
            health.put("servidor", servidor);

            // Estado del demonio
            Map<String, Object> demonio = new HashMap<>();
            demonio.put("running", hl7ProcessorService.isDaemonRunning());
            demonio.put("processingInbound", hl7ProcessorService.isProcessingInbound());
            demonio.put("processingOutbound", hl7ProcessorService.isProcessingOutbound());
            demonio.put("inicioServicio", hl7ProcessorService.getInicioServicio());
            demonio.put("ultimoProcesamiento", hl7ProcessorService.getUltimoProcesamiento());
            health.put("demonio", demonio);

            // Métricas básicas
            Map<String, Object> metricas = new HashMap<>();
            metricas.put("mensajesProcesados", hl7ProcessorService.getTotalMensajesProcesados());
            metricas.put("mensajesEnviados", hl7ProcessorService.getTotalMensajesEnviados());
            metricas.put("totalErrores", hl7ProcessorService.getTotalErrores());
            health.put("metricas", metricas);

            // Verificar si hay problemas críticos
            if (!hl7ServerConfig.isServerRunning() || !hl7ProcessorService.isDaemonRunning()) {
                isHealthy = false;
            }

            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("❌ Error en health check: {}", e.getMessage(), e);
            health.put("status", "ERROR");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(health);
        }
    }

    /**
     * 📊 DASHBOARD - Resumen completo para dashboard
     */
    /**
     * 📊 DASHBOARD - Resumen completo para dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        log.debug("📊 Dashboard solicitado");

        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Resumen del servidor
            dashboard.put("servidor", Map.of(
                    "estado", hl7ServerConfig.isServerRunning() ? "ACTIVO" : "INACTIVO",
                    "puerto", hl7ServerConfig.getServerPort(),
                    "conexiones", hl7ServerConfig.getConexionesActivas(),
                    "mensajesRecibidos", hl7ServerConfig.getMensajesRecibidos(),
                    "mensajesEnviados", hl7ServerConfig.getMensajesEnviados()
            ));

            // Estadísticas recientes (últimas 24 horas)
            LocalDateTime hace24h = LocalDateTime.now().minusHours(24);

            // Mensajes inbound por equipo
            List<Object[]> estadisticasEquipo = hl7MessageRepository.getEstadisticasPorEquipo(hace24h);
            dashboard.put("equipos", estadisticasEquipo);

            // Mensajes outbound por destino
            List<Object[]> estadisticasDestino = hl7MessageOutboundRepository.getEstadisticasPorDestino(hace24h);
            dashboard.put("destinos", estadisticasDestino);

            // Resumen por servicio de equipos
            LocalDateTime hace10min = LocalDateTime.now().minusMinutes(10);
            List<Object[]> resumenServicios = hl7EquipoConfigRepository.getResumenPorServicio(hace10min);
            dashboard.put("servicios", resumenServicios);

            // Alertas críticas
            List<Object[]> alertas = hl7EquipoConfigRepository.getAlertasCriticas();
            dashboard.put("alertas", alertas);

            // Configuración actual
            dashboard.put("configuracion", hl7Properties.getResumenConfiguracion());

            dashboard.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Error generando dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ===== ENDPOINTS DE MENSAJES INBOUND =====

    /**
     * 📥 LISTAR MENSAJES INBOUND RECIENTES
     */
    @GetMapping("/mensajes/inbound")
    public ResponseEntity<Map<String, Object>> getMensajesInbound(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String equipoOrigen,
            @RequestParam(required = false) String estado) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            List<HL7Message> mensajes;

            if (equipoOrigen != null && !equipoOrigen.isEmpty()) {
                LocalDateTime hace24h = LocalDateTime.now().minusHours(24);
                mensajes = hl7MessageRepository.findMensajesPorEquipo(equipoOrigen, hace24h);
            } else {
                mensajes = hl7MessageRepository.findAll(pageable).getContent();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mensajes", mensajes);
            response.put("total", mensajes.size());
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error obteniendo mensajes inbound: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔍 OBTENER MENSAJE INBOUND POR ID
     */
    @GetMapping("/mensajes/inbound/{id}")
    public ResponseEntity<Object> getMensajeInboundById(@PathVariable Long id) {
        try {
            Optional<HL7Message> mensaje = hl7MessageRepository.findById(id);

            if (mensaje.isPresent()) {
                return ResponseEntity.ok(mensaje.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("❌ Error obteniendo mensaje inbound {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ===== ENDPOINTS DE MENSAJES OUTBOUND =====

    /**
     * 📤 LISTAR MENSAJES OUTBOUND RECIENTES
     */
    @GetMapping("/mensajes/outbound")
    public ResponseEntity<Map<String, Object>> getMensajesOutbound(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sistemaDestino,
            @RequestParam(required = false) String estado) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            List<HL7MessageOutbound> mensajes;

            if (sistemaDestino != null && !sistemaDestino.isEmpty()) {
                mensajes = hl7MessageOutboundRepository.findMensajesPorDestino(sistemaDestino);
            } else {
                mensajes = hl7MessageOutboundRepository.findAll(pageable).getContent();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mensajes", mensajes);
            response.put("total", mensajes.size());
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error obteniendo mensajes outbound: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📊 ESTADÍSTICAS OUTBOUND
     */
    @GetMapping("/mensajes/outbound/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticasOutbound() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Resumen de estado de cola
            List<Object[]> resumenEstado = hl7MessageOutboundRepository.getResumenEstadoCola();
            stats.put("resumenEstado", resumenEstado);

            // Throughput por hora (últimas 24 horas)
            LocalDateTime hace24h = LocalDateTime.now().minusHours(24);
            List<Object[]> throughput = hl7MessageOutboundRepository.getThroughputPorHora(hace24h);
            stats.put("throughput", throughput);

            // Mensajes que requieren intervención
            long requierenIntervencion = hl7MessageOutboundRepository.countMensajesRequierenIntervencion();
            stats.put("requierenIntervencion", requierenIntervencion);

            // Mensajes pendientes antiguos (más de 1 hora)
            LocalDateTime hace1h = LocalDateTime.now().minusHours(1);
            long pendientesAntiguos = hl7MessageOutboundRepository.countMensajesPendientesAntiguos(hace1h);
            stats.put("pendientesAntiguos", pendientesAntiguos);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas outbound: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ===== ENDPOINTS DE EQUIPOS =====

    /**
     * 🏥 LISTAR EQUIPOS CONFIGURADOS
     */
    @GetMapping("/equipos")
    public ResponseEntity<List<HL7EquipoConfig>> getEquipos(
            @RequestParam(required = false) String servicio,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String activo) {

        try {
            List<HL7EquipoConfig> equipos = hl7EquipoConfigRepository.findEquiposPorCriterios(
                    servicio, fabricante, activo);

            return ResponseEntity.ok(equipos);

        } catch (Exception e) {
            log.error("❌ Error obteniendo equipos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 🚨 EQUIPOS SIN COMUNICACIÓN
     */
    @GetMapping("/equipos/sin-comunicacion")
    public ResponseEntity<List<HL7EquipoConfig>> getEquiposSinComunicacion(
            @RequestParam(defaultValue = "10") int minutos) {

        try {
            LocalDateTime tiempoLimite = LocalDateTime.now().minusMinutes(minutos);
            List<HL7EquipoConfig> equipos = hl7EquipoConfigRepository.findEquiposSinComunicacion(tiempoLimite);

            return ResponseEntity.ok(equipos);

        } catch (Exception e) {
            log.error("❌ Error obteniendo equipos sin comunicación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 📊 TOP EQUIPOS POR MENSAJES
     */
    @GetMapping("/equipos/top")
    public ResponseEntity<List<Object[]>> getTopEquipos(
            @RequestParam(defaultValue = "10") int limite) {

        try {
            // Usar query nativa limitada
            List<Object[]> topEquipos = hl7EquipoConfigRepository.getTopEquiposPorMensajes();

            // Limitar manualmente si es necesario
            if (topEquipos.size() > limite) {
                topEquipos = topEquipos.subList(0, limite);
            }

            return ResponseEntity.ok(topEquipos);

        } catch (Exception e) {
            log.error("❌ Error obteniendo top equipos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ===== ENDPOINTS DE CONTROL DEL DEMONIO =====

    /**
     * ▶️ FORZAR PROCESAMIENTO MANUAL
     */
    @PostMapping("/demonio/procesar")
    public ResponseEntity<Map<String, String>> forzarProcesamiento() {
        try {
            log.info("🔧 Procesamiento manual solicitado vía API");
            hl7ProcessorService.forzarProcesamiento();

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "mensaje", "Procesamiento manual iniciado",
                    "timestamp", LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            log.error("❌ Error forzando procesamiento: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📊 MÉTRICAS DEL DEMONIO
     */
    @GetMapping("/demonio/metricas")
    public ResponseEntity<Map<String, Object>> getMetricasDemonio() {
        try {
            Map<String, Object> metricas = new HashMap<>();
            metricas.put("running", hl7ProcessorService.isDaemonRunning());
            metricas.put("processingInbound", hl7ProcessorService.isProcessingInbound());
            metricas.put("processingOutbound", hl7ProcessorService.isProcessingOutbound());
            metricas.put("totalMensajesProcesados", hl7ProcessorService.getTotalMensajesProcesados());
            metricas.put("totalMensajesEnviados", hl7ProcessorService.getTotalMensajesEnviados());
            metricas.put("totalErrores", hl7ProcessorService.getTotalErrores());
            metricas.put("inicioServicio", hl7ProcessorService.getInicioServicio());
            metricas.put("ultimoProcesamiento", hl7ProcessorService.getUltimoProcesamiento());

            return ResponseEntity.ok(metricas);

        } catch (Exception e) {
            log.error("❌ Error obteniendo métricas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ===== ENDPOINTS DE TESTING =====

    /**
     * 🧪 GENERAR MENSAJE DE PRUEBA
     */
    @PostMapping("/test/generar-mensaje")
    public ResponseEntity<Map<String, String>> generarMensajePrueba(
            @RequestParam String tipo,
            @RequestParam(defaultValue = "1") Long pacienteId) {

        try {
            log.info("🧪 Generando mensaje de prueba: {} para paciente: {}", tipo, pacienteId);
            hl7GeneratorService.generarMensajePrueba(tipo, pacienteId);

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "mensaje", "Mensaje de prueba generado: " + tipo,
                    "pacienteId", pacienteId.toString(),
                    "timestamp", LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            log.error("❌ Error generando mensaje de prueba: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📋 OBTENER CONFIGURACIÓN ACTUAL
     */
    @GetMapping("/configuracion")
    public ResponseEntity<Map<String, Object>> getConfiguracion() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("servidor", hl7Properties.getServer());
            config.put("demonio", hl7Properties.getDaemon());
            config.put("outbound", hl7Properties.getOutbound());
            config.put("seguridad", hl7Properties.getSecurity());
            config.put("monitoreo", hl7Properties.getMonitoring());
            config.put("resumen", hl7Properties.getResumenConfiguracion());

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("❌ Error obteniendo configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🌐 INFORMACIÓN DEL SERVIDOR TCP
     */
    @GetMapping("/servidor/info")
    public ResponseEntity<Map<String, Object>> getInfoServidor() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("running", hl7ServerConfig.isServerRunning());
            info.put("host", hl7ServerConfig.getServerHost());
            info.put("puerto", hl7ServerConfig.getServerPort());
            info.put("conexionesActivas", hl7ServerConfig.getConexionesActivas());
            info.put("totalConexiones", hl7ServerConfig.getTotalConexiones());
            info.put("mensajesRecibidos", hl7ServerConfig.getMensajesRecibidos());
            info.put("mensajesEnviados", hl7ServerConfig.getMensajesEnviados());

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("❌ Error obteniendo info del servidor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}