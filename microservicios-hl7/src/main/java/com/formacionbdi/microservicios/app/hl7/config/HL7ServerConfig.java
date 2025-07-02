// =====================================================
// ⚙️ CONFIG 1: HL7 SERVER CONFIG - SERVIDOR TCP MLLP COMPLETO
// =====================================================
package com.formacionbdi.microservicios.app.hl7.config;

import com.formacionbdi.microservicios.app.hl7.models.entity.HL7EquipoConfig;
import com.formacionbdi.microservicios.app.hl7.repository.HL7EquipoConfigRepository;
import com.formacionbdi.microservicios.app.hl7.repository.HL7MessageRepository;
import com.formacionbdi.microservicios.app.hl7.services.HL7ProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentMap;

/**
 * ⚙️ HL7 SERVER CONFIG - CONFIGURACIÓN DEL SERVIDOR TCP MLLP
 * 🎯 Responsabilidades:
 *    - Servidor TCP no bloqueante en puerto 2575
 *    - Protocolo MLLP (Minimal Lower Layer Protocol)
 *    - Recepción de mensajes HL7 de equipos médicos
 *    - ACK/NACK automático
 *    - Control de duplicados MSH.10
 *    - Integración con HL7ProcessorService
 *
 * 👨‍💻 Desarrollador: Alan Cairampoma
 * 🔧 Tecnología: Java 11 + NIO + Spring Boot
 * ✅ VERSIÓN COMPLETA: Con procesamiento real de BD
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class HL7ServerConfig {

    // ===== CARACTERES MLLP ESTÁNDAR - ✅ HARDCODEADOS PARA MÁXIMA COMPATIBILIDAD =====
    private static final char MLLP_START_CHAR = 0x0B;   // Vertical Tab
    private static final char MLLP_END_CHAR_1 = 0x1C;   // File Separator
    private static final char MLLP_END_CHAR_2 = 0x0D;   // Carriage Return

    // ===== CONFIGURACIÓN DESDE YAML =====
    @Value("${hl7.server.port:2575}")
    private int serverPort;

    @Value("${hl7.server.host:0.0.0.0}")
    private String serverHost;

    @Value("${hl7.server.max-connections:200}")
    private int maxConnections;

    @Value("${hl7.server.timeout:60000}")
    private int timeout;

    @Value("${hl7.server.auto-ack:true}")
    private boolean autoAck;

    @Value("${hl7.server.thread-pool-size:100}")
    private int threadPoolSize;

    @Value("${hl7.server.buffer-size:16384}")
    private int bufferSize;

    // ===== INYECCIÓN DE DEPENDENCIAS =====
    @Autowired
    private HL7Properties hl7Properties;

    @Autowired
    private HL7ProcessorService hl7ProcessorService;

    @Autowired
    private HL7MessageRepository hl7MessageRepository;

    @Autowired
    private HL7EquipoConfigRepository equipoConfigRepository;

    // ===== SERVIDOR TCP VARIABLES =====
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private final AtomicBoolean serverRunning = new AtomicBoolean(false);
    private Thread serverThread;

    // ===== MÉTRICAS DEL SERVIDOR =====
    private final AtomicLong totalConexiones = new AtomicLong(0);
    private final AtomicLong mensajesRecibidos = new AtomicLong(0);
    private final AtomicLong mensajesEnviados = new AtomicLong(0);
    private final AtomicLong mensajesExitosos = new AtomicLong(0);
    private final AtomicLong mensajesError = new AtomicLong(0);
    private final AtomicLong mensajesDuplicados = new AtomicLong(0);
    private final Set<SocketChannel> conexionesActivas = ConcurrentHashMap.newKeySet();

    // ===== CACHE DE EQUIPOS Y CONTROL DE DUPLICADOS =====
    private final ConcurrentMap<String, Boolean> equiposAutorizados = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> contadorExitosEquipo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> contadorErroresEquipo = new ConcurrentHashMap<>();

    // ===== INICIALIZACIÓN DEL SERVIDOR =====
    @PostConstruct
    public void iniciarServidorHL7() {
        log.info("🚀 INICIANDO SERVIDOR HL7 TCP MLLP");
        log.info("📡 Puerto: {}", serverPort);
        log.info("🌐 Host: {}", serverHost);
        log.info("🔗 Max conexiones: {}", maxConnections);
        log.info("⏱️ Timeout: {}ms", timeout);
        log.info("✅ Auto ACK: {}", autoAck);

        // ✅ LOG CARACTERES MLLP PARA VERIFICACIÓN
        logCaracteresMLLP();

        // ✅ CARGAR EQUIPOS AUTORIZADOS EN CACHE
        cargarEquiposAutorizados();

        try {
            inicializarServidor();
            iniciarHiloServidor();
            log.info("✅ SERVIDOR HL7 INICIADO CORRECTAMENTE EN PUERTO {}", serverPort);
        } catch (Exception e) {
            log.error("💥 ERROR CRÍTICO iniciando servidor HL7: {}", e.getMessage(), e);
            throw new RuntimeException("Error iniciando servidor HL7", e);
        }
    }

    @PreDestroy
    public void detenerServidorHL7() {
        log.info("🛑 DETENIENDO SERVIDOR HL7");
        serverRunning.set(false);

        try {
            // Cerrar todas las conexiones activas
            for (SocketChannel channel : conexionesActivas) {
                try {
                    channel.close();
                } catch (IOException e) {
                    log.warn("⚠️ Error cerrando conexión: {}", e.getMessage());
                }
            }

            // Cerrar selector y servidor
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }

            // Esperar que termine el hilo del servidor
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
                serverThread.join(5000);
            }

            // ✅ ESTADÍSTICAS FINALES DETALLADAS
            log.info("📊 ESTADÍSTICAS FINALES DEL SERVIDOR:");
            log.info("   - Total conexiones: {}", totalConexiones.get());
            log.info("   - Mensajes recibidos: {}", mensajesRecibidos.get());
            log.info("   - Mensajes exitosos: {}", mensajesExitosos.get());
            log.info("   - Mensajes con error: {}", mensajesError.get());
            log.info("   - Mensajes duplicados: {}", mensajesDuplicados.get());
            log.info("   - Mensajes enviados: {}", mensajesEnviados.get());
            log.info("   - Tasa de éxito: {}%",
                    mensajesRecibidos.get() > 0 ?
                            (mensajesExitosos.get() * 100.0 / mensajesRecibidos.get()) : 0);

        } catch (Exception e) {
            log.error("⚠️ Error deteniendo servidor HL7: {}", e.getMessage());
        }
    }

    // ===== CONFIGURACIÓN DEL SERVIDOR =====
    private void inicializarServidor() throws IOException {
        // Crear selector para I/O no bloqueante
        selector = Selector.open();

        // Crear y configurar servidor socket
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().setReuseAddress(true);
        serverChannel.socket().setSoTimeout(timeout);

        // Bind al puerto
        InetSocketAddress address = new InetSocketAddress(serverHost, serverPort);
        serverChannel.bind(address);

        // Registrar para aceptar conexiones
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        serverRunning.set(true);
    }

    private void iniciarHiloServidor() {
        serverThread = new Thread(this::ejecutarServidorLoop, "HL7-Server-Thread");
        serverThread.setDaemon(false);
        serverThread.start();
    }

    // ===== LOOP PRINCIPAL DEL SERVIDOR =====
    private void ejecutarServidorLoop() {
        log.info("🔄 INICIANDO LOOP PRINCIPAL DEL SERVIDOR HL7");

        while (serverRunning.get()) {
            try {
                // Esperar por eventos (conexiones, datos, etc.)
                int readyChannels = selector.select(1000); // Timeout 1 segundo

                if (readyChannels == 0) {
                    continue; // No hay eventos, continuar
                }

                // Procesar eventos
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    try {
                        if (key.isValid()) {
                            if (key.isAcceptable()) {
                                aceptarNuevaConexion(key);
                            } else if (key.isReadable()) {
                                leerDatosCliente(key);
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ Error procesando evento: {}", e.getMessage());
                        cerrarConexion(key);
                    }
                }

            } catch (IOException e) {
                if (serverRunning.get()) {
                    log.error("💥 Error en loop del servidor: {}", e.getMessage());
                }
            } catch (Exception e) {
                log.error("💥 Error inesperado en servidor: {}", e.getMessage(), e);
            }
        }

        log.info("🛑 FINALIZANDO LOOP DEL SERVIDOR HL7");
    }

    // ===== MANEJO DE CONEXIONES =====
    private void aceptarNuevaConexion(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            // Verificar límite de conexiones
            if (conexionesActivas.size() >= maxConnections) {
                log.warn("⚠️ Máximo de conexiones alcanzado ({}), rechazando conexión", maxConnections);
                clientChannel.close();
                return;
            }

            // Configurar cliente
            clientChannel.configureBlocking(false);
            clientChannel.socket().setTcpNoDelay(true);
            clientChannel.socket().setKeepAlive(true);

            // Registrar para lectura
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            clientKey.attach(ByteBuffer.allocate(bufferSize));

            // Agregar a conexiones activas
            conexionesActivas.add(clientChannel);
            totalConexiones.incrementAndGet();

            String clientAddress = clientChannel.getRemoteAddress().toString();
            log.info("🔗 NUEVA CONEXIÓN HL7: {} (Total activas: {})", clientAddress, conexionesActivas.size());
        }
    }

    private void leerDatosCliente(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Cliente cerró la conexión
            cerrarConexion(key);
            return;
        }

        if (bytesRead > 0) {
            // Procesar datos recibidos
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            buffer.clear();

            String mensaje = new String(data, StandardCharsets.UTF_8);
            procesarMensajeHL7(clientChannel, mensaje);
        }
    }

    private void cerrarConexion(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            String clientAddress = channel.getRemoteAddress().toString();

            conexionesActivas.remove(channel);
            key.cancel();
            channel.close();

            log.info("🔌 CONEXIÓN CERRADA: {} (Total activas: {})", clientAddress, conexionesActivas.size());
        } catch (IOException e) {
            log.warn("⚠️ Error cerrando conexión: {}", e.getMessage());
        }
    }

    // ===== PROCESAMIENTO DE MENSAJES HL7 =====
    private void procesarMensajeHL7(SocketChannel clientChannel, String mensajeRaw) {
        try {
            log.debug("📨 MENSAJE HL7 RECIBIDO de {}: {} caracteres",
                    clientChannel.getRemoteAddress(), mensajeRaw.length());

            // ✅ DEBUG DETALLADO DE CARACTERES RECIBIDOS
            if (log.isDebugEnabled()) {
                logDetallesMensajeRecibido(mensajeRaw);
            }

            // ✅ VALIDAR FORMATO MLLP CON NUEVA LÓGICA
            if (!esFormatoMLLPValido(mensajeRaw)) {
                log.warn("⚠️ Mensaje con formato MLLP inválido");
                enviarNACK(clientChannel, "Invalid MLLP format");
                return;
            }

            // Extraer mensaje HL7 (quitar caracteres MLLP)
            String mensajeHL7 = extraerMensajeHL7(mensajeRaw);

            if (mensajeHL7.isEmpty()) {
                log.warn("⚠️ Mensaje HL7 vacío después de extraer MLLP");
                enviarNACK(clientChannel, "Empty HL7 message");
                return;
            }

            // ✅ PROCESAR MENSAJE HL7 CON LÓGICA COMPLETA
            boolean procesadoExitoso = procesarContenidoHL7(clientChannel, mensajeHL7);

            // Enviar respuesta
            if (autoAck) {
                if (procesadoExitoso) {
                    enviarACK(clientChannel, "Message received successfully");
                } else {
                    enviarNACK(clientChannel, "Error processing message");
                }
            }

            mensajesRecibidos.incrementAndGet();

        } catch (Exception e) {
            log.error("❌ Error procesando mensaje HL7: {}", e.getMessage(), e);
            mensajesError.incrementAndGet();
            try {
                enviarNACK(clientChannel, "Internal processing error");
            } catch (IOException ioE) {
                log.error("❌ Error enviando NACK: {}", ioE.getMessage());
            }
        }
    }

    /**
     * ✅ PROCESAMIENTO COMPLETO DE CONTENIDO HL7 - VERSIÓN REFACTORIZADA
     * 🎯 Integra con HL7ProcessorService para procesamiento real
     * 🔧 Control de duplicados, validaciones y auditoría completa
     */
    private boolean procesarContenidoHL7(SocketChannel clientChannel, String mensajeHL7) {
        String ipOrigen = null;
        String equipoOrigen = null;
        String controlId = null;
        String tipoMensaje = null;

        try {
            // ===== 1. EXTRAER INFORMACIÓN DEL CLIENTE =====
            ipOrigen = extraerIPCliente(clientChannel);
            log.debug("🌐 IP origen: {}", ipOrigen);

            // ===== 2. VALIDACIONES BÁSICAS DEL MENSAJE =====
            if (!validarEstructuraHL7Basica(mensajeHL7)) {
                log.warn("⚠️ Estructura HL7 inválida desde IP: {}", ipOrigen);
                mensajesError.incrementAndGet();
                registrarErrorValidacion(null, null, ipOrigen, mensajeHL7, "Estructura HL7 básica inválida");
                return false;
            }

            // ===== 3. EXTRAER DATOS CRÍTICOS DEL MSH =====
            try {
                controlId = extraerControlId(mensajeHL7);
                equipoOrigen = extraerEquipoOrigen(mensajeHL7);
                tipoMensaje = extraerTipoMensaje(mensajeHL7);

                log.debug("📋 Datos extraídos - Control ID: {}, Equipo: {}, Tipo: {}",
                        controlId, equipoOrigen, tipoMensaje);

            } catch (Exception e) {
                log.error("📝 Error extrayendo datos del MSH: {}", e.getMessage());
                registrarErrorParseo(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                        "Error extrayendo datos del MSH: " + e.getMessage());
                mensajesError.incrementAndGet();
                return false;
            }

            // ===== 4. CONTROL DE DUPLICADOS =====
            try {
                if (esMensajeDuplicado(controlId)) {
                    log.warn("❌ Mensaje duplicado detectado - Control ID: {} desde equipo: {}",
                            controlId, equipoOrigen);
                    registrarMensajeDuplicado(controlId, equipoOrigen, ipOrigen, mensajeHL7);
                    mensajesDuplicados.incrementAndGet();
                    return false; // Se considera "error" para enviar NACK
                }
            } catch (Exception e) {
                log.error("💾 Error verificando duplicados en BD: {}", e.getMessage());
                registrarErrorBaseDatos(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                        "Error verificando duplicados: " + e.getMessage());
                mensajesError.incrementAndGet();
                return false;
            }

            // ===== 5. VALIDACIÓN DE EQUIPO AUTORIZADO =====
            try {
                if (!validarEquipoAutorizado(equipoOrigen, ipOrigen)) {
                    log.warn("🚫 Equipo no autorizado - Equipo: {} desde IP: {}", equipoOrigen, ipOrigen);
                    registrarErrorValidacion(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                            "Equipo no autorizado para enviar mensajes HL7");
                    mensajesError.incrementAndGet();
                    return false;
                }
            } catch (Exception e) {
                log.error("💾 Error validando equipo autorizado en BD: {}", e.getMessage());
                registrarErrorBaseDatos(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                        "Error validando equipo autorizado: " + e.getMessage());
                mensajesError.incrementAndGet();
                return false;
            }

            // ===== 6. PROCESAMIENTO REAL CON HL7PROCESSORSERVICE =====
            try {
                boolean exitoProcesamiento = hl7ProcessorService.procesarMensajeHL7Directo(
                        equipoOrigen,
                        ipOrigen,
                        mensajeHL7
                );

                // ===== 7. LOG DEL RESULTADO Y MÉTRICAS =====
                if (exitoProcesamiento) {
                    log.info("✅ Mensaje HL7 procesado exitosamente - Control ID: {}, Equipo: {}, IP: {}",
                            controlId, equipoOrigen, ipOrigen);

                    mensajesExitosos.incrementAndGet();
                    incrementarContadorExitos(equipoOrigen);

                } else {
                    log.error("❌ Error procesando mensaje HL7 - Control ID: {}, Equipo: {}, IP: {}",
                            controlId, equipoOrigen, ipOrigen);

                    registrarErrorGeneral(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                            "HL7ProcessorService retornó false");
                    mensajesError.incrementAndGet();
                    incrementarContadorErrores(equipoOrigen);
                }

                return exitoProcesamiento;

            } catch (Exception e) {
                log.error("💥 Error en HL7ProcessorService - Control ID: {}, Error: {}",
                        controlId, e.getMessage(), e);
                registrarErrorGeneral(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                        "Error en HL7ProcessorService: " + e.getMessage());
                mensajesError.incrementAndGet();
                incrementarContadorErrores(equipoOrigen);
                return false;
            }

        } catch (Exception e) {
            log.error("💥 Error inesperado procesando HL7 - Control ID: {}, Equipo: {}, Error: {}",
                    controlId, equipoOrigen, e.getMessage(), e);
            registrarErrorGeneral(controlId, equipoOrigen, ipOrigen, mensajeHL7,
                    "Error inesperado: " + e.getMessage());
            mensajesError.incrementAndGet();
            return false;
        }
    }

    // ===== MÉTODOS AUXILIARES DE EXTRACCIÓN =====

    /**
     * ✅ EXTRAER IP DEL CLIENTE DE FORMA SEGURA
     */
    private String extraerIPCliente(SocketChannel clientChannel) {
        try {
            String direccionCompleta = clientChannel.getRemoteAddress().toString();
            log.debug("🔍 Dirección completa: {}", direccionCompleta);

            // Extraer solo la IP (formato: /127.0.0.1:puerto)
            if (direccionCompleta.startsWith("/")) {
                String sinBarra = direccionCompleta.substring(1);
                String[] partes = sinBarra.split(":");
                String ip = partes[0];
                log.debug("🌐 IP extraída: {}", ip);
                return ip;
            }

            return direccionCompleta.replaceAll("[/:].*", "");
        } catch (IOException e) {
            log.warn("⚠️ Error extrayendo IP del cliente: {}", e.getMessage());
            return "127.0.0.1"; // IP por defecto para localhost
        }
    }

    /**
     * ✅ VALIDAR ESTRUCTURA BÁSICA HL7
     */
    private boolean validarEstructuraHL7Basica(String mensajeHL7) {
        if (mensajeHL7 == null || mensajeHL7.trim().isEmpty()) {
            return false;
        }

        // Debe comenzar con MSH
        if (!mensajeHL7.startsWith("MSH")) {
            return false;
        }

        // Debe tener al menos separadores básicos
        if (mensajeHL7.length() < 10 || !mensajeHL7.contains("|")) {
            return false;
        }

        // Verificar que tenga al menos los campos mínimos de MSH
        String[] campos = mensajeHL7.split("\\r|\\n")[0].split("\\|");
        return campos.length >= 11; // MSH debe tener al menos 11 campos
    }

    /**
     * ✅ EXTRAER CONTROL ID (MSH.10)
     */
    private String extraerControlId(String mensajeHL7) {
        try {
            String lineaMSH = mensajeHL7.split("\\r|\\n")[0];
            String[] campos = lineaMSH.split("\\|");
            return campos.length > 9 ? campos[9] : "UNKNOWN_" + System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("⚠️ Error extrayendo Control ID: {}", e.getMessage());
            return "ERROR_" + System.currentTimeMillis();
        }
    }

    /**
     * ✅ EXTRAER EQUIPO ORIGEN (MSH.3)
     */
    private String extraerEquipoOrigen(String mensajeHL7) {
        try {
            String lineaMSH = mensajeHL7.split("\\r|\\n")[0];
            String[] campos = lineaMSH.split("\\|");
            return campos.length > 2 ? campos[2] : "UNKNOWN_EQUIPMENT";
        } catch (Exception e) {
            log.warn("⚠️ Error extrayendo equipo origen: {}", e.getMessage());
            return "ERROR_EQUIPMENT";
        }
    }

    /**
     * ✅ EXTRAER TIPO DE MENSAJE (MSH.9)
     */
    private String extraerTipoMensaje(String mensajeHL7) {
        try {
            String lineaMSH = mensajeHL7.split("\\r|\\n")[0];
            String[] campos = lineaMSH.split("\\|");
            return campos.length > 8 ? campos[8] : "UNKNOWN";
        } catch (Exception e) {
            log.warn("⚠️ Error extrayendo tipo mensaje: {}", e.getMessage());
            return "ERROR";
        }
    }

    /**
     * ✅ VERIFICAR SI ES MENSAJE DUPLICADO
     */
    private boolean esMensajeDuplicado(String controlId) {
        try {
            return hl7MessageRepository.existeMensajeId(controlId) > 0;
        } catch (Exception e) {
            log.warn("⚠️ Error verificando duplicado para Control ID {}: {}", controlId, e.getMessage());
            return false; // En caso de error, procesar mensaje
        }
    }

    /**
     * ✅ VALIDAR EQUIPO AUTORIZADO - CORREGIDO PARA BUSCAR POR MSH.3
     */
    private boolean validarEquipoAutorizado(String equipoOrigen, String ipOrigen) {
        try {
            // 1. Verificar en cache primero
            Boolean cacheResult = equiposAutorizados.get(equipoOrigen);
            if (cacheResult != null) {
                log.debug("🔍 Cache hit: Equipo {} = {}", equipoOrigen, cacheResult);
                return cacheResult;
            }

            log.debug("🔍 Cache miss: Consultando BD para equipo: {} desde IP: {}", equipoOrigen, ipOrigen);

            // 2. Buscar equipo con método unificado (sin variables duplicadas)
            HL7EquipoConfig equipoEncontrado = buscarEquipoEnBaseDatos(equipoOrigen);

            if (equipoEncontrado == null) {
                // No encontrado en ninguna búsqueda
                return procesarEquipoNoEncontrado(equipoOrigen, ipOrigen);
            }

            // 3. Validar equipo encontrado y guardar en cache
            return procesarEquipoEncontrado(equipoEncontrado, equipoOrigen, ipOrigen);

        } catch (Exception e) {
            log.error("⚠️ Error validando equipo autorizado {}: {}", equipoOrigen, e.getMessage(), e);
            return false; // En caso de error de BD, denegar acceso por seguridad
        }
    }

    /**
     * ✅ BUSCAR EQUIPO EN BASE DE DATOS - MÉTODO AUXILIAR
     * 🎯 Unifica la búsqueda por aplicacion_nombre y codigo_equipo
     * 🔧 Elimina duplicación de lógica de búsqueda
     */
    private HL7EquipoConfig buscarEquipoEnBaseDatos(String equipoOrigen) {
        try {
            // Intento 1: Buscar por aplicacion_nombre (MSH.3) - Método preferido
            List<HL7EquipoConfig> equiposPorAplicacion = equipoConfigRepository.findByAplicacionNombre(equipoOrigen);
            if (!equiposPorAplicacion.isEmpty()) {
                log.debug("✅ Equipo encontrado por aplicacion_nombre: {}", equipoOrigen);
                return equiposPorAplicacion.get(0);
            }

            // Intento 2: Buscar por codigo_equipo como fallback
            Optional<HL7EquipoConfig> equipoPorCodigo = equipoConfigRepository.findByCodigoEquipo(equipoOrigen);
            if (equipoPorCodigo.isPresent()) {
                log.debug("✅ Equipo encontrado por codigo_equipo: {}", equipoOrigen);
                return equipoPorCodigo.get();
            }

            return null; // No encontrado
        } catch (Exception e) {
            log.error("💥 Error buscando equipo en BD: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ PROCESAR EQUIPO NO ENCONTRADO - MÉTODO AUXILIAR
     * 🎯 Maneja el caso cuando no se encuentra el equipo
     */
    private boolean procesarEquipoNoEncontrado(String equipoOrigen, String ipOrigen) {
        equiposAutorizados.put(equipoOrigen, false);
        log.warn("❌ Equipo NO encontrado en BD: {} desde IP: {}", equipoOrigen, ipOrigen);
        log.warn("💡 Sugerencia: Verificar que aplicacion_nombre = '{}' en hl7_equipos_config", equipoOrigen);
        return false;
    }

    /**
     * ✅ PROCESAR EQUIPO ENCONTRADO - MÉTODO AUXILIAR
     * 🎯 Valida configuración y guarda en cache (SIN variables duplicadas)
     */
    private boolean procesarEquipoEncontrado(HL7EquipoConfig equipo, String equipoOrigen, String ipOrigen) {
        // Validar configuración del equipo
        boolean equipoEsValido = validarConfiguracionBasicaEquipo(equipo);
        boolean ipEsValida = validarIPDelEquipo(equipo, ipOrigen);

        // Resultado final (UNA SOLA variable)
        boolean resultadoFinal = equipoEsValido && ipEsValida;

        // Guardar en cache
        equiposAutorizados.put(equipoOrigen, resultadoFinal);

        // Log del resultado
        logResultadoValidacion(equipo, equipoOrigen, ipOrigen, resultadoFinal, equipoEsValido, ipEsValida);

        return resultadoFinal;
    }

    /**
     * ✅ VALIDAR CONFIGURACIÓN BÁSICA EQUIPO - MÉTODO AUXILIAR
     * 🎯 Valida solo estado activo y mantenimiento
     */
    private boolean validarConfiguracionBasicaEquipo(HL7EquipoConfig equipo) {
        return "S".equals(equipo.getActivo()) && "N".equals(equipo.getEnMantenimiento());
    }

    /**
     * ✅ VALIDAR IP DEL EQUIPO - MÉTODO AUXILIAR
     * 🎯 Maneja validación de IP con casos especiales
     */
    private boolean validarIPDelEquipo(HL7EquipoConfig equipo, String ipOrigen) {
        // Si no hay IP configurada, permitir cualquiera
        if (equipo.getIpPermitida() == null || equipo.getIpPermitida().trim().isEmpty()) {
            return true;
        }

        // Verificar IP exacta o localhost
        return equipo.getIpPermitida().equals(ipOrigen) ||
                "127.0.0.1".equals(ipOrigen) ||
                "localhost".equals(ipOrigen);
    }

    /**
     * ✅ LOG RESULTADO VALIDACIÓN - MÉTODO AUXILIAR
     * 🎯 Centraliza el logging con información detallada
     */
    private void logResultadoValidacion(HL7EquipoConfig equipo, String equipoOrigen, String ipOrigen,
                                        boolean resultadoFinal, boolean equipoEsValido, boolean ipEsValida) {
        if (resultadoFinal) {
            log.debug("✅ Equipo autorizado: {} (codigo: {}, IP: {})",
                    equipoOrigen, equipo.getCodigoEquipo(), ipOrigen);
        } else {
            log.debug("❌ Equipo encontrado pero no autorizado: {} - Válido: {}, IP válida: {}",
                    equipoOrigen, equipoEsValido, ipEsValida);
        }
    }


    /**
     * ✅ CARGAR EQUIPOS AUTORIZADOS - MÉTODO CORREGIDO
     * 🎯 Carga tanto por codigo_equipo como por aplicacion_nombre
     * 🔧 Evita duplicación en cache y soluciona el problema de cache
     */
    private void cargarEquiposAutorizados() {
        try {
            int equiposCargados = 0;

            // Cargar todos los equipos operativos
            List<HL7EquipoConfig> equiposOperativos = equipoConfigRepository.findEquiposOperativos();

            log.info("🔍 Cargando {} equipos operativos en cache...", equiposOperativos.size());

            for (HL7EquipoConfig equipo : equiposOperativos) {

                // ✅ GUARDAR EN CACHE POR CODIGO_EQUIPO
                if (equipo.getCodigoEquipo() != null && !equipo.getCodigoEquipo().trim().isEmpty()) {
                    equiposAutorizados.put(equipo.getCodigoEquipo(), true);
                    equiposCargados++;
                    log.debug("✅ Cache: codigo_equipo = '{}'", equipo.getCodigoEquipo());
                }

                // ✅ GUARDAR EN CACHE POR APLICACION_NOMBRE (MSH.3) - ¡CRÍTICO!
                if (equipo.getAplicacionNombre() != null &&
                        !equipo.getAplicacionNombre().trim().isEmpty() &&
                        !equipo.getAplicacionNombre().equals(equipo.getCodigoEquipo())) {

                    equiposAutorizados.put(equipo.getAplicacionNombre(), true);
                    equiposCargados++;
                    log.debug("✅ Cache: aplicacion_nombre = '{}'", equipo.getAplicacionNombre());
                }

                // ✅ LOG DETALLADO DEL EQUIPO CARGADO
                log.info("📝 Equipo cargado: ID={}, codigo='{}', aplicacion='{}', activo={}, IP={}",
                        equipo.getId(),
                        equipo.getCodigoEquipo(),
                        equipo.getAplicacionNombre(),
                        equipo.getActivo(),
                        equipo.getIpPermitida());
            }

            log.info("✅ Cache de equipos cargado: {} entradas desde {} equipos operativos",
                    equiposCargados, equiposOperativos.size());

            // ✅ LOG DE DEBUG CON EQUIPOS EN CACHE
            if (log.isDebugEnabled()) {
                log.debug("🔍 Equipos en cache: {}", equiposAutorizados.keySet());
            }

            // ✅ VERIFICACIÓN ESPECÍFICA PARA TU SIMULADOR
            if (equiposAutorizados.containsKey("PHILIPS_MX450")) {
                log.info("🎯 PHILIPS_MX450 encontrado en cache = {}",
                        equiposAutorizados.get("PHILIPS_MX450"));
            } else {
                log.warn("⚠️ PHILIPS_MX450 NO encontrado en cache!");
                log.warn("💡 Verificar que aplicacion_nombre = 'PHILIPS_MX450' en hl7_equipos_config");
            }

        } catch (Exception e) {
            log.error("⚠️ Error cargando equipos autorizados en cache: {}", e.getMessage(), e);
        }
    }

        /**
         * ✅ INCREMENTAR CONTADOR DE ÉXITOS POR EQUIPO
         */
    private void incrementarContadorExitos(String equipoOrigen) {
        contadorExitosEquipo.merge(equipoOrigen, 1L, Long::sum);
    }

    /**
     * ✅ INCREMENTAR CONTADOR DE ERRORES POR EQUIPO
     */
    private void incrementarContadorErrores(String equipoOrigen) {
        contadorErroresEquipo.merge(equipoOrigen, 1L, Long::sum);
    }

    // ===== MÉTODOS DE REGISTRO DE ERRORES =====

    private void registrarMensajeDuplicado(String controlId, String equipoOrigen, String ipOrigen, String mensajeHL7) {
        // Implementar registro de duplicados en BD si es necesario
        log.debug("📝 Registrando mensaje duplicado - Control ID: {}", controlId);
    }

    private void registrarErrorParseo(String controlId, String equipoOrigen, String ipOrigen,
                                      String mensajeHL7, String error) {
        // Implementar registro de errores de parseo
        log.debug("📝 Registrando error de parseo - Control ID: {}, Error: {}", controlId, error);
    }

    private void registrarErrorValidacion(String controlId, String equipoOrigen, String ipOrigen,
                                          String mensajeHL7, String error) {
        // Implementar registro de errores de validación
        log.debug("📝 Registrando error de validación - Control ID: {}, Error: {}", controlId, error);
    }

    private void registrarErrorBaseDatos(String controlId, String equipoOrigen, String ipOrigen,
                                         String mensajeHL7, String error) {
        // Implementar registro de errores de BD
        log.debug("📝 Registrando error de BD - Control ID: {}, Error: {}", controlId, error);
    }

    private void registrarErrorGeneral(String controlId, String equipoOrigen, String ipOrigen,
                                       String mensajeHL7, String error) {
        // Implementar registro de errores generales
        log.debug("📝 Registrando error general - Control ID: {}, Error: {}", controlId, error);
    }

    /**
     * ✅ VALIDACIÓN MLLP MEJORADA CON CARACTERES HARDCODEADOS
     */
    private boolean esFormatoMLLPValido(String mensaje) {
        if (mensaje == null || mensaje.length() < 3) {
            log.debug("❌ Mensaje null o muy corto: {}", mensaje != null ? mensaje.length() : "null");
            return false;
        }

        // Verificar carácter de inicio
        char primerChar = mensaje.charAt(0);
        boolean inicioValido = (primerChar == MLLP_START_CHAR);

        // Verificar caracteres de fin
        char penultimoChar = mensaje.charAt(mensaje.length() - 2);
        char ultimoChar = mensaje.charAt(mensaje.length() - 1);
        boolean finValido = (penultimoChar == MLLP_END_CHAR_1) && (ultimoChar == MLLP_END_CHAR_2);

        // ✅ LOG DETALLADO PARA DEBUGGING
        if (log.isDebugEnabled()) {
            log.debug("🔍 VALIDACIÓN MLLP DETALLADA:");
            log.debug("   - Longitud mensaje: {}", mensaje.length());
            log.debug("   - Primer char esperado: 0x{} ({}), recibido: 0x{} ({}), válido: {}",
                    Integer.toHexString(MLLP_START_CHAR).toUpperCase(),
                    (int) MLLP_START_CHAR,
                    Integer.toHexString(primerChar).toUpperCase(),
                    (int) primerChar,
                    inicioValido);
            log.debug("   - Penúltimo char esperado: 0x{} ({}), recibido: 0x{} ({})",
                    Integer.toHexString(MLLP_END_CHAR_1).toUpperCase(),
                    (int) MLLP_END_CHAR_1,
                    Integer.toHexString(penultimoChar).toUpperCase(),
                    (int) penultimoChar);
            log.debug("   - Último char esperado: 0x{} ({}), recibido: 0x{} ({})",
                    Integer.toHexString(MLLP_END_CHAR_2).toUpperCase(),
                    (int) MLLP_END_CHAR_2,
                    Integer.toHexString(ultimoChar).toUpperCase(),
                    (int) ultimoChar);
            log.debug("   - Fin válido: {}", finValido);
            log.debug("   - RESULTADO FINAL: {}", inicioValido && finValido);
        }

        return inicioValido && finValido;
    }

    /**
     * ✅ EXTRAER MENSAJE HL7 LIMPIO
     */
    private String extraerMensajeHL7(String mensajeMLLP) {
        if (mensajeMLLP.length() < 3) {
            return "";
        }

        // Quitar primer carácter (MLLP_START) y últimos dos caracteres (MLLP_END)
        int inicio = 1; // Después del carácter de inicio
        int fin = mensajeMLLP.length() - 2; // Antes de los caracteres de fin

        if (fin <= inicio) {
            return "";
        }

        String mensajeHL7 = mensajeMLLP.substring(inicio, fin);
        log.debug("✅ Mensaje HL7 extraído: {} caracteres", mensajeHL7.length());

        return mensajeHL7;
    }

    /**
     * ✅ LOG DETALLADO DEL MENSAJE RECIBIDO PARA DEBUGGING
     */
    private void logDetallesMensajeRecibido(String mensaje) {
        log.debug("📋 ANÁLISIS DETALLADO DEL MENSAJE:");
        log.debug("   - Longitud total: {}", mensaje.length());

        if (mensaje.length() > 0) {
            // Mostrar primeros 10 caracteres con sus códigos hex
            int limite = Math.min(10, mensaje.length());
            StringBuilder detalleInicio = new StringBuilder();
            for (int i = 0; i < limite; i++) {
                char c = mensaje.charAt(i);
                detalleInicio.append(String.format("pos[%d]=0x%s(%d) ", i,
                        Integer.toHexString(c).toUpperCase(), (int) c));
            }
            log.debug("   - Primeros {} chars: {}", limite, detalleInicio.toString());

            // Mostrar últimos 10 caracteres con sus códigos hex
            int inicioFin = Math.max(0, mensaje.length() - 10);
            StringBuilder detalleFin = new StringBuilder();
            for (int i = inicioFin; i < mensaje.length(); i++) {
                char c = mensaje.charAt(i);
                detalleFin.append(String.format("pos[%d]=0x%s(%d) ", i,
                        Integer.toHexString(c).toUpperCase(), (int) c));
            }
            log.debug("   - Últimos chars: {}", detalleFin.toString());
        }
    }

    /**
     * ✅ LOG CARACTERES MLLP DEL SERVIDOR
     */
    private void logCaracteresMLLP() {
        log.info("🔧 PROTOCOLO MLLP DEL SERVIDOR:");
        log.info("   - Start char: 0x{} (decimal: {})",
                Integer.toHexString(MLLP_START_CHAR).toUpperCase(), (int) MLLP_START_CHAR);
        log.info("   - End char 1: 0x{} (decimal: {})",
                Integer.toHexString(MLLP_END_CHAR_1).toUpperCase(), (int) MLLP_END_CHAR_1);
        log.info("   - End char 2: 0x{} (decimal: {})",
                Integer.toHexString(MLLP_END_CHAR_2).toUpperCase(), (int) MLLP_END_CHAR_2);
        log.info("   - Compatible con cliente Java: SÍ");

        // Comparar con HL7Properties si está disponible
        if (hl7Properties != null) {
            try {
                char propsStart = hl7Properties.getStartCharValue();
                char propsEnd1 = hl7Properties.getEndChar1Value();
                char propsEnd2 = hl7Properties.getEndChar2Value();

                boolean compatibleStart = (propsStart == MLLP_START_CHAR);
                boolean compatibleEnd1 = (propsEnd1 == MLLP_END_CHAR_1);
                boolean compatibleEnd2 = (propsEnd2 == MLLP_END_CHAR_2);

                log.info("   - Compatibilidad con HL7Properties:");
                log.info("     * Start: {} (props: 0x{}, servidor: 0x{})",
                        compatibleStart ? "✅" : "❌",
                        Integer.toHexString(propsStart).toUpperCase(),
                        Integer.toHexString(MLLP_START_CHAR).toUpperCase());
                log.info("     * End1: {} (props: 0x{}, servidor: 0x{})",
                        compatibleEnd1 ? "✅" : "❌",
                        Integer.toHexString(propsEnd1).toUpperCase(),
                        Integer.toHexString(MLLP_END_CHAR_1).toUpperCase());
                log.info("     * End2: {} (props: 0x{}, servidor: 0x{})",
                        compatibleEnd2 ? "✅" : "❌",
                        Integer.toHexString(propsEnd2).toUpperCase(),
                        Integer.toHexString(MLLP_END_CHAR_2).toUpperCase());
            } catch (Exception e) {
                log.warn("⚠️ No se pudieron verificar HL7Properties: {}", e.getMessage());
            }
        }
    }

    // ===== ENVÍO DE RESPUESTAS ACK/NACK =====
    private void enviarACK(SocketChannel clientChannel, String mensaje) throws IOException {
        String ack = construirACK("AA", mensaje);
        enviarRespuesta(clientChannel, ack);
        log.debug("✅ ACK enviado");
    }

    private void enviarNACK(SocketChannel clientChannel, String mensaje) throws IOException {
        String nack = construirACK("AE", mensaje);
        enviarRespuesta(clientChannel, nack);
        log.debug("❌ NACK enviado");
    }

    private String construirACK(String codigo, String mensaje) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String controlId = "ACK" + System.currentTimeMillis();

        return "MSH|^~\\&|" + "HIS_SYSTEM" + "|" + "HOSPITAL" +
                "|SENDER|FACILITY|" + timestamp + "||ACK^|" + controlId + "|P|2.5\r" +
                "MSA|" + codigo + "|" + controlId + "|" + mensaje + "\r";
    }

    /**
     * ✅ ENVIAR RESPUESTA CON PROTOCOLO MLLP CORRECTO
     */
    private void enviarRespuesta(SocketChannel clientChannel, String respuesta) throws IOException {
        // Construir mensaje con caracteres MLLP hardcodeados
        StringBuilder mensajeMLLP = new StringBuilder();
        mensajeMLLP.append(MLLP_START_CHAR);  // 0x0B
        mensajeMLLP.append(respuesta);        // Mensaje HL7
        mensajeMLLP.append(MLLP_END_CHAR_1);  // 0x1C
        mensajeMLLP.append(MLLP_END_CHAR_2);  // 0x0D

        ByteBuffer buffer = ByteBuffer.wrap(mensajeMLLP.toString().getBytes(StandardCharsets.UTF_8));

        while (buffer.hasRemaining()) {
            clientChannel.write(buffer);
        }

        mensajesEnviados.incrementAndGet();
        log.debug("📤 Respuesta MLLP enviada: {} bytes", mensajeMLLP.length());
    }

    // ===== CONFIGURACIÓN DE THREAD POOL =====
    @Bean(name = "hl7TaskExecutor")
    public ThreadPoolTaskExecutor hl7TaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize / 2);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("HL7-Task-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();

        log.info("✅ Thread Pool HL7 configurado - Core: {}, Max: {}",
                threadPoolSize / 2, threadPoolSize);

        return executor;
    }

    // ===== MÉTODOS PÚBLICOS PARA MONITOREO =====
    public boolean isServerRunning() {
        return serverRunning.get();
    }

    public int getConexionesActivas() {
        return conexionesActivas.size();
    }

    public long getTotalConexiones() {
        return totalConexiones.get();
    }

    public long getMensajesRecibidos() {
        return mensajesRecibidos.get();
    }

    public long getMensajesEnviados() {
        return mensajesEnviados.get();
    }

    public long getMensajesExitosos() {
        return mensajesExitosos.get();
    }

    public long getMensajesError() {
        return mensajesError.get();
    }

    public long getMensajesDuplicados() {
        return mensajesDuplicados.get();
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    /**
     * ✅ OBTENER ESTADÍSTICAS POR EQUIPO
     */
    public java.util.Map<String, Long> getEstadisticasExitosPorEquipo() {
        return new java.util.HashMap<>(contadorExitosEquipo);
    }

    public java.util.Map<String, Long> getEstadisticasErroresPorEquipo() {
        return new java.util.HashMap<>(contadorErroresEquipo);
    }

    /**
     * ✅ OBTENER EQUIPOS AUTORIZADOS
     */
    public java.util.Set<String> getEquiposAutorizados() {
        return equiposAutorizados.keySet();
    }

    /**
     * ✅ REFRESCAR CACHE DE EQUIPOS
     */
    public void refrescarCacheEquipos() {
        equiposAutorizados.clear();
        cargarEquiposAutorizados();
        log.info("🔄 Cache de equipos refrescado");
    }

    /**
     * ✅ OBTENER MÉTRICAS COMPLETAS DEL SERVIDOR
     */
    public java.util.Map<String, Object> getMetricasCompletas() {
        java.util.Map<String, Object> metricas = new java.util.HashMap<>();

        metricas.put("serverRunning", serverRunning.get());
        metricas.put("conexionesActivas", conexionesActivas.size());
        metricas.put("totalConexiones", totalConexiones.get());
        metricas.put("mensajesRecibidos", mensajesRecibidos.get());
        metricas.put("mensajesExitosos", mensajesExitosos.get());
        metricas.put("mensajesError", mensajesError.get());
        metricas.put("mensajesDuplicados", mensajesDuplicados.get());
        metricas.put("mensajesEnviados", mensajesEnviados.get());

        // Calcular tasa de éxito
        double tasaExito = mensajesRecibidos.get() > 0 ?
                (mensajesExitosos.get() * 100.0 / mensajesRecibidos.get()) : 0.0;
        metricas.put("tasaExitoPorcentaje", Math.round(tasaExito * 100.0) / 100.0);

        // Estadísticas por equipo
        metricas.put("exitosPorEquipo", getEstadisticasExitosPorEquipo());
        metricas.put("erroresPorEquipo", getEstadisticasErroresPorEquipo());
        metricas.put("equiposAutorizados", getEquiposAutorizados());

        return metricas;
    }

    /**
     * ✅ OBTENER CARACTERES MLLP DEL SERVIDOR
     */
    public char getMLLPStartChar() {
        return MLLP_START_CHAR;
    }

    public char getMLLPEndChar1() {
        return MLLP_END_CHAR_1;
    }

    public char getMLLPEndChar2() {
        return MLLP_END_CHAR_2;
    }

    /**
     * ✅ VERIFICAR COMPATIBILIDAD MLLP CON CLIENTE
     */
    public boolean esCompatibleConCliente(char clientStart, char clientEnd1, char clientEnd2) {
        return (clientStart == MLLP_START_CHAR) &&
                (clientEnd1 == MLLP_END_CHAR_1) &&
                (clientEnd2 == MLLP_END_CHAR_2);
    }

    /**
     * ✅ RESETEAR MÉTRICAS (PARA TESTING)
     */
    public void resetearMetricas() {
        totalConexiones.set(0);
        mensajesRecibidos.set(0);
        mensajesEnviados.set(0);
        mensajesExitosos.set(0);
        mensajesError.set(0);
        mensajesDuplicados.set(0);
        contadorExitosEquipo.clear();
        contadorErroresEquipo.clear();
        log.info("🔄 Métricas del servidor reseteadas");
    }

    // ===== EXCEPCIONES PERSONALIZADAS =====

    public static class HL7ParseException extends Exception {
        public HL7ParseException(String message) {
            super(message);
        }

        public HL7ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class HL7ValidationException extends Exception {
        public HL7ValidationException(String message) {
            super(message);
        }

        public HL7ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DatabaseException extends Exception {
        public DatabaseException(String message) {
            super(message);
        }

        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}