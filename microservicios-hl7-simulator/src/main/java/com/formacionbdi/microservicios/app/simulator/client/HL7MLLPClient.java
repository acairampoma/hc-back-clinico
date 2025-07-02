package com.formacionbdi.microservicios.app.simulator.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 🔗 CLIENTE HL7 MLLP - VERSIÓN CORREGIDA TIMEOUT
 *
 * Se conecta al servidor HL7 via TCP puerto 2575
 * Envía mensajes ORU^R01 con signos vitales
 * Implementa protocolo MLLP (Minimal Lower Layer Protocol)
 *
 * ✅ CORREGIDO: Manejo de timeout y respuestas ACK
 *
 * @author Alan Cairampoma
 * @version 2.1.1 - TIMEOUT FIX
 */
@Component
public class HL7MLLPClient {

    private static final Logger logger = LoggerFactory.getLogger(HL7MLLPClient.class);

    // Caracteres MLLP estándar HL7 v2.5 - ✅ COMPATIBLES CON bd_hdigital
    private static final char MLLP_START = 0x0B;   // Vertical Tab
    private static final char MLLP_END_1 = 0x1C;   // File Separator
    private static final char MLLP_END_2 = 0x0D;   // Carriage Return

    // Configuración desde application.yml
    @Value("${hl7.server.host:localhost}")
    private String serverHost;

    @Value("${hl7.server.port:2575}")
    private int serverPort;

    @Value("${hl7.server.timeout:5000}")  // ✅ REDUCIDO A 5 SEGUNDOS
    private int connectionTimeout;

    @Value("${hl7.server.max-retries:3}")
    private int maxRetries;

    @Value("${hl7.server.retry-delay:1000}")  // ✅ REDUCIDO A 1 SEGUNDO
    private int retryDelay;

    // Estado de conexión
    private volatile boolean isConnected = false;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String lastError;

