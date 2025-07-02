package com.formacionbdi.microservicios.app.hl7.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 📡 ENTIDAD PRINCIPAL: Mensajes HL7 que RECIBIMOS de equipos médicos
 * 🎯 Mapea tabla: hl7_mensajes
 * 👨‍💻 Desarrollador: Alan Cairampoma
 */
@Entity
@Table(name = "hl7_mensajes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class HL7Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== IDENTIFICACIÓN DEL MENSAJE =====
    @Column(name = "mensaje_id", unique = true, nullable = false, length = 50)
    private String mensajeId; // MSH.10 Control ID

    @Column(name = "tipo_mensaje", nullable = false, length = 20)
    private String tipoMensaje; // ORU^R01, ADT^A01, etc.

    @Column(name = "version_hl7", length = 10)
    private String versionHl7 = "2.5";

    // ===== ORIGEN DEL MENSAJE =====
    @Column(name = "equipo_origen", nullable = false, length = 50)
    private String equipoOrigen; // MSH.3 Sending Application

    @Column(name = "facility_origen", length = 50)
    private String facilityOrigen; // MSH.4 Sending Facility

    @Column(name = "ip_origen")
    private String ipOrigen; // IP desde donde llegó

    @Column(name = "puerto_origen")
    private Integer puertoOrigen;

    // ===== DESTINO (NUESTRO SISTEMA) =====
    @Column(name = "aplicacion_destino", length = 50)
    private String aplicacionDestino = "HIS_SYSTEM"; // MSH.5

    @Column(name = "facility_destino", length = 50)
    private String facilityDestino = "HOSPITAL"; // MSH.6

    // ===== TIMESTAMPS =====
    @Column(name = "fecha_mensaje", nullable = false)
    private LocalDateTime fechaMensaje; // MSH.7 Date/Time of Message

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    // ===== PACIENTE RELACIONADO =====
    @Column(name = "paciente_id")
    private Long pacienteId; // FK a tabla pacientes

    @Column(name = "numero_documento", length = 15)
    private String numeroDocumento; // PID.3 Patient ID

    @Column(name = "numero_cuenta", length = 20)
    private String numeroCuenta; // PV1.19 Visit Number

    @Column(name = "numero_cama", length = 15)
    private String numeroCama; // PV1.3 Assigned Patient Location

    // ===== CONTENIDO DEL MENSAJE =====
    @Column(name = "mensaje_raw", columnDefinition = "TEXT")
    private String mensajeRaw; // Mensaje HL7 completo original

    @Type(type = "jsonb")
    @Column(name = "mensaje_parseado", columnDefinition = "jsonb")
    private Map<String, Object> mensajeParseado; // Mensaje en JSON

    // ===== SEGMENTOS PARSEADOS =====
    @Type(type = "jsonb")
    @Column(name = "segmento_msh", columnDefinition = "jsonb")
    private Map<String, Object> segmentoMsh; // Message Header

    @Type(type = "jsonb")
    @Column(name = "segmento_pid", columnDefinition = "jsonb")
    private Map<String, Object> segmentoPid; // Patient Identification

    @Type(type = "jsonb")
    @Column(name = "segmento_pv1", columnDefinition = "jsonb")
    private Map<String, Object> segmentoPv1; // Patient Visit

    @Type(type = "jsonb")
    @Column(name = "segmento_obr", columnDefinition = "jsonb")
    private Map<String, Object> segmentoObr; // Observation Request

    @Type(type = "jsonb")
    @Column(name = "segmentos_obx", columnDefinition = "jsonb")
    private Map<String, Object> segmentosObx; // Observation Results (array)

    // ===== ESTADOS DE PROCESAMIENTO =====
    @Column(name = "estado", length = 2)
    private String estado = "01"; // 01=Recibido, 02=Procesado, 03=Error, 04=Rechazado

    @Column(name = "prioridad", length = 1)
    private String prioridad = "R"; // E=Emergencia, U=Urgente, R=Regular

    // ===== VALIDACIÓN Y ERRORES =====
    @Column(name = "es_valido", length = 1)
    private String esValido = "S";

    @Column(name = "errores_validacion", columnDefinition = "TEXT")
    private String erroresValidacion;

    @Column(name = "campos_faltantes", columnDefinition = "TEXT")
    private String camposFaltantes;

    // ===== RESPUESTA ACK/NACK =====
    @Column(name = "ack_enviado", length = 1)
    private String ackEnviado = "N";

    @Column(name = "ack_codigo", length = 10)
    private String ackCodigo; // AA=Application Accept, AE=Application Error

    @Column(name = "ack_mensaje", columnDefinition = "TEXT")
    private String ackMensaje;

    @Column(name = "fecha_ack")
    private LocalDateTime fechaAck;

    // ===== PROCESAMIENTO EN NUESTRO SISTEMA =====
    @Column(name = "tabla_destino", length = 50)
    private String tablaDestino; // Tabla donde se guardó (ej: hospitalizacion_signos_vitales)

    @Column(name = "registro_destino_id")
    private Long registroDestinoId; // ID del registro creado

    // ===== REINTENTO Y LOGGING =====
    @Column(name = "intentos_procesamiento")
    private Integer intentosProcesamiento = 0;

    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @Column(name = "requiere_intervencion", length = 1)
    private String requiereIntervencion = "N";

    // ===== AUDITORÍA =====
    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "procesado_por", length = 50)
    private String procesadoPor; // Usuario o proceso

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        if (fechaRecepcion == null) {
            fechaRecepcion = LocalDateTime.now();
        }
    }

    // ===== MÉTODOS DE CONVENIENCIA =====
    public boolean esProcesado() {
        return "02".equals(estado);
    }

    public boolean tieneError() {
        return "03".equals(estado);
    }

    public boolean esUrgente() {
        return "U".equals(prioridad) || "E".equals(prioridad);
    }

    public boolean ackRecibido() {
        return "S".equals(ackEnviado);
    }
}