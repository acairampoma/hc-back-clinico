package com.formacionbdi.microservicios.app.orden.services.impl;

import com.formacionbdi.microservicios.app.orden.exception.*;
import com.formacionbdi.microservicios.app.orden.models.dto.*;
import com.formacionbdi.microservicios.app.orden.models.entity.OrdenCab;
import com.formacionbdi.microservicios.app.orden.models.entity.OrdenDet;
import com.formacionbdi.microservicios.app.orden.repository.OrdenCabRepository;
import com.formacionbdi.microservicios.app.orden.repository.OrdenDetRepository;
import com.formacionbdi.microservicios.app.orden.services.OrdenService;
import com.formacionbdi.microservicios.app.orden.models.entity.Examen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 🔥 SERVICE IMPL CON PROGRAMACIÓN FUNCIONAL ÉPICA
 * Mejores prácticas + Functions + Streams + Performance
 * Puerto: 8006
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrdenServiceImpl implements OrdenService {

    private final OrdenCabRepository ordenCabRepository;
    private final OrdenDetRepository ordenDetRepository;
    private final OrdenValidationHelper validationHelper;

    // =====================================================
    // 🔧 FUNCTIONS REUTILIZABLES (PROGRAMACIÓN FUNCIONAL)
    // =====================================================

    // Conversores funcionales
    private final Function<OrdenCab, OrdenCompletaDTO> toCompletaDTO = this::convertirAOrdenCompleta;
    private final Function<OrdenCab, OrdenResumenDTO> toResumenDTO = this::convertirAOrdenResumen;
    private final Function<OrdenCabDTO, OrdenCab> toEntity = this::convertirAEntity;

    // Predicados de validación
    private final Predicate<OrdenCab> esActiva = orden -> "S".equals(orden.getActivo());
    private final Predicate<OrdenCab> estaFirmada = orden -> "S".equals(orden.getFirmada());
    private final Predicate<OrdenCab> puedeModificarse = OrdenCab::puedeSerModificada;
    private final Predicate<OrdenCab> esEmergencia = orden -> "E".equals(orden.getPrioridad());
    private final Predicate<OrdenCab> esUrgente = orden -> "U".equals(orden.getPrioridad());

    // Suppliers para respuestas
    private final Supplier<OrdenNotFoundException> ordenNoEncontrada = () ->
            new OrdenNotFoundException("Orden no encontrada");

    // Comparators funcionales
    private final Comparator<OrdenCab> porFechaDesc =
            Comparator.comparing(OrdenCab::getFechaOrden).reversed();
    private final Comparator<OrdenCab> porPrioridadDesc =
            Comparator.comparing(OrdenCab::getPrioridad, this::compararPrioridades);

    // =====================================================
    // 🚀 CRUD PRINCIPALES CON PROGRAMACIÓN FUNCIONAL
    // =====================================================

    @Override
    @Transactional
    public OrdenCompletaDTO crearOrden(OrdenCabDTO ordenDTO) {
        log.info("🔥 Creando nueva orden médica - Tipo: {}, Origen: {}:{}",
                ordenDTO.getTipoOrden(), ordenDTO.getTipoOrigen(), ordenDTO.getOrigenId());

        // Validaciones
        validationHelper.validarCreacionOrden(ordenDTO);
        this.validarOrdenNoDuplicada(ordenDTO);

        // 🔥 PATRÓN EXACTO DE RECETAS - UNIFORMIDAD TOTAL

        // 1. Preparar y guardar cabecera (como recetas)
        OrdenCabDTO ordenPreparada = prepararOrdenParaCreacion(ordenDTO);
        OrdenCabDTO ordenConFirma = aplicarFirmaAutomatica(ordenPreparada);
        OrdenCab ordenEntity = convertirCabeceraAEntity(ordenConFirma);  // SIN exámenes
        OrdenCab ordenCabGuardada = establecerDatosCreacion(ordenEntity);
        ordenCabGuardada = ordenCabRepository.save(ordenCabGuardada);

        log.info("✅ Orden cabecera guardada con ID: {}", ordenCabGuardada.getId());

        // 2. Guardar exámenes IGUAL que recetas guardan medicamentos
        if (ordenDTO.getExamenes() != null && !ordenDTO.getExamenes().isEmpty()) {
            guardarExamenesDetalle(ordenCabGuardada.getId(), ordenDTO.getExamenes(), ordenDTO.getMedicoId());
            log.info("✅ Guardados {} exámenes para orden {}",
                    ordenDTO.getExamenes().size(), ordenCabGuardada.getId());
        }

        // 3. Retornar DTO completo con exámenes
        return toCompletaDTO.apply(ordenCabGuardada);
    }

    private OrdenCab convertirCabeceraAEntity(OrdenCabDTO dto) {
        return OrdenCab.builder()
                .pacienteId(dto.getPacienteId())
                .medicoId(dto.getMedicoId())
                .tipoOrigen(dto.getTipoOrigen())
                .origenId(dto.getOrigenId())
                .tipoOrden(dto.getTipoOrden())
                .fechaProgramada(dto.getFechaProgramada())
                .diagnosticoPrincipal(dto.getDiagnosticoPrincipal())
                .justificacionClinica(dto.getJustificacionClinica())
                .prioridad(Optional.ofNullable(dto.getPrioridad()).orElse("N"))
                .creadoPor(dto.getMedicoId())
                // ✅ SIN examenes - Igual que recetas sin medicamentos
                .build();
    }

    /**
     * 🔥 GUARDAR EXÁMENES - ACTUALIZADO CON 3 CAMPOS NUEVOS
     * ✅ Solo examenId e indicaciones, el JOIN trae el resto
     */
    private void guardarExamenesDetalle(Long ordenId, List<OrdenDetDTO> examenes, Long medicoId) {
        log.debug("🔬 Guardando {} exámenes para orden {}", examenes.size(), ordenId);

        LocalDateTime ahora = LocalDateTime.now();

        for (int i = 0; i < examenes.size(); i++) {
            OrdenDetDTO examenDTO = examenes.get(i);

            // ✅ BUSCAR LA CABECERA
            OrdenCab ordenCab = ordenCabRepository.findById(ordenId)
                    .orElseThrow(() -> new OrdenNotFoundException("Orden no encontrada: " + ordenId));

            // ✅ CREAR ENTITY CON LOS 3 CAMPOS NUEVOS
            OrdenDet examenEntity = OrdenDet.builder()
                    .ordenCab(ordenCab)
                    .examenId(examenDTO.getExamenId())     // ✅ Solo el ID de tabla examenes

                    // ✅ AGREGAR LOS 3 CAMPOS NUEVOS
                    .cantidad(Optional.ofNullable(examenDTO.getCant()).orElse(1))
                    .desIndicacion(examenDTO.getDesIndicacion())
                    .desConsideraciones(examenDTO.getDesConsideraciones())

                    .ordenItem(i + 1)                       // ✅ Orden secuencial
                    .estadoDetalle("01")                    // ✅ Pendiente
                    .activo("S")                            // ✅ Activo
                    .creadoPor(medicoId)                    // ✅ Médico que crea
                    .creadoEn(ahora)
                    .build();

            // ✅ GUARDAR INDIVIDUAL
            OrdenDet examenGuardado = ordenDetRepository.save(examenEntity);
            log.debug("✅ Examen guardado: ID {} - ExamenId {}",
                    examenGuardado.getId(), examenDTO.getExamenId());
        }
    }

    @Override
    public Optional<OrdenCompletaDTO> obtenerOrdenCompleta(Long ordenId) {
        log.info("🔍 Obteniendo orden completa ID: {}", ordenId);

        return ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .map(toCompletaDTO);
    }

    @Override
    public Optional<OrdenCompletaDTO> obtenerOrdenPorNumero(String numeroOrden) {
        log.info("🔍 Obteniendo orden por número: {}", numeroOrden);

        return ordenCabRepository.findByNumeroOrden(numeroOrden)
                .filter(esActiva)
                .map(toCompletaDTO);
    }

    /**
     * 🔧 ACTUALIZAR EL MÉTODO PRINCIPAL actualizarOrden()
     */
    @Override
    @Transactional
    public OrdenCompletaDTO actualizarOrden(Long ordenId, ActualizarOrdenDTO actualizarDTO, Long medicoId) {
        log.info("📝 Actualizando orden ID: {} por médico: {}", ordenId, medicoId);

        return ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .map(orden -> validarYActualizarOrden(orden, actualizarDTO, medicoId))
                .map(ordenCabRepository::save)
                .map(orden -> procesarOperacionesExamenes(orden, actualizarDTO, medicoId)) // ← AQUÍ PASAS EL DTO
                .map(toCompletaDTO)
                .orElseThrow(ordenNoEncontrada);
    }

    @Override
    @Transactional
    public OrdenCompletaDTO cambiarEstadoOrden(Long ordenId, ActualizarEstadoOrdenDTO estadoDTO) {
        log.info("🔄 Cambiando estado orden ID: {} a estado: {}", ordenId, estadoDTO.getEstado());

        return ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .map(orden -> aplicarCambioEstado(orden, estadoDTO))
                .map(ordenCabRepository::save)
                .map(toCompletaDTO)
                .orElseThrow(ordenNoEncontrada);
    }

    // =====================================================
    // 🚀 CONSULTAS FUNCIONALES CON STREAMS
    // =====================================================

    @Override
    public List<OrdenResumenDTO> obtenerTodasLasOrdenes() {
        log.info("📋 Obteniendo todas las órdenes activas");

        return ordenCabRepository.findAll()
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorPaciente(Long pacienteId) {
        log.info("👤 Obteniendo órdenes para paciente: {}", pacienteId);

        return ordenCabRepository.findByPacienteId(pacienteId)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorOrigen(String tipoOrigen, Long origenId) {
        log.info("🏥 Obteniendo órdenes para origen: {}:{}", tipoOrigen, origenId);

        return ordenCabRepository.findByOrigen(tipoOrigen, origenId)
                .stream()
                .filter(esActiva)
                .sorted(porPrioridadDesc.thenComparing(porFechaDesc))
                .map(toResumenDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorEstado(String estado) {
        return ejecutarConsultaConFiltro(
                () -> ordenCabRepository.findByEstado(estado),
                "📊 Obteniendo órdenes por estado: " + estado
        );
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorPrioridad(String prioridad) {
        return ejecutarConsultaConFiltro(
                () -> ordenCabRepository.findByPrioridad(prioridad),
                "⚡ Obteniendo órdenes por prioridad: " + prioridad
        );
    }

    // =====================================================
    // 🔬 GESTIÓN DE EXÁMENES CON JOIN MÁGICO
    // =====================================================

    @Override
    public List<OrdenExamenDTO> obtenerExamenesDeOrden(Long ordenId) {
        log.info("🔬 Obteniendo exámenes de orden: {}", ordenId);

        // ✅ USAR EL QUERY CON JOIN QUE YA TIENES EN TU REPOSITORY
        return ordenDetRepository.findExamenesConInfoByOrdenId(ordenId)
                .stream()
                .map(this::convertirObjectArrayAOrdenExamenDTO)  // ← NUEVO MÉTODO
                .collect(Collectors.toList());
    }

    /**
     * 🔥 NUEVO MÉTODO: Convertir Object[] del JOIN a DTO
     * Object[0] = OrdenDet
     * Object[1] = Examen
     */
    private OrdenExamenDTO convertirObjectArrayAOrdenExamenDTO(Object[] row) {
        OrdenDet ordenDet = (OrdenDet) row[0];
        Examen examen = (Examen) row[1];  // ← AQUÍ ESTÁ LA INFO DEL EXAMEN

        return OrdenExamenDTO.builder()
                .id(ordenDet.getId())
                .examenId(ordenDet.getExamenId())
                .nomExamen(examen.getNombre())           // ✅ AHORA SÍ TENDRÁ VALOR
                .categoria(examen.getCategoria())        // ✅ CATEGORÍA DEL EXAMEN
                .cant(ordenDet.getCantidad())
                .desIndicacion(ordenDet.getDesIndicacion())
                .desConsideraciones(ordenDet.getDesConsideraciones())
                .build();
    }

    /**
     * 🔥 MÉTODO PRINCIPAL ACTUALIZADO - CON CAMPOS REQUERIDOS
     */
    private OrdenExamenDTO convertirExamenEntityADTO(OrdenDet examen) {

        // ✅ DEBUG TEMPORAL
        log.debug("🔬 Convirtiendo examen: ID={}, ExamenId={}, Cant={}",
                examen.getId(), examen.getExamenId(), examen.getCantidad());

        return OrdenExamenDTO.builder()
                .id(examen.getId())
                .examenId(examen.getExamenId())
                .cant(examen.getCantidad())
                .desIndicacion(examen.getDesIndicacion())
                .desConsideraciones(examen.getDesConsideraciones())
                .build();
    }

    /**
     * Agregar examen a una orden existente
     */
    @Override
    @Transactional
    public OrdenCompletaDTO agregarExamenAOrden(Long ordenId, OrdenDetDTO examenDTO, Long medicoId) {
        log.info("➕ Agregando examen {} a orden: {}", examenDTO.getExamenId(), ordenId);

        return ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .map(orden -> crearYGuardarExamen(orden, examenDTO, medicoId))
                .map(OrdenDet::getOrdenCab)
                .map(toCompletaDTO)
                .orElseThrow(ordenNoEncontrada);
    }

    // =====================================================
    // 📊 ESTADÍSTICAS CON PROGRAMACIÓN FUNCIONAL
    // =====================================================

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorEstado() {
        log.info("📊 Generando estadísticas por estado");

        return ordenCabRepository.findEstadisticasPorEstado()
                .stream()
                .map(this::convertirObjectArrayAEstadistica)
                .sorted(Comparator.comparing(EstadisticaDTO::getTotal).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamenEstadisticaDTO> obtenerExamenesMasSolicitados() {
        log.info("🏆 Obteniendo exámenes más solicitados");

        return ordenDetRepository.findExamenesMasSolicitados()
                .stream()
                .limit(10) // Top 10
                .map(this::convertirObjectArrayAExamenEstadistica)
                .collect(Collectors.toList());
    }

    // =====================================================
    // 🛠️ MÉTODOS HELPER FUNCIONALES
    // =====================================================

    /**
     * Pipeline funcional para consultas con filtro estándar
     */
    private List<OrdenResumenDTO> ejecutarConsultaConFiltro(Supplier<List<OrdenCab>> consulta, String logMessage) {
        log.info(logMessage);

        return consulta.get()
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validar que no existe orden duplicada del mismo tipo hoy
     */
    private void validarOrdenNoDuplicada(OrdenCabDTO ordenDTO) {
        boolean existe = ordenCabRepository.existeOrdenMismoTipoHoy(
                ordenDTO.getTipoOrigen(),
                ordenDTO.getOrigenId(),
                ordenDTO.getTipoOrden()
        );
        validationHelper.validarOrdenNoDuplicada(
                ordenDTO.getTipoOrigen(),
                ordenDTO.getOrigenId(),
                ordenDTO.getTipoOrden(),
                existe
        );
    }

    /**
     * Preparar orden para creación con datos automáticos
     */
    private OrdenCabDTO prepararOrdenParaCreacion(OrdenCabDTO ordenDTO) {
        // Generar número único
        if (ordenDTO.getTipoOrden() == null) {
            ordenDTO.setTipoOrden(inferirTipoOrdenDeExamenes(ordenDTO.getExamenes()));
        }

        // Asignar orden a exámenes
        IntStream.range(0, ordenDTO.getExamenes().size())
                .forEach(i -> ordenDTO.getExamenes().get(i).setOrdenItem(i + 1));

        return ordenDTO;
    }

    /**
     * Aplicar firma automática según reglas de negocio
     */
    private OrdenCabDTO aplicarFirmaAutomatica(OrdenCabDTO ordenDTO) {
        // Auto-firma si es emergencia o ≤ 3 exámenes
        boolean autoFirmar = "E".equals(ordenDTO.getPrioridad()) ||
                ordenDTO.getExamenes().size() <= 3;

        if (autoFirmar) {
            log.info("🖊️ Aplicando firma automática - Prioridad: {}, Exámenes: {}",
                    ordenDTO.getPrioridad(), ordenDTO.getExamenes().size());
        }

        return ordenDTO;
    }

    /**
     * Establecer datos de creación
     */
    private OrdenCab establecerDatosCreacion(OrdenCab orden) {
        orden.setNumeroOrden(generarNumeroOrden());
        LocalDateTime ahora = LocalDateTime.now();
        orden.setCreadoEn(ahora);
        orden.setActualizadoEn(ahora);
        return orden;
    }

    /**
     * Guardar exámenes transaccional
     */
    private OrdenCab guardarExamenes(OrdenCab ordenGuardada) {
        if (ordenGuardada.getExamenes() != null && !ordenGuardada.getExamenes().isEmpty()) {
            List<OrdenDet> examenes = ordenGuardada.getExamenes()
                    .stream()
                    .peek(examen -> {
                        // 🔥 ESTABLECER CAMPOS OBLIGATORIOS
                        examen.setOrdenCab(ordenGuardada);           // ✅ Relación con cabecera
                        examen.setCreadoPor(ordenGuardada.getCreadoPor()); // ✅ Médico que crea
                        examen.setCreadoEn(LocalDateTime.now());     // ✅ Fecha de creación


                        // 🔥 VALORES POR DEFECTO
                        if (examen.getEstadoDetalle() == null) {
                            examen.setEstadoDetalle("01"); // Pendiente
                        }
                        if (examen.getActivo() == null) {
                            examen.setActivo("S"); // Activo
                        }
                        if (examen.getOrdenItem() == null) {
                            examen.setOrdenItem(1); // Orden por defecto
                        }
                    })
                    .collect(Collectors.toList());

            ordenDetRepository.saveAll(examenes);
        }
        return ordenGuardada;
    }

    // =====================================================
    // 🔄 CONVERSORES FUNCIONALES
    // =====================================================

    /**
     * Convertir Entity a DTO completo (con JOIN mágico)
     */
    private OrdenCompletaDTO convertirAOrdenCompleta(OrdenCab orden) {
        List<OrdenExamenDTO> examenes = obtenerExamenesDeOrden(orden.getId());

        return OrdenCompletaDTO.builder()
                .id(orden.getId())
                .numeroOrden(orden.getNumeroOrden())
                .pacienteId(orden.getPacienteId())
                .medicoId(orden.getMedicoId())
                .tipoOrigen(orden.getTipoOrigen())
                .tipoOrigenDescripcion(obtenerDescripcionOrigen(orden.getTipoOrigen()))
                .origenId(orden.getOrigenId())
                .tipoOrden(orden.getTipoOrden())
                .fechaOrden(orden.getFechaOrden())
                .fechaProgramada(orden.getFechaProgramada())
                .diagnosticoPrincipal(orden.getDiagnosticoPrincipal())
                .justificacionClinica(orden.getJustificacionClinica())
                .prioridad(orden.getPrioridad())
                .prioridadDescripcion(obtenerDescripcionPrioridad(orden.getPrioridad()))
                .estado(orden.getEstado())
                .estadoDescripcion(obtenerDescripcionEstado(orden.getEstado()))
                .firmada(orden.getFirmada())
                .fechaFirma(orden.getFechaFirma())
                .firmaDigital(orden.getFirmaDigital() != null ? java.util.Base64.getEncoder().encodeToString(orden.getFirmaDigital().toString().getBytes()) : null)
                .examenes(examenes)
                .creadoEn(orden.getCreadoEn())
                .creadoPor(orden.getCreadoPor())
                .totalExamenes(contarExamenesActivos(orden.getId()))
                .build();
    }

    /**
     * Convertir Entity a DTO resumen
     */
    private OrdenResumenDTO convertirAOrdenResumen(OrdenCab orden) {
        return OrdenResumenDTO.builder()
                .id(orden.getId())
                .numeroOrden(orden.getNumeroOrden())
                .pacienteId(orden.getPacienteId())
                .tipoOrigen(orden.getTipoOrigen())
                .tipoOrigenDescripcion(obtenerDescripcionOrigen(orden.getTipoOrigen()))
                .tipoOrden(orden.getTipoOrden())
                .estado(orden.getEstado())
                .estadoDescripcion(obtenerDescripcionEstado(orden.getEstado()))
                .prioridad(orden.getPrioridad())
                .prioridadDescripcion(obtenerDescripcionPrioridad(orden.getPrioridad()))
                .fechaOrden(orden.getFechaOrden())
                .fechaProgramada(orden.getFechaProgramada())
                .totalExamenes(orden.getExamenes().size())
                .firmada(orden.getFirmada())
                .build();
    }

    /**
     * Convertir DTO a Entity
     */
    private OrdenCab convertirAEntity(OrdenCabDTO dto) {
        // 1. Crear la cabecera
        OrdenCab ordenCab = OrdenCab.builder()
                .pacienteId(dto.getPacienteId())
                .medicoId(dto.getMedicoId())
                .tipoOrigen(dto.getTipoOrigen())
                .origenId(dto.getOrigenId())
                .tipoOrden(dto.getTipoOrden())
                .fechaProgramada(dto.getFechaProgramada())
                .diagnosticoPrincipal(dto.getDiagnosticoPrincipal())
                .justificacionClinica(dto.getJustificacionClinica())
                .prioridad(Optional.ofNullable(dto.getPrioridad()).orElse("N"))
                .creadoPor(dto.getMedicoId()) // El médico que crea
                .build();

        // 2. Convertir exámenes si existen
        if (dto.getExamenes() != null && !dto.getExamenes().isEmpty()) {
            List<OrdenDet> examenes = dto.getExamenes()
                    .stream()
                    .map(examenDTO -> {
                        OrdenDet examen = convertirExamenDTOAEntity(examenDTO);
                        // 🔥 ESTABLECER LA RELACIÓN BIDIRECCIONAL
                        examen.setOrdenCab(ordenCab);  // ✅ CRÍTICO
                        examen.setCreadoPor(dto.getMedicoId()); // ✅ CRÍTICO
                        return examen;
                    })
                    .collect(Collectors.toList());

            ordenCab.setExamenes(examenes);
        }

        return ordenCab;
    }

    // =====================================================
    // 🔧 UTILIDADES FUNCIONALES
    // =====================================================

    @Override
    public String generarNumeroOrden() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String secuencia = String.format("%03d", new Random().nextInt(1000));
        return String.format("ORD-%s-%s", fecha, secuencia);
    }

    /**
     * Comparador personalizado para prioridades
     */
    private int compararPrioridades(String p1, String p2) {
        Map<String, Integer> orden = Map.of("E", 3, "U", 2, "N", 1);
        return orden.getOrDefault(p2, 0).compareTo(orden.getOrDefault(p1, 0));
    }

    /**
     * Inferir tipo de orden basado en exámenes
     */
    private String inferirTipoOrdenDeExamenes(List<OrdenDetDTO> examenes) {
        // Lógica simple: tomar el tipo más común
        return examenes.stream()
                .map(OrdenDetDTO::getCategoria)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("LAB");
    }

    // Métodos auxiliares para obtener descripciones
    private String obtenerDescripcionOrigen(String tipoOrigen) {
        Map<String, String> origenes = Map.of(
            "HOS", "Hospitalización",
            "AMB", "Ambulatorio",
            "EME", "Emergencia",
            "CON", "Consultorio"
        );
        return origenes.getOrDefault(tipoOrigen, tipoOrigen);
    }

    private String obtenerDescripcionPrioridad(String prioridad) {
        Map<String, String> prioridades = Map.of(
            "U", "Urgente",
            "E", "Emergencia",
            "N", "Normal"
        );
        return prioridades.getOrDefault(prioridad, prioridad);
    }

    private String obtenerDescripcionEstado(String estado) {
        Map<String, String> estados = Map.of(
            "01", "Solicitada",
            "02", "En Proceso",
            "03", "Completada",
            "04", "Cancelada",
            "05", "Anulada"
        );
        return estados.getOrDefault(estado, estado);
    }

    private Integer contarExamenesActivos(Long ordenId) {
        return ordenDetRepository.countByOrdenIdAndActivo(ordenId, "S");
    }

    // =====================================================
    // 📝 MÉTODOS FALTANTES IMPLEMENTADOS
    // =====================================================

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorMedico(Long medicoId) {
        return ejecutarConsultaConFiltro(
                () -> ordenCabRepository.findByMedicoId(medicoId),
                "👨‍⚕️ Obteniendo órdenes por médico: " + medicoId
        );
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorTipo(String tipoOrden) {
        return ejecutarConsultaConFiltro(
                () -> ordenCabRepository.findByTipoOrden(tipoOrden),
                "🔬 Obteniendo órdenes por tipo: " + tipoOrden
        );
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorFecha(LocalDate fecha) {
        return ejecutarConsultaConFiltro(
                () -> ordenCabRepository.findByFechaOrden(fecha),
                "📅 Obteniendo órdenes por fecha: " + fecha
        );
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesProgramadas(LocalDate fecha) {
        return ejecutarConsultaConFiltro(
                () -> ordenCabRepository.findByFechaProgramada(fecha),
                "📋 Obteniendo órdenes programadas para: " + fecha
        );
    }

    @Override
    @Transactional
    public void eliminarOrden(Long ordenId, Long medicoId) {
        ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .ifPresentOrElse(
                        orden -> {
                            orden.setActivo("N");
                            orden.setActualizadoPor(medicoId);
                            orden.setActualizadoEn(LocalDateTime.now());
                            ordenCabRepository.save(orden);
                            log.info("🗑️ Orden eliminada: {}", orden.getNumeroOrden());
                        },
                        () -> { throw new OrdenNotFoundException(ordenId); }
                );
    }

    @Override
    public boolean existeOrden(Long ordenId) {
        return ordenCabRepository.findById(ordenId)
                .map(esActiva::test)
                .orElse(false);
    }

    @Override
    public boolean puedeCrearOrden(String tipoOrigen, Long origenId, String tipoOrden) {
        return !ordenCabRepository.existeOrdenMismoTipoHoy(tipoOrigen, origenId, tipoOrden);
    }

    // =====================================================
    // 🔧 MÉTODOS HELPER ADICIONALES FALTANTES
    // =====================================================

    /**
     * Validar y actualizar orden con DTO
     */
    private OrdenCab validarYActualizarOrden(OrdenCab orden, ActualizarOrdenDTO actualizarDTO, Long medicoId) {
        validationHelper.validarActualizacionOrden(orden, actualizarDTO);

        // Aplicar cambios solo a campos no nulos
        Optional.ofNullable(actualizarDTO.getDiagnosticoPrincipal())
                .ifPresent(orden::setDiagnosticoPrincipal);
        Optional.ofNullable(actualizarDTO.getJustificacionClinica())
                .ifPresent(orden::setJustificacionClinica);
        Optional.ofNullable(actualizarDTO.getFechaProgramada())
                .ifPresent(orden::setFechaProgramada);
        Optional.ofNullable(actualizarDTO.getPrioridad())
                .ifPresent(orden::setPrioridad);

        orden.setActualizadoPor(medicoId);
        orden.setActualizadoEn(LocalDateTime.now());

        return orden;
    }

    /**
     * Aplicar cambio de estado
     */
    private OrdenCab aplicarCambioEstado(OrdenCab orden, ActualizarEstadoOrdenDTO estadoDTO) {
        validationHelper.validarCambioEstado(orden, estadoDTO.getEstado());

        orden.setEstado(estadoDTO.getEstado());
        orden.setActualizadoPor(estadoDTO.getMedicoId());
        orden.setActualizadoEn(LocalDateTime.now());

        return orden;
    }

    /**
     * 🔥 PROCESAR OPERACIONES CON EXÁMENES - IMPLEMENTACIÓN COMPLETA
     * Agrega este método en tu OrdenServiceImpl
     */
    private OrdenCab procesarOperacionesExamenes(OrdenCab orden, ActualizarOrdenDTO actualizarDTO, Long medicoId) {
        if (!actualizarDTO.tieneOperacionesExamenes()) {
            return orden; // No hay operaciones, retornar orden tal como está
        }

        log.info("🔬 Procesando operaciones con exámenes para orden: {}", orden.getId());

        // 1️⃣ ELIMINAR EXÁMENES
        if (actualizarDTO.getEliminarExamenes() != null && !actualizarDTO.getEliminarExamenes().isEmpty()) {
            log.info("🗑️ Eliminando {} exámenes", actualizarDTO.getEliminarExamenes().size());

            actualizarDTO.getEliminarExamenes().forEach(examenDetalleId -> {
                try {
                    eliminarExamenDeOrden(orden.getId(), examenDetalleId, medicoId);
                    log.info("✅ Examen eliminado: {}", examenDetalleId);
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo eliminar examen {}: {}", examenDetalleId, e.getMessage());
                }
            });
        }

        // 2️⃣ AGREGAR NUEVOS EXÁMENES
        if (actualizarDTO.getAgregarExamenes() != null && !actualizarDTO.getAgregarExamenes().isEmpty()) {
            log.info("➕ Agregando {} exámenes nuevos", actualizarDTO.getAgregarExamenes().size());

            actualizarDTO.getAgregarExamenes().forEach(nuevoExamen -> {
                try {
                    OrdenDetDTO examenDTO = convertirNuevoExamenAOrdenDetDTO(nuevoExamen);
                    agregarExamenAOrden(orden.getId(), examenDTO, medicoId);
                    log.info("✅ Examen agregado: {}", nuevoExamen.getExamenId());
                } catch (Exception e) {
                    log.error("❌ Error al agregar examen {}: {}", nuevoExamen.getExamenId(), e.getMessage());
                    throw new OrdenBusinessException("ORDEN_ADD_EXAM",
                            "No se pudo agregar el examen: " + e.getMessage());
                }
            });
        }

        // 3️⃣ MODIFICAR EXÁMENES EXISTENTES
        if (actualizarDTO.getModificarExamenes() != null && !actualizarDTO.getModificarExamenes().isEmpty()) {
            log.info("✏️ Modificando {} exámenes existentes", actualizarDTO.getModificarExamenes().size());

            actualizarDTO.getModificarExamenes().forEach(modificarExamen -> {
                try {
                    // Necesitas el ID del examen del request
                    Long examenDetalleId = modificarExamen.getId(); // Agregar este campo al DTO
                    actualizarExamenEnOrden(orden.getId(), examenDetalleId, modificarExamen, medicoId);
                    log.info("✅ Examen modificado: {}", examenDetalleId);
                } catch (Exception e) {
                    log.error("❌ Error al modificar examen: {}", e.getMessage());
                    throw new OrdenBusinessException("ORDEN_MODIFY_EXAM",
                            "No se pudo modificar el examen: " + e.getMessage());
                }
            });
        }

        // 4️⃣ RECARGAR LA ORDEN CON LOS CAMBIOS
        return ordenCabRepository.findById(orden.getId())
                .orElseThrow(() -> new OrdenNotFoundException(orden.getId()));
    }

    /**
     * 🔄 CONVERTIR NuevoExamenDTO a OrdenDetDTO
     */
    private OrdenDetDTO convertirNuevoExamenAOrdenDetDTO(ActualizarOrdenDTO.NuevoExamenDTO nuevoExamen) {
        return OrdenDetDTO.builder()
                .examenId(nuevoExamen.getExamenId())
                .cant(1) // Default
                .desIndicacion(nuevoExamen.getIndicaciones())
                .desConsideraciones(nuevoExamen.getPreparacionEspecial())
                .build();
    }
    /**
     * Crear y guardar nuevo examen con campos actualizados
     */
    private OrdenDet crearYGuardarExamen(OrdenCab orden, OrdenDetDTO examenDTO, Long medicoId) {
        OrdenDet examen = convertirExamenDTOAEntity(examenDTO);
        examen.setOrdenCab(orden);
        examen.setCreadoPor(medicoId);
        examen.setCreadoEn(LocalDateTime.now());
        examen.setActivo("S");
        examen.setEstadoDetalle("01"); // Estado inicial: Solicitado
        examen.setOrdenItem(Optional.ofNullable(examenDTO.getOrdenItem()).orElse(1));

        // Campos actualizados
        examen.setCantidad(Optional.ofNullable(examenDTO.getCant()).orElse(1));
        examen.setDesIndicacion(examenDTO.getDesIndicacion());
        examen.setDesConsideraciones(examenDTO.getDesConsideraciones());

        return ordenDetRepository.save(examen);
    }

    /**
     * Convertir ExamenDTO a Entity con campos actualizados
     */
    private OrdenDet convertirExamenDTOAEntity(OrdenDetDTO dto) {
        return OrdenDet.builder()
                .examenId(dto.getExamenId())
                .ordenItem(Optional.ofNullable(dto.getOrdenItem()).orElse(1))
                .cantidad(Optional.ofNullable(dto.getCant()).orElse(1))
                .desIndicacion(dto.getDesIndicacion())
                .desConsideraciones(dto.getDesConsideraciones())
                .estadoDetalle("01") // Estado inicial: Solicitado
                .activo("S")
                .build();
    }

    /**
     * Convertir Object[] a EstadisticaDTO
     */
    private EstadisticaDTO convertirObjectArrayAEstadistica(Object[] row) {
        return new EstadisticaDTO() {
            @Override
            public String getCategoria() {
                return (String) row[0];
            }

            @Override
            public Long getTotal() {
                return ((Number) row[1]).longValue();
            }

            @Override
            public String getDescripcion() {
                return getCategoria(); // Por ahora igual a categoría
            }
        };
    }

    // =====================================================
    // 🔧 MÉTODOS HELPER PARA DESCRIPCIONES
    // =====================================================

    private String getEstadoDetalleDescripcion(String estado) {
        switch (estado) {
            case "01": return "Pendiente";
            case "02": return "En Proceso";
            case "03": return "Completado";
            case "04": return "Cancelado";
            default: return "Desconocido";
        }
    }

    private String getCategoriaDescripcion(String categoria) {
        switch (categoria) {
            case "LAB": return "Laboratorio";
            case "IMG": return "Imagenología";
            case "PROC": return "Procedimiento";
            case "Hematología": return "Hematología";
            case "Bioquímica": return "Bioquímica";
            case "Oncología": return "Oncología";
            case "Cardiología": return "Cardiología";
            case "Neumología": return "Neumología";
            default: return "Otros";
        }
    }

    /**
     * Convertir Object[] a ExamenEstadisticaDTO
     */
    private ExamenEstadisticaDTO convertirObjectArrayAExamenEstadistica(Object[] row) {
        return new ExamenEstadisticaDTO() {
            @Override
            public String getCodigoExamen() {
                return (String) row[0];
            }

            @Override
            public String getNombreExamen() {
                return (String) row[1];
            }

            @Override
            public Long getTotalSolicitado() {
                return ((Number) row[2]).longValue();
            }

            @Override
            public String getCategoria() {
                return ""; // Se puede agregar si está en la query
            }
        };
    }

    @Override
    @Transactional
    public OrdenExamenDTO actualizarExamenEnOrden(Long ordenId, Long examenDetalleId,
                                                  ActualizarOrdenDTO.ModificarExamenDTO examenDTO, Long medicoId) {
        log.info("✏️ Actualizando examen {} en orden: {}", examenDetalleId, ordenId);

        return ordenDetRepository.findById(examenDetalleId)
                .filter(examen -> examen.getOrdenCab().getId().equals(ordenId))
                .filter(examen -> examen.esActivo())
                .map(examen -> aplicarCambiosAExamen(examen, examenDTO, medicoId))
                .map(ordenDetRepository::save)
                .map(this::convertirExamenEntityADTO)
                .orElseThrow(() -> new ExamenNotFoundException(examenDetalleId));
    }

    /**
     * Aplicar cambios a examen existente con campos actualizados
     */
    private OrdenDet aplicarCambiosAExamen(OrdenDet examen, ActualizarOrdenDTO.ModificarExamenDTO examenDTO, Long medicoId) {
        // Campos actualizados
        Optional.ofNullable(examenDTO.getCant()).ifPresent(examen::setCantidad);
        Optional.ofNullable(examenDTO.getDesIndicacion()).ifPresent(examen::setDesIndicacion);
        Optional.ofNullable(examenDTO.getDesConsideraciones()).ifPresent(examen::setDesConsideraciones);

        examen.setActualizadoPor(medicoId);
        examen.setActualizadoEn(LocalDateTime.now());

        return examen;
    }

    @Override
    @Transactional
    public void eliminarExamenDeOrden(Long ordenId, Long examenDetalleId, Long medicoId) {
        log.info("🗑️ Eliminando examen {} de orden: {}", examenDetalleId, ordenId);

        ordenDetRepository.findById(examenDetalleId)
                .filter(examen -> examen.getOrdenCab().getId().equals(ordenId))
                .filter(examen -> examen.esActivo())
                .ifPresentOrElse(
                        examen -> {
                            examen.setActivo("N");
                            examen.setActualizadoPor(medicoId);
                            examen.setActualizadoEn(LocalDateTime.now());
                            ordenDetRepository.save(examen);
                        },
                        () -> { throw new ExamenNotFoundException(examenDetalleId); }
                );
    }

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorTipo() {
        log.info("📊 Generando estadísticas por tipo de orden");

        return ordenCabRepository.findEstadisticasPorTipo()
                .stream()
                .map(this::convertirObjectArrayAEstadistica)
                .sorted(Comparator.comparing(EstadisticaDTO::getTotal).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorPrioridad() {
        log.info("⚡ Generando estadísticas por prioridad");

        return ordenCabRepository.findEstadisticasPorPrioridad()
                .stream()
                .map(this::convertirObjectArrayAEstadistica)
                .sorted(Comparator.comparing(EstadisticaDTO::getTotal).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorCategoria() {
        log.info("🏷️ Generando estadísticas por categoría de examen");

        return ordenDetRepository.findEstadisticasPorCategoria()
                .stream()
                .map(this::convertirObjectArrayAEstadistica)
                .sorted(Comparator.comparing(EstadisticaDTO::getTotal).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean existeOrdenPorNumero(String numeroOrden) {
        return ordenCabRepository.findByNumeroOrden(numeroOrden)
                .map(esActiva::test)
                .orElse(false);
    }
}