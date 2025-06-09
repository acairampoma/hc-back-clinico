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
 * REFACTORIZADA: Incluye métodos para notas médicas
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PacientePorCamaServiceImpl implements PacientePorCamaService {

    private final PacientePorCamaRepository repository;
    private final ObjectMapper objectMapper;

    // ===============================================
    // MÉTODOS BÁSICOS EXISTENTES
    // ===============================================

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
            List<Object[]> resultados = repository.obtenerCamaPorNumero(bedNumber.trim());

            if (resultados.isEmpty()) {
                log.warn("No se encontró la cama con número: {}", bedNumber);
                throw new ResourceNotFoundException("No se encontró la cama con número: " + bedNumber);
            }

            Object[] primerResultado = resultados.get(0);
            PacientePorCamaDTO cama = convertirResultadoADTO(primerResultado);
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
            List<Object[]> resultados = repository.buscarPacientePorDni(dni.trim());

            if (resultados.isEmpty()) {
                log.warn("No se encontró paciente con DNI: {}", dni);
                throw new ResourceNotFoundException("No se encontró paciente con DNI: " + dni);
            }

            Object[] primerResultado = resultados.get(0);
            PacientePorCamaDTO paciente = convertirResultadoADTO(primerResultado);
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

    // ===============================================
    // MÉTODOS NUEVOS PARA NOTAS MÉDICAS
    // ===============================================

    @Override
    public Map<String, Object> obtenerDatosParaNotasMedicas(String bedNumber) {
        if (bedNumber == null || bedNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de cama no puede estar vacío");
        }

        try {
            log.info("Obteniendo datos para notas médicas de cama: {}", bedNumber);
            List<Object[]> resultados = repository.obtenerDatosNotasMedicas(bedNumber.trim());

            if (resultados.isEmpty()) {
                log.warn("No se encontraron datos para notas médicas en cama: {}", bedNumber);
                throw new ResourceNotFoundException("No se encontró paciente en la cama: " + bedNumber);
            }

            Object[] datos = resultados.get(0);
            Map<String, Object> datosNotas = new HashMap<>();

            // Mapear los datos del array a un Map
            datosNotas.put("bed_number", datos[0]);
            datosNotas.put("hospitalizacion_id", datos[1] != null ? Long.valueOf(datos[1].toString()) : null);
            datosNotas.put("numero_cuenta", datos[2]);
            datosNotas.put("paciente_id", datos[3] != null ? Long.valueOf(datos[3].toString()) : null);
            datosNotas.put("medico_tratante_id", datos[4] != null ? Long.valueOf(datos[4].toString()) : null);
            datosNotas.put("especialidad_id", datos[5] != null ? Long.valueOf(datos[5].toString()) : null);
            datosNotas.put("fullname", datos[6]);
            datosNotas.put("primary_diagnosis", datos[7]);

            log.info("Datos para notas médicas obtenidos exitosamente para cama: {}", bedNumber);
            return datosNotas;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener datos para notas médicas de cama {}: {}", bedNumber, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener datos para notas médicas");
        }
    }

    @Override
    public PacientePorCamaDTO buscarPacientePorHospitalizacionId(Long hospitalizacionId) {
        if (hospitalizacionId == null) {
            throw new IllegalArgumentException("El ID de hospitalización no puede ser nulo");
        }

        try {
            log.info("Buscando paciente por hospitalización ID: {}", hospitalizacionId);
            List<Object[]> resultados = repository.buscarPacientePorHospitalizacionId(hospitalizacionId);

            if (resultados.isEmpty()) {
                log.warn("No se encontró paciente con hospitalización ID: {}", hospitalizacionId);
                throw new ResourceNotFoundException("No se encontró paciente con hospitalización ID: " + hospitalizacionId);
            }

            Object[] primerResultado = resultados.get(0);
            PacientePorCamaDTO paciente = convertirResultadoADTO(primerResultado);
            log.info("Paciente con hospitalización ID {} encontrado en cama {}", hospitalizacionId, paciente.getBedNumber());
            return paciente;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al buscar paciente con hospitalización ID {}: {}", hospitalizacionId, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al buscar paciente con hospitalización ID: " + hospitalizacionId);
        }
    }

    @Override
    public PacientePorCamaDTO buscarPacientePorNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de cuenta no puede estar vacío");
        }

        try {
            log.info("Buscando paciente por número de cuenta: {}", numeroCuenta);
            List<Object[]> resultados = repository.buscarPacientePorNumeroCuenta(numeroCuenta.trim());

            if (resultados.isEmpty()) {
                log.warn("No se encontró paciente con número de cuenta: {}", numeroCuenta);
                throw new ResourceNotFoundException("No se encontró paciente con número de cuenta: " + numeroCuenta);
            }

            Object[] primerResultado = resultados.get(0);
            PacientePorCamaDTO paciente = convertirResultadoADTO(primerResultado);
            log.info("Paciente con número de cuenta {} encontrado en cama {}", numeroCuenta, paciente.getBedNumber());
            return paciente;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al buscar paciente con número de cuenta {}: {}", numeroCuenta, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al buscar paciente con número de cuenta: " + numeroCuenta);
        }
    }

    @Override
    public boolean tieneNotasPendientes(String bedNumber) {
        if (bedNumber == null || bedNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de cama no puede estar vacío");
        }

        try {
            log.info("Verificando notas pendientes para cama: {}", bedNumber);
            Optional<Boolean> resultado = repository.tieneNotasPendientes(bedNumber.trim());

            boolean tieneNotas = resultado.orElse(false);
            log.info("Cama {} {} notas médicas pendientes", bedNumber, tieneNotas ? "tiene" : "no tiene");
            return tieneNotas;

        } catch (Exception e) {
            log.error("Error al verificar notas pendientes para cama {}: {}", bedNumber, e.getMessage(), e);
            // En caso de error, devolver false por seguridad
            return false;
        }
    }

    @Override
    public Map<String, Object> obtenerEstadisticasPorEspecialidad() {
        try {
            log.info("Obteniendo estadísticas por especialidad");
            List<Object[]> resultados = repository.obtenerEstadisticasPorEspecialidad();

            Map<String, Object> estadisticas = new HashMap<>();
            List<Map<String, Object>> especialidades = new ArrayList<>();

            for (Object[] resultado : resultados) {
                Map<String, Object> especialidad = new HashMap<>();
                especialidad.put("nombre", resultado[0]);
                especialidad.put("total_pacientes", ((Number) resultado[1]).longValue());
                especialidades.add(especialidad);
            }

            estadisticas.put("especialidades", especialidades);
            estadisticas.put("total_especialidades", especialidades.size());

            log.info("Se obtuvieron estadísticas de {} especialidades", especialidades.size());
            return estadisticas;

        } catch (Exception e) {
            log.error("Error al obtener estadísticas por especialidad: {}", e.getMessage(), e);
            throw new EstructuraHospitalException("Error al obtener estadísticas por especialidad");
        }
    }

    @Override
    public Long contarPacientesPorEspecialidad(String especialidad) {
        if (especialidad == null || especialidad.trim().isEmpty()) {
            throw new IllegalArgumentException("La especialidad no puede estar vacía");
        }

        try {
            log.info("Contando pacientes por especialidad: {}", especialidad);
            Long count = repository.contarPacientesPorEspecialidad(especialidad.trim());
            log.info("Se encontraron {} pacientes en especialidad: {}", count, especialidad);
            return count;

        } catch (Exception e) {
            log.error("Error al contar pacientes por especialidad {}: {}", especialidad, e.getMessage(), e);
            throw new EstructuraHospitalException("Error al contar pacientes por especialidad: " + especialidad);
        }
    }

    // ===============================================
    // MÉTODOS DE ESTADÍSTICAS EXISTENTES
    // ===============================================

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
            List<Object[]> resultado = repository.obtenerCamaPorNumero(bedNumber.trim());
            return !resultado.isEmpty();
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

    // ===============================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // ===============================================

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
            if (resultado == null || resultado.length == 0) {
                throw new IllegalArgumentException("Resultado inválido: array vacío o null");
            }

            log.debug("Procesando resultado con {} elementos", resultado.length);

            String bedNumber = null;
            String patientDataJson = null;

            // Extraer bedNumber (primer elemento)
            if (resultado[0] != null) {
                bedNumber = resultado[0].toString();
            }

            // Extraer patientDataJson (segundo elemento si existe)
            if (resultado.length > 1 && resultado[1] != null) {
                patientDataJson = resultado[1].toString();
            }

            PacientePorCamaDTO dto = new PacientePorCamaDTO();
            dto.setBedNumber(bedNumber);

            // Si hay datos del paciente, convertir JSON a objeto
            if (patientDataJson != null && !patientDataJson.trim().isEmpty() && !"null".equals(patientDataJson)) {
                try {
                    log.debug("Parseando JSON para cama: {}", bedNumber);
                    PacientePorCamaDTO.PatientData patientData = objectMapper.readValue(
                            patientDataJson, PacientePorCamaDTO.PatientData.class);
                    dto.setPatientData(patientData);
                    log.debug("JSON parseado exitosamente para cama: {}", bedNumber);
                } catch (Exception jsonException) {
                    log.warn("Error al parsear JSON de paciente para cama {}: {}", bedNumber, jsonException.getMessage());
                    dto.setPatientData(null);
                }
            } else {
                log.debug("No hay datos de paciente para cama {}", bedNumber);
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