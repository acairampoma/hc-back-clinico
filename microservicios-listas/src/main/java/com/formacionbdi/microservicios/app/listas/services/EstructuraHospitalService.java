package com.formacionbdi.microservicios.app.listas.services;

import com.formacionbdi.microservicios.app.listas.models.dto.EstructuraHospitalDTO;
import java.util.List;

/**
 * Interface del servicio para gestión de estructura hospitalaria
 */
public interface EstructuraHospitalService {

    /**
     * Obtiene la estructura completa de todos los hospitales
     * @return Lista de estructuras hospitalarias
     */
    List<EstructuraHospitalDTO> obtenerTodasLasEstructuras();

    /**
     * Obtiene la estructura del primer hospital disponible
     * @return Estructura hospitalaria
     */
    EstructuraHospitalDTO obtenerPrimeraEstructura();

    /**
     * Obtiene la estructura de un hospital específico por ID
     * @param hospitalId ID del hospital
     * @return Estructura hospitalaria
     */
    EstructuraHospitalDTO obtenerEstructuraPorHospitalId(Long hospitalId);

    /**
     * Obtiene solo la información básica (sin detalle de camas)
     * @return Estructura básica del hospital
     */
    EstructuraHospitalDTO obtenerEstructuraBasica();

    /**
     * Verifica si existe estructura para un hospital específico
     * @param hospitalId ID del hospital
     * @return true si existe, false si no
     */
    boolean existeEstructuraPorHospitalId(Long hospitalId);

    /**
     * Obtiene estadísticas generales de disponibilidad
     * @return Objeto con estadísticas de camas
     */
    Object obtenerEstadisticasDisponibilidad();
}