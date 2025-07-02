// =====================================================
// 🏭 SERVICE 2: HL7 GENERATOR SERVICE - GENERADOR DE MENSAJES
// =====================================================
package com.formacionbdi.microservicios.app.hl7.services;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7MessageOutbound;
import com.formacionbdi.microservicios.app.hl7.repository.HL7MessageOutboundRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🏭 HL7 GENERATOR SERVICE - GENERADOR DE MENSAJES OUTBOUND
 * 🎯 Responsabilidades:
 *    - Generar mensajes HL7 outbound automáticamente
 *    - Escuchar eventos de hospitalización (trigger simulation)
 *    - Crear mensajes ADT, SIU, ORM, RDE según evento
 *    - Insertar en cola de envío para el demonio
 *
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot + PostgreSQL
 */
@Slf4j
@Service
public class HL7GeneratorService {

    @Autowired
    private HL7MessageOutboundRepository hl7MessageOutboundRepository;

    // ===== CONFIGURACIÓN DESDE YAML =====
    @Value("${hl7.outbound.enabled:true}")
    private boolean outboundEnabled;

    @Value("${hl7.server.host:localhost}")
    private String serverHost;

    @Value("${hl7.server.port:2575}")
    private int serverPort;

    // ===== CONSTANTES HL7 =====
    private static final String HL7_VERSION = "2.5";
    private static final String SENDING_APPLICATION = "HIS_SYSTEM";
    private static final String SENDING_FACILITY = "HOSPITAL";
    private static final DateTimeFormatter HL7_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ===== EVENTOS DE HOSPITALIZACIÓN (TRIGGER SIMULATION) =====

