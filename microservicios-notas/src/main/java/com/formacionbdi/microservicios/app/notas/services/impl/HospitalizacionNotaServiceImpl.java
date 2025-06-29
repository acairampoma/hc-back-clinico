package com.formacionbdi.microservicios.app.notas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    private final HospitalizacionNotaRepository notaRepository;
    private final ObjectMapper objectMapper;

    // ===== 🔧 FUNCIONES HELPER FUNCIONALES =====

    private final Function<HospitalizacionNota, HospitalizacionNotaDTO> toDTO = this::convertirADTO;
    private final Function<HospitalizacionNotaDTO, HospitalizacionNota> toEntity = this::convertirAEntity;

    private final Predicate<HospitalizacionNota> esBorrador = nota -> "01".equals(nota.getEstado());
    private final Predicate<HospitalizacionNota> esFinalizada = nota -> "02".equals(nota.getEstado());

    private boolean tieneAudio(HospitalizacionNota nota) {
        return Optional.ofNullable(nota.getAudioData())
                .map(audio -> {
                    try {
                        JsonNode node = objectMapper.readTree(audio);
                        return node.has("tiene_audio") && node.get("tiene_audio").asBoolean();
                    } catch (JsonProcessingException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private boolean tieneFirma(HospitalizacionNota nota) {
        return Optional.ofNullable(nota.getFirmaDigital())
                .map(firma -> {
                    try {
                        JsonNode node = objectMapper.readTree(firma);
                        return node.has("tiene_firma") && node.get("tiene_firma").asBoolean();
                    } catch (JsonProcessingException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    // ===== 🔒 VALIDACIONES CRÍTICAS =====

    @Override
    public boolean puedeCrearNota(Long medicoId, Long hospitalizacionId) {
        log.debug("🔍 Validando si médico {} puede crear nota para hospitalización {}",
                medicoId, hospitalizacionId);

        return notaRepository.puedeCrearNota(hospitalizacionId, medicoId);
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

        boolean tienePermisos = notaRepository.findById(notaId)
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

        return notaRepository.findByHospitalizacionIdOrderByFecha(hospitalizacionId)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalizacionNotaDTO> obtenerNotaPorId(Long notaId) {
        return notaRepository.findById(notaId).map(toDTO);
    }

    @Override
    public List<HospitalizacionNotaDTO> obtenerNotasFinalizadas(Long hospitalizacionId) {
        return notaRepository.findNotasFinalizadasPorHospitalizacion(hospitalizacionId)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalizacionNotaDTO> obtenerNotasBorrador(Long hospitalizacionId) {
        return notaRepository.findNotasBorradorPorHospitalizacion(hospitalizacionId)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalizacionNotaDTO> buscarPorNumeroCuenta(String numeroCuenta) {
        return notaRepository.findByNumeroCuenta(numeroCuenta)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    // ===== 📝 OPERACIONES CRUD INTELIGENTES =====

    @Override
    @Transactional
    public HospitalizacionNotaDTO crearNota(HospitalizacionNotaDTO notaDTO) {
        log.info("Creando nota para hospitalización: {}", notaDTO.getHospitalizacionId());
        
        // Validar datos JSON
        validarDatosJson(notaDTO);
        
        // Establecer fecha de creación
        notaDTO.setCreadoEn(LocalDateTime.now());

        // Convertir y guardar
        HospitalizacionNota nota = convertirAEntity(notaDTO);
        nota = notaRepository.save(nota);
        
        return convertirADTO(nota);
    }

    @Override
    @Transactional
    public HospitalizacionNotaDTO actualizarNota(Long notaId, HospitalizacionNotaDTO notaDTO, Long medicoId) {
        log.info("🔄 Actualizando nota {} por médico {}", notaId, medicoId);

        validarNotaPuedeSerModificada(notaId, medicoId);

        return notaRepository.findById(notaId)
                .map(nota -> actualizarCamposNota(nota, notaDTO))
                .map(notaRepository::save)
                .map(toDTO)
                .orElseThrow(() -> NotaNotFoundException.nota(notaId));
    }

    @Override
    @Transactional
    public HospitalizacionNotaDTO finalizarNota(Long notaId, Long medicoId) {
        log.info("🏁 Finalizando nota {} por médico {}", notaId, medicoId);

        return notaRepository.findById(notaId)
                .filter(esBorrador)
                .filter(nota -> Objects.equals(nota.getCreadoPor(), medicoId))
                .map(this::validarYFinalizar)
                .map(notaRepository::save)
                .map(toDTO)
                .orElseThrow(() -> NotaBusinessException.notaYaFinalizada(notaId));
    }

    @Override
    @Transactional
    public void eliminarNota(Long notaId, Long medicoId) {
        log.info("🗑️ Eliminando nota {} por médico {}", notaId, medicoId);

        validarNotaPuedeSerModificada(notaId, medicoId);
        notaRepository.deleteById(notaId);
    }

    // ===== 🎯 OPERACIONES ESPECÍFICAS =====

    @Override
    public List<HospitalizacionNotaDTO> buscarPorTipo(Long hospitalizacionId, String tipoNota) {
        return notaRepository.findByHospitalizacionYTipo(hospitalizacionId, tipoNota)
                .stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalizacionNotaDTO> buscarPorMedicoYFechas(Long medicoId,
                                                               LocalDateTime fechaInicio,
                                                               LocalDateTime fechaFin) {
        return notaRepository.findByMedicoYRangoFechas(medicoId, fechaInicio, fechaFin)
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

        return notaRepository.findNotasParaLimpiezaAudio(fechaLimite)
                .stream()
                .mapToInt(this::limpiarAudioDeNota)
                .sum();
    }

    @Override
    @Transactional
    public boolean eliminarAudioNota(Long notaId, Long medicoId) {
        validarPermisosMedico(notaId, medicoId);

        return notaRepository.findById(notaId)
                .filter(this::tieneAudio)
                .map(this::marcarAudioComoEliminado)
                .map(notaRepository::save)
                .isPresent();
    }

    @Override
    public boolean tieneAudioDisponible(Long notaId) {
        return notaRepository.findById(notaId)
                .filter(this::tieneAudio)
                .isPresent();
    }

    // ===== 📊 ESTADÍSTICAS Y REPORTES =====

    @Override
    public Map<String, Object> obtenerEstadisticasHospitalizacion(Long hospitalizacionId) {
        var notas = notaRepository.findByHospitalizacionIdOrderByFecha(hospitalizacionId);

        return Map.of(
                "total_notas", notas.size(),
                "notas_borrador", notas.stream().mapToLong(nota -> esBorrador.test(nota) ? 1 : 0).sum(),
                "notas_finalizadas", notas.stream().mapToLong(nota -> esFinalizada.test(nota) ? 1 : 0).sum(),
                "notas_con_audio", notas.stream().mapToLong(nota -> tieneAudio(nota) ? 1 : 0).sum(),
                "notas_con_firma", notas.stream().mapToLong(nota -> tieneFirma(nota) ? 1 : 0).sum(),
                "medicos_participantes", notas.stream().map(HospitalizacionNota::getCreadoPor).distinct().count()
        );
    }

    @Override
    public Map<String, Long> contarNotasPorEstado(Long hospitalizacionId) {
        return notaRepository.contarNotasPorEstado(hospitalizacionId)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    public boolean existenNotasParaHospitalizacion(Long hospitalizacionId) {
        return notaRepository.findByHospitalizacionIdOrderByFecha(hospitalizacionId)
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
        var contador = notaRepository.findByHospitalizacionIdOrderByFecha(hospitalizacionId).size() + 1;
        return String.format("NOTA-%d-%03d", hospitalizacionId, contador);
    }

    // ===== 🔨 MÉTODOS PRIVADOS FUNCIONALES =====

    private HospitalizacionNota buscarNotaPorId(Long notaId) {
        return notaRepository.findById(notaId)
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
        notaRepository.findNotasBorradorPorHospitalizacion(notaDTO.getHospitalizacionId())
                .stream()
                .filter(nota -> Objects.equals(nota.getCreadoPor(), notaDTO.getCreadoPor()))
                .filter(this::tieneAudio)
                .forEach(this::limpiarAudioDeNota);
    }

    private HospitalizacionNota prepararNuevaNota(HospitalizacionNotaDTO dto) {
        var ahora = LocalDateTime.now();
        dto.setCreadoEn(ahora);
        if (dto.getEstado() == null) {
            dto.setEstado("01"); // Borrador por defecto
        }
        return toEntity.apply(dto);
    }

    private HospitalizacionNota actualizarCamposNota(HospitalizacionNota nota, HospitalizacionNotaDTO dto) {
        try {
            nota.setTituloNota(dto.getTituloNota());
            nota.setContenidoNota(dto.getContenidoNota());
            nota.setSignosVitales(dto.getSignosVitales() != null ? objectMapper.writeValueAsString(dto.getSignosVitales()) : null);
            nota.setFirmaDigital(dto.getFirmaDigital() != null ? objectMapper.writeValueAsString(dto.getFirmaDigital()) : null);
            nota.setAudioData(dto.getAudioData() != null ? objectMapper.writeValueAsString(dto.getAudioData()) : null);
            nota.setActualizadoPor(dto.getActualizadoPor());
            nota.setActualizadoEn(LocalDateTime.now());
            return nota;
        } catch (JsonProcessingException e) {
            throw new JsonInvalidoException("Error al convertir JSON a String", e);
        }
    }

    private HospitalizacionNota validarYFinalizar(HospitalizacionNota nota) {
        // Validar firma si es requerida (lógica de negocio)
        if (esEvolucion(nota) && !tieneFirma(nota)) {
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
            notaRepository.save(nota);
            log.debug("🧹 Audio limpiado de nota {}", nota.getId());
            return 1;
        } catch (Exception e) {
            log.warn("⚠️ Error limpiando audio de nota {}: {}", nota.getId(), e.getMessage());
            return 0;
        }
    }

    private HospitalizacionNota marcarAudioComoEliminado(HospitalizacionNota nota) {
        try {
            JsonNode audioNode = objectMapper.readTree(nota.getAudioData());
            ((com.fasterxml.jackson.databind.node.ObjectNode) audioNode).put("audio_eliminado", true);
            nota.setAudioData(objectMapper.writeValueAsString(audioNode));
        } catch (JsonProcessingException e) {
            throw new JsonInvalidoException("Error al procesar el JSON del audio");
        }
        return nota;
    }

    private void validarJsonNode(String campo, JsonNode jsonNode) {
        if (jsonNode != null) {
            try {
                // La validación ahora es más directa ya que ya tenemos el JsonNode
                if (!jsonNode.isObject()) {
                    throw new JsonInvalidoException("El campo " + campo + " debe ser un objeto JSON válido");
                }
            } catch (Exception e) {
                throw new JsonInvalidoException("Error al validar el campo " + campo + ": " + e.getMessage());
            }
        }
    }

    private HospitalizacionNotaDTO convertirADTO(HospitalizacionNota entity) {
        try {
            return HospitalizacionNotaDTO.builder()
                    .id(entity.getId())
                    .numeroNota(entity.getNumeroNota())
                    .hospitalizacionId(entity.getHospitalizacionId())
                    .numeroCuenta(entity.getNumeroCuenta())
                    .tipoNota(entity.getTipoNota())
                    .tituloNota(entity.getTituloNota())
                    .contenidoNota(entity.getContenidoNota())
                    .signosVitales(entity.getSignosVitales() != null ? objectMapper.readTree(entity.getSignosVitales()) : null)
                    .firmaDigital(entity.getFirmaDigital() != null ? objectMapper.readTree(entity.getFirmaDigital()) : null)
                    .audioData(entity.getAudioData() != null ? objectMapper.readTree(entity.getAudioData()) : null)
                    .estado(entity.getEstado())
                    .creadoPor(entity.getCreadoPor())
                    .creadoEn(entity.getCreadoEn())
                    .actualizadoPor(entity.getActualizadoPor())
                    .actualizadoEn(entity.getActualizadoEn())
                    .build();
        } catch (JsonProcessingException e) {
            throw new JsonInvalidoException("Error al convertir String a JSON", e);
        }
    }

    private HospitalizacionNota convertirAEntity(HospitalizacionNotaDTO dto) {
        try {
            return HospitalizacionNota.builder()
                    .id(dto.getId())
                    .numeroNota(dto.getNumeroNota())
                    .hospitalizacionId(dto.getHospitalizacionId())
                    .numeroCuenta(dto.getNumeroCuenta())
                    .tipoNota(dto.getTipoNota())
                    .tituloNota(dto.getTituloNota())
                    .contenidoNota(dto.getContenidoNota())
                    .signosVitales(dto.getSignosVitales() != null ? objectMapper.writeValueAsString(dto.getSignosVitales()) : null)
                    .firmaDigital(dto.getFirmaDigital() != null ? objectMapper.writeValueAsString(dto.getFirmaDigital()) : null)
                    .audioData(dto.getAudioData() != null ? objectMapper.writeValueAsString(dto.getAudioData()) : null)
                    .estado(dto.getEstado())
                    .creadoPor(dto.getCreadoPor())
                    .creadoEn(dto.getCreadoEn())
                    .actualizadoPor(dto.getActualizadoPor())
                    .actualizadoEn(dto.getActualizadoEn())
                    .build();
        } catch (JsonProcessingException e) {
            throw new JsonInvalidoException("Error al convertir JSON a String", e);
        }
    }

    /**
     * Verifica si existe una nota en borrador para una hospitalización y médico específicos
     */
    private boolean existeNotaBorradorPorHospitalizacion(Long hospitalizacionId, Long medicoId) {
        return notaRepository.existeNotaBorradorPorHospitalizacion(hospitalizacionId, medicoId);
    }
}