package com.formacionbdi.microservicios.app.catalogos.services;

import com.formacionbdi.microservicios.app.catalogos.models.dto.CatalogosDTO;
import java.util.List;
import java.util.Optional;

/**
 * 🔍 Interface del servicio para gestión de catálogos
 * Puerto: 8009 - Microservicio Catálogos
 */
public interface CatalogosService {

    // ===== BÚSQUEDAS GENERALES =====
    List<CatalogosDTO> buscarPorTermino(String termino);
    Optional<CatalogosDTO> buscarPorCodigoExacto(String codigo);

    // ===== BÚSQUEDAS POR TIPO =====
    List<CatalogosDTO> buscarExamenes(String termino);
    List<CatalogosDTO> buscarMedicamentos(String termino);
    List<CatalogosDTO> buscarCatalogos(String termino);

    // ===== BÚSQUEDAS POR CATEGORÍA =====
    List<CatalogosDTO> buscarPorCategoria(String categoria);
    List<CatalogosDTO> buscarEnTablaEspecifica(String tablaCodigo);

    // ===== LISTADOS COMPLETOS =====
    List<CatalogosDTO> obtenerTodosLosExamenes();
    List<CatalogosDTO> obtenerTodosLosMedicamentos();
    List<String> obtenerTodasLasCategorias();
    List<String> obtenerTiposDeTabla();
    List<CatalogosDTO> buscarEnTablaEspecifica(String tablaOrigen, String termino);
}