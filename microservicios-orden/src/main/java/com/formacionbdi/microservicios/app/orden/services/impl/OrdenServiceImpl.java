package com.formacionbdi.microservicios.app.orden.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formacionbdi.microservicios.app.orden.exception.*;
import com.formacionbdi.microservicios.app.orden.models.dto.*;
import com.formacionbdi.microservicios.app.orden.models.entity.OrdenCab;
import com.formacionbdi.microservicios.app.orden.models.entity.OrdenDet;
import com.formacionbdi.microservicios.app.orden.models.entity.Examen;
import com.formacionbdi.microservicios.app.orden.repository.OrdenCabRepository;
import com.formacionbdi.microservicios.app.orden.repository.OrdenDetRepository;
import com.formacionbdi.microservicios.app.orden.services.OrdenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * 🩺 Service Implementation con Java 17 y Records - REFACTORIZADO
 * Puerto: 8006
 *
 * ✅ Features Java 17 implementadas:
 * - Records para DTOs inmutables (ActualizarEstadoOrdenDTO, OrdenResumenDTO)
 * - Switch expressions optimizadas
 * - Text blocks para JSON PostgreSQL
 * - Pattern matching avanzado
 * - Functions y Predicates reutilizables
 * - Stream API con .toList()
 * - Sealed classes para excepciones
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrdenServiceImpl implements OrdenService {

    private final OrdenCabRepository ordenCabRepository;
    private final OrdenDetRepository ordenDetRepository;
    private final ObjectMapper objectMapper;

    // =====================================================
    // 🔥 FUNCTIONS FUNCIONALES REUTILIZABLES (JAVA 17)
    // =====================================================

    private final Function<OrdenCab, OrdenCompletaDTO> toCompletaDTO = this::convertirAOrdenCompleta;
    private final Function<OrdenCab, OrdenResumenDTO> toResumenDTO = this::convertirAOrdenResumen;
    private final Function<OrdenCabDTO, OrdenCab> toEntity = this::convertirAEntity;
    private final Function<Object[], OrdenExamenDTO> toOrdenExamenDTO = this::convertirObjectArrayAOrdenExamen;

    // Predicados de validación con pattern matching
    private final Predicate<OrdenCab> esActiva = orden -> "S".equals(orden.getActivo());
    private final Predicate<OrdenCab> puedeModificarse = this::validarSiPuedeModificarse;
    private final Predicate<OrdenCab> estaFirmada = orden -> "S".equals(orden.getFirmada());
    private final Predicate<OrdenCab> esEmergencia = orden -> "E".equals(orden.getPrioridad());

    // Suppliers para excepciones con mensajes específicos
    private final Supplier<OrdenNotFoundException> ordenNoEncontrada =
            () -> new OrdenNotFoundException("Orden no encontrada");

    // Comparators funcionales con Java 17
    private final Comparator<OrdenCab> porFechaDesc =
            Comparator.comparing(OrdenCab::getFechaOrden).reversed();
    private final Comparator<OrdenCab> porPrioridadDesc =
            Comparator.comparing(OrdenCab::getPrioridad, this::compararPrioridades);

    // =====================================================
    // 🚀 CRUD PRINCIPALES CON RECORDS Y SWITCH EXPRESSIONS
    // =====================================================

    @Override
    @Transactional
    public OrdenCompletaDTO crearOrden(OrdenCabDTO ordenDTO) {
        log.info("🔥 Creando orden: tipo={}, examenes={}",
                ordenDTO.tipoOrden(), ordenDTO.examenes().size());

        try {
            // Text block para JSON (Java 17)
            String jsonData = prepararJsonParaCreacion(ordenDTO);
            String resultado = ordenCabRepository.crearOrdenAtomica(jsonData);

            Long ordenId = extraerIdDeRespuestaJson(resultado);
            log.info("✅ Orden creada con ID: {}", ordenId);

            return ordenCabRepository.findById(ordenId)
                    .filter(esActiva)
                    .map(toCompletaDTO)
                    .orElseThrow(() -> new OrdenBusinessException("ORDEN_100",
                            "Error al recuperar orden creada"));

        } catch (Exception e) {
            log.error("❌ Error creando orden: {}", e.getMessage(), e);
            throw new OrdenBusinessException("ORDEN_101",
                    "Error al crear orden: " + e.getMessage());
        }
    }

    @Override
    public Optional<OrdenCompletaDTO> obtenerOrdenCompleta(Long ordenId) {
        log.debug("🔍 Obteniendo orden completa: {}", ordenId);

        return ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .map(toCompletaDTO);
    }

    @Override
    public Optional<OrdenCompletaDTO> obtenerOrdenPorNumero(String numeroOrden) {
        log.debug("🔍 Obteniendo orden por número: {}", numeroOrden);

        return ordenCabRepository.findByNumeroOrden(numeroOrden)
                .filter(esActiva)
                .map(toCompletaDTO);
    }

    @Override
    @Transactional
    public OrdenCompletaDTO actualizarOrden(Long ordenId, ActualizarOrdenDTO actualizarDTO, Long medicoId) {
        log.info("📝 Actualizando orden {}: {}", ordenId, actualizarDTO.getResumenCambios());

        var orden = ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .orElseThrow(() -> new OrdenNotFoundException("Orden no encontrada con ID: " + ordenId));

        try {
            String jsonData = prepararJsonParaActualizacion(ordenId, actualizarDTO, medicoId);
            ordenCabRepository.actualizarOrdenAtomica(jsonData);

            log.info("✅ Orden actualizada: {}", ordenId);

            return ordenCabRepository.findById(ordenId)
                    .map(toCompletaDTO)
                    .orElseThrow(() -> new OrdenBusinessException("ORDEN_102",
                            "Error al recuperar orden actualizada"));

        } catch (Exception e) {
            log.error("❌ Error actualizando orden: {}", e.getMessage(), e);
            throw new OrdenBusinessException("ORDEN_103",
                    "Error al actualizar orden: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrdenCompletaDTO cambiarEstadoOrden(Long ordenId, ActualizarEstadoOrdenDTO estadoDTO) {
        log.info("🔄 Cambiando estado orden {} a: {}", ordenId, estadoDTO.nuevoEstado());

        var orden = ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .orElseThrow(() -> new OrdenNotFoundException("Orden no encontrada con ID: " + ordenId));

        if (!validarTransicionEstado(orden.getEstado(), estadoDTO.nuevoEstado())) {
            throw new OrdenBusinessException("ORDEN_ESTADO",
                    "No se puede cambiar de estado %s a %s".formatted(orden.getEstado(), estadoDTO.nuevoEstado()));
        }

        try {
            String jsonData = prepararJsonParaCambioEstado(ordenId, estadoDTO.nuevoEstado(),
                    estadoDTO.medicoId(), estadoDTO.observacion());
            ordenCabRepository.cambiarEstadoAtomica(jsonData);

            log.info("✅ Estado cambiado a {}: {}", estadoDTO.nuevoEstado(), ordenId);

            return ordenCabRepository.findById(ordenId)
                    .map(toCompletaDTO)
                    .orElseThrow(() -> new OrdenBusinessException("ORDEN_104",
                            "Error al recuperar orden con estado actualizado"));

        } catch (Exception e) {
            log.error("❌ Error cambiando estado: {}", e.getMessage(), e);
            throw new OrdenBusinessException("ORDEN_105",
                    "Error al cambiar estado: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void eliminarOrden(Long ordenId, Long medicoId) {
        log.info("🗑️ Eliminando orden: {}", ordenId);

        var orden = ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .orElseThrow(() -> new OrdenNotFoundException("Orden no encontrada con ID: " + ordenId));

        try {
            // JSON con text block (Java 17)
            String jsonData = """
                {
                    "orden_id": %d,
                    "medico_id": %d,
                    "activo": "N"
                }
                """.formatted(ordenId, medicoId);

            ordenCabRepository.actualizarOrdenAtomica(jsonData);
            log.info("✅ Orden eliminada: {}", ordenId);

        } catch (Exception e) {
            throw new OrdenBusinessException("ORDEN_110",
                    "Error al eliminar orden: " + e.getMessage());
        }
    }

    // =====================================================
    // 🔍 CONSULTAS CON STREAMS MODERNOS (JAVA 17)
    // =====================================================

    @Override
    public List<OrdenResumenDTO> obtenerTodasLasOrdenes() {
        log.debug("📋 Obteniendo todas las órdenes activas");

        return ordenCabRepository.findAll()
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorPaciente(Long pacienteId) {
        log.debug("👤 Obteniendo órdenes del paciente: {}", pacienteId);

        return ordenCabRepository.findByPacienteId(pacienteId)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorMedico(Long medicoId) {
        log.debug("👨‍⚕️ Obteniendo órdenes del médico: {}", medicoId);

        return ordenCabRepository.findByMedicoId(medicoId)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorEstado(String estado) {
        log.debug("📊 Obteniendo órdenes por estado: {}", estado);

        return ordenCabRepository.findByEstado(estado)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorOrigen(String tipoOrigen, Long origenId) {
        log.debug("🏥 Obteniendo órdenes por origen: {}:{}", tipoOrigen, origenId);

        return ordenCabRepository.findByOrigen(tipoOrigen, origenId)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorPrioridad(String prioridad) {
        log.debug("🚨 Obteniendo órdenes por prioridad: {}", prioridad);

        return ordenCabRepository.findByPrioridad(prioridad)
                .stream()
                .filter(esActiva)
                .sorted(porPrioridadDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorTipo(String tipoOrden) {
        log.debug("📝 Obteniendo órdenes por tipo: {}", tipoOrden);

        return ordenCabRepository.findByTipoOrden(tipoOrden)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesPorFecha(LocalDate fecha) {
        log.debug("📅 Obteniendo órdenes por fecha: {}", fecha);

        return ordenCabRepository.findByFechaOrden(fecha)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<OrdenResumenDTO> obtenerOrdenesProgramadas(LocalDate fecha) {
        log.debug("⏰ Obteniendo órdenes programadas para: {}", fecha);

        return ordenCabRepository.findByFechaProgramada(fecha)
                .stream()
                .filter(esActiva)
                .sorted(porFechaDesc)
                .map(toResumenDTO)
                .toList(); // Java 17 ✅
    }

    // =====================================================
    // 🔬 GESTIÓN DE EXÁMENES CON JOINS
    // =====================================================

    @Override
    public List<OrdenExamenDTO> obtenerExamenesDeOrden(Long ordenId) {
        log.debug("🔬 Obteniendo exámenes de orden: {}", ordenId);

        return ordenDetRepository.findExamenesConInfoByOrdenId(ordenId)
                .stream()
                .map(toOrdenExamenDTO)
                .sorted(Comparator.comparing(OrdenExamenDTO::ordenItem,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList(); // Java 17 ✅
    }

    @Override
    @Transactional
    public OrdenCompletaDTO agregarExamenAOrden(Long ordenId, OrdenDetDTO examenDTO, Long medicoId) {
        log.info("➕ Agregando examen {} a orden: {}", examenDTO.examenId(), ordenId);

        var orden = ordenCabRepository.findById(ordenId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .orElseThrow(() -> new OrdenNotFoundException("Orden no encontrada con ID: " + ordenId));

        // Verificar que examen no exista ya
        if (ordenDetRepository.existeExamenEnOrden(ordenId, examenDTO.examenId())) {
            throw new OrdenBusinessException("ORDEN_DUPLICADO",
                    "El examen %d ya existe en la orden".formatted(examenDTO.examenId()));
        }

        var examenEntity = convertirExamenDTOAEntity(examenDTO, orden, medicoId);
        ordenDetRepository.save(examenEntity);

        log.info("✅ Examen agregado a orden: {}", ordenId);

        return ordenCabRepository.findById(ordenId)
                .map(toCompletaDTO)
                .orElseThrow(() -> new OrdenBusinessException("ORDEN_106",
                        "Error al recuperar orden actualizada"));
    }

    @Override
    public OrdenExamenDTO actualizarExamenEnOrden(Long ordenId, Long examenDetalleId,
                                                  ActualizarOrdenDTO.ModificarExamenDTO examenDTO, Long medicoId) {
        log.info("✏️ Actualizando examen {} en orden: {}", examenDetalleId, ordenId);

        return ordenDetRepository.findById(examenDetalleId)
                .filter(examen -> examen.getOrdenCab().getId().equals(ordenId))
                .filter(examen -> "S".equals(examen.getActivo()))
                .map(examen -> aplicarCambiosAExamen(examen, examenDTO, medicoId))
                .map(ordenDetRepository::save)
                .map(this::convertirExamenEntityADTO)
                .orElseThrow(ExamenNotFoundException.enOrden(ordenId, examenDetalleId));
    }

    @Override
    @Transactional
    public void eliminarExamenDeOrden(Long ordenId, Long examenDetalleId, Long medicoId) {
        log.info("🗑️ Eliminando examen {} de orden: {}", examenDetalleId, ordenId);

        // Usar query con parámetros Long correctos
        int filasAfectadas = ordenDetRepository.eliminarExamenLogico(ordenId, examenDetalleId, medicoId);

        if (filasAfectadas == 0) {
            throw ExamenNotFoundException.enOrden(ordenId, examenDetalleId).get();
        }

        log.info("✅ Examen eliminado de orden");
    }

    // =====================================================
    // 📊 ESTADÍSTICAS CON RECORDS (JAVA 17)
    // =====================================================

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorEstado() {
        log.debug("📊 Generando estadísticas por estado");

        return ordenCabRepository.findEstadisticasPorEstado()
                .stream()
                .map(this::convertirObjectArrayAEstadisticaEstado)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorTipo() {
        log.debug("📋 Generando estadísticas por tipo");

        return ordenCabRepository.findEstadisticasPorTipo()
                .stream()
                .map(this::convertirObjectArrayAEstadisticaTipo)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorPrioridad() {
        log.debug("⚡ Generando estadísticas por prioridad");

        return ordenCabRepository.findEstadisticasPorPrioridad()
                .stream()
                .map(this::convertirObjectArrayAEstadisticaPrioridad)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<ExamenEstadisticaDTO> obtenerExamenesMasSolicitados() {
        log.debug("🏆 Obteniendo exámenes más solicitados");

        return ordenDetRepository.findExamenesMasSolicitados()
                .stream()
                .limit(10)
                .map(this::convertirObjectArrayAExamenEstadistica)
                .toList(); // Java 17 ✅
    }

    @Override
    public List<EstadisticaDTO> obtenerEstadisticasPorCategoria() {
        log.debug("🏷️ Generando estadísticas por categoría");

        return ordenDetRepository.findEstadisticasPorCategoria()
                .stream()
                .map(this::convertirObjectArrayAEstadisticaCategoria)
                .toList(); // Java 17 ✅
    }

    // =====================================================
    // ✅ VALIDACIONES Y UTILIDADES
    // =====================================================

    @Override
    public boolean existeOrden(Long ordenId) {
        return ordenCabRepository.findById(ordenId)
                .map(esActiva::test)
                .orElse(false);
    }

    @Override
    public boolean existeOrdenPorNumero(String numeroOrden) {
        return ordenCabRepository.findByNumeroOrden(numeroOrden)
                .map(esActiva::test)
                .orElse(false);
    }

    @Override
    public boolean puedeCrearOrden(String tipoOrigen, Long origenId, String tipoOrden) {
        return !ordenCabRepository.existeOrdenMismoTipoHoy(tipoOrigen, origenId, tipoOrden);
    }

    @Override
    public String generarNumeroOrden() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String secuencia = "%03d".formatted(new Random().nextInt(1000));
        return "ORD-%s-%s".formatted(fecha, secuencia); // Java 17 formatted strings ✅
    }

    // =====================================================
    // 🛠️ MÉTODOS HELPER Y CONVERSORES (JAVA 17)
    // =====================================================

    /**
     * Validación de negocio: ¿Puede modificarse la orden?
     * Corregido para Java 17 - sin pattern matching no soportado
     */
    private boolean validarSiPuedeModificarse(OrdenCab orden) {
        // Verificar si está firmada
        if ("S".equals(orden.getFirmada())) {
            return false; // Ya firmada, no se puede modificar
        }

        // Verificar estado
        String estado = orden.getEstado();
        if (estado == null) {
            return true; // Estado null permite modificación
        }

        return switch (estado) {
            case "04", "05" -> false; // Completada o Cancelada
            default -> true; // Otros estados permiten modificación
        };
    }

    /**
     * Validación de transiciones con switch expression
     */
    private boolean validarTransicionEstado(String estadoActual, String nuevoEstado) {
        if (estadoActual == null) {
            return List.of("01", "02").contains(nuevoEstado);
        }

        var transicionesValidas = switch (estadoActual) {
            case "01" -> List.of("02", "05"); // Solicitada → Programada/Cancelada
            case "02" -> List.of("03", "05"); // Programada → En Proceso/Cancelada
            case "03" -> List.of("04");       // En Proceso → Completada
            case "04", "05" -> List.<String>of(); // Estados terminales
            default -> List.<String>of();
        };

        return transicionesValidas.contains(nuevoEstado);
    }

    /**
     * Convertir Entity a DTO completo usando record OrdenResumenDTO
     */
    private OrdenResumenDTO convertirAOrdenResumen(OrdenCab orden) {
        // Obtener cantidad de exámenes activos
        int cantidadExamenes = ordenDetRepository.countExamenesActivos(orden.getId(), "S");

        // Convertir LocalDateTime a LocalDate para fechaOrden
        LocalDate fechaOrden = orden.getFechaOrden() != null ?
                orden.getFechaOrden().toLocalDate() : null;

        // Usar el record con todos los campos
        return new OrdenResumenDTO(
                orden.getId(),
                orden.getNumeroOrden(),
                orden.getPacienteId(),
                "Paciente ID: " + orden.getPacienteId(), // Placeholder para nombre
                orden.getMedicoId(),
                "Médico ID: " + orden.getMedicoId(), // Placeholder para nombre
                orden.getTipoOrigen(),
                obtenerDescripcionOrigen(orden.getTipoOrigen()),
                orden.getOrigenId(),
                orden.getTipoOrden(),
                fechaOrden,
                orden.getFechaProgramada(),
                orden.getDiagnosticoPrincipal(),
                orden.getPrioridad(),
                obtenerDescripcionPrioridad(orden.getPrioridad()),
                orden.getEstado(),
                obtenerDescripcionEstado(orden.getEstado()),
                orden.getFirmada(),
                cantidadExamenes,
                orden.getCreadoEn()
        );
    }

    private OrdenCompletaDTO convertirAOrdenCompleta(OrdenCab orden) {
        var examenes = obtenerExamenesDeOrden(orden.getId());

        return new OrdenCompletaDTO(
                orden.getId(),
                orden.getNumeroOrden(),
                orden.getPacienteId(),
                orden.getMedicoId(),
                orden.getTipoOrigen(),
                obtenerDescripcionOrigen(orden.getTipoOrigen()),
                orden.getOrigenId(),
                orden.getTipoOrden(),
                orden.getFechaOrden(),
                orden.getFechaProgramada(),
                orden.getDiagnosticoPrincipal(),
                orden.getJustificacionClinica(),
                orden.getPrioridad(),
                obtenerDescripcionPrioridad(orden.getPrioridad()),
                orden.getEstado(),
                obtenerDescripcionEstado(orden.getEstado()),
                orden.getFirmada(),
                orden.getFechaFirma(),
                orden.getFirmaDigital() != null ? orden.getFirmaDigital().toString() : null,
                examenes,
                orden.getCreadoEn(),
                orden.getCreadoPor(),
                examenes.size()
        );
    }

    private OrdenCab convertirAEntity(OrdenCabDTO dto) {
        return OrdenCab.builder()
                .pacienteId(dto.pacienteId())
                .medicoId(dto.medicoId())
                .tipoOrigen(dto.tipoOrigen())
                .origenId(dto.origenId())
                .tipoOrden(dto.tipoOrden())
                .fechaProgramada(dto.fechaProgramada())
                .diagnosticoPrincipal(dto.diagnosticoPrincipal())
                .justificacionClinica(dto.justificacionClinica())
                .prioridad(Optional.ofNullable(dto.prioridad()).orElse("N"))
                .numeroOrden(generarNumeroOrden())
                .creadoPor(dto.medicoId())
                .build();
    }

    private OrdenExamenDTO convertirObjectArrayAOrdenExamen(Object[] row) {
        var ordenDet = (OrdenDet) row[0];
        var examen = (Examen) row[1];

        return OrdenExamenDTO.completo(
                ordenDet.getId(),
                ordenDet.getExamenId(),
                ordenDet.getCantidad(),
                ordenDet.getDesIndicacion(),
                ordenDet.getDesConsideraciones(),
                ordenDet.getEstadoDetalle(),
                examen.getCodigo(),
                examen.getNombre(),
                examen.getCategoria(),
                examen.getRequiereAyuno()
        );
    }

    private OrdenDet convertirExamenDTOAEntity(OrdenDetDTO dto, OrdenCab orden, Long medicoId) {
        Integer nextItem = ordenDetRepository.getNextOrdenItem(orden.getId());

        return OrdenDet.builder()
                .ordenCab(orden)
                .examenId(dto.examenId())
                .cantidad(dto.getCantidadSegura())
                .desIndicacion(dto.desIndicacion())
                .desConsideraciones(dto.desConsideraciones())
                .ordenItem(nextItem)
                .estadoDetalle("01")
                .activo("S")
                .creadoPor(medicoId)
                .build();
    }

    // =====================================================
    // 🔥 JSON ATÓMICO CON TEXT BLOCKS (JAVA 17)
    // =====================================================

    private String prepararJsonParaCreacion(OrdenCabDTO ordenDTO) {
        try {
            var examenesJson = objectMapper.writeValueAsString(
                    ordenDTO.examenes().stream()
                            .map(examen -> Map.of(
                                    "examen_id", examen.examenId(),
                                    "cantidad", examen.getCantidadSegura(),
                                    "des_indicacion", Optional.ofNullable(examen.desIndicacion()).orElse(""),
                                    "des_consideraciones", Optional.ofNullable(examen.desConsideraciones()).orElse("")
                            ))
                            .toList()
            );

            // Text block con formateo (Java 17) ✅
            return """
                {
                    "paciente_id": %d,
                    "medico_id": %d,
                    "tipo_origen": "%s",
                    "origen_id": %d,
                    "tipo_orden": "%s",
                    "prioridad": "%s",
                    "diagnostico_principal": "%s",
                    "justificacion_clinica": "%s",
                    "fecha_programada": "%s",
                    "examenes": %s
                }
                """.formatted(
                    ordenDTO.pacienteId(),
                    ordenDTO.medicoId(),
                    ordenDTO.tipoOrigen(),
                    ordenDTO.origenId(),
                    ordenDTO.tipoOrden(),
                    Optional.ofNullable(ordenDTO.prioridad()).orElse("N"),
                    Optional.ofNullable(ordenDTO.diagnosticoPrincipal()).orElse(""),
                    ordenDTO.justificacionClinica(),
                    ordenDTO.fechaProgramada() != null ?
                            ordenDTO.fechaProgramada().toString() :
                            LocalDateTime.now().toLocalDate().toString(),
                    examenesJson
            );
        } catch (Exception e) {
            throw new OrdenBusinessException("ORDEN_106",
                    "Error preparando datos para crear orden: " + e.getMessage());
        }
    }

    private String prepararJsonParaActualizacion(Long ordenId, ActualizarOrdenDTO dto, Long medicoId) {
        try {
            var jsonData = new HashMap<String, Object>();
            jsonData.put("orden_id", ordenId);
            jsonData.put("medico_id", medicoId);

            // Solo agregar campos que no sean null
            Optional.ofNullable(dto.diagnosticoPrincipal()).ifPresent(v -> jsonData.put("diagnostico_principal", v));
            Optional.ofNullable(dto.justificacionClinica()).ifPresent(v -> jsonData.put("justificacion_clinica", v));
            Optional.ofNullable(dto.prioridad()).ifPresent(v -> jsonData.put("prioridad", v));
            Optional.ofNullable(dto.fechaProgramada()).ifPresent(v -> jsonData.put("fecha_programada", v.toString()));

            return objectMapper.writeValueAsString(jsonData);
        } catch (Exception e) {
            throw new OrdenBusinessException("ORDEN_107",
                    "Error preparando datos para actualizar orden: " + e.getMessage());
        }
    }

    private String prepararJsonParaCambioEstado(Long ordenId, String nuevoEstado, Long medicoId, String observacion) {
        try {
            // Switch expression para descripción (Java 17) ✅
            var descripcionEstado = switch (nuevoEstado) {
                case "01" -> "Solicitada";
                case "02" -> "Programada";
                case "03" -> "En Proceso";
                case "04" -> "Completada";
                case "05" -> "Cancelada";
                default -> throw new OrdenBusinessException("ORDEN_108", "Estado inválido: " + nuevoEstado);
            };

            // Text block con observación opcional (Java 17) ✅
            var jsonTemplate = observacion != null && !observacion.isBlank() ?
                    """
                    {
                        "orden_id": %d,
                        "estado": "%s",
                        "estado_descripcion": "%s",
                        "medico_id": %d,
                        "observacion": "%s"
                    }
                    """ :
                    """
                    {
                        "orden_id": %d,
                        "estado": "%s",
                        "estado_descripcion": "%s",
                        "medico_id": %d
                    }
                    """;

            return observacion != null && !observacion.isBlank() ?
                    jsonTemplate.formatted(ordenId, nuevoEstado, descripcionEstado, medicoId, observacion) :
                    jsonTemplate.formatted(ordenId, nuevoEstado, descripcionEstado, medicoId);

        } catch (Exception e) {
            throw new OrdenBusinessException("ORDEN_109",
                    "Error preparando datos para cambio de estado: " + e.getMessage());
        }
    }

    private Long extraerIdDeRespuestaJson(String respuestaJson) {
        try {
            var jsonNode = objectMapper.readTree(respuestaJson);
            return jsonNode.path("orden_id").asLong();
        } catch (Exception e) {
            throw new OrdenBusinessException("ORDEN_110",
                    "Error extrayendo ID de respuesta PostgreSQL: " + e.getMessage());
        }
    }

    // =====================================================
    // 🎯 MÉTODOS HELPER ADICIONALES CON SWITCH EXPRESSIONS
    // =====================================================

    /**
     * Aplicar cambios a examen con pattern matching
     */
    private OrdenDet aplicarCambiosAExamen(OrdenDet examen, ActualizarOrdenDTO.ModificarExamenDTO examenDTO, Long medicoId) {
        Optional.ofNullable(examenDTO.cantidad()).ifPresent(examen::setCantidad);
        Optional.ofNullable(examenDTO.desIndicacion()).ifPresent(examen::setDesIndicacion);
        Optional.ofNullable(examenDTO.desConsideraciones()).ifPresent(examen::setDesConsideraciones);

        examen.setActualizadoPor(medicoId);
        examen.setActualizadoEn(LocalDateTime.now());
        return examen;
    }

    /**
     * Convertir examen entity a DTO con validaciones
     */
    private OrdenExamenDTO convertirExamenEntityADTO(OrdenDet examen) {
        return OrdenExamenDTO.crear(
                examen.getId(),
                examen.getExamenId(),
                "Examen ID: " + examen.getExamenId(),
                examen.getCantidad(),
                examen.getEstadoDetalle()
        );
    }

    /**
     * Convertir Object[] a EstadisticaDTO para Estados con switch expression
     */
    private EstadisticaDTO convertirObjectArrayAEstadisticaEstado(Object[] row) {
        String categoria = (String) row[0];
        Long total = ((Number) row[1]).longValue();

        String descripcion = switch (categoria) {
            case "01" -> "Solicitada";
            case "02" -> "Programada";
            case "03" -> "En Proceso";
            case "04" -> "Completada";
            case "05" -> "Cancelada";
            default -> "Desconocido";
        };

        return EstadisticaImpl.crear(categoria, total, descripcion);
    }

    /**
     * Convertir Object[] a EstadisticaDTO para Tipos
     */
    private EstadisticaDTO convertirObjectArrayAEstadisticaTipo(Object[] row) {
        String categoria = (String) row[0];
        Long total = ((Number) row[1]).longValue();

        String descripcion = switch (categoria) {
            case "LAB" -> "Laboratorio";
            case "IMG" -> "Imagenología";
            case "PROC" -> "Procedimientos";
            case "FUNC" -> "Pruebas Funcionales";
            default -> categoria;
        };

        return EstadisticaImpl.crear(categoria, total, descripcion);
    }

    /**
     * Convertir Object[] a EstadisticaDTO para Prioridades
     */
    private EstadisticaDTO convertirObjectArrayAEstadisticaPrioridad(Object[] row) {
        String categoria = (String) row[0];
        Long total = ((Number) row[1]).longValue();

        String descripcion = switch (categoria) {
            case "E" -> "Emergencia";
            case "U" -> "Urgente";
            case "N" -> "Normal";
            default -> categoria;
        };

        return EstadisticaImpl.crear(categoria, total, descripcion);
    }

    /**
     * Convertir Object[] a EstadisticaDTO para Categorías
     */
    private EstadisticaDTO convertirObjectArrayAEstadisticaCategoria(Object[] row) {
        String categoria = (String) row[0];
        Long total = ((Number) row[1]).longValue();

        String descripcion = switch (categoria) {
            case "Hematología" -> "Hematología";
            case "Bioquímica" -> "Bioquímica";
            case "Microbiología" -> "Microbiología";
            case "Inmunología" -> "Inmunología";
            case "Radiología" -> "Radiología";
            case "Cardiología" -> "Cardiología";
            default -> "Categoría: " + categoria;
        };

        return EstadisticaImpl.crear(categoria, total, descripcion);
    }

    /**
     * Convertir Object[] a ExamenEstadisticaDTO
     */
    private ExamenEstadisticaDTO convertirObjectArrayAExamenEstadistica(Object[] row) {
        String codigoExamen = (String) row[0];
        String nombreExamen = (String) row[1];
        Long totalSolicitado = ((Number) row[2]).longValue();
        String categoria = row.length > 3 ? (String) row[3] : "";

        return ExamenEstadisticaImpl.crear(codigoExamen, nombreExamen, totalSolicitado, categoria);
    }

    // =====================================================
    // 🏗️ RECORDS IMPLEMENTANDO INTERFACES (JAVA 17)
    // =====================================================

    /**
     * Record que implementa EstadisticaDTO (Java 17) ✅
     */
    public record EstadisticaImpl(String categoria, Long total, String descripcion)
            implements EstadisticaDTO {

        @Override
        public String getCategoria() {
            return categoria;
        }

        @Override
        public Long getTotal() {
            return total;
        }

        @Override
        public String getDescripcion() {
            return descripcion;
        }

        public static EstadisticaImpl crear(String categoria, Long total, String descripcion) {
            return new EstadisticaImpl(categoria, total, descripcion);
        }
    }

    /**
     * Record que implementa ExamenEstadisticaDTO (Java 17) ✅
     */
    public record ExamenEstadisticaImpl(String codigoExamen, String nombreExamen,
                                        Long totalSolicitado, String categoria)
            implements ExamenEstadisticaDTO {

        @Override
        public String getCodigoExamen() {
            return codigoExamen;
        }

        @Override
        public String getNombreExamen() {
            return nombreExamen;
        }

        @Override
        public Long getTotalSolicitado() {
            return totalSolicitado;
        }

        @Override
        public String getCategoria() {
            return categoria;
        }

        public static ExamenEstadisticaImpl crear(String codigo, String nombre, Long total, String cat) {
            return new ExamenEstadisticaImpl(codigo, nombre, total, cat);
        }
    }

    /**
     * Comparador de prioridades con switch expression
     */
    private int compararPrioridades(String p1, String p2) {
        var peso1 = switch (p1 != null ? p1 : "N") {
            case "E" -> 3; // Emergencia
            case "U" -> 2; // Urgente
            case "N" -> 1; // Normal
            default -> 0;
        };

        var peso2 = switch (p2 != null ? p2 : "N") {
            case "E" -> 3;
            case "U" -> 2;
            case "N" -> 1;
            default -> 0;
        };

        return Integer.compare(peso2, peso1); // Orden descendente
    }

    /**
     * Obtener descripción de origen con switch expression
     */
    private String obtenerDescripcionOrigen(String tipoOrigen) {
        return switch (tipoOrigen != null ? tipoOrigen : "") {
            case "HOS" -> "Hospitalización";
            case "AMB" -> "Ambulatorio";
            case "EMR" -> "Emergencia";
            case "CON" -> "Consultorio";
            default -> "Desconocido";
        };
    }

    /**
     * Obtener descripción de prioridad con switch expression
     */
    private String obtenerDescripcionPrioridad(String prioridad) {
        return switch (prioridad != null ? prioridad : "N") {
            case "E" -> "Emergencia";
            case "U" -> "Urgente";
            case "N" -> "Normal";
            default -> "Normal";
        };
    }

    /**
     * Obtener descripción de estado con switch expression
     */
    private String obtenerDescripcionEstado(String estado) {
        return switch (estado != null ? estado : "01") {
            case "01" -> "Solicitada";
            case "02" -> "Programada";
            case "03" -> "En Proceso";
            case "04" -> "Completada";
            case "05" -> "Cancelada";
            default -> "Desconocido";
        };
    }
}