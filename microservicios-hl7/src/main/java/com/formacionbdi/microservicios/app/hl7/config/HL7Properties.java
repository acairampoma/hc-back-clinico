// =====================================================
// ⚙️ CONFIG 2: HL7 PROPERTIES - CONFIGURACIÓN PROPERTIES
// =====================================================
package com.formacionbdi.microservicios.app.hl7.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.DecimalMax;
import java.util.List;

/**
 * ⚙️ HL7 PROPERTIES - CONFIGURACIÓN CENTRALIZADA
 * 🎯 Responsabilidades:
 *    - Centralizar todas las configuraciones HL7
 *    - Validación de parámetros
 *    - Configuración por perfiles (dev/test/prod)
 *    - Properties tipadas y documentadas
 *
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + Spring Boot Configuration Properties
 * ✅ VERSIÓN CORREGIDA: Caracteres MLLP compatibles con simulador
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "hl7")
@Validated
public class HL7Properties {

    // ===== CONFIGURACIÓN DEL SERVIDOR =====
    @NotNull
    private Server server = new Server();

    @Data
    public static class Server {
        @Min(1024)
        @Max(65535)
        private int port = 2575;

        @NotBlank
        private String host = "0.0.0.0";

        @Min(1)
        @Max(1000)
        private int maxConnections = 200;

        @Min(1000)
        @Max(300000)
        private int timeout = 60000;

        private boolean autoAck = true;

        @Min(10)
        @Max(500)
        private int threadPoolSize = 100;

        private boolean keepAlive = true;
        private boolean tcpNoDelay = true;

        @Min(1000)
        @Max(120000)
        private int soTimeout = 60000;

        @Min(1024)
        @Max(65536)
        private int bufferSize = 16384;
    }

    // ===== CONFIGURACIÓN DEL PROTOCOLO - ✅ CORREGIDO =====
    @NotNull
    private Protocol protocol = new Protocol();

    @Data
    public static class Protocol {
        @NotBlank
        private String version = "2.5";

        @NotBlank
        private String charset = "UTF-8";

        // ✅ CORREGIDO: Usar caracteres hexadecimales directos
        @NotBlank
        private String startChar = String.valueOf((char) 0x0B);  // Vertical Tab

        // ✅ CORREGIDO: Concatenar los dos caracteres de fin MLLP
        @NotBlank
        private String endChar = String.valueOf((char) 0x1C) + String.valueOf((char) 0x0D);  // FS + CR

        @Min(1000)
        @Max(30000)
        private int mllpTimeout = 10000;

        private boolean validateStructure = true;
        private boolean strictValidation = false;

        @NotBlank
        private String encoding = "UTF-8";

        @Min(1024)
        @Max(10485760) // 10MB
        private int messageMaxSize = 2097152; // 2MB

        private boolean compressionEnabled = false;
    }

    // ===== CONFIGURACIÓN DEL PROCESAMIENTO =====
    @NotNull
    private Processing processing = new Processing();

    @Data
    public static class Processing {
        @Min(1)
        @Max(10)
        private int retryAttempts = 5;

        private boolean deadLetterQueue = true;

        @Min(10)
        @Max(1000)
        private int batchSize = 50;

        private boolean asyncProcessing = true;

        @Min(100)
        @Max(10000)
        private int maxQueueSize = 2000;

        @Min(5000)
        @Max(300000)
        private int processingTimeout = 60000;

        private boolean parallelProcessing = true;

        @Min(5)
        @Max(100)
        private int workerThreads = 20;
    }

    // ===== CONFIGURACIÓN OUTBOUND =====
    @NotNull
    private Outbound outbound = new Outbound();

    @Data
    public static class Outbound {
        private boolean enabled = true;

        @Min(100)
        @Max(10000)
        private int queueCapacity = 2000;

        @Min(1)
        @Max(50)
        private int senderThreads = 10;

        @Min(1000)
        @Max(60000)
        private int retryDelay = 10000;

        @Min(60000)
        @Max(3600000)
        private int maxRetryDelay = 600000;

        @Min(5000)
        @Max(60000)
        private int connectionTimeout = 15000;
    }

    // ===== CONFIGURACIÓN DEL DEMONIO =====
    @NotNull
    private Daemon daemon = new Daemon();

    @Data
    public static class Daemon {
        private boolean enabled = true;

        @Min(1000)
        @Max(60000)
        private long checkInterval = 5000;

        @Min(10)
        @Max(1000)
        private int batchProcessSize = 100;

        @Min(60000)
        @Max(1800000)
        private long maxIdleTime = 300000;

        private boolean autoRestart = true;

        @Min(10000)
        @Max(300000)
        private long heartbeatInterval = 30000;
    }

    // ===== CONFIGURACIÓN DE EQUIPOS =====
    @NotNull
    private Equipment equipment = new Equipment();

    @Data
    public static class Equipment {
        @Min(10000)
        @Max(300000)
        private long monitorFrequency = 30000;

        @Min(10000)
        @Max(300000)
        private long vitalSignsInterval = 30000;

        private boolean autoDiscovery = false;

        private List<String> allowedMessageTypes = List.of(
                "ORU^R01", "ADT^A01", "ADT^A02", "ADT^A03"
        );
    }

    // ===== CONFIGURACIÓN DE SEGURIDAD =====
    @NotNull
    private Security security = new Security();

    @Data
    public static class Security {
        private boolean enabled = true;

        @NotBlank
        private String allowedIps = "127.0.0.1,192.168.1.0/24,10.0.0.0/8";

        @Min(1024)
        @Max(10485760)
        private int maxMessageSize = 2097152;

        private RateLimiting rateLimiting = new RateLimiting();

        @Data
        public static class RateLimiting {
            private boolean enabled = true;

            @Min(10)
            @Max(10000)
            private int maxRequestsPerMinute = 1000;

            @Min(100)
            @Max(100000)
            private int maxRequestsPerHour = 10000;
        }
    }

    // ===== CONFIGURACIÓN DE MONITOREO =====
    @NotNull
    private Monitoring monitoring = new Monitoring();

    @Data
    public static class Monitoring {
        private boolean enabled = true;

        @Min(5000)
        @Max(300000)
        private long metricsInterval = 30000;

        @Min(5000)
        @Max(300000)
        private long healthCheckInterval = 15000;

        private boolean performanceMonitoring = true;
        private boolean errorTracking = true;
        private boolean throughputMonitoring = true;
    }

    // ===== CONFIGURACIÓN DE CACHE =====
    @NotNull
    private Cache cache = new Cache();

    @Data
    public static class Cache {
        private boolean enabled = true;

        @NotBlank
        private String spec = "maximumSize=1000,expireAfterWrite=10m";

        private List<String> cacheNames = List.of(
                "hl7-equipos", "hl7-config", "hl7-templates"
        );
    }

    // ===== CONFIGURACIÓN ASYNC =====
    @NotNull
    private Async async = new Async();

    @Data
    public static class Async {
        @Min(5)
        @Max(50)
        private int corePoolSize = 10;

        @Min(10)
        @Max(200)
        private int maxPoolSize = 50;

        @Min(100)
        @Max(2000)
        private int queueCapacity = 500;

        @NotBlank
        private String threadNamePrefix = "HL7-Daemon-";

        @Min(30)
        @Max(300)
        private int keepAliveSeconds = 60;

        private boolean allowCoreThreadTimeout = true;
    }

    // ===== CONFIGURACIÓN DE LOGGING =====
    @NotNull
    private Logging logging = new Logging();

    @Data
    public static class Logging {
        private boolean messageLogging = true;
        private boolean performanceLogging = true;
        private boolean errorDetailLogging = true;
        private boolean connectionLogging = true;

        @Min(1)
        @Max(90)
        private int retentionDays = 30;

        @NotBlank
        private String logLevel = "INFO";

        private boolean structuredLogging = false;
    }

    // ===== CONFIGURACIÓN DE DESTINOS =====
    @NotNull
    private Destinations destinations = new Destinations();

    @Data
    public static class Destinations {
        private HisExterno hisExterno = new HisExterno();
        private SistemaCitas sistemaCitas = new SistemaCitas();
        private Laboratorio laboratorio = new Laboratorio();
        private Farmacia farmacia = new Farmacia();
        private MonitorSystem monitorSystem = new MonitorSystem();

        @Data
        public static class HisExterno {
            @NotBlank
            private String ip = "192.168.1.100";

            @Min(1024)
            @Max(65535)
            private int puerto = 2575;

            private boolean activo = true;
            private int timeoutSegundos = 30;
        }

        @Data
        public static class SistemaCitas {
            @NotBlank
            private String ip = "192.168.1.101";

            @Min(1024)
            @Max(65535)
            private int puerto = 2575;

            private boolean activo = true;
            private int timeoutSegundos = 30;
        }

        @Data
        public static class Laboratorio {
            @NotBlank
            private String ip = "192.168.1.102";

            @Min(1024)
            @Max(65535)
            private int puerto = 2575;

            private boolean activo = true;
            private int timeoutSegundos = 30;
        }

        @Data
        public static class Farmacia {
            @NotBlank
            private String ip = "192.168.1.103";

            @Min(1024)
            @Max(65535)
            private int puerto = 2575;

            private boolean activo = true;
            private int timeoutSegundos = 30;
        }

        @Data
        public static class MonitorSystem {
            @NotBlank
            private String ip = "192.168.1.200";

            @Min(1024)
            @Max(65535)
            private int puerto = 2575;

            private boolean activo = true;
            private int timeoutSegundos = 30;
        }
    }

    // ===== CONFIGURACIÓN DE ALERTAS =====
    @NotNull
    private Alerts alerts = new Alerts();

    @Data
    public static class Alerts {
        private boolean enabled = true;
        private boolean emailEnabled = false;
        private boolean slackEnabled = false;

        @NotBlank
        private String emailSmtp = "smtp.hospital.com";

        private int emailPort = 587;

        @NotBlank
        private String emailFrom = "hl7-system@hospital.com";

        private String emailTo = "admin@hospital.com";
        private String slackWebhook = "";

        private Thresholds thresholds = new Thresholds();

        @Data
        public static class Thresholds {
            @Min(1)
            @Max(60)
            private int equipoSinComunicacionMinutos = 10;

            @Min(10)
            @Max(10000)
            private int colaPendientesAlerta = 100;

            // Para double usamos @DecimalMin/@DecimalMax
            @javax.validation.constraints.DecimalMin("1.0")
            @javax.validation.constraints.DecimalMax("50.0")
            private double tasaErrorMaxima = 5.0;

            @Min(100)
            @Max(100000)
            private int mensajesPorMinutoMaximo = 1000;
        }
    }

    // ===== POST CONSTRUCT - VALIDACIÓN E INICIALIZACIÓN =====
    @PostConstruct
    public void inicializar() {
        log.info("🔧 INICIALIZANDO CONFIGURACIÓN HL7");

        // ✅ LOG CARACTERES MLLP PARA DEBUGGING
        logCaracteresMllp();

        // Validar configuración
        validarConfiguracion();

        // Log de configuración clave
        logConfiguracionClave();

        log.info("✅ CONFIGURACIÓN HL7 INICIALIZADA CORRECTAMENTE");
    }

    /**
     * ✅ LOG CARACTERES MLLP PARA DEBUGGING
     */
    private void logCaracteresMllp() {
        log.info("🔧 PROTOCOLO MLLP:");
        log.info("   - Start char: 0x{} ({})",
                Integer.toHexString(protocol.getStartChar().charAt(0)).toUpperCase(),
                protocol.getStartChar().length());
        log.info("   - End chars: 0x{} 0x{} ({})",
                Integer.toHexString(protocol.getEndChar().charAt(0)).toUpperCase(),
                Integer.toHexString(protocol.getEndChar().charAt(1)).toUpperCase(),
                protocol.getEndChar().length());
        log.info("   - MLLP Timeout: {}ms", protocol.getMllpTimeout());
    }

    private void validarConfiguracion() {
        // Validaciones personalizadas
        if (server.getMaxConnections() > 500) {
            log.warn("⚠️ ADVERTENCIA: Muchas conexiones máximas configuradas: {}", server.getMaxConnections());
        }

        if (daemon.getCheckInterval() < 2000) {
            log.warn("⚠️ ADVERTENCIA: Intervalo de demonio muy bajo: {}ms", daemon.getCheckInterval());
        }

        if (processing.getBatchSize() > 200) {
            log.warn("⚠️ ADVERTENCIA: Batch size muy grande: {}", processing.getBatchSize());
        }

        // ✅ VALIDAR CARACTERES MLLP
        validarCaracteresMllp();

        // Validar IPs de destino
        validarDestinos();
    }

    /**
     * ✅ VALIDAR CARACTERES MLLP
     */
    private void validarCaracteresMllp() {
        if (protocol.getStartChar().length() != 1) {
            log.error("❌ ERROR: Start char debe tener exactamente 1 carácter, tiene: {}",
                    protocol.getStartChar().length());
        }

        if (protocol.getEndChar().length() != 2) {
            log.error("❌ ERROR: End char debe tener exactamente 2 caracteres, tiene: {}",
                    protocol.getEndChar().length());
        }

        // Verificar que los caracteres sean los correctos
        char expectedStart = 0x0B;
        char expectedEnd1 = 0x1C;
        char expectedEnd2 = 0x0D;

        if (protocol.getStartChar().charAt(0) != expectedStart) {
            log.warn("⚠️ ADVERTENCIA: Start char no es el estándar MLLP. " +
                            "Esperado: 0x{}, Actual: 0x{}",
                    Integer.toHexString(expectedStart).toUpperCase(),
                    Integer.toHexString(protocol.getStartChar().charAt(0)).toUpperCase());
        }

        if (protocol.getEndChar().length() >= 2) {
            if (protocol.getEndChar().charAt(0) != expectedEnd1 ||
                    protocol.getEndChar().charAt(1) != expectedEnd2) {
                log.warn("⚠️ ADVERTENCIA: End chars no son el estándar MLLP. " +
                                "Esperado: 0x{} 0x{}, Actual: 0x{} 0x{}",
                        Integer.toHexString(expectedEnd1).toUpperCase(),
                        Integer.toHexString(expectedEnd2).toUpperCase(),
                        Integer.toHexString(protocol.getEndChar().charAt(0)).toUpperCase(),
                        Integer.toHexString(protocol.getEndChar().charAt(1)).toUpperCase());
            }
        }
    }

    private void validarDestinos() {
        if (!destinations.getHisExterno().isActivo() &&
                !destinations.getSistemaCitas().isActivo() &&
                !destinations.getLaboratorio().isActivo()) {
            log.warn("⚠️ ADVERTENCIA: Todos los destinos están desactivados");
        }
    }

    private void logConfiguracionClave() {
        log.info("📡 SERVIDOR HL7:");
        log.info("   - Puerto: {}", server.getPort());
        log.info("   - Host: {}", server.getHost());
        log.info("   - Max conexiones: {}", server.getMaxConnections());
        log.info("   - Auto ACK: {}", server.isAutoAck());

        log.info("🤖 DEMONIO:");
        log.info("   - Habilitado: {}", daemon.isEnabled());
        log.info("   - Intervalo: {}ms", daemon.getCheckInterval());
        log.info("   - Batch size: {}", daemon.getBatchProcessSize());

        log.info("📤 OUTBOUND:");
        log.info("   - Habilitado: {}", outbound.isEnabled());
        log.info("   - Cola capacity: {}", outbound.getQueueCapacity());
        log.info("   - Sender threads: {}", outbound.getSenderThreads());

        log.info("🔒 SEGURIDAD:");
        log.info("   - Habilitada: {}", security.isEnabled());
        log.info("   - IPs permitidas: {}", security.getAllowedIps());
        log.info("   - Rate limiting: {}", security.getRateLimiting().isEnabled());

        log.info("📊 MONITOREO:");
        log.info("   - Habilitado: {}", monitoring.isEnabled());
        log.info("   - Métricas cada: {}ms", monitoring.getMetricsInterval());
        log.info("   - Health check cada: {}ms", monitoring.getHealthCheckInterval());
    }

    // ===== MÉTODOS UTILITARIOS =====

    /**
     * 🎯 OBTENER CONFIGURACIÓN DE DESTINO POR NOMBRE
     */
    public Object getDestinoConfig(String nombreDestino) {
        switch (nombreDestino.toUpperCase()) {
            case "HIS_EXTERNO":
                return destinations.getHisExterno();
            case "SISTEMA_CITAS":
                return destinations.getSistemaCitas();
            case "LABORATORIO":
                return destinations.getLaboratorio();
            case "FARMACIA":
                return destinations.getFarmacia();
            case "MONITOR_SYSTEM":
                return destinations.getMonitorSystem();
            default:
                log.warn("⚠️ Destino no encontrado: {}", nombreDestino);
                return null;
        }
    }

    /**
     * 🔍 VERIFICAR SI UN TIPO DE MENSAJE ESTÁ PERMITIDO
     */
    public boolean isTipoMensajePermitido(String tipoMensaje) {
        return equipment.getAllowedMessageTypes().contains(tipoMensaje);
    }

    /**
     * ✅ OBTENER CARACTERES MLLP COMO VALORES PRIMITIVOS
     */
    public char getStartCharValue() {
        return protocol.getStartChar().charAt(0);
    }

    public char getEndChar1Value() {
        return protocol.getEndChar().charAt(0);
    }

    public char getEndChar2Value() {
        return protocol.getEndChar().charAt(1);
    }

    /**
     * 📊 OBTENER RESUMEN DE CONFIGURACIÓN
     */
    public String getResumenConfiguracion() {
        return String.format(
                "HL7 Config: Server[%s:%d] Daemon[%s] Outbound[%s] Security[%s] MLLP[0x%s,0x%s,0x%s]",
                server.getHost(),
                server.getPort(),
                daemon.isEnabled() ? "ON" : "OFF",
                outbound.isEnabled() ? "ON" : "OFF",
                security.isEnabled() ? "ON" : "OFF",
                Integer.toHexString(getStartCharValue()).toUpperCase(),
                Integer.toHexString(getEndChar1Value()).toUpperCase(),
                Integer.toHexString(getEndChar2Value()).toUpperCase()
        );
    }
}