    /**
     * 🏥 EVENTO: HOSPITALIZACIÓN INSERTADA
     * Simula el trigger que se dispara cuando se inserta en hospitalizacion_cab
     */
    @TransactionalEventListener
    @Transactional
    public void onHospitalizacionInsertada(HospitalizacionEvent evento) {
        if (!outboundEnabled) {
            log.debug("⚠️ Outbound deshabilitado - evento ignorado");
            return;
        }

        log.info("🏥 EVENTO RECIBIDO: Hospitalización insertada - Paciente ID: {}", evento.getPacienteId());

        try {
            // Generar mensaje ADT^A01 (Admisión)
            generarMensajeADT_A01(evento);

            // Si tiene datos vitales, generar orden para monitor
            if (evento.isRequiereMonitoreo()) {
                generarOrdenMonitoreo(evento);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando evento hospitalización: {}", e.getMessage(), e);
        }
    }

    /**
     * 🚑 EVENTO: TRANSFERENCIA DE CAMA
     * Simula cambio de cama/servicio
     */
    @TransactionalEventListener
    public void onTransferenciaCama(TransferenciaEvent evento) {
        if (!outboundEnabled) return;

        log.info("🚑 EVENTO RECIBIDO: Transferencia - Paciente ID: {} de {} a {}",
                evento.getPacienteId(), evento.getCamaOrigen(), evento.getCamaDestino());

        try {
            // Generar mensaje ADT^A02 (Transferencia)
            generarMensajeADT_A02(evento);
        } catch (Exception e) {
            log.error("❌ Error procesando transferencia: {}", e.getMessage(), e);
        }
    }

    /**
     * 🏠 EVENTO: ALTA DE PACIENTE
     */
    @TransactionalEventListener
    public void onAltaPaciente(AltaEvent evento) {
        if (!outboundEnabled) return;

        log.info("🏠 EVENTO RECIBIDO: Alta paciente - Paciente ID: {}", evento.getPacienteId());

        try {
            // Generar mensaje ADT^A03 (Alta)
            generarMensajeADT_A03(evento);
        } catch (Exception e) {
            log.error("❌ Error procesando alta: {}", e.getMessage(), e);
        }
    }

    // ===== GENERADORES DE MENSAJES HL7 =====

    /**
     * 🏥 GENERAR ADT^A01 - ADMISIÓN
     */
    @Transactional
    private void generarMensajeADT_A01(HospitalizacionEvent evento) {
        log.debug("🏥 Generando ADT^A01 para paciente: {}", evento.getPacienteId());

        // Crear el mensaje HL7 completo
        String mensajeHL7 = construirADT_A01(evento);

        // Crear datos JSON estructurados
        Map<String, Object> mensajeJson = new HashMap<>();
        mensajeJson.put("evento", "ADMISION");
        mensajeJson.put("pacienteId", evento.getPacienteId());
        mensajeJson.put("numeroDocumento", evento.getNumeroDocumento());
        mensajeJson.put("numeroCuenta", evento.getNumeroCuenta());
        mensajeJson.put("servicio", evento.getServicio());
        mensajeJson.put("cama", evento.getCama());
        mensajeJson.put("fechaIngreso", evento.getFechaIngreso().toString());

        // Crear registro outbound
        HL7MessageOutbound mensaje = HL7MessageOutbound.builder()
                .mensajeId(generarControlID())
                .tipoMensaje("ADT^A01")
                .versionHl7(HL7_VERSION)
                .aplicacionOrigen(SENDING_APPLICATION)
                .facilityOrigen(SENDING_FACILITY)
                .sistemaDestino("HIS_EXTERNO")
                .ipDestino("192.168.1.100") // IP del sistema externo
                .puertoDestino(2575)
                .tablaOrigen("hospitalizacion_cab")
                .registroOrigenId(evento.getHospitalizacionId())
                .procesoOrigen("HOSPITALIZAR")
                .pacienteId(evento.getPacienteId())
                .numeroDocumento(evento.getNumeroDocumento())
                .numeroCuenta(evento.getNumeroCuenta())
                .mensajeHl7(mensajeHL7)
                .mensajeJson(mensajeJson)
                .prioridad(evento.isEsUrgente() ? "U" : "R")
                .generadoPor("HL7_GENERATOR")
                .build();

        // Guardar en cola de envío
        hl7MessageOutboundRepository.save(mensaje);

        log.info("✅ ADT^A01 generado y encolado - ID: {}", mensaje.getMensajeId());
    }

    /**
     * 🚑 GENERAR ADT^A02 - TRANSFERENCIA
     */
    @Transactional
    private void generarMensajeADT_A02(TransferenciaEvent evento) {
        log.debug("🚑 Generando ADT^A02 para paciente: {}", evento.getPacienteId());

        String mensajeHL7 = construirADT_A02(evento);

        Map<String, Object> mensajeJson = new HashMap<>();
        mensajeJson.put("evento", "TRANSFERENCIA");
        mensajeJson.put("pacienteId", evento.getPacienteId());
        mensajeJson.put("camaOrigen", evento.getCamaOrigen());
        mensajeJson.put("camaDestino", evento.getCamaDestino());
        mensajeJson.put("fechaTransferencia", evento.getFechaTransferencia().toString());

        HL7MessageOutbound mensaje = HL7MessageOutbound.builder()
                .mensajeId(generarControlID())
                .tipoMensaje("ADT^A02")
                .versionHl7(HL7_VERSION)
                .aplicacionOrigen(SENDING_APPLICATION)
                .facilityOrigen(SENDING_FACILITY)
                .sistemaDestino("HIS_EXTERNO")
                .ipDestino("192.168.1.100")
                .puertoDestino(2575)
                .tablaOrigen("hospitalizacion_mov")
                .registroOrigenId(evento.getMovimientoId())
                .procesoOrigen("MOVER_CAMA")
                .pacienteId(evento.getPacienteId())
                .mensajeHl7(mensajeHL7)
                .mensajeJson(mensajeJson)
                .prioridad("R")
                .generadoPor("HL7_GENERATOR")
                .build();

        hl7MessageOutboundRepository.save(mensaje);
        log.info("✅ ADT^A02 generado y encolado - ID: {}", mensaje.getMensajeId());
    }

    /**
     * 🏠 GENERAR ADT^A03 - ALTA
     */
    @Transactional
    private void generarMensajeADT_A03(AltaEvent evento) {
        log.debug("🏠 Generando ADT^A03 para paciente: {}", evento.getPacienteId());

        String mensajeHL7 = construirADT_A03(evento);

        Map<String, Object> mensajeJson = new HashMap<>();
        mensajeJson.put("evento", "ALTA");
        mensajeJson.put("pacienteId", evento.getPacienteId());
        mensajeJson.put("tipoAlta", evento.getTipoAlta());
        mensajeJson.put("fechaAlta", evento.getFechaAlta().toString());

        HL7MessageOutbound mensaje = HL7MessageOutbound.builder()
                .mensajeId(generarControlID())
                .tipoMensaje("ADT^A03")
                .versionHl7(HL7_VERSION)
                .aplicacionOrigen(SENDING_APPLICATION)
                .facilityOrigen(SENDING_FACILITY)
                .sistemaDestino("HIS_EXTERNO")
                .ipDestino("192.168.1.100")
                .puertoDestino(2575)
                .tablaOrigen("hos_alta_cab")
                .registroOrigenId(evento.getAltaId())
                .procesoOrigen("ALTA_PAC")
                .pacienteId(evento.getPacienteId())
                .mensajeHl7(mensajeHL7)
                .mensajeJson(mensajeJson)
                .prioridad("R")
                .generadoPor("HL7_GENERATOR")
                .build();

        hl7MessageOutboundRepository.save(mensaje);
        log.info("✅ ADT^A03 generado y encolado - ID: {}", mensaje.getMensajeId());
    }

    /**
     * 📊 GENERAR ORDEN DE MONITOREO (ORM^O01)
     */
    @Transactional
    private void generarOrdenMonitoreo(HospitalizacionEvent evento) {
        log.debug("📊 Generando orden de monitoreo para paciente: {}", evento.getPacienteId());

        String mensajeHL7 = construirORM_O01(evento);

        Map<String, Object> mensajeJson = new HashMap<>();
        mensajeJson.put("evento", "ORDEN_MONITOREO");
        mensajeJson.put("pacienteId", evento.getPacienteId());
        mensajeJson.put("tipoMonitoreo", "SIGNOS_VITALES");

        HL7MessageOutbound mensaje = HL7MessageOutbound.builder()
                .mensajeId(generarControlID())
                .tipoMensaje("ORM^O01")
                .versionHl7(HL7_VERSION)
                .aplicacionOrigen(SENDING_APPLICATION)
                .facilityOrigen(SENDING_FACILITY)
                .sistemaDestino("MONITOR_SYSTEM")
                .ipDestino("192.168.1.200")
                .puertoDestino(2575)
                .tablaOrigen("hospitalizacion_cab")
                .registroOrigenId(evento.getHospitalizacionId())
                .procesoOrigen("GEN_ORDEN")
                .pacienteId(evento.getPacienteId())
                .mensajeHl7(mensajeHL7)
                .mensajeJson(mensajeJson)
                .prioridad("U") // Urgente para monitoreo
                .generadoPor("HL7_GENERATOR")
                .build();

        hl7MessageOutboundRepository.save(mensaje);
        log.info("✅ ORM^O01 (Orden monitoreo) generado - ID: {}", mensaje.getMensajeId());
    }

    // ===== CONSTRUCTORES DE MENSAJES HL7 =====

    private String construirADT_A01(HospitalizacionEvent evento) {
        String timestamp = LocalDateTime.now().format(HL7_TIMESTAMP);
        String controlId = generarControlID();

        return "MSH|^~\\&|" + SENDING_APPLICATION + "|" + SENDING_FACILITY +
                "|HIS_EXTERNO|HOSPITAL|" + timestamp + "||ADT^A01^ADT_A01|" + controlId + "|P|" + HL7_VERSION + "\r" +
                "EVN||" + timestamp + "|||HL7_GENERATOR\r" +
                "PID|1||" + evento.getNumeroDocumento() + "^^^HOSPITAL^MR||" + evento.getNombrePaciente() +
                "||" + evento.getFechaNacimiento() + "|" + evento.getSexo() + "\r" +
                "PV1|1|I|" + evento.getServicio() + "^" + evento.getCama() + "^01|||||||||||||||" +
                evento.getNumeroCuenta() + "|||||||||||||||||||||||||" + timestamp + "\r";
    }

    private String construirADT_A02(TransferenciaEvent evento) {
        String timestamp = LocalDateTime.now().format(HL7_TIMESTAMP);
        String controlId = generarControlID();

        return "MSH|^~\\&|" + SENDING_APPLICATION + "|" + SENDING_FACILITY +
                "|HIS_EXTERNO|HOSPITAL|" + timestamp + "||ADT^A02^ADT_A02|" + controlId + "|P|" + HL7_VERSION + "\r" +
                "EVN||" + timestamp + "\r" +
                "PID|1||" + evento.getNumeroDocumento() + "^^^HOSPITAL^MR\r" +
                "PV1|1|I|" + evento.getCamaDestino() + "|||||||||||||||||||||||||||" + timestamp + "\r";
    }

    private String construirADT_A03(AltaEvent evento) {
        String timestamp = LocalDateTime.now().format(HL7_TIMESTAMP);
        String controlId = generarControlID();

        return "MSH|^~\\&|" + SENDING_APPLICATION + "|" + SENDING_FACILITY +
                "|HIS_EXTERNO|HOSPITAL|" + timestamp + "||ADT^A03^ADT_A03|" + controlId + "|P|" + HL7_VERSION + "\r" +
                "EVN||" + timestamp + "\r" +
                "PID|1||" + evento.getNumeroDocumento() + "^^^HOSPITAL^MR\r" +
                "PV1|1|O||||||||||||||||||||||||||||" + timestamp + "\r";
    }

    private String construirORM_O01(HospitalizacionEvent evento) {
        String timestamp = LocalDateTime.now().format(HL7_TIMESTAMP);
        String controlId = generarControlID();

        return "MSH|^~\\&|" + SENDING_APPLICATION + "|" + SENDING_FACILITY +
                "|MONITOR_SYSTEM|HOSPITAL|" + timestamp + "||ORM^O01^ORM_O01|" + controlId + "|P|" + HL7_VERSION + "\r" +
                "PID|1||" + evento.getNumeroDocumento() + "^^^HOSPITAL^MR||" + evento.getNombrePaciente() + "\r" +
                "PV1|1|I|" + evento.getServicio() + "^" + evento.getCama() + "^01\r" +
                "ORC|NW|" + controlId + "||||||" + timestamp + "\r" +
                "OBR|1|" + controlId + "||VITALS^Signos Vitales^LOCAL|||" + timestamp + "\r";
    }

    // ===== MÉTODOS UTILITARIOS =====

    private String generarControlID() {
        return "HL7" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ===== MÉTODOS PÚBLICOS PARA TESTING =====

    /**
     * 🧪 GENERAR MENSAJE DE PRUEBA
     */
    public void generarMensajePrueba(String tipo, Long pacienteId) {
        log.info("🧪 Generando mensaje de prueba: {} para paciente: {}", tipo, pacienteId);

        switch (tipo.toUpperCase()) {
            case "ADT_A01":
                HospitalizacionEvent eventoHosp = new HospitalizacionEvent();
                eventoHosp.setPacienteId(pacienteId);
                eventoHosp.setNumeroDocumento("12345678");
                eventoHosp.setNumeroCuenta("HC" + System.currentTimeMillis());
                eventoHosp.setServicio("UCI");
                eventoHosp.setCama("UCI-001");
                eventoHosp.setFechaIngreso(LocalDateTime.now());
                eventoHosp.setNombrePaciente("PACIENTE PRUEBA");
                eventoHosp.setHospitalizacionId(System.currentTimeMillis());
                generarMensajeADT_A01(eventoHosp);
                break;

            default:
                log.warn("⚠️ Tipo de mensaje de prueba no reconocido: {}", tipo);
        }
    }

    // ===== CLASES DE EVENTOS INTERNOS =====

    public static class HospitalizacionEvent {
        private Long pacienteId;
        private String numeroDocumento;
        private String numeroCuenta;
        private String servicio;
        private String cama;
        private LocalDateTime fechaIngreso;
        private String nombrePaciente;
        private String fechaNacimiento = "19900101";
        private String sexo = "M";
        private Long hospitalizacionId;
        private boolean esUrgente = false;
        private boolean requiereMonitoreo = true;

        // Getters y Setters
        public Long getPacienteId() { return pacienteId; }
        public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
        public String getNumeroDocumento() { return numeroDocumento; }
        public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
        public String getNumeroCuenta() { return numeroCuenta; }
        public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
        public String getServicio() { return servicio; }
        public void setServicio(String servicio) { this.servicio = servicio; }
        public String getCama() { return cama; }
        public void setCama(String cama) { this.cama = cama; }
        public LocalDateTime getFechaIngreso() { return fechaIngreso; }
        public void setFechaIngreso(LocalDateTime fechaIngreso) { this.fechaIngreso = fechaIngreso; }
        public String getNombrePaciente() { return nombrePaciente; }
        public void setNombrePaciente(String nombrePaciente) { this.nombrePaciente = nombrePaciente; }
        public String getFechaNacimiento() { return fechaNacimiento; }
        public String getSexo() { return sexo; }
        public Long getHospitalizacionId() { return hospitalizacionId; }
        public void setHospitalizacionId(Long hospitalizacionId) { this.hospitalizacionId = hospitalizacionId; }
        public boolean isEsUrgente() { return esUrgente; }
        public boolean isRequiereMonitoreo() { return requiereMonitoreo; }
    }

    public static class TransferenciaEvent {
        private Long pacienteId;
        private String numeroDocumento;
        private String camaOrigen;
        private String camaDestino;
        private LocalDateTime fechaTransferencia;
        private Long movimientoId;

        // Getters y Setters
        public Long getPacienteId() { return pacienteId; }
        public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
        public String getNumeroDocumento() { return numeroDocumento; }
        public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
        public String getCamaOrigen() { return camaOrigen; }
        public void setCamaOrigen(String camaOrigen) { this.camaOrigen = camaOrigen; }
        public String getCamaDestino() { return camaDestino; }
        public void setCamaDestino(String camaDestino) { this.camaDestino = camaDestino; }
        public LocalDateTime getFechaTransferencia() { return fechaTransferencia; }
        public void setFechaTransferencia(LocalDateTime fechaTransferencia) { this.fechaTransferencia = fechaTransferencia; }
        public Long getMovimientoId() { return movimientoId; }
        public void setMovimientoId(Long movimientoId) { this.movimientoId = movimientoId; }
    }

    public static class AltaEvent {
        private Long pacienteId;
        private String numeroDocumento;
        private String tipoAlta;
        private LocalDateTime fechaAlta;
        private Long altaId;

        // Getters y Setters
        public Long getPacienteId() { return pacienteId; }
        public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
        public String getNumeroDocumento() { return numeroDocumento; }
        public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
        public String getTipoAlta() { return tipoAlta; }
        public void setTipoAlta(String tipoAlta) { this.tipoAlta = tipoAlta; }
        public LocalDateTime getFechaAlta() { return fechaAlta; }
        public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }
        public Long getAltaId() { return altaId; }
        public void setAltaId(Long altaId) { this.altaId = altaId; }
    }
}