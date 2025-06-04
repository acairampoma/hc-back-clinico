package com.formacionbdi.microservicios.app.listas.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formacionbdi.microservicios.app.listas.exception.EstructuraHospitalException;
import com.formacionbdi.microservicios.app.listas.exception.ResourceNotFoundException;
import com.formacionbdi.microservicios.app.listas.models.dto.PacientePorCamaDTO;
import com.formacionbdi.microservicios.app.listas.repository.PacientePorCamaRepository;
import com.formacionbdi.microservicios.app.listas.services.PacientePorCamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio para gestión de pacientes por cama
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PacientePorCamaServiceImpl implements PacientePorCamaService {

    private final PacientePorCamaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public List<PacientePorCamaDTO> obtenerTodasLasCamas() {
        try {
            log.info("Obteniendo todas las camas con información de pacientes");
            List<Object[]> resultados = repository.obtenerTodasLasCamas();

            if (resultados.isEmpty()) {
                log.warn("No se encontraron camas en el sistema");
                return new ArrayList<>();
            }

            List<PacientePorCamaDTO> camas = convertirResultadosADTO(resultados);
            log.info("Se obtuvieron {} camas exitosamente", camas.size());
            return camas;

        } catch (Exception e) {
            log.error("Error al obtener todas las camas: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener la información de las camas");
        }
    }

    @Override
    public List<PacientePorCamaDTO> obtenerCamasOcupadas() {
        try {
            log.info("Obteniendo camas ocupadas");
            List<Object[]> resultados = repository.obtenerCamasOcupadas();

            List<PacientePorCamaDTO> camasOcupadas = convertirResultadosADTO(resultados);
            log.info("Se encontraron {} camas ocupadas", camasOcupadas.size());
            return camasOcupadas;

        } catch (Exception e) {
            log.error("Error al obtener camas ocupadas: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener las camas ocupadas");
        }
    }

    @Override
    public List<PacientePorCamaDTO> obtenerCamasDisponibles() {
        try {
            log.info("Obteniendo camas disponibles");
            List<Object[]> resultados = repository.obtenerCamasDisponibles();

            List<PacientePorCamaDTO> camasDisponibles = convertirResultadosADTO(resultados);
            log.info("Se encontraron {} camas disponibles", camasDisponibles.size());
            return camasDisponibles;

        } catch (Exception e) {
            log.error("Error al obtener camas disponibles: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener las camas disponibles");
        }
    }

    @Override
    public PacientePorCamaDTO obtenerCamaPorNumero(String bedNumber) {
        if (bedNumber == null || bedNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de cama no puede estar vacío");
        }

        try {
            log.info("Buscando cama por número: {}", bedNumber);
            Optional<Object[]> resultado = repository.obtenerCamaPorNumero(bedNumber.trim());

            if (resultado.isEmpty()) {
                log.warn("No se encontró la cama con número: {}", bedNumber);
                throw new ResourceNotFoundException("No se encontró la cama con número: " + bedNumber);
            }

            PacientePorCamaDTO cama = convertirResultadoADTO(resultado.get());
            log.info("Cama {} encontrada exitosamente", bedNumber);
            return cama;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al buscar cama {}: {}", bedNumber, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al buscar la cama: " + bedNumber);
        }
    }

    @Override
    public List<PacientePorCamaDTO> obtenerCamasPorRango(String startBed, String endBed) {
        if (startBed == null || endBed == null || startBed.trim().isEmpty() || endBed.trim().isEmpty()) {
            throw new IllegalArgumentException("Los números de cama del rango no pueden estar vacíos");
        }

        try {
            log.info("Obteniendo camas en rango: {} - {}", startBed, endBed);
            List<Object[]> resultados = repository.obtenerCamasPorRango(startBed.trim(), endBed.trim());

            List<PacientePorCamaDTO> camas = convertirResultadosADTO(resultados);
            log.info("Se encontraron {} camas en el rango {} - {}", camas.size(), startBed, endBed);
            return camas;

        } catch (Exception e) {
            log.error("Error al obtener camas en rango {} - {}: {}", startBed, endBed, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener camas en el rango especificado");
        }
    }

    @Override
    public PacientePorCamaDTO buscarPacientePorDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }

        try {
            log.info("Buscando paciente por DNI: {}", dni);
            Optional<Object[]> resultado = repository.buscarPacientePorDni(dni.trim());

            if (resultado.isEmpty()) {
                log.warn("No se encontró paciente con DNI: {}", dni);
                throw new ResourceNotFoundException("No se encontró paciente con DNI: " + dni);
            }

            PacientePorCamaDTO paciente = convertirResultadoADTO(resultado.get());
            log.info("Paciente con DNI {} encontrado en cama {}", dni, paciente.getBedNumber());
            return paciente;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al buscar paciente con DNI {}: {}", dni, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al buscar paciente con DNI: " + dni);
        }
    }

    @Override
    public List<PacientePorCamaDTO> buscarPacientesPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }

        try {
            log.info("Buscando pacientes por nombre: {}", nombre);
            List<Object[]> resultados = repository.buscarPacientesPorNombre(nombre.trim());

            List<PacientePorCamaDTO> pacientes = convertirResultadosADTO(resultados);
            log.info("Se encontraron {} pacientes con nombre similar a: {}", pacientes.size(), nombre);
            return pacientes;

        } catch (Exception e) {
            log.error("Error al buscar pacientes por nombre {}: {}", nombre, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al buscar pacientes por nombre: " + nombre);
        }
    }

    @Override
    public List<PacientePorCamaDTO> buscarPacientesPorMedico(String medico) {
        if (medico == null || medico.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del médico no puede estar vacío");
        }

        try {
            log.info("Buscando pacientes por médico: {}", medico);
            List<Object[]> resultados = repository.buscarPacientesPorMedico(medico.trim());

            List<PacientePorCamaDTO> pacientes = convertirResultadosADTO(resultados);
            log.info("Se encontraron {} pacientes atendidos por: {}", pacientes.size(), medico);
            return pacientes;

        } catch (Exception e) {
            log.error("Error al buscar pacientes por médico {}: {}", medico, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al buscar pacientes por médico: " + medico);
        }
    }

    @Override
    public Map<String, Object> obtenerEstadisticasOcupacion() {
        try {
            log.info("Obteniendo estadísticas de ocupación");
            Object[] resultado = repository.obtenerEstadisticasOcupacion();

            if (resultado == null || resultado.length < 4) {
                log.warn("No se pudieron obtener estadísticas de ocupación");
                return crearEstadisticasVacias();
            }

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("total_camas", ((Number) resultado[0]).longValue());
            estadisticas.put("camas_ocupadas", ((Number) resultado[1]).longValue());
            estadisticas.put("camas_disponibles", ((Number) resultado[2]).longValue());
            estadisticas.put("porcentaje_ocupacion", ((Number) resultado[3]).doubleValue());

            log.info("Estadísticas obtenidas: {} camas totales, {} ocupadas ({}%)",
                    estadisticas.get("total_camas"),
                    estadisticas.get("camas_ocupadas"),
                    estadisticas.get("porcentaje_ocupacion"));

            return estadisticas;

        } catch (Exception e) {
            log.error("Error al obtener estadísticas de ocupación: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener estadísticas de ocupación");
        }
    }

    @Override
    public boolean isCamaOcupada(String bedNumber) {
        if (bedNumber == null || bedNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de cama no puede estar vacío");
        }

        try {
            log.debug("Verificando si cama {} está ocupada", bedNumber);
            Optional<Boolean> resultado = repository.isCamaOcupada(bedNumber.trim());

            if (resultado.isEmpty()) {
                throw new ResourceNotFoundException("No se encontró la cama con número: " + bedNumber);
            }

            boolean ocupada = resultado.get();
            log.debug("Cama {} está {}", bedNumber, ocupada ? "ocupada" : "disponible");
            return ocupada;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al verificar estado de cama {}: {}", bedNumber, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al verificar estado de la cama: " + bedNumber);
        }
    }

    @Override
    public boolean existeCama(String bedNumber) {
        if (bedNumber == null || bedNumber.trim().isEmpty()) {
            return false;
        }

        try {
            Optional<Object[]> resultado = repository.obtenerCamaPorNumero(bedNumber.trim());
            return resultado.isPresent();
        } catch (Exception e) {
            log.error("Error al verificar existencia de cama {}: {}", bedNumber, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Long> obtenerConteosCamas() {
        try {
            log.info("Obteniendo conteos de camas");
            Map<String, Long> conteos = new HashMap<>();
            conteos.put("total", repository.contarTotalCamas());
            conteos.put("ocupadas", repository.contarCamasOcupadas());
            conteos.put("disponibles", repository.contarCamasDisponibles());

            log.info("Conteos obtenidos: {} total, {} ocupadas, {} disponibles",
                    conteos.get("total"), conteos.get("ocupadas"), conteos.get("disponibles"));

            return conteos;

        } catch (Exception e) {
            log.error("Error al obtener conteos de camas: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener conteos de camas");
        }
    }

    @Override
    public Map<String, Double> obtenerPorcentajesOcupacion() {
        try {
            Map<String, Long> conteos = obtenerConteosCamas();
            Map<String, Double> porcentajes = new HashMap<>();

            Long total = conteos.get("total");
            if (total > 0) {
                porcentajes.put("ocupacion", (conteos.get("ocupadas") * 100.0) / total);
                porcentajes.put("disponibilidad", (conteos.get("disponibles") * 100.0) / total);
            } else {
                porcentajes.put("ocupacion", 0.0);
                porcentajes.put("disponibilidad", 0.0);
            }

            return porcentajes;

        } catch (Exception e) {
            log.error("Error al calcular porcentajes: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al calcular porcentajes de ocupación");
        }
    }

    @Override
    public List<PacientePorCamaDTO> buscarConFiltros(Map<String, Object> filtros) {
        if (filtros == null || filtros.isEmpty()) {
            return obtenerTodasLasCamas();
        }

        try {
            log.info("Buscando con filtros: {}", filtros);

            // Por ahora implementamos búsqueda básica
            // Se puede extender para filtros más complejos
            String estado = (String) filtros.get("estado");

            if ("ocupada".equalsIgnoreCase(estado)) {
                return obtenerCamasOcupadas();
            } else if ("disponible".equalsIgnoreCase(estado)) {
                return obtenerCamasDisponibles();
            }

            return obtenerTodasLasCamas();

        } catch (Exception e) {
            log.error("Error al buscar con filtros: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al aplicar filtros de búsqueda");
        }
    }

    /**
     * Convierte una lista de resultados Object[] a DTOs
     */
    private List<PacientePorCamaDTO> convertirResultadosADTO(List<Object[]> resultados) {
        return resultados.stream()
                .map(this::convertirResultadoADTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte un resultado Object[] a DTO
     */
    private PacientePorCamaDTO convertirResultadoADTO(Object[] resultado) {
        try {
            String bedNumber = (String) resultado[0];
            String patientDataJson = (String) resultado[1];

            PacientePorCamaDTO dto = new PacientePorCamaDTO();
            dto.setBedNumber(bedNumber);

            // Si hay datos del paciente, convertir JSON a objeto
            if (patientDataJson != null && !patientDataJson.trim().isEmpty()) {
                PacientePorCamaDTO.PatientData patientData = objectMapper.readValue(
                        patientDataJson, PacientePorCamaDTO.PatientData.class);
                dto.setPatientData(patientData);
            }

            return dto;

        } catch (Exception e) {
            log.error("Error al convertir resultado a DTO: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al procesar datos de cama");
        }
    }

    /**
     * Crea estadísticas vacías en caso de error
     */
    private Map<String, Object> crearEstadisticasVacias() {
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("total_camas", 0L);
        estadisticas.put("camas_ocupadas", 0L);
        estadisticas.put("camas_disponibles", 0L);
        estadisticas.put("porcentaje_ocupacion", 0.0);
        return estadisticas;
    }
}