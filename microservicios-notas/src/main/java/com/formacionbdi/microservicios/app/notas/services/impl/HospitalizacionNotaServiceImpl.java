package com.formacionbdi.microservicios.app.notas.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formacionbdi.microservicios.app.notas.exception.*;
import com.formacionbdi.microservicios.app.notas.models.dto.HospitalizacionNotaDTO;
import com.formacionbdi.microservicios.app.notas.models.entity.HospitalizacionNota;
import com.formacionbdi.microservicios.app.notas.repository.HospitalizacionNotaRepository;
import com.formacionbdi.microservicios.app.notas.services.HospitalizacionNotaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 🚀 IMPLEMENTACIÓN FUNCIONAL del servicio de notas vitales
 * Aplicando programación funcional, código limpio y mejores prácticas
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HospitalizacionNotaServiceImpl implements HospitalizacionNotaService {

    private final HospitalizacionNotaRepository repository;
    private final ObjectMapper objectMapper;

    // ===== 🔧 FUNCIONES HELPER FUNCIONALES =====

    private final Function<HospitalizacionNota, HospitalizacionNotaDTO> toDTO = this::convertirADTO;
    private final Function<HospitalizacionNotaDTO, HospitalizacionNota> toEntity = this::convertirAEntity;

    private final Predicate<HospitalizacionNota> esBorrador = nota -> "01".equals(nota.getEstado());
    private final Predicate<HospitalizacionNota> esFinalizada = nota -> "02".equals(nota.getEstado());
    private final Predicate<HospitalizacionNota> tieneAudio = nota ->
            Optional.ofNullable(nota.getAudioData())
                    .map(audio -> audio.has("tiene_audio") && audio.get("tiene_audio").asBoolean())
                    .orElse(false);

    private final Predicate<HospitalizacionNota> tieneFirma = nota ->
            Optional.ofNullable(nota.getFirmaDigital())
                    .map(firma -> firma.has("tiene_firma") && firma.get("tiene_firma").asBoolean())
                    .orElse(false);

    // ===== 🔒 VALIDACIONES CRÍTICAS =====

    @Override
    public boolean puedeCrearNota(Long medicoId, Long hospitalizacionId) {
        log.debug("🔍 Validando si médico {} puede crear nota para hospitalización {}",
                medicoId, hospitalizacionId);

        return repository.puedeCrearNota(hospitalizacionId, medicoId);
    }

    @Override
    public void validarNotaPuedeSerModificada(Long notaId, Long medicoId) {
        var nota = buscarNotaPorId(notaId);

        // Composición de validaciones
        Optional.of(nota)
                .filter(esBorrador)
                .filter(n -> Objects.equals(n.getCreadoPor(), medicoId))
                .orElseThrow(() -> esFinalizada.test(nota)
                        ? NotaBusinessException.notaYaFinalizada(notaId)
                        : NotaBusinessException.permisosDenegados(medicoId, notaId));
    }

    @Override
    public void validarPermisosMedico(Long notaId, Long medicoId) {
        buscarNotaPorId(notaId);

        boolean tienePermisos = repository.findById(notaId)
                .map(HospitalizacionNota::getCreadoPor)
                .filter(creadorId -> Objects.equals(creadorId, medicoId))
                .isPresent();

        if (!tienePermisos) {
            throw NotaBusinessException.permisosDenegados(medicoId, notaId);
        }
    }

    // ===== 📖 CONSULTAS PRINCIPALES =====

    @Override
    public List<HospitalizacionNotaDTO> obtenerNotasPorHospitalizacion(Long hospitalizacionId) {
        log.debug("📖 Obteniendo notas para hospitalización {}", hospitalizacionId);

        return repository.findByHospitalizacionIdOrderByFecha(hospitalizacionId)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalizacionNotaDTO> obtenerNotaPorId(Long notaId) {
        return repository.findById(notaId).map(toDTO);
    }

    @Override
    public List<HospitalizacionNotaDTO> obtenerNotasFinalizadas(Long hospitalizacionId) {
        return repository.findNotasFinalizadasPorHospitalizacion(hospitalizacionId)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalizacionNotaDTO> obtenerNotasBorrador(Long hospitalizacionId) {
        return repository.findNotasBorradorPorHospitalizacion(hospitalizacionId)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalizacionNotaDTO> buscarPorNumeroCuenta(String numeroCuenta) {
        return repository.findByNumeroCuenta(numeroCuenta)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    // ===== 📝 OPERACIONES CRUD INTELIGENTES =====

    @Override
    @Transactional
    public HospitalizacionNotaDTO crearNota(HospitalizacionNotaDTO notaDTO) {
        log.info("📝 Creando nueva nota para hospitalización {}", notaDTO.getHospitalizacionId());

        // Validaciones funcionales
        validarCreacionNota(notaDTO);

        // Auto-limpieza de audio anterior (funcional)
        limpiarAudioAnteriorSiExiste(notaDTO);

        // Preparar y guardar
        var notaEntity = prepararNuevaNota(notaDTO);
        var notaGuardada = repository.save(notaEntity);

        log.info("✅ Nota creada exitosamente con ID {}", notaGuardada.getId());
        return toDTO.apply(notaGuardada);
    }

    @Override
    @Transactional
    public HospitalizacionNotaDTO actualizarNota(Long notaId, HospitalizacionNotaDTO notaDTO, Long medicoId) {
        log.info("🔄 Actualizando nota {} por médico {}", notaId, medicoId);

        validarNotaPuedeSerModificada(notaId, medicoId);

        return repository.findById(notaId)
                .map(nota -> actualizarCamposNota(nota, notaDTO))
                .map(repository::save)
                .map(toDTO)
                .orElseThrow(() -> NotaNotFoundException.nota(notaId));
    }

    @Override
    @Transactional
    public HospitalizacionNotaDTO finalizarNota(Long notaId, Long medicoId) {
        log.info("🏁 Finalizando nota {} por médico {}", notaId, medicoId);

        return repository.findById(notaId)
                .filter(esBorrador)
                .filter(nota -> Objects.equals(nota.getCreadoPor(), medicoId))
                .map(this::validarYFinalizar)
                .map(repository::save)
                .map(toDTO)
                .orElseThrow(() -> NotaBusinessException.notaYaFinalizada(notaId));
    }

    @Override
    @Transactional
    public void eliminarNota(Long notaId, Long medicoId) {
        log.info("🗑️ Eliminando nota {} por médico {}", notaId, medicoId);

        validarNotaPuedeSerModificada(notaId, medicoId);
        repository.deleteById(notaId);
    }

    // ===== 🎯 OPERACIONES ESPECÍFICAS =====

    @Override
    public List<HospitalizacionNotaDTO> buscarPorTipo(Long hospitalizacionId, String tipoNota) {
        return repository.findByHospitalizacionYTipo(hospitalizacionId, tipoNota)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalizacionNotaDTO> buscarPorMedicoYFechas(Long medicoId,
                                                               LocalDateTime fechaInicio,
                                                               LocalDateTime fechaFin) {
        return repository.findByMedicoYRangoFechas(medicoId, fechaInicio, fechaFin)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    // ===== 🧹 GESTIÓN DE AUDIO Y LIMPIEZA =====

    @Override
    @Transactional
    public int limpiarAudioAntiguoAutomatico(int diasAntiguedad) {
        log.info("🧹 Iniciando limpieza automática de audio (>{} días)", diasAntiguedad);

        var fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);

        return repository.findNotasParaLimpiezaAudio(fechaLimite)
                .stream()
                .mapToInt(this::limpiarAudioDeNota)
                .sum();
    }

    @Override
    @Transactional
    public boolean eliminarAudioNota(Long notaId, Long medicoId) {
        validarPermisosMedico(notaId, medicoId);

        return repository.findById(notaId)
                .filter(tieneAudio)
                .map(this::marcarAudioComoEliminado)
                .map(repository::save)
                .isPresent();
    }

    @Override
    public boolean tieneAudioDisponible(Long notaId) {
        return repository.findById(notaId)
                .filter(tieneAudio)
                .isPresent();
    }

    // ===== 📊 ESTADÍSTICAS Y REPORTES =====

    @Override
    public Map<String, Object> obtenerEstadisticasHospitalizacion(Long hospitalizacionId) {
        var notas = repository.findByHospitalizacionIdOrderByFecha(hospitalizacionId);

        return Map.of(
                "total_notas", notas.size(),
                "notas_borrador", notas.stream().mapToLong(nota -> esBorrador.test(nota) ? 1 : 0).sum(),
                "notas_finalizadas", notas.stream().mapToLong(nota -> esFinalizada.test(nota) ? 1 : 0).sum(),
                "notas_con_audio", notas.stream().mapToLong(nota -> tieneAudio.test(nota) ? 1 : 0).sum(),
                "notas_con_firma", notas.stream().mapToLong(nota -> tieneFirma.test(nota) ? 1 : 0).sum(),
                "medicos_participantes", notas.stream().map(HospitalizacionNota::getCreadoPor).distinct().count()
        );
    }

    @Override
    public Map<String, Long> contarNotasPorEstado(Long hospitalizacionId) {
        return repository.contarNotasPorEstado(hospitalizacionId)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    public boolean existenNotasParaHospitalizacion(Long hospitalizacionId) {
        return repository.findByHospitalizacionIdOrderByFecha(hospitalizacionId)
                .stream()
                .findAny()
                .isPresent();
    }

    // ===== 📄 GENERACIÓN DE DOCUMENTOS =====

    @Override
    public byte[] generarPdfNota(Long notaId) {
        // TODO: Implementar generación de PDF individual
        throw new NotaProcessingException("PDF_GENERATION", "Funcionalidad en desarrollo");
    }

    @Override
    public byte[] generarPdfConsolidado(Long hospitalizacionId) {
        // TODO: Implementar generación de PDF consolidado
        throw new NotaProcessingException("PDF_GENERATION", "Funcionalidad en desarrollo");
    }

    // ===== 🔧 UTILIDADES =====

    @Override
    public void validarDatosJson(HospitalizacionNotaDTO notaDTO) {
        validarJsonNode("signos_vitales", notaDTO.getSignosVitales());
        validarJsonNode("firma_digital", notaDTO.getFirmaDigital());
        validarJsonNode("audio_data", notaDTO.getAudioData());
    }

    @Override
    public String generarNumeroNota(Long hospitalizacionId) {
        var contador = repository.findByHospitalizacionIdOrderByFecha(hospitalizacionId).size() + 1;
        return String.format("NOTA-%d-%03d", hospitalizacionId, contador);
    }

    // ===== 🔨 MÉTODOS PRIVADOS FUNCIONALES =====

    private HospitalizacionNota buscarNotaPorId(Long notaId) {
        return repository.findById(notaId)
                .orElseThrow(() -> NotaNotFoundException.nota(notaId));
    }

    private void validarCreacionNota(HospitalizacionNotaDTO notaDTO) {
        if (!puedeCrearNota(notaDTO.getCreadoPor(), notaDTO.getHospitalizacionId())) {
            throw NotaBusinessException.notaPuedeCrear(
                    notaDTO.getCreadoPor(),
                    notaDTO.getHospitalizacionId()
            );
        }
        validarDatosJson(notaDTO);
    }

    private void limpiarAudioAnteriorSiExiste(HospitalizacionNotaDTO notaDTO) {
        repository.findNotasBorradorPorHospitalizacion(notaDTO.getHospitalizacionId())
                .stream()
                .filter(nota -> Objects.equals(nota.getCreadoPor(), notaDTO.getCreadoPor()))
                .filter(tieneAudio)
                .forEach(this::limpiarAudioDeNota);
    }

    private HospitalizacionNota prepararNuevaNota(HospitalizacionNotaDTO dto) {
        var nota = toEntity.apply(dto);
        var ahora = LocalDateTime.now();

        nota.setNumeroNota(generarNumeroNota(dto.getHospitalizacionId()));
        nota.setEstado("01"); // Borrador
        nota.setFechaNota(ahora);
        nota.setCreadoEn(ahora);

        return nota;
    }

    private HospitalizacionNota actualizarCamposNota(HospitalizacionNota nota, HospitalizacionNotaDTO dto) {
        var ahora = LocalDateTime.now();

        Optional.ofNullable(dto.getTituloNota()).ifPresent(nota::setTituloNota);
        Optional.ofNullable(dto.getContenidoNota()).ifPresent(nota::setContenidoNota);
        Optional.ofNullable(dto.getTurno()).ifPresent(nota::setTurno);
        Optional.ofNullable(dto.getSignosVitales()).ifPresent(nota::setSignosVitales);
        Optional.ofNullable(dto.getFirmaDigital()).ifPresent(nota::setFirmaDigital);
        Optional.ofNullable(dto.getAudioData()).ifPresent(nota::setAudioData);

        nota.setActualizadoPor(dto.getActualizadoPor());
        nota.setActualizadoEn(ahora);

        return nota;
    }

    private HospitalizacionNota validarYFinalizar(HospitalizacionNota nota) {
        // Validar firma si es requerida (lógica de negocio)
        if (esEvolucion(nota) && !tieneFirma.test(nota)) {
            throw NotaBusinessException.firmaRequerida(nota.getId());
        }

        nota.setEstado("02"); // Finalizada
        nota.setActualizadoEn(LocalDateTime.now());

        return nota;
    }

    private boolean esEvolucion(HospitalizacionNota nota) {
        return "01".equals(nota.getTipoNota());
    }

    private int limpiarAudioDeNota(HospitalizacionNota nota) {
        try {
            marcarAudioComoEliminado(nota);
            repository.save(nota);
            log.debug("🧹 Audio limpiado de nota {}", nota.getId());
            return 1;
        } catch (Exception e) {
            log.warn("⚠️ Error limpiando audio de nota {}: {}", nota.getId(), e.getMessage());
            return 0;
        }
    }

    private HospitalizacionNota marcarAudioComoEliminado(HospitalizacionNota nota) {
        var audioActualizado = objectMapper.createObjectNode();
        audioActualizado.put("tiene_audio", false);
        audioActualizado.put("audio_eliminado", true);
        audioActualizado.put("fecha_eliminacion", LocalDateTime.now().toString());

        // Conservar transcripción si existe
        Optional.ofNullable(nota.getAudioData())
                .filter(audio -> audio.has("transcripcion"))
                .ifPresent(audio -> audioActualizado.set("transcripcion", audio.get("transcripcion")));

        nota.setAudioData(audioActualizado);
        return nota;
    }

    private void validarJsonNode(String campo, JsonNode json) {
        if (json != null) {
            try {
                objectMapper.treeToValue(json, Object.class);
            } catch (Exception e) {
                throw NotaValidationException.jsonInvalido(campo, e.getMessage());
            }
        }
    }

    private HospitalizacionNotaDTO convertirADTO(HospitalizacionNota entity) {
        return HospitalizacionNotaDTO.builder()
                .id(entity.getId())
                .numeroNota(entity.getNumeroNota())
                .hospitalizacionId(entity.getHospitalizacionId())
                .numeroCuenta(entity.getNumeroCuenta())
                .tipoNota(entity.getTipoNota())
                .tituloNota(entity.getTituloNota())
                .contenidoNota(entity.getContenidoNota())
                .turno(entity.getTurno())
                .fechaNota(entity.getFechaNota())
                .estado(entity.getEstado())
                .signosVitales(entity.getSignosVitales())
                .firmaDigital(entity.getFirmaDigital())
                .audioData(entity.getAudioData())
                .creadoPor(entity.getCreadoPor())
                .creadoEn(entity.getCreadoEn())
                .actualizadoPor(entity.getActualizadoPor())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    private HospitalizacionNota convertirAEntity(HospitalizacionNotaDTO dto) {
        var entity = new HospitalizacionNota();
        entity.setId(dto.getId());
        entity.setNumeroNota(dto.getNumeroNota());
        entity.setHospitalizacionId(dto.getHospitalizacionId());
        entity.setNumeroCuenta(dto.getNumeroCuenta());
        entity.setTipoNota(dto.getTipoNota());
        entity.setTituloNota(dto.getTituloNota());
        entity.setContenidoNota(dto.getContenidoNota());
        entity.setTurno(dto.getTurno());
        entity.setFechaNota(dto.getFechaNota());
        entity.setEstado(dto.getEstado());
        entity.setSignosVitales(dto.getSignosVitales());
        entity.setFirmaDigital(dto.getFirmaDigital());
        entity.setAudioData(dto.getAudioData());
        entity.setCreadoPor(dto.getCreadoPor());
        entity.setCreadoEn(dto.getCreadoEn());
        entity.setActualizadoPor(dto.getActualizadoPor());
        entity.setActualizadoEn(dto.getActualizadoEn());
        return entity;
    }
}