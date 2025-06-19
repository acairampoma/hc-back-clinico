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
 * 🚀 IMPLEMENTACIÓN FUNCIONAL del servicio de recetas médicas
 * ✅ PROCESAMIENTO CORRECTO DE FIRMA DIGITAL DESDE FRONTEND
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

    // ===== 📝 OPERACIÓN PRINCIPAL - CREAR RECETA CON FIRMA =====

    @Override
    @Transactional
    public RecetaCompletaDTO crearReceta(RecetaCabDTO recetaDTO) {
        log.info("📝 Creando receta para {} ID {} - Firmada: {}",
                recetaDTO.getTipoOrigen(), recetaDTO.getOrigenId(), recetaDTO.getFirmada());

        // Validaciones básicas
        validationHelper.validarCreacionReceta(recetaDTO);

        // Convertir DTO a Entity
        RecetaCab recetaEntity = convertirDTOaEntity(recetaDTO);

        // Establecer datos automáticos
        establecerDatosAutomaticos(recetaEntity);

        // Procesar firma digital
        procesarFirmaDigital(recetaEntity, recetaDTO);

        // Guardar cabecera
        RecetaCab recetaGuardada = recetaCabRepository.save(recetaEntity);
        verificarFirmaDespuesDelSave(recetaGuardada);


        log.info("✅ Receta guardada ID: {} - Firmada: {} - Firma Digital: {}",
                recetaGuardada.getId(),
                recetaGuardada.getFirmada(),
                recetaGuardada.getFirmaDigital() != null ? "SÍ" : "NO");

        // Guardar medicamentos
        if (recetaDTO.getMedicamentos() != null && !recetaDTO.getMedicamentos().isEmpty()) {
            guardarMedicamentosDetalle(recetaGuardada.getId(), recetaDTO.getMedicamentos());
        }

        return toCompletaDTO.apply(recetaGuardada);
    }

    @Override
    @Transactional
    public RecetaCompletaDTO actualizarReceta(Long recetaId, RecetaCabDTO recetaDTO, Long medicoId) {
        log.info("🔄 Actualizando receta {} por médico {}", recetaId, medicoId);

        validationHelper.validarActualizacionReceta(recetaId, medicoId);

        return recetaCabRepository.findById(recetaId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .map(receta -> actualizarCamposReceta(receta, recetaDTO, medicoId))
                .map(recetaCabRepository::save)
                .map(toCompletaDTO)
                .orElseThrow(() -> RecetaNotFoundException.receta(recetaId));
    }

    @Override
    @Transactional
    public RecetaCompletaDTO cambiarEstadoReceta(Long recetaId, String nuevoEstado, Long medicoId) {
        log.info("📊 Cambiando estado de receta {} a {} por médico {}", recetaId, nuevoEstado, medicoId);

        return recetaCabRepository.findById(recetaId)
                .filter(receta -> Objects.equals(receta.getMedicoId(), medicoId))
                .map(receta -> aplicarCambioEstado(receta, nuevoEstado, medicoId))
                .map(recetaCabRepository::save)
                .map(toCompletaDTO)
                .orElseThrow(() -> RecetaBusinessException.permisosDenegados(medicoId, recetaId));
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

    // ===== 🔧 MÉTODOS PRIVADOS =====

    private RecetaCab convertirDTOaEntity(RecetaCabDTO dto) {
        return RecetaCab.builder()
                .id(dto.getId())
                .numeroReceta(dto.getNumeroReceta())
                .pacienteId(dto.getPacienteId())
                .medicoId(dto.getMedicoId())
                .tipoOrigen(dto.getTipoOrigen())
                .origenId(dto.getOrigenId())
                .fechaReceta(dto.getFechaReceta())
                .fechaVencimiento(dto.getFechaVencimiento())
                .diagnosticoPrincipal(dto.getDiagnosticoPrincipal())
                .indicacionesGenerales(dto.getIndicacionesGenerales())
                .estado(dto.getEstado())
                .activo(dto.getActivo())
                .creadoPor(dto.getCreadoPor())
                .creadoEn(dto.getCreadoEn())
                // 🎯 AGREGAR ESTAS 3 LÍNEAS QUE FALTAN:
                .firmada(dto.getFirmada())           // ← NUEVA
                .fechaFirma(dto.getFechaFirma())     // ← NUEVA
                .firmaDigital(dto.getFirmaDigital()) // ← NUEVA
                .build();
    }

    private void establecerDatosAutomaticos(RecetaCab receta) {
        LocalDateTime ahora = LocalDateTime.now();

        if (receta.getNumeroReceta() == null) {
            receta.setNumeroReceta(generarNumeroReceta());
        }
        if (receta.getFechaReceta() == null) {
            receta.setFechaReceta(ahora);
        }
        if (receta.getEstado() == null) {
            receta.setEstado("01");
        }
        if (receta.getActivo() == null) {
            receta.setActivo("S");
        }
        if (receta.getCreadoEn() == null) {
            receta.setCreadoEn(ahora);
        }
        if (receta.getFechaVencimiento() == null) {
            receta.setFechaVencimiento(LocalDate.now().plusDays(30));
        }
    }

    private void procesarFirmaDigital(RecetaCab receta, RecetaCabDTO dto) {
        log.info("🔐 PROCESANDO FIRMA DIGITAL");

        try {
            JsonNode firmaOriginal = dto.getFirmaDigital();
            log.info("Firma en DTO: {}", firmaOriginal != null ? "EXISTE" : "NULL");

            if (firmaOriginal == null) {
                log.info("No hay firma - estableciendo valores por defecto");
                receta.setFirmada("N");
                receta.setFechaFirma(null);
                receta.setFirmaDigital(null);
                return;
            }

            // Verificar estructura
            log.info("Tipo de nodo: {}", firmaOriginal.getNodeType());
            log.info("Tiene imagen_base64: {}", firmaOriginal.has("imagen_base64"));

            if (!firmaOriginal.has("imagen_base64")) {
                log.warn("Firma sin imagen_base64");
                receta.setFirmada("N");
                receta.setFechaFirma(null);
                receta.setFirmaDigital(null);
                return;
            }

            String imagenBase64 = firmaOriginal.get("imagen_base64").asText();
            log.info("Tamaño imagen: {} caracteres", imagenBase64.length());
            log.info("Primeros 50 chars: {}", imagenBase64.substring(0, Math.min(50, imagenBase64.length())));

            if (imagenBase64.length() < 100) {
                log.warn("Imagen muy pequeña: {} chars", imagenBase64.length());
                receta.setFirmada("N");
                receta.setFechaFirma(null);
                receta.setFirmaDigital(null);
                return;
            }

            // AQUÍ ESTÁ EL PUNTO CRÍTICO - ASIGNAR LA FIRMA
            log.info("Asignando firma digital a la entidad...");
            receta.setFirmaDigital(firmaOriginal);
            receta.setFirmada("S");
            receta.setFechaFirma(dto.getFechaFirma() != null ? dto.getFechaFirma() : LocalDateTime.now());

            log.info("Firma asignada - Estado final:");
            log.info("  - firmada: {}", receta.getFirmada());
            log.info("  - fecha_firma: {}", receta.getFechaFirma());
            log.info("  - firma_digital es null: {}", receta.getFirmaDigital() == null);

            // VERIFICACIÓN ADICIONAL
            if (receta.getFirmaDigital() != null) {
                log.info("  - firma_digital JSON: {}", receta.getFirmaDigital().toString().substring(0, Math.min(100, receta.getFirmaDigital().toString().length())));
            }

        } catch (Exception e) {
            log.error("ERROR procesando firma digital: {}", e.getMessage(), e);
            receta.setFirmada("N");
            receta.setFechaFirma(null);
            receta.setFirmaDigital(null);
        }
    }

    // MÉTODO ADICIONAL: Verificar después del save
    private void verificarFirmaDespuesDelSave(RecetaCab recetaGuardada) {
        log.info("🔍 VERIFICANDO FIRMA DESPUÉS DEL SAVE:");
        log.info("  - ID: {}", recetaGuardada.getId());
        log.info("  - firmada: {}", recetaGuardada.getFirmada());
        log.info("  - fecha_firma: {}", recetaGuardada.getFechaFirma());
        log.info("  - firma_digital es null: {}", recetaGuardada.getFirmaDigital() == null);

        if (recetaGuardada.getFirmaDigital() != null) {
            log.info("  - firma_digital tiene contenido: SÍ");
            log.info("  - tamaño JSON: {}", recetaGuardada.getFirmaDigital().toString().length());
        } else {
            log.warn("  - firma_digital: NULL - NO SE GUARDÓ");
        }
    }

    private void guardarMedicamentosDetalle(Long recetaId, List<RecetaDetDTO> medicamentos) {
        log.debug("💊 Guardando {} medicamentos para receta {}", medicamentos.size(), recetaId);

        LocalDateTime ahora = LocalDateTime.now();

        for (int i = 0; i < medicamentos.size(); i++) {
            RecetaDetDTO medicamentoDTO = medicamentos.get(i);

            RecetaDet medicamentoEntity = RecetaDet.builder()
                    .recetaId(recetaId)
                    .medicamentoId(medicamentoDTO.getMedicamentoId())
                    .codigoMedicamento(medicamentoDTO.getCodigoMedicamento())
                    .diagnosticoMedicamento(medicamentoDTO.getDiagnosticoMedicamento())
                    .dosis(medicamentoDTO.getDosis())
                    .frecuencia(medicamentoDTO.getFrecuencia())
                    .duracionTratamiento(medicamentoDTO.getDuracionTratamiento())
                    .cantidadTotal(medicamentoDTO.getCantidadTotal())
                    .unidadCantidad(medicamentoDTO.getUnidadCantidad())
                    .viaAdministracion(medicamentoDTO.getViaAdministracion())
                    .instruccionesEspeciales(medicamentoDTO.getInstruccionesEspeciales())
                    .conAlimentos(medicamentoDTO.getConAlimentos())
                    .momentoAdministracion(medicamentoDTO.getMomentoAdministracion())
                    .ordenItem(i + 1)
                    .estado("01")
                    .activo("S")
                    .creadoPor(medicamentoDTO.getCreadoPor())
                    .creadoEn(ahora)
                    .build();

            recetaDetRepository.save(medicamentoEntity);
        }
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

    private RecetaCab actualizarCamposReceta(RecetaCab receta, RecetaCabDTO dto, Long medicoId) {
        LocalDateTime ahora = LocalDateTime.now();

        Optional.ofNullable(dto.getDiagnosticoPrincipal()).ifPresent(receta::setDiagnosticoPrincipal);
        Optional.ofNullable(dto.getIndicacionesGenerales()).ifPresent(receta::setIndicacionesGenerales);
        Optional.ofNullable(dto.getFechaVencimiento()).ifPresent(receta::setFechaVencimiento);

        if (dto.getFirmaDigital() != null) {
            procesarFirmaDigital(receta, dto);
        }

        receta.setActualizadoPor(medicoId);
        receta.setActualizadoEn(ahora);

        return receta;
    }

    private RecetaCab aplicarCambioEstado(RecetaCab receta, String nuevoEstado, Long medicoId) {
        validarCambioEstado(receta.getEstado(), nuevoEstado);

        receta.setEstado(nuevoEstado);
        receta.setActualizadoPor(medicoId);
        receta.setActualizadoEn(LocalDateTime.now());

        return receta;
    }

    private void validarCambioEstado(String estadoActual, String nuevoEstado) {
        Map<String, List<String>> transicionesValidas = Map.of(
                "01", List.of("02", "04"),
                "02", List.of(),
                "04", List.of()
        );

        if (!transicionesValidas.getOrDefault(estadoActual, Collections.emptyList()).contains(nuevoEstado)) {
            throw RecetaValidationException.estadoInvalido(nuevoEstado);
        }
    }

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
        log.debug("🔄 Convirtiendo receta {} con medicamentos y firma", receta.getId());

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