package com.formacionbdi.microservicios.app.hl7.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * ⚙️ ENTIDAD CONFIGURACIÓN: Equipos médicos autorizados para HL7
 * 🎯 Mapea tabla: hl7_equipos_config
 * 👨‍💻 Desarrollador: Alan Cairampoma
 */
@Entity
@Table(name = "hl7_equipos_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HL7EquipoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== IDENTIFICACIÓN DEL EQUIPO =====
    @Column(name = "codigo_equipo", unique = true, nullable = false, length = 20)
    private String codigoEquipo;

    @Column(name = "nombre_equipo", nullable = false, length = 100)
    private String nombreEquipo;

    @Column(name = "fabricante", length = 50)
    private String fabricante;

    @Column(name = "modelo", length = 50)
    private String modelo;

    @Column(name = "numero_serie", length = 50)
    private String numeroSerie;

    // ===== CONFIGURACIÓN DE RED =====
    @Column(name = "ip_permitida", length = 50)
    private String ipPermitida; // IP desde donde puede enviar mensajes

    @Column(name = "puerto_origen")
    private Integer puertoOrigen;

    @Column(name = "requiere_vpn", length = 1)
    private String requiereVpn = "N";

    // ===== CONFIGURACIÓN HL7 =====
    @Column(name = "version_hl7", length = 10)
    private String versionHl7 = "2.5";

    @Column(name = "aplicacion_nombre", nullable = false, length = 50)
    private String aplicacionNombre; // MSH.3 que debe usar

    @Column(name = "facility_nombre", length = 50)
    private String facilityNombre; // MSH.4 que debe usar

    // ===== TIPOS DE MENSAJE PERMITIDOS =====
    @Column(name = "tipos_mensaje_permitidos", columnDefinition = "TEXT")
    private String tiposMensajePermitidos; // JSON array: ["ORU^R01", "ADT^A08"]

    // ===== CONFIGURACIÓN ESPECÍFICA =====
    @Column(name = "frecuencia_envio_segundos")
    private Integer frecuenciaEnvioSegundos = 30; // Cada cuánto envía datos

    @Column(name = "formato_timestamp", length = 20)
    private String formatoTimestamp = "YYYYMMDDHHMMSS";

    @Column(name = "charset", length = 10)
    private String charset = "UTF-8";

    // ===== UBICACIÓN FÍSICA =====
    @Column(name = "servicio", length = 50)
    private String servicio; // UCI, EMERGENCIA, HOSPITALIZACION

    @Column(name = "piso")
    private Integer piso;

    @Column(name = "ala", length = 10)
    private String ala;

    @Column(name = "numero_cama", length = 15)
    private String numeroCama;

    // ===== ESTADOS Y MONITORING =====
    @Column(name = "activo", length = 1)
    private String activo = "S";

    @Column(name = "en_mantenimiento", length = 1)
    private String enMantenimiento = "N";

    @Column(name = "ultimo_mensaje")
    private LocalDateTime ultimoMensaje;

    @Column(name = "total_mensajes_enviados")
    private Integer totalMensajesEnviados = 0;

    @Column(name = "total_mensajes_error")
    private Integer totalMensajesError = 0;

    // ===== ALERTAS =====
    @Column(name = "alerta_sin_comunicacion_minutos")
    private Integer alertaSinComunicacionMinutos = 10; // Alerta si no envía en X minutos

    @Column(name = "notificar_errores", length = 1)
    private String notificarErrores = "S";

    @Column(name = "email_notificacion", length = 100)
    private String emailNotificacion;

    // ===== AUDITORÍA =====
    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "creado_por")
    private Long creadoPor;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // ===== MÉTODOS DE CONVENIENCIA =====
    public boolean estaActivo() {
        return "S".equals(activo);
    }

    public boolean estaEnMantenimiento() {
        return "S".equals(enMantenimiento);
    }

    public boolean sinComunicacion() {
        if (ultimoMensaje == null) return true;
        return ultimoMensaje.isBefore(
                LocalDateTime.now().minusMinutes(alertaSinComunicacionMinutos)
        );
    }

    public void registrarMensajeRecibido() {
        this.ultimoMensaje = LocalDateTime.now();
        this.totalMensajesEnviados++;
    }

    public void registrarError() {
        this.totalMensajesError++;
    }

    public double getTasaError() {
        if (totalMensajesEnviados == 0) return 0.0;
        return (double) totalMensajesError / totalMensajesEnviados * 100;
    }
}