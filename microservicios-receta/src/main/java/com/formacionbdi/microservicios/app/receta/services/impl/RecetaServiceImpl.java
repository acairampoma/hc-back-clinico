package com.formacionbdi.microservicios.app.receta.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formacionbdi.microservicios.app.receta.exception.*;
import com.formacionbdi.microservicios.app.receta.models.dto.*;
import com.formacionbdi.microservicios.app.receta.models.entity.*;
import com.formacionbdi.microservicios.app.receta.repository.*;
import com.formacionbdi.microservicios.app.receta.services.RecetaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 🚀 RECETA SERVICE REFACTORIZADO - TRANSACCIONAL DIRECTO
 * ✅ ELIMINAMOS lógica compleja del service
 * ✅ DELEGAMOS transacciones atómicas al repository
 * ✅ SERVICE queda súper limpio y funcional
 * ✅ REPOSITORY maneja toda la complejidad de guardado
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecetaServiceImpl implements RecetaService {

    private final RecetaCabRepository recetaCabRepository;
    private final RecetaDetRepository recetaDetRepository;
    private final MedicamentoVademecumRepository vademecumRepository;
    private final RecetaValidationHelper validationHelper;
    private final ObjectMapper objectMapper;

    // ===== 🔧 FUNCIONES HELPER FUNCIONALES =====
    private final Function<RecetaCab, RecetaCompletaDTO> toCompletaDTO = this::convertirARecetaCompleta;
    private final Function<MedicamentoVademecum, MedicamentoVademecumDTO> toMedicamentoDTO = this::convertirMedicamentoADTO;

    private final Predicate<RecetaCab> esActiva = receta -> "01".equals(receta.getEstado());
    private final Predicate<RecetaCab> estaFirmada = receta -> "S".equals(receta.getFirmada());
    private final Predicate<RecetaCab> puedeModificarse = RecetaCab::puedeSerModificada;

    // ===== 📖 CONSULTAS PRINCIPALES =====

    @Override
    public List<RecetaCompletaDTO> obtenerRecetasPorOrigen(String tipoOrigen, Long origenId) {
        log.debug("📖 Obteniendo recetas para {} ID {}", tipoOrigen, origenId);
        return recetaCabRepository.findByTipoOrigenAndOrigenId(tipoOrigen, origenId)
                .stream()
                .map(toCompletaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RecetaCompletaDTO> obtenerRecetaPorId(Long recetaId) {
        log.debug("📖 Obteniendo receta ID {}", recetaId);
        return recetaCabRepository.findById(recetaId).map(toCompletaDTO);
    }

    @Override
    public Optional<RecetaCompletaDTO> obtenerRecetaPorNumero(String numeroReceta) {
        log.debug("📖 Obteniendo receta número {}", numeroReceta);
        return recetaCabRepository.findByNumeroReceta(numeroReceta).map(toCompletaDTO);
    }

    @Override
    public List<RecetaCompletaDTO> obtenerRecetasPorPaciente(Long pacienteId) {
        log.debug("📖 Obteniendo recetas del paciente {}", pacienteId);
        return recetaCabRepository.findByPacienteId(pacienteId)
                .stream()
                .map(toCompletaDTO)
                .collect(Collectors.toList());
    }

    // ===== 📝 OPERACIÓN PRINCIPAL - USAR TUS MÉTODOS EXISTENTES =====

    @Override
    @Transactional
    public RecetaCompletaDTO crearReceta(RecetaCabDTO recetaDTO) {
        log.info("📝 Creando receta para {} ID {} - Firmada: {}",
                recetaDTO.getTipoOrigen(), recetaDTO.getOrigenId(), recetaDTO.getFirmada());

        try {
            // 1. VALIDACIONES PREVIAS (rápidas)
            validationHelper.validarCreacionReceta(recetaDTO);

            // 2. PREPARAR JSON PARA TU FUNCIÓN
            String datosRecetaJson = prepararJsonParaCreacion(recetaDTO);
            log.debug("📦 JSON preparado: {}", datosRecetaJson.substring(0, Math.min(200, datosRecetaJson.length())));

            // 3. EJECUTAR TU MÉTODO EXISTENTE crearRecetaAtomica
            String resultadoJson = recetaCabRepository.crearRecetaAtomica(datosRecetaJson);
            log.info("✅ Función PostgreSQL ejecutada - Resultado: {}", resultadoJson.substring(0, Math.min(100, resultadoJson.length())));

            // 4. PROCESAR RESULTADO
            JsonNode resultado = objectMapper.readTree(resultadoJson);

            if (resultado.get("success").asBoolean()) {
                Long recetaId = resultado.get("receta_id").asLong();
                log.info("✅ Receta creada exitosamente ID: {}", recetaId);

                // 5. RETORNAR RECETA COMPLETA
                return recetaCabRepository.findById(recetaId)
                        .map(toCompletaDTO)
                        .orElseThrow(() -> RecetaNotFoundException.receta(recetaId));
            } else {
                String error = resultado.get("error_message").asText();
                log.error("❌ Error en función PostgreSQL: {}", error);
                throw new RecetaBusinessException(error);
            }

        } catch (Exception e) {
            log.error("❌ Error creando receta: {}", e.getMessage(), e);
            throw new RecetaBusinessException("Error creando receta: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RecetaCompletaDTO actualizarReceta(Long recetaId, RecetaCabDTO recetaDTO, Long medicoId) {
        log.info("🔄 Actualizando receta {} por médico {}", recetaId, medicoId);

        try {
            // 1. VALIDACIONES PREVIAS
            validationHelper.validarActualizacionReceta(recetaId, medicoId);

            // 2. PREPARAR JSON PARA TU FUNCIÓN PUT
            String datosActualizacionJson = prepararJsonParaActualizacion(recetaId, recetaDTO, medicoId);

            // 3. EJECUTAR TU MÉTODO EXISTENTE actualizarRecetaAtomica
            String resultadoJson = recetaCabRepository.actualizarRecetaAtomica(datosActualizacionJson);

            // 4. PROCESAR RESULTADO
            JsonNode resultado = objectMapper.readTree(resultadoJson);

            if (resultado.get("success").asBoolean()) {
                log.info("✅ Receta actualizada exitosamente ID: {}", recetaId);
                return recetaCabRepository.findById(recetaId)
                        .map(toCompletaDTO)
                        .orElseThrow(() -> RecetaNotFoundException.receta(recetaId));
            } else {
                String error = resultado.get("error_message").asText();
                throw new RecetaBusinessException(error);
            }

        } catch (Exception e) {
            log.error("❌ Error actualizando receta: {}", e.getMessage(), e);
            throw new RecetaBusinessException("Error actualizando receta: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RecetaCompletaDTO cambiarEstadoReceta(Long recetaId, String nuevoEstado, Long medicoId) {
        log.info("📊 Cambiando estado de receta {} a {} por médico {}", recetaId, nuevoEstado, medicoId);

        try {
            // 1. PREPARAR JSON PARA TU FUNCIÓN PATCH
            String datosEstadoJson = prepararJsonParaCambioEstado(recetaId, nuevoEstado, medicoId);

            // 2. EJECUTAR TU MÉTODO EXISTENTE cambiarEstadoAtomica
            String resultadoJson = recetaCabRepository.cambiarEstadoAtomica(datosEstadoJson);

            // 3. PROCESAR RESULTADO
            JsonNode resultado = objectMapper.readTree(resultadoJson);

            if (resultado.get("success").asBoolean()) {
                log.info("✅ Estado cambiado exitosamente ID: {}", recetaId);
                return recetaCabRepository.findById(recetaId)
                        .map(toCompletaDTO)
                        .orElseThrow(() -> RecetaNotFoundException.receta(recetaId));
            } else {
                String error = resultado.get("error_message").asText();
                throw new RecetaBusinessException(error);
            }

        } catch (Exception e) {
            log.error("❌ Error cambiando estado: {}", e.getMessage(), e);
            throw new RecetaBusinessException("Error cambiando estado: " + e.getMessage());
        }
    }

    // ===== 🔍 BÚSQUEDAS DE VADEMÉCUM =====

    @Override
    public List<MedicamentoVademecumDTO> buscarMedicamentos(String busqueda, String categoria) {
        log.debug("🔍 Buscando medicamentos: '{}', categoría: '{}'", busqueda, categoria);

        List<MedicamentoVademecum> medicamentos = Optional.ofNullable(categoria)
                .filter(cat -> !cat.trim().isEmpty())
                .map(vademecumRepository::findByCategoria)
                .orElseGet(() -> vademecumRepository.buscarMedicamentos(busqueda));

        return medicamentos.stream()
                .filter(med -> busqueda == null ||
                        med.getGenericName().toLowerCase().contains(busqueda.toLowerCase()))
                .map(toMedicamentoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MedicamentoVademecumDTO> obtenerMedicamento(Long medicamentoId) {
        return vademecumRepository.findById(medicamentoId).map(toMedicamentoDTO);
    }

    @Override
    public Optional<MedicamentoVademecumDTO> obtenerMedicamentoPorCodigo(String codigoMedicamento) {
        return vademecumRepository.findByCodigoMedicamento(codigoMedicamento).map(toMedicamentoDTO);
    }

    @Override
    public List<String> obtenerCategoriasMedicamentos() {
        return vademecumRepository.findMedicamentosDisponibles()
                .stream()
                .map(MedicamentoVademecum::getCategoria)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // ===== 📊 ESTADÍSTICAS =====

    @Override
    public Map<String, Object> obtenerEstadisticasMedico(Long medicoId) {
        log.debug("📊 Obteniendo estadísticas del médico {}", medicoId);

        Object[] stats = recetaCabRepository.obtenerEstadisticasMedico(medicoId);

        return Map.of(
                "total_recetas", stats[0],
                "recetas_activas", stats[1],
                "recetas_despachadas", stats[2],
                "recetas_firmadas", stats[3],
                "medico_id", medicoId
        );
    }

    @Override
    public List<Map<String, Object>> obtenerMedicamentosMasPrescritos(int limite) {
        return recetaDetRepository.findMedicamentosMasPrescritos()
                .stream()
                .limit(limite)
                .map(this::convertirEstadisticaMedicamento)
                .collect(Collectors.toList());
    }

    // ===== 🔒 VALIDACIONES =====

    @Override
    public boolean puedeCrearReceta(String tipoOrigen, Long origenId) {
        return !recetaCabRepository.existeRecetaMismoOrigenHoy(tipoOrigen, origenId);
    }

    @Override
    public boolean puedeModificarReceta(Long recetaId, Long medicoId) {
        return recetaCabRepository.findById(recetaId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .filter(receta -> Objects.equals(receta.getMedicoId(), medicoId))
                .isPresent();
    }

    // ===== 🛠️ UTILIDADES =====

    @Override
    public String generarNumeroReceta() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String secuencia = String.format("%03d", new Random().nextInt(1000));
        String numeroReceta = String.format("REC-%s-%s", fecha, secuencia);
        log.debug("🔢 Número de receta generado: {}", numeroReceta);
        return numeroReceta;
    }

    @Override
    public boolean validarDisponibilidadMedicamentos(RecetaCabDTO recetaDTO) {
        return Optional.ofNullable(recetaDTO.getMedicamentos())
                .orElse(Collections.emptyList())
                .stream()
                .allMatch(this::esMedicamentoDisponible);
    }

    @Override
    public RecetaCabDTO aplicarFirmaAutomatica(RecetaCabDTO recetaDTO) {
        log.debug("✅ Aplicando firma automática - preservando firma del frontend");
        return recetaDTO;
    }

    // ===== 🔧 MÉTODOS PRIVADOS PARA JSON =====

    private String prepararJsonParaCreacion(RecetaCabDTO recetaDTO) {
        try {
            Map<String, Object> datos = new HashMap<>();

            // Datos básicos
            datos.put("numero_receta", Optional.ofNullable(recetaDTO.getNumeroReceta()).orElse(generarNumeroReceta()));
            datos.put("paciente_id", recetaDTO.getPacienteId());
            datos.put("medico_id", recetaDTO.getMedicoId());
            datos.put("tipo_origen", recetaDTO.getTipoOrigen());
            datos.put("origen_id", recetaDTO.getOrigenId());
            datos.put("diagnostico_principal", recetaDTO.getDiagnosticoPrincipal());
            datos.put("indicaciones_generales", recetaDTO.getIndicacionesGenerales());

            // Fechas
            datos.put("fecha_receta", Optional.ofNullable(recetaDTO.getFechaReceta()).orElse(LocalDateTime.now()));
            datos.put("fecha_vencimiento", Optional.ofNullable(recetaDTO.getFechaVencimiento()).orElse(LocalDate.now().plusDays(30)));

            // Estado y firma
            datos.put("estado", Optional.ofNullable(recetaDTO.getEstado()).orElse("01"));
            datos.put("firmada", Optional.ofNullable(recetaDTO.getFirmada()).orElse("N"));
            datos.put("fecha_firma", recetaDTO.getFechaFirma());
            datos.put("firma_digital", recetaDTO.getFirmaDigital());

            // Auditoría
            datos.put("creado_por", recetaDTO.getCreadoPor());
            datos.put("creado_en", Optional.ofNullable(recetaDTO.getCreadoEn()).orElse(LocalDateTime.now()));
            datos.put("activo", "S");

            // Medicamentos
            if (recetaDTO.getMedicamentos() != null && !recetaDTO.getMedicamentos().isEmpty()) {
                List<Map<String, Object>> medicamentos = recetaDTO.getMedicamentos().stream()
                        .map(this::convertirMedicamentoAMap)
                        .collect(Collectors.toList());
                datos.put("medicamentos", medicamentos);
            }

            return objectMapper.writeValueAsString(datos);

        } catch (Exception e) {
            log.error("❌ Error preparando JSON para creación: {}", e.getMessage(), e);
            throw new RecetaBusinessException("Error preparando datos para creación");
        }
    }

    private String prepararJsonParaActualizacion(Long recetaId, RecetaCabDTO recetaDTO, Long medicoId) {
        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("receta_id", recetaId);
            datos.put("medico_id", medicoId);
            datos.put("diagnostico_principal", recetaDTO.getDiagnosticoPrincipal());
            datos.put("indicaciones_generales", recetaDTO.getIndicacionesGenerales());
            datos.put("fecha_vencimiento", recetaDTO.getFechaVencimiento());
            datos.put("firma_digital", recetaDTO.getFirmaDigital());
            datos.put("actualizado_por", medicoId);
            datos.put("actualizado_en", LocalDateTime.now());

            return objectMapper.writeValueAsString(datos);

        } catch (Exception e) {
            log.error("❌ Error preparando JSON para actualización: {}", e.getMessage(), e);
            throw new RecetaBusinessException("Error preparando datos para actualización");
        }
    }

    private String prepararJsonParaCambioEstado(Long recetaId, String nuevoEstado, Long medicoId) {
        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("receta_id", recetaId);
            datos.put("nuevo_estado", nuevoEstado);
            datos.put("medico_id", medicoId);
            datos.put("actualizado_por", medicoId);
            datos.put("actualizado_en", LocalDateTime.now());

            return objectMapper.writeValueAsString(datos);

        } catch (Exception e) {
            log.error("❌ Error preparando JSON para cambio de estado: {}", e.getMessage(), e);
            throw new RecetaBusinessException("Error preparando datos para cambio de estado");
        }
    }

    private Map<String, Object> convertirMedicamentoAMap(RecetaDetDTO medicamento) {
        Map<String, Object> map = new HashMap<>();
        map.put("medicamento_id", medicamento.getMedicamentoId());
        map.put("codigo_medicamento", medicamento.getCodigoMedicamento());
        map.put("diagnostico_medicamento", medicamento.getDiagnosticoMedicamento());
        map.put("dosis", medicamento.getDosis());
        map.put("frecuencia", medicamento.getFrecuencia());
        map.put("duracion_tratamiento", medicamento.getDuracionTratamiento());
        map.put("cantidad_total", medicamento.getCantidadTotal());
        map.put("unidad_cantidad", medicamento.getUnidadCantidad());
        map.put("via_administracion", medicamento.getViaAdministracion());
        map.put("instrucciones_especiales", medicamento.getInstruccionesEspeciales());
        map.put("con_alimentos", medicamento.getConAlimentos());
        map.put("momento_administracion", medicamento.getMomentoAdministracion());
        map.put("orden_item", medicamento.getOrdenItem());
        map.put("creado_por", medicamento.getCreadoPor());
        return map;
    }

    // ===== 🔧 MÉTODOS DE CONVERSIÓN EXISTENTES =====

    private boolean esMedicamentoDisponible(RecetaDetDTO medicamento) {
        return vademecumRepository.findByCodigoMedicamento(medicamento.getCodigoMedicamento())
                .map(med -> "S".equals(med.getDisponible()) && "S".equals(med.getActivo()))
                .orElse(false);
    }

    private Map<String, Object> convertirEstadisticaMedicamento(Object[] stat) {
        return Map.of(
                "nombre_medicamento", stat[0],
                "concentracion", stat[1],
                "total_prescripciones", stat[2]
        );
    }

    private RecetaCompletaDTO convertirARecetaCompleta(RecetaCab receta) {
        log.debug("🔄 Convirtiendo receta {} con medicamentos", receta.getId());

        List<RecetaDetDTO> medicamentos = cargarMedicamentosComoDTO(receta.getId());

        return RecetaCompletaDTO.builder()
                .recetaInfo(RecetaCompletaDTO.RecetaInfoDTO.builder()
                        .id(receta.getId())
                        .numeroReceta(receta.getNumeroReceta())
                        .fechaReceta(receta.getFechaReceta())
                        .fechaVencimiento(receta.getFechaVencimiento())
                        .estado(receta.getEstado())
                        .estadoDescripcion(obtenerDescripcionEstado(receta.getEstado()))
                        .diagnosticoPrincipal(receta.getDiagnosticoPrincipal())
                        .indicacionesGenerales(receta.getIndicacionesGenerales())
                        .firmada(receta.getFirmada())
                        .fechaFirma(receta.getFechaFirma())
                        .tipoOrigen(receta.getTipoOrigen())
                        .origenId(receta.getOrigenId())
                        .build())
                .firmaDigital(receta.getFirmaDigital())
                .medicamentos(medicamentos)
                .build();
    }

    private List<RecetaDetDTO> cargarMedicamentosComoDTO(Long recetaId) {
        log.debug("💊 Cargando medicamentos como DTO para receta {}", recetaId);

        List<RecetaDet> medicamentos = recetaDetRepository.findByRecetaIdOrderByOrden(recetaId);

        return medicamentos.stream()
                .map(this::convertirMedicamentoARecetaDetDTO)
                .collect(Collectors.toList());
    }

    private RecetaDetDTO convertirMedicamentoARecetaDetDTO(RecetaDet detalle) {
        Optional<MedicamentoVademecum> vademecumOpt = vademecumRepository.findById(detalle.getMedicamentoId());

        RecetaDetDTO.RecetaDetDTOBuilder builder = RecetaDetDTO.builder()
                .id(detalle.getId())
                .recetaId(detalle.getRecetaId())
                .medicamentoId(detalle.getMedicamentoId())
                .codigoMedicamento(detalle.getCodigoMedicamento())
                .diagnosticoMedicamento(detalle.getDiagnosticoMedicamento())
                .dosis(detalle.getDosis())
                .frecuencia(detalle.getFrecuencia())
                .duracionTratamiento(detalle.getDuracionTratamiento())
                .cantidadTotal(detalle.getCantidadTotal())
                .unidadCantidad(detalle.getUnidadCantidad())
                .viaAdministracion(detalle.getViaAdministracion())
                .instruccionesEspeciales(detalle.getInstruccionesEspeciales())
                .conAlimentos(detalle.getConAlimentos())
                .momentoAdministracion(detalle.getMomentoAdministracion())
                .ordenItem(detalle.getOrdenItem())
                .estado(detalle.getEstado())
                .creadoPor(detalle.getCreadoPor())
                .creadoEn(detalle.getCreadoEn());

        if (vademecumOpt.isPresent()) {
            MedicamentoVademecum vademecum = vademecumOpt.get();
            builder
                    .nombreMedicamento(vademecum.getGenericName())
                    .concentracion(vademecum.getConcentracion())
                    .formaFarmaceutica(vademecum.getFormaFarmaceutica())
                    .categoria(vademecum.getCategoria())
                    .brandNames(vademecum.getBrandNames() != null ?
                            vademecum.getBrandNames().toString() : "N/A");
        } else {
            builder
                    .nombreMedicamento("Medicamento no encontrado")
                    .concentracion("N/A")
                    .formaFarmaceutica("N/A")
                    .categoria("N/A")
                    .brandNames("N/A");
        }

        return builder.build();
    }

    private MedicamentoVademecumDTO convertirMedicamentoADTO(MedicamentoVademecum medicamento) {
        return MedicamentoVademecumDTO.builder()
                .id(medicamento.getId())
                .codigoMedicamento(medicamento.getCodigoMedicamento())
                .genericName(medicamento.getGenericName())
                .concentracion(medicamento.getConcentracion())
                .formaFarmaceutica(medicamento.getFormaFarmaceutica())
                .categoria(medicamento.getCategoria())
                .viaAdministracion(medicamento.getViaAdministracion())
                .requiereReceta(medicamento.getRequiereReceta())
                .controlado(medicamento.getControlado())
                .disponible(medicamento.getDisponible())
                .activo(medicamento.getActivo())
                .creadoPor(medicamento.getCreadoPor())
                .creadoEn(medicamento.getCreadoEn())
                .build();
    }

    private String obtenerDescripcionEstado(String estado) {
        Map<String, String> descripciones = Map.of(
                "01", "Activa",
                "02", "Despachada",
                "03", "Vencida",
                "04", "Anulada"
        );
        return descripciones.getOrDefault(estado, "Desconocido");
    }
}