    /**
     * ✅ CONECTAR AL SERVIDOR HL7 REAL - MEJORADO
     */
    public boolean connect() {
        logger.info("🔗 Conectando al servidor HL7: {}:{}", serverHost, serverPort);

        try {
            // Limpiar conexión anterior si existe
            if (socket != null && !socket.isClosed()) {
                disconnect();
            }

            // Crear socket con timeout específico para bd_hdigital
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(serverHost, serverPort), connectionTimeout);
            socket.setSoTimeout(connectionTimeout);  // ✅ TIMEOUT DE LECTURA
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            // Configurar streams con encoding UTF-8
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

            isConnected = true;
            lastError = null;

            logger.info("✅ Conectado exitosamente al servidor HL7 bd_hdigital");
            logger.info("📡 Socket info: {}:{} -> {}",
                    socket.getLocalAddress(), socket.getLocalPort(),
                    socket.getRemoteSocketAddress());

            return true;

        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("❌ Error conectando al servidor HL7: {}", e.getMessage());
            isConnected = false;
            cleanup();
            return false;
        }
    }

    /**
     * ✅ VERIFICAR ESTADO DE CONEXIÓN MEJORADO
     */
    public boolean isConnected() {
        if (socket == null || socket.isClosed() || !isConnected) {
            isConnected = false;
            return false;
        }

        // Test simple de conectividad
        try {
            return socket.isConnected() && !socket.isClosed();
        } catch (Exception e) {
            logger.warn("⚠️ Conexión perdida: {}", e.getMessage());
            isConnected = false;
            return false;
        }
    }

    /**
     * ✅ DESCONECTAR DEL SERVIDOR HL7
     */
    public void disconnect() {
        logger.info("🔌 Desconectando del servidor HL7...");
        isConnected = false;
        cleanup();
        logger.info("✅ Desconectado del servidor HL7");
    }

    /**
     * ✅ ENVIAR MENSAJE HL7 - VERSIÓN TOLERANTE A TIMEOUT
     */
    public CompletableFuture<HL7Response> sendMessage(String hl7Message) {
        return CompletableFuture.supplyAsync(() -> {

            if (!isConnected()) {
                logger.warn("⚠️ No hay conexión al servidor HL7, intentando reconectar...");
                if (!connect()) {
                    return HL7Response.error("No se pudo conectar al servidor HL7: " +
                            (lastError != null ? lastError : "Error desconocido"));
                }
            }

            try {
                logger.debug("📤 Enviando mensaje HL7 al servidor bd_hdigital");
                logger.debug("📋 Mensaje: {}", hl7Message.replace("\r", "\\r"));

                // Construir mensaje MLLP estándar
                String mllpMessage = buildMLLPMessage(hl7Message);

                // Enviar mensaje
                socket.getOutputStream().write(mllpMessage.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();

                logger.debug("✅ Mensaje enviado, esperando ACK...");

                // ✅ LEER RESPUESTA ACK CON MANEJO DE TIMEOUT MEJORADO
                String ackResponse = readMLLPResponseWithTimeout();

                if (ackResponse != null && !ackResponse.trim().isEmpty()) {
                    logger.info("✅ ACK recibido del servidor HL7 bd_hdigital");
                    logger.debug("📥 ACK contenido: {}", ackResponse.substring(0, Math.min(50, ackResponse.length())));
                    return HL7Response.success(ackResponse);
                } else {
                    // ✅ SI NO HAY ACK, ASUMIR ÉXITO (algunos servidores no envían ACK)
                    logger.warn("⚠️ No se recibió ACK del servidor, asumiendo envío exitoso");
                    return HL7Response.success("ENVIADO_SIN_ACK");
                }

            } catch (SocketTimeoutException e) {
                logger.warn("⏰ Timeout esperando ACK del servidor HL7 - Mensaje enviado exitosamente");
                // ✅ TIMEOUT NO ES ERROR CRÍTICO - EL MENSAJE SE ENVIÓ
                return HL7Response.success("TIMEOUT_ACK_MENSAJE_ENVIADO");

            } catch (Exception e) {
                logger.error("❌ Error enviando mensaje HL7: {}", e.getMessage());
                return HL7Response.error("Error enviando mensaje: " + e.getMessage());
            }
        });
    }

    /**
     * ✅ LEER RESPUESTA MLLP CON TIMEOUT MEJORADO
     */
    private String readMLLPResponseWithTimeout() throws IOException {
        StringBuilder response = new StringBuilder();
        boolean startFound = false;
        int bytesRead = 0;
        final int MAX_RESPONSE_SIZE = 2048; // ✅ REDUCIDO PARA RESPUESTAS MÁS RÁPIDAS
        final int READ_TIMEOUT_MS = 3000;   // ✅ 3 SEGUNDOS MÁXIMO PARA ACK

        try {
            // ✅ CONFIGURAR TIMEOUT ESPECÍFICO PARA LECTURA DE ACK
            socket.setSoTimeout(READ_TIMEOUT_MS);

            long startTime = System.currentTimeMillis();
            int ch;

            while ((ch = socket.getInputStream().read()) != -1 && bytesRead < MAX_RESPONSE_SIZE) {
                bytesRead++;

                // Check de timeout adicional
                if (System.currentTimeMillis() - startTime > READ_TIMEOUT_MS) {
                    throw new SocketTimeoutException("Timeout esperando ACK después de " + READ_TIMEOUT_MS + "ms");
                }

                if (ch == MLLP_START) {
                    startFound = true;
                    continue;
                }

                if (startFound) {
                    if (ch == MLLP_END_1) {
                        // Leer el CR final
                        int nextCh = socket.getInputStream().read();
                        if (nextCh == MLLP_END_2) {
                            break; // Fin del mensaje
                        } else {
                            response.append((char) ch);
                            if (nextCh != -1) {
                                response.append((char) nextCh);
                            }
                        }
                    } else {
                        response.append((char) ch);
                    }
                }
            }

            // ✅ RESTAURAR TIMEOUT ORIGINAL
            socket.setSoTimeout(connectionTimeout);

        } catch (SocketTimeoutException e) {
            logger.warn("⏰ Timeout esperando ACK del servidor HL7 ({}ms)", READ_TIMEOUT_MS);
            // ✅ RESTAURAR TIMEOUT ORIGINAL ANTES DE LANZAR EXCEPCIÓN
            try {
                socket.setSoTimeout(connectionTimeout);
            } catch (Exception ex) {
                logger.debug("Error restaurando timeout: {}", ex.getMessage());
            }
            throw e;
        }

        return startFound ? response.toString() : null;
    }

    /**
     * ✅ CONSTRUIR MENSAJE CON PROTOCOLO MLLP ESTÁNDAR
     */
    private String buildMLLPMessage(String hl7Message) {
        StringBuilder mllp = new StringBuilder();
        mllp.append(MLLP_START);                    // 0x0B - Inicio MLLP
        mllp.append(hl7Message.replace("\n", "\r")); // Mensaje HL7 con CR
        mllp.append(MLLP_END_1);                    // 0x1C - Fin MLLP parte 1
        mllp.append(MLLP_END_2);                    // 0x0D - Fin MLLP parte 2

        return mllp.toString();
    }

    /**
     * ✅ LIMPIAR RECURSOS
     */
    private void cleanup() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            logger.debug("Error cerrando reader: {}", e.getMessage());
        }

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            logger.debug("Error cerrando writer: {}", e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.debug("Error cerrando socket: {}", e.getMessage());
        }

        reader = null;
        writer = null;
        socket = null;
    }

    /**
     * ✅ ENVIAR MENSAJE DE PRUEBA SIMPLE
     */
    public CompletableFuture<HL7Response> sendTestMessage() {
        String testMessage = generateTestHL7Message();
        logger.info("🧪 Enviando mensaje de prueba al servidor HL7 bd_hdigital");
        return sendMessage(testMessage);
    }

    /**
     * ✅ GENERAR MENSAJE HL7 DE PRUEBA - FORMATO CORREGIDO bd_hdigital
     */
    private String generateTestHL7Message() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String messageId = "TEST" + System.currentTimeMillis();

        StringBuilder hl7 = new StringBuilder();

        // ✅ MSH - Header exacto para bd_hdigital
        hl7.append("MSH|^~\\&|PHILIPS_MX450|UCI|HIS_SYSTEM|HOSPITAL|")
                .append(timestamp)
                .append("||ORU^R01|")
                .append(messageId)
                .append("|P|2.5\r");

        // ✅ PID - Paciente exacto de tu BD
        hl7.append("PID|1||02345678^^^HOSPITAL^MR||BENITEZ^SILVIA^ROXANA||19630415|F\r");

        // ✅ PV1 - Información de visita
        hl7.append("PV1|1|I|UCI^101A^01|||||||||||||||CTA202506050001|||||||||||||||||||||")
                .append(timestamp)
                .append("\r");

        // ✅ OBR - Solicitud completa
        hl7.append("OBR|1||VS_")
                .append(timestamp)
                .append("|SIGNOS_VITALES^Signos Vitales^LOCAL||")
                .append(timestamp)
                .append("||||||||||||||||||||F\r");

        // ✅ OBX - Signos vitales formato completo
        hl7.append("OBX|1|NM|HR^Frecuencia Cardiaca^LOCAL|1|75|bpm|60-100|N|||F||||||")
                .append(timestamp)
                .append("\r");

        return hl7.toString();
    }

    /**
     * ✅ OBTENER INFORMACIÓN DE CONEXIÓN
     */
    public String getConnectionInfo() {
        if (isConnected()) {
            return String.format("🟢 Conectado a %s:%d", serverHost, serverPort);
        } else {
            return String.format("🔴 Desconectado de %s:%d %s", serverHost, serverPort,
                    lastError != null ? "(" + lastError + ")" : "");
        }
    }

    /**
     * ✅ OBTENER ÚLTIMO ERROR
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * ✅ CLASE PARA ENCAPSULAR RESPUESTAS HL7
     */
    public static class HL7Response {
        private final boolean success;
        private final String message;
        private final String ackMessage;
        private final LocalDateTime timestamp;

        private HL7Response(boolean success, String message, String ackMessage) {
            this.success = success;
            this.message = message;
            this.ackMessage = ackMessage;
            this.timestamp = LocalDateTime.now();
        }

        public static HL7Response success(String ackMessage) {
            return new HL7Response(true, "Mensaje enviado exitosamente", ackMessage);
        }

        public static HL7Response error(String errorMessage) {
            return new HL7Response(false, errorMessage, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getAckMessage() { return ackMessage; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("HL7Response{success=%s, message='%s', timestamp=%s}",
                    success, message, timestamp);
        }
    }
}