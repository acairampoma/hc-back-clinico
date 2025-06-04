package com.formacionbdi.microservicios.app.listas.services;

import com.formacionbdi.microservicios.app.listas.models.dto.PacientePorCamaDTO;
import java.util.List;
import java.util.Map;

/**
 * Interface del servicio para gestión de pacientes por cama
 */
public interface PacientePorCamaService {

    /**
     * Obtiene todas las camas con información de pacientes
     * @return Lista de todas las camas (ocupadas y disponibles)
     */
    List<PacientePorCamaDTO> obtenerTodasLasCamas();

    /**
     * Obtiene solo las camas ocupadas (con pacientes)
     * @return Lista de camas ocupadas
     */
    List<PacientePorCamaDTO> obtenerCamasOcupadas();

    /**
     * Obtiene solo las camas disponibles (sin pacientes)
     * @return Lista de camas disponibles
     */
    List<PacientePorCamaDTO> obtenerCamasDisponibles();

    /**
     * Obtiene información de una cama específica por número
     * @param bedNumber Número de la cama
     * @return Información de la cama
     * @throws ResourceNotFoundException si la cama no existe
     */
    PacientePorCamaDTO obtenerCamaPorNumero(String bedNumber);

    /**
     * Obtiene camas en un rango específico
     * @param startBed Cama inicial del rango
     * @param endBed Cama final del rango
     * @return Lista de camas en el rango especificado
     */
    List<PacientePorCamaDTO> obtenerCamasPorRango(String startBed, String endBed);

    /**
     * Busca un paciente por su DNI
     * @param dni Documento de identidad del paciente
     * @return Información de la cama donde está el paciente
     * @throws ResourceNotFoundException si no se encuentra el paciente
     */
    PacientePorCamaDTO buscarPacientePorDni(String dni);

    /**
     * Busca pacientes por nombre (búsqueda parcial)
     * @param nombre Nombre a buscar (puede ser parcial)
     * @return Lista de camas con pacientes que coincidan con el nombre
     */
    List<PacientePorCamaDTO> buscarPacientesPorNombre(String nombre);

    /**
     * Busca pacientes por médico tratante
     * @param medico Nombre del médico tratante
     * @return Lista de camas con pacientes atendidos por el médico
     */
    List<PacientePorCamaDTO> buscarPacientesPorMedico(String medico);

    /**
     * Obtiene estadísticas completas de ocupación de camas
     * @return Mapa con estadísticas de ocupación
     */
    Map<String, Object> obtenerEstadisticasOcupacion();

    /**
     * Verifica si una cama específica está ocupada
     * @param bedNumber Número de la cama
     * @return true si está ocupada, false si está disponible
     * @throws ResourceNotFoundException si la cama no existe
     */
    boolean isCamaOcupada(String bedNumber);

    /**
     * Verifica si existe una cama con el número especificado
     * @param bedNumber Número de la cama
     * @return true si existe, false si no existe
     */
    boolean existeCama(String bedNumber);

    /**
     * Obtiene el conteo de camas por estado
     * @return Mapa con conteos (total, ocupadas, disponibles)
     */
    Map<String, Long> obtenerConteosCamas();

    /**
     * Obtiene resumen de ocupación por porcentajes
     * @return Mapa con porcentajes de ocupación
     */
    Map<String, Double> obtenerPorcentajesOcupacion();

    /**
     * Busca camas y pacientes con filtros múltiples
     * @param filtros Mapa de filtros a aplicar
     * @return Lista de camas que cumplan los filtros
     */
    List<PacientePorCamaDTO> buscarConFiltros(Map<String, Object> filtros);
}