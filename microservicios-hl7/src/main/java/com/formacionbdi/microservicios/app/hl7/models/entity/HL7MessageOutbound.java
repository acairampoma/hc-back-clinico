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
 * 📤 ENTIDAD OUTBOUND: Mensajes HL7 que ENVIAMOS a sistemas externos
 * 🎯 Mapea tabla: hl7_mensajes_outbound
 * 👨‍💻 Desarrollador: Alan Cairampoma
 */
@Entity
@Table(name = "hl7_mensajes_outbound")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class HL7MessageOutbound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== IDENTIFICACIÓN DEL MENSAJE =====
    @Column(name = "mensaje_id", unique = true, nullable = false, length = 50)
    private String mensajeId; // Control ID que generamos

    @Column(name = "tipo_mensaje", nullable = false, length = 20)
    private String tipoMensaje; // ADT^A28, SIU^S13, ORM^O01, etc.

    @Column(name = "version_hl7", length = 10)
    private String versionHl7 = "2.5";

    // ===== ORIGEN (NUESTRO SISTEMA) =====
    @Column(name = "aplicacion_origen", length = 50)
    private String aplicacionOrigen = "HIS_SYSTEM"; // MSH.3

    @Column(name = "facility_origen", length = 50)
    private String facilityOrigen = "HOSPITAL"; // MSH.4

    // ===== DESTINO =====
    @Column(name = "sistema_destino", nullable = false, length = 50)
    private String sistemaDestino; // Sistema que recibirá el mensaje

    @Column(name = "ip_destino")
    private String ipDestino; // IP del sistema destino

    @Column(name = "puerto_destino")
    private Integer puertoDestino = 2575; // Puerto MLLP destino

    // ===== DATOS DEL PROCESO QUE GENERA EL MENSAJE =====
    @Column(name = "tabla_origen", nullable = false, length = 50)
    private String tablaOrigen; // Tabla que generó el evento

    @Column(name = "registro_origen_id", nullable = false)
    private Long registroOrigenId; // ID del registro que generó el evento

    @Column(name = "proceso_origen", nullable = false, length = 100)
    private String procesoOrigen; // Descripción del proceso

    // ===== DATOS DEL PACIENTE =====
    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "numero_documento", length = 15)
    private String numeroDocumento;

    @Column(name = "numero_cuenta", length = 20)
    private String numeroCuenta; // Si está hospitalizado

    // ===== CONTENIDO DEL MENSAJE HL7 =====
    @Column(name = "mensaje_hl7", columnDefinition = "TEXT", nullable = false)
    private String mensajeHl7; // Mensaje HL7 completo generado

    @Type(type = "jsonb")
    @Column(name = "mensaje_json", columnDefinition = "jsonb")
    private Map<String, Object> mensajeJson; // Datos estructurados para generar HL7

    // ===== ESTADOS DE ENVÍO =====
    @Column(name = "estado", length = 2)
    private String estado = "01"; // 01=Pendiente, 02=Enviado, 03=Error, 04=ACK_Recibido

    @Column(name = "prioridad", length = 1)
    private String prioridad = "R"; // E=Emergencia, U=Urgente, R=Regular

    // ===== CONTROL DE ENVÍO =====
    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "intentos_envio")
    private Integer intentosEnvio = 0;

    @Column(name = "max_intentos")
    private Integer maxIntentos = 3;

    // ===== RESPUESTA DEL DESTINO =====
    @Column(name = "ack_recibido", length = 1)
    private String ackRecibido = "N";

    @Column(name = "ack_codigo", length = 10)
    private String ackCodigo; // AA=Application Accept, AE=Application Error

    @Column(name = "ack_mensaje", columnDefinition = "TEXT")
    private String ackMensaje;

    @Column(name = "fecha_ack")
    private LocalDateTime fechaAck;

    // ===== ERRORES Y REINTENTOS =====
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @Column(name = "requiere_intervencion", length = 1)
    private String requiereIntervencion = "N";

    // ===== AUDITORÍA =====
    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "generado_por", length = 50)
    private String generadoPor;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        if (fechaGeneracion == null) {
            fechaGeneracion = LocalDateTime.now();
        }
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // ===== MÉTODOS DE CONVENIENCIA =====
    public boolean estaPendiente() {
        return "01".equals(estado);
    }

    public boolean fueEnviado() {
        return "02".equals(estado);
    }

    public boolean tieneError() {
        return "03".equals(estado);
    }

    public boolean ackConfirmado() {
        return "04".equals(estado);
    }

    public boolean puedeReintentar() {
        return intentosEnvio < maxIntentos && !"N".equals(requiereIntervencion);
    }

    public void incrementarIntentos() {
        this.intentosEnvio++;
        if (this.intentosEnvio >= this.maxIntentos) {
            this.requiereIntervencion = "S";
        }
    }
}