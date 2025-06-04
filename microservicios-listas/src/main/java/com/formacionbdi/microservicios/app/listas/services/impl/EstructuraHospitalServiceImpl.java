package com.formacionbdi.microservicios.app.listas.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formacionbdi.microservicios.app.listas.exception.EstructuraHospitalException;
import com.formacionbdi.microservicios.app.listas.exception.ResourceNotFoundException;
import com.formacionbdi.microservicios.app.listas.models.dto.EstructuraHospitalDTO;
import com.formacionbdi.microservicios.app.listas.repository.EstructuraHospitalRepository;
import com.formacionbdi.microservicios.app.listas.services.EstructuraHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio para gestión de estructura hospitalaria
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EstructuraHospitalServiceImpl implements EstructuraHospitalService {

    private final EstructuraHospitalRepository estructuraRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<EstructuraHospitalDTO> obtenerTodasLasEstructuras() {
        log.info("Obteniendo todas las estructuras hospitalarias");

        try {
            List<String> estructurasJson = estructuraRepository.obtenerEstructuraCompleta();

            if (estructurasJson.isEmpty()) {
                throw new ResourceNotFoundException("No se encontraron estructuras hospitalarias");
            }

            return estructurasJson.stream()
                    .map(this::convertirJsonADto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error al obtener todas las estructuras", e);
            throw new EstructuraHospitalException("Error al obtener estructuras hospitalarias", e);
        }
    }

    @Override
    public EstructuraHospitalDTO obtenerPrimeraEstructura() {
        log.info("Obteniendo primera estructura hospitalaria");

        try {
            String estructuraJson = estructuraRepository.obtenerPrimeraEstructura()
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró estructura hospitalaria"));

            return convertirJsonADto(estructuraJson);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener primera estructura", e);
            throw new EstructuraHospitalException("Error al obtener estructura hospitalaria", e);
        }
    }

    @Override
    public EstructuraHospitalDTO obtenerEstructuraPorHospitalId(Long hospitalId) {
        log.info("Obteniendo estructura para hospital ID: {}", hospitalId);

        if (hospitalId == null || hospitalId <= 0) {
            throw new EstructuraHospitalException("ID de hospital inválido");
        }

        try {
            String estructuraJson = estructuraRepository.obtenerEstructuraPorHospitalId(hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró estructura para el hospital ID: " + hospitalId));

            return convertirJsonADto(estructuraJson);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener estructura para hospital ID: {}", hospitalId, e);
            throw new EstructuraHospitalException("Error al obtener estructura del hospital", e);
        }
    }

    @Override
    public EstructuraHospitalDTO obtenerEstructuraBasica() {
        log.info("Obteniendo estructura básica hospitalaria");

        try {
            String estructuraJson = estructuraRepository.obtenerEstructuraBasica()
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró estructura básica"));

            return convertirJsonADto(estructuraJson);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener estructura básica", e);
            throw new EstructuraHospitalException("Error al obtener estructura básica", e);
        }
    }

    @Override
    public boolean existeEstructuraPorHospitalId(Long hospitalId) {
        log.info("Verificando existencia de estructura para hospital ID: {}", hospitalId);

        if (hospitalId == null || hospitalId <= 0) {
            return false;
        }

        try {
            return estructuraRepository.existeEstructuraPorHospitalId(hospitalId);
        } catch (Exception e) {
            log.error("Error al verificar existencia para hospital ID: {}", hospitalId, e);
            return false;
        }
    }

    @Override
    public Object obtenerEstadisticasDisponibilidad() {
        log.info("Obteniendo estadísticas de disponibilidad");

        try {
            EstructuraHospitalDTO estructura = obtenerPrimeraEstructura();

            Map<String, Object> estadisticas = new HashMap<>();
            int totalCamas = 0;
            int camasDisponibles = 0;
            int camasOcupadas = 0;
            int camasMantenimiento = 0;
            int camasLimpieza = 0;

            // Contar camas por estado
            for (EstructuraHospitalDTO.PisoDTO piso : estructura.getFloors()) {
                if (piso.getWings() != null) {
                    totalCamas += contarCamasPorEstado(piso.getWings().getEast(), estadisticas);
                    totalCamas += contarCamasPorEstado(piso.getWings().getWest(), estadisticas);
                }
            }

            // Calcular totales (esto es un ejemplo, ajusta según tu lógica)
            estadisticas.put("totalCamas", totalCamas);
            estadisticas.put("camasDisponibles", camasDisponibles);
            estadisticas.put("camasOcupadas", camasOcupadas);
            estadisticas.put("camasMantenimiento", camasMantenimiento);
            estadisticas.put("camasLimpieza", camasLimpieza);
            estadisticas.put("porcentajeOcupacion", totalCamas > 0 ? (camasOcupadas * 100.0 / totalCamas) : 0);

            return estadisticas;

        } catch (Exception e) {
            log.error("Error al obtener estadísticas", e);
            throw new EstructuraHospitalException("Error al calcular estadísticas", e);
        }
    }

    /**
     * Convierte JSON string a DTO
     */
    private EstructuraHospitalDTO convertirJsonADto(String json) {
        try {
            return objectMapper.readValue(json, EstructuraHospitalDTO.class);
        } catch (Exception e) {
            log.error("Error al convertir JSON a DTO: {}", json, e);
            throw new EstructuraHospitalException("Error al procesar datos del hospital", e);
        }
    }

    /**
     * Cuenta camas por estado en un ala específica
     */
    private int contarCamasPorEstado(EstructuraHospitalDTO.AlaDTO ala, Map<String, Object> estadisticas) {
        if (ala == null || ala.getBeds() == null) {
            return 0;
        }
        return ala.getBeds().size();
    }
}