package com.formacionbdi.microservicios.app.receta.services.impl;

import com.formacionbdi.microservicios.app.receta.exception.*;
import com.formacionbdi.microservicios.app.receta.models.dto.*;
import com.formacionbdi.microservicios.app.receta.models.entity.*;
import com.formacionbdi.microservicios.app.receta.repository.*;
import com.formacionbdi.microservicios.app.receta.services.RecetaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 🚀 IMPLEMENTACIÓN FUNCIONAL del servicio de recetas médicas
 * Patrón: Programación funcional + Código limpio + Exception Layer robusto
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

    // ===== 🔧 FUNCIONES HELPER FUNCIONALES =====

    private final Function<RecetaCab, RecetaCompletaDTO> toCompletaDTO = this::convertirARecetaCompleta;
    private final Function<RecetaCabDTO, RecetaCab> toEntity = this::convertirAEntity;
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

        return recetaCabRepository.findById(recetaId)
                .map(toCompletaDTO);
    }

    @Override
    public Optional<RecetaCompletaDTO> obtenerRecetaPorNumero(String numeroReceta) {
        log.debug("📖 Obteniendo receta número {}", numeroReceta);

        return recetaCabRepository.findByNumeroReceta(numeroReceta)
                .map(toCompletaDTO);
    }

    @Override
    public List<RecetaCompletaDTO> obtenerRecetasPorPaciente(Long pacienteId) {
        log.debug("📖 Obteniendo recetas del paciente {}", pacienteId);

        return recetaCabRepository.findByPacienteId(pacienteId)
                .stream()
                .map(toCompletaDTO)
                .collect(Collectors.toList());
    }

    // ===== 📝 OPERACIONES CRUD FUNCIONALES =====

    @Override
    @Transactional
    public RecetaCompletaDTO crearReceta(RecetaCabDTO recetaDTO) {
        log.info("📝 Creando receta para {} ID {}", recetaDTO.getTipoOrigen(), recetaDTO.getOrigenId());

        // Validaciones (Exception Layer maneja todo)
        validationHelper.validarCreacionReceta(recetaDTO);

        // 1. Preparar y guardar cabecera
        RecetaCabDTO recetaPreparada = prepararRecetaParaCreacion(recetaDTO);
        RecetaCabDTO recetaConFirma = aplicarFirmaAutomatica(recetaPreparada);
        RecetaCab recetaEntity = toEntity.apply(recetaConFirma);
        RecetaCab recetaCabGuardada = establecerDatosCreacion(recetaEntity);
        recetaCabGuardada = recetaCabRepository.save(recetaCabGuardada);

        log.info("✅ Receta cabecera guardada con ID: {}", recetaCabGuardada.getId());

        // 2. Guardar medicamentos si existen
        if (recetaDTO.getMedicamentos() != null && !recetaDTO.getMedicamentos().isEmpty()) {
            guardarMedicamentosDetalle(recetaCabGuardada.getId(), recetaDTO.getMedicamentos());
            log.info("✅ Guardados {} medicamentos para receta {}",
                    recetaDTO.getMedicamentos().size(), recetaCabGuardada.getId());
        }

        // 3. Retornar DTO completo con medicamentos
        return toCompletaDTO.apply(recetaCabGuardada);
    }

    @Override
    @Transactional
    public RecetaCompletaDTO actualizarReceta(Long recetaId, RecetaCabDTO recetaDTO, Long medicoId) {
        log.info("🔄 Actualizando receta {} por médico {}", recetaId, medicoId);

        // Validaciones (Exception Layer maneja todo)
        validationHelper.validarActualizacionReceta(recetaId, medicoId);

        return recetaCabRepository.findById(recetaId)
                .filter(esActiva)
                .filter(puedeModificarse)
                .map(receta -> actualizarCamposReceta(receta, recetaDTO, medicoId))
                .map(recetaCabRepository::save)
                .map(this::actualizarMedicamentos)
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

    // ===== 🔍 BÚSQUEDAS DE VADEMÉCUM FUNCIONALES =====

    @Override
    public List<MedicamentoVademecumDTO> buscarMedicamentos(String busqueda, String categoria) {
        log.debug("🔍 Buscando medicamentos: '{}', categoría: '{}'", busqueda, categoria);

        List<MedicamentoVademecum> medicamentos = Optional.ofNullable(categoria)
                .filter(cat -> !cat.trim().isEmpty())
                .map(vademecumRepository::findByCategoria)
                .orElseGet(() -> vademecumRepository.buscarMedicamentos(busqueda));

        return medicamentos.stream()
                .filter(med -> busqueda == null || med.getGenericName().toLowerCase().contains(busqueda.toLowerCase()))
                .map(toMedicamentoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MedicamentoVademecumDTO> obtenerMedicamento(Long medicamentoId) {
        return vademecumRepository.findById(medicamentoId)
                .map(toMedicamentoDTO);
    }

    @Override
    public Optional<MedicamentoVademecumDTO> obtenerMedicamentoPorCodigo(String codigoMedicamento) {
        return vademecumRepository.findByCodigoMedicamento(codigoMedicamento)
                .map(toMedicamentoDTO);
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

    // ===== 📊 ESTADÍSTICAS FUNCIONALES =====

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

    // ===== 🔒 VALIDACIONES FUNCIONALES =====

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

    // ===== 🛠️ UTILIDADES FUNCIONALES =====

    @Override
    public String generarNumeroReceta() {
        // Generar número más corto que quepa en varchar(20)
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String secuencia = String.format("%03d", new Random().nextInt(1000));
        String numeroReceta = String.format("REC-%s-%s", fecha, secuencia);

        log.debug("🔢 Número de receta generado: {} (longitud: {})", numeroReceta, numeroReceta.length());

        return numeroReceta;
        // Formato resultante: REC-20250604-123 = 17 caracteres ✅
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
        // Lógica de firma automática
        boolean debeAutoFirmar = determinarSiDebeAutoFirmar(recetaDTO);

        if (debeAutoFirmar) {
            recetaDTO.setFirmada("S");
            recetaDTO.setFechaFirma(LocalDateTime.now());
            // TODO: Implementar estructura de firma digital
        }

        return recetaDTO;
    }

    // ===== 💊 MÉTODOS PARA CARGAR MEDICAMENTOS CON TU DTO =====

    private List<RecetaDetDTO> cargarMedicamentosComoDTO(Long recetaId) {
        log.debug("💊 Cargando medicamentos como DTO para receta {}", recetaId);

        List<RecetaDet> medicamentos = recetaDetRepository.findByRecetaIdOrderByOrden(recetaId);

        return medicamentos.stream()
                .map(this::convertirMedicamentoARecetaDetDTO)
                .collect(Collectors.toList());
    }

    private RecetaDetDTO convertirMedicamentoARecetaDetDTO(RecetaDet detalle) {
        log.debug("🔄 Convirtiendo medicamento ID {} de receta {}", detalle.getMedicamentoId(), detalle.getRecetaId());

        // Buscar info del vademécum
        Optional<MedicamentoVademecum> vademecumOpt = vademecumRepository.findById(detalle.getMedicamentoId());

        // Crear DTO base con TODOS los campos
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

        // ✅ AGREGAR INFO DEL VADEMÉCUM (tu DTO ya tiene estos campos!)
        if (vademecumOpt.isPresent()) {
            MedicamentoVademecum vademecum = vademecumOpt.get();
            log.debug("✅ Medicamento encontrado en vademécum: {}", vademecum.getGenericName());

            builder
                    .nombreMedicamento(vademecum.getGenericName())
                    .concentracion(vademecum.getConcentracion())
                    .formaFarmaceutica(vademecum.getFormaFarmaceutica())
                    .categoria(vademecum.getCategoria())
                    .brandNames(vademecum.getBrandNames() != null ? vademecum.getBrandNames().toString() : "N/A");
        } else {
            log.warn("⚠️ Medicamento ID {} no encontrado en vademécum", detalle.getMedicamentoId());
            // Valores por defecto si no se encuentra el medicamento
            builder
                    .nombreMedicamento("Medicamento no encontrado")
                    .concentracion("N/A")
                    .formaFarmaceutica("N/A")
                    .categoria("N/A")
                    .brandNames("N/A");
        }

        return builder.build();
    }

    // ===== 🔨 MÉTODOS PRIVADOS FUNCIONALES =====

    private RecetaCabDTO prepararRecetaParaCreacion(RecetaCabDTO dto) {
        dto.setNumeroReceta(generarNumeroReceta());
        dto.setFechaReceta(LocalDateTime.now());
        dto.setEstado("01"); // Activa

        // ✅ AGREGAR FECHA DE VENCIMIENTO AUTOMÁTICA
        if (dto.getFechaVencimiento() == null) {
            dto.setFechaVencimiento(LocalDate.now().plusDays(30)); // 30 días por defecto
        }

        return dto;
    }
    private RecetaCab establecerDatosCreacion(RecetaCab receta) {
        LocalDateTime ahora = LocalDateTime.now();
        receta.setCreadoEn(ahora);
        receta.setActivo("S");
        return receta;
    }

    private RecetaCab guardarMedicamentos(RecetaCab recetaGuardada) {
        // TODO: Implementar guardado de medicamentos
        // Pipeline funcional para medicamentos
        log.debug("💊 Guardando medicamentos para receta {}", recetaGuardada.getId());
        return recetaGuardada;
    }

    private void guardarMedicamentosDetalle(Long recetaId, List<RecetaDetDTO> medicamentos) {
        log.debug("💊 Guardando {} medicamentos para receta {}", medicamentos.size(), recetaId);

        LocalDateTime ahora = LocalDateTime.now();

        for (int i = 0; i < medicamentos.size(); i++) {
            RecetaDetDTO medicamentoDTO = medicamentos.get(i);

            // Crear entidad RecetaDet
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
                    .ordenItem(i + 1) // Orden secuencial automático
                    .estado("01") // Activo por defecto
                    .activo("S") // Activo
                    .creadoPor(medicamentoDTO.getCreadoPor())
                    .creadoEn(ahora)
                    .build();

            // Guardar medicamento
            RecetaDet medicamentoGuardado = recetaDetRepository.save(medicamentoEntity);
            log.debug("✅ Medicamento guardado: ID {} - {}",
                    medicamentoGuardado.getId(), medicamentoDTO.getCodigoMedicamento());
        }
    } // ✅ AQUÍ ESTABA EL CIERRE FALTANTE

    private RecetaCab actualizarCamposReceta(RecetaCab receta, RecetaCabDTO dto, Long medicoId) {
        LocalDateTime ahora = LocalDateTime.now();

        Optional.ofNullable(dto.getDiagnosticoPrincipal()).ifPresent(receta::setDiagnosticoPrincipal);
        Optional.ofNullable(dto.getIndicacionesGenerales()).ifPresent(receta::setIndicacionesGenerales);
        Optional.ofNullable(dto.getFechaVencimiento()).ifPresent(receta::setFechaVencimiento);

        receta.setActualizadoPor(medicoId);
        receta.setActualizadoEn(ahora);

        return receta;
    }

    private RecetaCab actualizarMedicamentos(RecetaCab receta) {
        // TODO: Implementar actualización de medicamentos
        log.debug("💊 Actualizando medicamentos para receta {}", receta.getId());
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
                "01", List.of("02", "04"), // Activa → Despachada/Anulada
                "02", List.of(),           // Despachada → No cambios
                "04", List.of()            // Anulada → No cambios
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

    private boolean determinarSiDebeAutoFirmar(RecetaCabDTO receta) {
        // Lógica de negocio para auto-firma
        // Ejemplo: Auto-firmar si es de hospitalización y tiene menos de 3 medicamentos
        return receta.esDeHospitalizacion() && receta.getTotalMedicamentos() <= 2;
    }

    private Map<String, Object> convertirEstadisticaMedicamento(Object[] stat) {
        return Map.of(
                "nombre_medicamento", stat[0],
                "concentracion", stat[1],
                "total_prescripciones", stat[2]
        );
    }

    // ===== 🔄 CONVERSORES FUNCIONALES =====

    private RecetaCompletaDTO convertirARecetaCompleta(RecetaCab receta) {
        log.debug("🔄 Convirtiendo receta {} con medicamentos", receta.getId());

        // ✅ CARGAR MEDICAMENTOS COMO DTO
        List<RecetaDetDTO> medicamentos = cargarMedicamentosComoDTO(receta.getId());

        log.debug("💊 Cargados {} medicamentos para receta {}", medicamentos.size(), receta.getId());

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
                        .tipoOrigen(receta.getTipoOrigen())
                        .origenId(receta.getOrigenId())
                        .build())
                .firmaDigital(receta.getFirmaDigital())
                .medicamentos(medicamentos)  // ✅ Ahora es List<RecetaDetDTO>
                .build();
    }

    private RecetaCab convertirAEntity(RecetaCabDTO dto) {
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
                .firmada(dto.getFirmada())
                .fechaFirma(dto.getFechaFirma())
                .firmaDigital(dto.getFirmaDigital())
                .creadoPor(dto.getCreadoPor())
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